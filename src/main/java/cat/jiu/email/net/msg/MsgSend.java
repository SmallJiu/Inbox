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
import java.util.function.Supplier;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.element.EmailSenderGroup;
import cat.jiu.email.ui.container.ContainerEmailSend;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.Level;

import cat.jiu.email.element.Cooling;
import cat.jiu.email.element.Email;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailSendEvent;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.SizeReport;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TextFormatting;

import net.minecraftforge.common.MinecraftForge;

public class MsgSend extends BaseMessage {
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	protected String addressed;
	protected EmailSenderGroup group;
	protected Email email;

	public MsgSend() {}
	public MsgSend(EmailSenderGroup group, String addressee, Email email) {
		this.group = group;
		this.addressed = addressee;
		this.email = email;
		this.email.setCreateTimeToNow();
	}

	@Override
	public void fromBytes(PacketBuffer buf) {
		CompoundNBT nbt = buf.readCompoundTag();

		this.addressed = nbt.getString("addresser");
		this.group = EmailSenderGroup.getGroupByID(nbt.getInt("group"));
		this.email = new Email(nbt.getCompound("email"));
	}

	@Override
	public void toBytes(PacketBuffer buf) {
		CompoundNBT nbt = new CompoundNBT();
		
		nbt.putString("addresser", this.addressed);
		nbt.putInt("group", EmailSenderGroup.getIDByGroup(this.group));
		nbt.put("email", this.email.writeTo(CompoundNBT.class));
		
		buf.writeCompoundTag(nbt);
	}

