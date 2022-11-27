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

import cat.jiu.email.element.Cooling;
import cat.jiu.email.element.Email;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailSendEvent;
import cat.jiu.email.event.EmailSendEvent.EmailSenderGroup;
import cat.jiu.email.ui.container.ContainerEmailSend;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailSizeReport;
import cat.jiu.email.util.EmailUtils;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgSend implements IMessage {
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	protected String addresserName;
	protected EmailSenderGroup group;
	protected Email email;
	
	public MsgSend() {}
	/**
	 * @param sender email sender, if null, will use player name to send
	 * @param group owner sender group
	 * @param title the email title
	 * @param addressee the email addressee
	 * @param msgs the email messages, can be null
	 * @param items the email items, can be null
	 */
	public MsgSend(EmailSenderGroup group, String addressee, Email email) {
		this.group = group;
		this.addresserName = addressee;
		this.email = email;
		this.email.setTime(new Date());
	}
	
	public static String getTime() {
		return dateFormat.format(new Date());
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		try {
			NBTTagCompound nbt = new PacketBuffer(buf).readCompoundTag();
			
			this.addresserName = nbt.getString("uid");
			this.group = EmailSenderGroup.getGroupByID(nbt.getInteger("sender_group"));
			this.email = new Email(nbt.getCompoundTag("email"));
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		NBTTagCompound nbt = new NBTTagCompound();
		
		nbt.setString("uid", this.addresserName);
		nbt.setInteger("sender_group", EmailSenderGroup.getIDByGroup(this.group));
		nbt.setTag("email", this.email.write(new NBTTagCompound()));
		
		new PacketBuffer(buf).writeCompoundTag(nbt);
	}

	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isServer()) {
			switch(this.group) {
				case PLAYER:
					this.player_to_player(ctx.getServerHandler().player);
					break;
				default: 
					this.system_to_player();
					break;
			}
		}
		return null;
	}
	
	private void system_to_player() {
		if(EmailMain.server != null) {
			EmailUtils.initNameAndUUID(EmailMain.server);
			UUID addresserUUID = EmailUtils.getUUID(this.addresserName);
			UUID senderUID = EmailUtils.getUUID(this.email.getSender().getKey());
			if(senderUID != null) {
				EntityPlayer player = EmailMain.server.getEntityWorld().getPlayerEntityByUUID(senderUID);
				if(player != null) {
					this.cmd_player_to_player((EntityPlayerMP) player, addresserUUID);
					return;
				}
			}
			if(addresserUUID == null) {
				EmailMain.log.error("Not Found Player: {}", this.addresserName);
				return;
			}
			EmailSendEvent.Pre pre = new EmailSendEvent.Pre(EmailMain.server, this.group, this.addresserName, addresserUUID, email); 
			if(MinecraftForge.EVENT_BUS.post(pre)) {
				return;
			}
			Inbox inbox = Inbox.get(addresserUUID);
			inbox.addEmail(this.email);
			
			MinecraftForge.EVENT_BUS.post(new EmailSendEvent.Post(EmailMain.server, this.group, addresserUUID, email));
			
			if(EmailUtils.saveInboxToDisk(inbox)) {
				EmailMain.log.info("{} send a email to Player: {}, UUID: {}", this.email.getSender(), this.addresserName, addresserUUID);
				MsgSend.sendLog(this.email.getSender().getKey(), this.addresserName, addresserUUID);
				EntityPlayer addresser = EmailMain.server.getEntityWorld().getPlayerEntityByUUID(addresserUUID);
				if(addresser!=null) {
					EmailMain.net.sendMessageToPlayer(new MsgUnread(inbox.getUnRead()), (EntityPlayerMP) addresser);
					if(email.hasItems()) {
						EmailMain.net.sendMessageToPlayer(new MsgUnreceive(inbox.getUnReceived()), (EntityPlayerMP) addresser);
					}
					addresser.sendMessage(EmailUtils.createTextComponent("info.email.from", this.email.getSender()));
				}
			}
			return;
		}else {
			EmailMain.log.error("Server is no Started!");
			return;
		}
	}
	
	private void cmd_player_to_player(EntityPlayerMP player, UUID addresserUUID) {
		if(addresserUUID == null) {
			player.sendMessage(EmailUtils.createTextComponent("info.email.error.not_found_player", TextFormatting.RED, this.addresserName));
			return;
		}else if(EmailConfigs.Send.Enable_Send_BlackList) {
			if(EmailAPI.isInBlackList(player)) {
				player.sendMessage(EmailUtils.createTextComponent("email.command.send.no_permission.black", TextFormatting.RED));
				return;
			}
		}else if(EmailConfigs.Send.Enable_Send_WhiteList) {
			if(!EmailAPI.isInWhiteList(player)) {
				player.sendMessage(EmailUtils.createTextComponent("email.command.send.no_permission.white", TextFormatting.RED));
				return;
			}
		}else if(player.getUniqueID().equals(addresserUUID) && !EmailConfigs.Send.Enable_Send_To_Self) {
			player.sendMessage(EmailUtils.createTextComponent("info.email.error.send_self", TextFormatting.RED));
			return;
		}
		EmailSendEvent.Pre pre = new EmailSendEvent.Pre(EmailMain.server, this.group, this.addresserName, addresserUUID, email); 
		if(MinecraftForge.EVENT_BUS.post(pre)) {
			return;
		}
		Inbox inbox = Inbox.get(addresserUUID);
		inbox.addEmail(this.email);
		
		MinecraftForge.EVENT_BUS.post(new EmailSendEvent.Post(EmailMain.server, this.group, addresserUUID, email));
		
		if(EmailUtils.saveInboxToDisk(inbox)) {
			EmailMain.log.info("{} send a email to Player: {}, UUID: {}", this.email.getSender(), this.addresserName, addresserUUID);
			MsgSend.sendLog(this.email.getSender().getKey(), this.addresserName, addresserUUID);
			EntityPlayer addresser = EmailMain.server.getEntityWorld().getPlayerEntityByUUID(addresserUUID);
			if(addresser!=null) {
				EmailMain.net.sendMessageToPlayer(new MsgUnread(inbox.getUnRead()), (EntityPlayerMP) addresser);
				if(email.hasItems()) {
					EmailMain.net.sendMessageToPlayer(new MsgUnreceive(inbox.getUnReceived()), (EntityPlayerMP) addresser);
				}
				addresser.sendMessage(EmailUtils.createTextComponent("info.email.from", this.email.getSender()));
			}
		}
	}
	
	private void player_to_player(EntityPlayerMP player) {
		if(EmailConfigs.Send.Enable_Send_BlackList) {
			if(EmailAPI.isInBlackList(player)) {
				player.sendMessage(EmailUtils.createTextComponent("email.command.send.no_permission.black", TextFormatting.RED));
				return;
			}
		}else if(EmailConfigs.Send.Enable_Send_WhiteList) {
			if(!EmailAPI.isInWhiteList(player)) {
				player.sendMessage(EmailUtils.createTextComponent("email.command.send.no_permission.white", TextFormatting.RED));
				return;
			}
		}
		
		WorldServer world = player.getServerWorld();
		if(player.openContainer instanceof ContainerEmailSend) {
			world.addScheduledTask(()->{
				EmailUtils.initNameAndUUID(world.getMinecraftServer());
				UUID addresserUUID = EmailUtils.getUUID(this.addresserName);
				if(addresserUUID == null) {
					EmailMain.net.sendMessageToPlayer(new SendRenderText("info.email.error.not_found_player", this.addresserName), player);
					return;
				}
				if(player.getUniqueID().equals(addresserUUID) && !EmailConfigs.Send.Enable_Send_To_Self) {
					EmailMain.net.sendMessageToPlayer(new SendRenderText("info.email.error.send_self"), player);
					return;
				}
				
				ContainerEmailSend container = (ContainerEmailSend) player.openContainer;
				container.setLock(true);

				this.email.clearItems();
				List<ItemStack> items = container.toItemList();
				items.forEach(stack->{
					this.email.addItem(stack);
				});
				String sender = this.email.getSender() == null ? player.getName() : this.email.getSender().getKey();
				
				EmailSendEvent.Pre pre = new EmailSendEvent.Pre(EmailMain.server, this.group, this.addresserName, addresserUUID, email); 
				if(MinecraftForge.EVENT_BUS.post(pre)) {
					container.putStack(items);
					container.setLock(false);
					return;
				}
				
				Inbox inbox = Inbox.get(addresserUUID);
				inbox.addEmail(email);
				
				if(EmailUtils.isInfiniteSize()) {
					MinecraftForge.EVENT_BUS.post(new EmailSendEvent.Post(EmailMain.server, this.group, addresserUUID, email));
					
					container.setLock(false);
					if(EmailUtils.saveInboxToDisk(inbox)) {
						EmailMain.net.sendMessageToPlayer(new SendRenderText(Color.GREEN, EmailUtils.parseTick(5, 0), "info.email.send.success"), player);
	        			if(EmailConfigs.Send.Enable_Send_Cooling) {
	        				Cooling.cooling(player.getName(), EmailUtils.getCoolingTicks() * 50);
	        			}
	        			EmailMain.log.info("{} send a email to Player: {}, UUID: {}", sender, this.addresserName, addresserUUID);
						MsgSend.sendLog(sender, this.addresserName, addresserUUID);
						EntityPlayer addresser = EmailMain.server.getEntityWorld().getPlayerEntityByUUID(addresserUUID);
						if(addresser!=null) {
							EmailMain.net.sendMessageToPlayer(new MsgUnread(inbox.getUnRead()), (EntityPlayerMP) addresser);
							if(email.hasItems()) {
								EmailMain.net.sendMessageToPlayer(new MsgUnreceive(inbox.getUnReceived()), (EntityPlayerMP) addresser);
							}
							addresser.sendMessage(EmailUtils.createTextComponent("info.email.from", sender));
						}
					}
				}else {
					EmailSizeReport report = EmailUtils.checkInboxSize(inbox);
					
					if(!EmailSizeReport.SUCCES.equals(report)) {
        				EmailMain.net.sendMessageToPlayer(new SendRenderText("info.email.error.send.to_big", Integer.toString(report.slot), Long.toString(report.size)), player);
                		container.setLock(false);
        			}else {
        				long size = inbox.getInboxSize();
        				if(size >= 2097152L) {
    						EmailMain.net.sendMessageToPlayer(new SendRenderText("info.email.error.send.to_big.email", Long.toString(size)), player);
    	        			container.setLock(false);
    						return;
    					}else {
    						MinecraftForge.EVENT_BUS.post(new EmailSendEvent.Post(EmailMain.server, this.group, addresserUUID, email));
    						
    						container.setLock(false);
    						if(EmailUtils.saveInboxToDisk(inbox, 10)) {
    							EmailMain.net.sendMessageToPlayer(new SendRenderText(Color.GREEN, EmailUtils.parseTick(5, 0), "info.email.send.success"), player);
    		        			if(EmailConfigs.Send.Enable_Send_Cooling) {
    		        				Cooling.cooling(player.getName(), EmailUtils.getCoolingTicks() * 50);
    		        			}
    		        			EmailMain.log.info("{} send a email to Player: {}, UUID: {}", sender, this.addresserName, addresserUUID);
    							MsgSend.sendLog(sender, this.addresserName, addresserUUID);
    							EntityPlayer addresser = EmailMain.server.getEntityWorld().getPlayerEntityByUUID(addresserUUID);
    							if(addresser!=null) {
    								EmailMain.net.sendMessageToPlayer(new MsgUnread(inbox.getUnRead()), (EntityPlayerMP) addresser);
    								if(email.hasItems()) {
    									EmailMain.net.sendMessageToPlayer(new MsgUnreceive(inbox.getUnReceived()), (EntityPlayerMP) addresser);
    								}
    								addresser.sendMessage(EmailUtils.createTextComponent("info.email.from", sender));
    							}
    						}
    					}
        			}
				}
			});
		}
	}
	
	public static class SendRenderText implements IMessage {
		public static final Object[] empty = new Object[0];
		protected String langKey;
		protected Object[] args;
		protected Color color;
		protected long renderTicks;
		
		public SendRenderText() {}
		public SendRenderText(String langKey) {
			this(langKey, empty);
		}
		public SendRenderText(String langKey, Object... args) {
			this(Color.RED, EmailUtils.parseTick(15, 0), langKey, args);
		}
		public SendRenderText(Color color, long renderTicks, String langKey, Object... args) {
			this.langKey = langKey;
			this.args = args;
			this.color = color;
			this.renderTicks = renderTicks;
		}
		public void fromBytes(ByteBuf buf) {
			try {
				NBTTagCompound nbt = new PacketBuffer(buf).readCompoundTag();
				
				this.langKey = nbt.getString("lang_key");
				if(nbt.hasKey("args")) {
					NBTTagList args = nbt.getTagList("args", 8);
					this.args = new Object[args.tagCount()];
					for(int i = 0; i < this.args.length; i++) {
						this.args[i] = ((NBTTagString)args.get(i)).getString();
					}
				}
				this.color = new Color(nbt.getInteger("color"));
				this.renderTicks = nbt.getLong("ticks");
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		public void toBytes(ByteBuf buf) {
			NBTTagCompound nbt = new NBTTagCompound();
			
			nbt.setString("lang_key", this.langKey);
			
			if(this.args.length > 0) {
				NBTTagList args = new NBTTagList();
				for(int i = 0; i < this.args.length; i++) {
					args.appendTag(new NBTTagString(String.valueOf(this.args[i])));
				}
				nbt.setTag("args", args);
			}
			
			nbt.setInteger("color", this.color.getRGB());
			nbt.setLong("ticks", this.renderTicks);
			
			new PacketBuffer(buf).writeCompoundTag(nbt);
		}
		
		public IMessage handler(MessageContext ctx) {
			((ContainerEmailSend)Minecraft.getMinecraft().player.openContainer).setRenderText(I18n.format(this.langKey, this.args), this.color, this.renderTicks);
			return null;
		}
	}
	
	public static class SendCooling implements IMessage {
		protected long tick;
		
		public SendCooling() {}
		public SendCooling(long tick) {
			this.tick = tick;
		}
		@Override
		public void fromBytes(ByteBuf buf) {
			this.tick = buf.readLong();
		}
		@Override
		public void toBytes(ByteBuf buf) {
			buf.writeLong(this.tick);
		}
		public IMessage handler(MessageContext ctx) {
			Container con = Minecraft.getMinecraft().player.openContainer;
			if(con instanceof ContainerEmailSend) {
				((ContainerEmailSend)con).setCooling(this.tick);
			}
			return null;
		}
	}
	
	public static void sendLog(String sender, String name, UUID uid) {
		File filepath = new File("./logs/email.log");

		if(!filepath.exists()) {
			try {
				filepath.createNewFile();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		StringBuilder s = new StringBuilder()
			.append("[")
			.append(dateFormat.format(new Date()))
			.append("] ")
			.append(new StringBuilder()
					.append(sender)
					.append(" send a email to Player: ")
					.append(name)
					.append(", UUID: ")
					.append(uid.toString()))
			.append("\n");
		
		try (FileOutputStream fos = new FileOutputStream(filepath, true);
			 OutputStreamWriter out = new OutputStreamWriter(fos)) {
			out.write(s.toString());
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}
