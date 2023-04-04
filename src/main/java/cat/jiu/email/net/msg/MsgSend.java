package cat.jiu.email.net.msg;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.Level;

import com.mojang.authlib.GameProfile;

import cat.jiu.email.element.Cooling;
import cat.jiu.email.element.Email;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.element.InboxText;
import cat.jiu.email.event.EmailSendEvent;
import cat.jiu.email.event.EmailSendEvent.EmailSenderGroup;
import cat.jiu.email.ui.container.ContainerEmailSend;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.SizeReport;
import cat.jiu.email.util.EmailUtils;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TextFormatting;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgSend implements IMessage {
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	protected String addresser;
	protected EmailSenderGroup group;
	protected Email email;
	
	public MsgSend() {}
	public MsgSend(EmailSenderGroup group, String addressee, Email email) {
		this.group = group;
		this.addresser = addressee;
		this.email = email;
		this.email.setCreateTimeToNow();
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		try {
			NBTTagCompound nbt = new PacketBuffer(buf).readCompoundTag();
			
			this.addresser = nbt.getString("addresser");
			this.group = EmailSenderGroup.getGroupByID(nbt.getInteger("group"));
			this.email = new Email(nbt.getCompoundTag("email"));
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		NBTTagCompound nbt = new NBTTagCompound();
		
		nbt.setString("addresser", this.addresser);
		nbt.setInteger("group", EmailSenderGroup.getIDByGroup(this.group));
		nbt.setTag("email", this.email.writeTo(NBTTagCompound.class));
		
		new PacketBuffer(buf).writeCompoundTag(nbt);
	}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isServer()) {
			EmailUtils.initNameAndUUID(EmailMain.server);
			EntityPlayerMP sender = ctx.getServerHandler().player;
			
			if(this.group.isPlayerSend() && this.addresser.equals(sender.getName()) && !EmailConfigs.Send.Enable_Send_To_Self) {
				this.sendMessage(sender, Level.ERROR, "info.email.error.send_self");
				return null;
			}
			
			if(!EmailUtils.hasName(this.addresser) && !("@a".equals(this.addresser) || "@p".equals(this.addresser))) {
				this.sendMessage(sender, Level.ERROR, "info.email.error.not_found_player", this.addresser);
				return null;
			}
			
			if("@a".equals(this.addresser) || "@p".equals(this.addresser)) {
				if(sender!=null) {
					
					boolean isOP = false;
					if(sender.getServer().getPlayerList().getOppedPlayers().getEntry(sender.getGameProfile()) != null) {
						isOP = true;
					}else if(!sender.getServer().isDedicatedServer()) {
						if(((IntegratedServer) sender.getServer()).getPublic() && Minecraft.getMinecraft().player.getPermissionLevel() >= 4) {
							isOP = true;
						}
					}
					
					boolean isOP_f = isOP;
					new Thread(()->{
						if(isOP_f) {
							long time = System.currentTimeMillis();
							int sended = 0;
							if(this.group.isPlayerSend()) {
								((ContainerEmailSend) sender.openContainer).setLock(true);
							}
							
							if("@a".equals(this.addresser)) {
								for(UUID uid : EmailUtils.getAllUUID()) {
									this.sendEmail(sender, uid.toString(), false);
									sended++;
								}
							}else if("@p".equals(this.addresser)) {
								for(GameProfile profile : sender.getServer().getOnlinePlayerProfiles()) {
									this.sendEmail(sender, profile.getId().toString(), false);
									sended++;
								}
							}
							
							if(this.group.isPlayerSend()) {
								((ContainerEmailSend) sender.openContainer).setLock(false);
							}
							
							this.sendMessage(sender, Level.INFO, "info.email.send.abs", sended, System.currentTimeMillis() - time);
						}else {
							this.sendMessage(sender, Level.WARN, "info.email.send.not_op", this.addresser);
						}
					}).start();
				}
			}else {
				this.sendEmail(sender, this.addresser, true);
			}
		}
		return null;
	}
	
	private void sendEmail(EntityPlayerMP sender, String addresser, boolean lock) {
		Inbox inbox = Inbox.get(addresser);
		ContainerEmailSend container = null;
		List<ItemStack> stacks = null;
		if(this.group.isPlayerSend()) {
			container = (ContainerEmailSend) sender.openContainer;
			if(lock) container.setLock(true);
			stacks = container.toItemList(false);
			this.email.addItems(stacks);
		}
		
		if(!EmailConfigs.isInfiniteSize()
		&& !this.checkEmailSize(inbox, sender, stacks, lock)) {
			if(this.group.isPlayerSend()) {
				if(lock) container.setLock(false);
			}
			return;
		}
		
		if(MinecraftForge.EVENT_BUS.post(new EmailSendEvent(Phase.START, this.group, addresser, this.email))) return;
		
		if(!inbox.isInSenderBlacklist(sender.getName())) {
			this.sendMessage(addresser, inbox.addEmail(this.email, true), sender, lock);
		}else {
			if(this.group.isPlayerSend()) {
				container.putStack(this.email.getItems());
				if(lock) container.setLock(false);
			}
			this.sendIsBlackMessage(sender, lock);
		}
		
		MinecraftForge.EVENT_BUS.post(new EmailSendEvent(Phase.END, this.group, addresser, this.email));
	}
	
	private boolean checkEmailSize(Inbox inbox, EntityPlayerMP msgSender, List<ItemStack> stacks, boolean lock) {
		SizeReport report = EmailUtils.checkEmailSize(this.email);
		if(!SizeReport.SUCCES.equals(report)) {
			if(msgSender!=null) {
				if(this.group.isPlayerSend()) {
					((ContainerEmailSend) msgSender.openContainer).putStack(stacks);
					if(lock) ((ContainerEmailSend) msgSender.openContainer).setLock(false);
					EmailMain.net.sendMessageToPlayer(new SendRenderText(new InboxText("info.email.error.send.to_big", report.slot, report.size)), msgSender);
				}else {
					EmailUtils.sendMessage(msgSender, TextFormatting.RED, "info.email.error.send.to_big", report.slot, report.size);
				}
			}else {
				EmailMain.log.warn("Email item is to big, please remove some item or nbt. Slot: {}, Size: {} / 2097152 Bytes", report.slot, report.size);
			}
			return false;
		}
		
		long size = inbox.getInboxSize() + EmailUtils.getSize(this.email.writeTo(NBTTagCompound.class));
		if(size >= 2097152L) {
			if(msgSender!=null) {
				if(this.group.isPlayerSend()) {
					((ContainerEmailSend) msgSender.openContainer).putStack(stacks);
					if(lock) ((ContainerEmailSend) msgSender.openContainer).setLock(false);
					EmailMain.net.sendMessageToPlayer(new SendRenderText(new InboxText("info.email.error.send.to_big.email", size)), msgSender);
				}else {
					EmailUtils.sendMessage(msgSender, TextFormatting.RED, "info.email.error.send.to_big.email", size);
				}
			}else {
				EmailMain.log.warn("Send email to big, Please reduce the e-mail size. Size: {} / 2097152 Bytes", size);
			}
			return false;
		}
		return true;
	}
	
	private void sendMessage(String addresser, boolean success, EntityPlayerMP msgSender, boolean lock) {
		if(EmailMain.SYSTEM.equals(this.email.getSender().getText())
		|| this.group.isSystemSend()) return;
		try {
			addresser = EmailUtils.getName(UUID.fromString(addresser));
		}catch(Exception e) {}
		
		if(success) {
			if(!this.group.isSystemSend() && msgSender!=null) {
				if(this.group.isPlayerSend()) {
					if(lock) ((ContainerEmailSend) msgSender.openContainer).setLock(false);
					Cooling.cooling(msgSender.getName());
					EmailMain.net.sendMessageToPlayer(new SendRenderText(Color.GREEN, new InboxText("info.email.send.success", addresser)), msgSender);
				}else {
					EmailUtils.sendMessage(msgSender, TextFormatting.GREEN, "info.email.send.success", addresser);
				}
				EntityPlayer player = getOnlinePlayer(addresser, msgSender.getServer());
				if(player != null) {
					EmailUtils.sendMessage(player, "info.email.from", this.email.getSender());
				}
			}
			sendLog(this.email.getSender().getText(), addresser, EmailUtils.getUUID(addresser));
		}else {
			if(!this.group.isSystemSend() && msgSender!=null) {
				if(this.group.isPlayerSend()) {
					if(lock) ((ContainerEmailSend) msgSender.openContainer).setLock(false);
					EmailMain.net.sendMessageToPlayer(new SendRenderText(Color.RED, new InboxText("info.email.send.fail")), msgSender);
				}else {
					EmailUtils.sendMessage(msgSender, TextFormatting.RED, "info.email.send.fail");
				}
			}else {
				EmailMain.log.info("Send e-mail fail, check log find the reason.");
			}
		}
	}
	
	private void sendIsBlackMessage(EntityPlayerMP msgSender, boolean lock) {
		if(msgSender!=null) {
			if(this.group.isPlayerSend()) {
				if(lock) ((ContainerEmailSend) msgSender.openContainer).setLock(false);
				EmailMain.net.sendMessageToPlayer(new SendRenderText(Color.RED, new InboxText("info.email.send.fail.blacklist")), msgSender);
			}else {
				EmailUtils.sendMessage(msgSender, TextFormatting.RED, "info.email.send.fail.blacklist");
			}
		}else {
			EmailMain.log.info("Send email fail, you have been block by the Addressee!");
		}
	}
	
	/**
	 * @param s name or uuid
	 */
	public static EntityPlayer getOnlinePlayer(String s, MinecraftServer server) {
		EntityPlayer player = null;
		if(server!=null && s!=null & !s.isEmpty()) {
			PlayerList playerList = server.getPlayerList();
			player = playerList.getPlayerByUsername(s);
			if(player==null) {
				try {
					player = playerList.getPlayerByUUID(UUID.fromString(s));
				}catch(Exception e) {}
			}
		}
		return player;
	}
	
	private void sendMessage(EntityPlayerMP sender, Level level, String msg, Object... arg) {
		if(this.group.isPlayerSend()) {
			Color color;
			if(level == Level.INFO) {
				color = Color.GREEN;
			}else if(level == Level.WARN) {
				color = Color.YELLOW;
			}else {
				color = Color.RED;
			}
			EmailMain.net.sendMessageToPlayer(new SendRenderText(color, new InboxText(msg, arg)), sender);
		}else {
			EmailMain.log.log(level, msg.replace("%s", "{}"), arg);
		}
	}
	
	private void sendLog(String sender, String name, UUID uid) {
		try {
			uid = UUID.fromString(name);
			name = EmailUtils.getName(uid);
		}catch(Exception e) {}
		
		File filepath = new File("./logs/email.log");

		if(!filepath.exists()) {
			try {
				filepath.createNewFile();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		StringBuilder msg = new StringBuilder()
				.append(sender)
				.append(" send a email to Player, name: ")
		 		.append(name)
		 		.append(", uuid: ")
		 		.append(uid==null ? addresser : uid.toString());
		
		StringBuilder s = new StringBuilder()
			.append("[")
			.append(dateFormat.format(new Date()))
			.append("] ")
			.append(msg)
			.append("\n");
		
		if(!this.group.isSystemSend()) EmailMain.log.info(msg);
		
		try (FileOutputStream fos = new FileOutputStream(filepath, true);
			 OutputStreamWriter out = new OutputStreamWriter(fos)) {
			out.write(s.toString());
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}