	@Override
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(()->{
			EmailUtils.initNameAndUUID(EmailMain.server);
			ServerPlayerEntity sender = ctx.get().getSender();

			if(this.group.isPlayerSend() && this.addressed.equals(sender.getName()) && !EmailConfigs.Send.Enable_Send_To_Self.get()) {
				this.sendMessage(sender, Level.ERROR, "info.email.error.send_self");
				return;
			}

			if(!EmailUtils.hasName(this.addressed) && !("@a".equals(this.addressed) || "@p".equals(this.addressed))) {
				this.sendMessage(sender, Level.ERROR, "info.email.error.not_found_player", this.addressed);
				return;
			}

			if("@a".equals(this.addressed) || "@p".equals(this.addressed)) {
				if(sender!=null) {
					new Thread(()->{
						if(EmailUtils.isOP(sender)) {
							long time = System.currentTimeMillis();
							int sended = 0;
							if(this.group.isPlayerSend()) {
								((ContainerEmailSend) sender.openContainer).setLock(true);
							}

							if("@a".equals(this.addressed)) {
								for(UUID uid : EmailUtils.getAllUUID()) {
									this.sendEmail(sender, uid.toString(), false);
									sended++;
								}
							}else if("@p".equals(this.addressed)) {
								for(PlayerEntity profile : sender.getServer().getPlayerList().getPlayers()) {
									this.sendEmail(sender, profile.getUniqueID().toString(), false);
									sended++;
								}
							}

							if(this.group.isPlayerSend()) {
								((ContainerEmailSend) sender.openContainer).setLock(false);
							}

							this.sendMessage(sender, Level.INFO, "info.email.send.abs", sended, System.currentTimeMillis() - time);
						}else {
							this.sendMessage(sender, Level.ERROR, "info.email.send.not_op.abs", this.addressed);
						}
					}).start();
				}
			}else {
				this.sendEmail(sender, this.addressed, true);
			}
		});
		return true;
	}
	
	private void sendEmail(ServerPlayerEntity sender, String addresses, boolean lock) {
		if(this.email.getExpirationTime()!=null && this.email.getExpirationTime().millis>0 && !EmailUtils.isOP(sender)) {
			this.sendMessage(sender, Level.ERROR, "info.email.send.not_op.expiration");
			return;
		}
		Inbox inbox = Inbox.get(addresses);
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
		
		if(MinecraftForge.EVENT_BUS.post(new EmailSendEvent(Phase.START, this.group, addresses, this.email))) return;
		
		if(!inbox.isInSenderBlacklist(sender.getName().getString())) {
			this.sendMessage(addresses, inbox.addEmail(this.email, true), sender, lock);
		}else {
			if(this.group.isPlayerSend()) {
				container.putStack(this.email.getItems());
				if(lock) container.setLock(false);
			}
			this.sendIsBlackMessage(sender, lock);
		}
		
		MinecraftForge.EVENT_BUS.post(new EmailSendEvent(Phase.END, this.group, addresses, this.email));
	}
	
	private boolean checkEmailSize(Inbox inbox, ServerPlayerEntity msgSender, List<ItemStack> stacks, boolean lock) {
		SizeReport report = EmailUtils.checkEmailSize(this.email);
		if(!SizeReport.SUCCES.equals(report)) {
			if(msgSender!=null) {
				if(this.group.isPlayerSend()) {
					((ContainerEmailSend) msgSender.openContainer).putStack(stacks);
					if(lock) ((ContainerEmailSend) msgSender.openContainer).setLock(false);
					EmailMain.net.sendMessageToPlayer(new MsgSendRenderText(new Text("info.email.error.send.to_big", report.slot, report.size)), msgSender);
				}else {
					EmailUtils.sendMessage(msgSender, TextFormatting.RED, "info.email.error.send.to_big", report.slot, report.size);
				}
			}else {
				EmailMain.log.warn("Email item is to big, please remove some item or nbt. Slot: {}, Size: {} / 2097152 Bytes", report.slot, report.size);
			}
			return false;
		}
		
		long size = inbox.getInboxSize() + EmailUtils.getSize(this.email.writeTo(CompoundNBT.class));
		if(size >= 2097152L) {
			if(msgSender!=null) {
				if(this.group.isPlayerSend()) {
					((ContainerEmailSend) msgSender.openContainer).putStack(stacks);
					if(lock) ((ContainerEmailSend) msgSender.openContainer).setLock(false);
					EmailMain.net.sendMessageToPlayer(new MsgSendRenderText(new Text("info.email.error.send.to_big.email", size)), msgSender);
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
	
	private void sendMessage(String addresser, boolean success, ServerPlayerEntity msgSender, boolean lock) {
		if(EmailMain.SYSTEM.equals(this.email.getSender().getText())
		|| this.group.isSystemSend()) return;
		try {
			addresser = EmailUtils.getName(UUID.fromString(addresser));
		}catch(Exception e) {}
		
		if(success) {
			if(!this.group.isSystemSend() && msgSender!=null) {
				if(this.group.isPlayerSend()) {
					if(lock) ((ContainerEmailSend) msgSender.openContainer).setLock(false);
					if(EmailConfigs.Send.Enable_Send_Cooling.get()) Cooling.cooling(msgSender.getName().getString());
					EmailMain.net.sendMessageToPlayer(new MsgSendRenderText(Color.GREEN, new Text("info.email.send.success", addresser)), msgSender);
				}else {
					EmailUtils.sendMessage(msgSender, TextFormatting.GREEN, "info.email.send.success", addresser);
				}
				PlayerEntity player = getOnlinePlayer(addresser, msgSender.getServer());
				if(player != null) {
					EmailUtils.sendMessage(player, "info.email.from", this.email.getSender());
				}
			}
			sendLog(this.email.getSender().getText(), addresser, EmailUtils.getUUID(addresser));
		}else {
			if(!this.group.isSystemSend() && msgSender!=null) {
				if(this.group.isPlayerSend()) {
					if(lock) ((ContainerEmailSend) msgSender.openContainer).setLock(false);
					EmailMain.net.sendMessageToPlayer(new MsgSendRenderText(Color.RED, new Text("info.email.send.fail")), msgSender);
				}else {
					EmailUtils.sendMessage(msgSender, TextFormatting.RED, "info.email.send.fail");
				}
			}else {
				EmailMain.log.info("Send e-mail fail, check log find the reason.");
			}
		}
	}
	
	private void sendIsBlackMessage(ServerPlayerEntity msgSender, boolean lock) {
		if(msgSender!=null) {
			if(this.group.isPlayerSend()) {
				if(lock) ((ContainerEmailSend) msgSender.openContainer).setLock(false);
				EmailMain.net.sendMessageToPlayer(new MsgSendRenderText(Color.RED, new Text("info.email.send.fail.blacklist")), msgSender);
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
	public static PlayerEntity getOnlinePlayer(String s, MinecraftServer server) {
		PlayerEntity player = null;
		if(server!=null && s!=null & !s.isEmpty()) {
			PlayerList playerList = server.getPlayerList();
			try {
				player = playerList.getPlayerByUUID(UUID.fromString(s));
			}catch(Exception e) {
				player = playerList.getPlayerByUsername(s);
			}
		}
		return player;
	}
	
	private void sendMessage(ServerPlayerEntity sender, Level level, String msg, Object... arg) {
		if(this.group.isPlayerSend()) {
			Color color;
			if(level == Level.INFO) {
				color = Color.GREEN;
			}else if(level == Level.WARN) {
				color = Color.YELLOW;
			}else {
				color = Color.RED;
			}
			EmailMain.net.sendMessageToPlayer(new MsgSendRenderText(color, new Text(msg, arg)), sender);
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
		 		.append(uid==null ? addressed : uid.toString());
		
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
