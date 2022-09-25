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

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import cat.jiu.email.Email;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.EmailSound;
import cat.jiu.email.event.EmailSendEvent;
import cat.jiu.email.event.EmailSendEvent.EmailSenderGroup;
import cat.jiu.email.ui.container.ContainerEmailSend;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailSizeReport;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonToStackUtil;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
	protected String sender;
	protected String addresserName;
	protected String time;
	protected String title;
	protected List<String> msgs;
	protected List<ItemStack> items;
	protected String itemFile;
	protected EmailSenderGroup group;
	protected EmailSound sound;
	
	public MsgSend() {}
	/**
	 * @param sender email sender, if null, will use player name to send
	 * @param group owner sender group
	 * @param title the email title
	 * @param addressee the email addressee
	 * @param msgs the email messages, can be null
	 * @param items the email items, can be null
	 */
	public MsgSend(EmailSenderGroup group, @Nullable String sender, String title, String addressee, @Nullable List<String> msgs, @Nullable EmailSound sound, @Nullable List<ItemStack> items) {
		this.items = items;
		this.sender = sender;
		this.group = group;
		this.title = title;
		this.addresserName = addressee;
		this.msgs = msgs;
		this.time = getTime();
	}
	public MsgSend(EmailSenderGroup group, @Nullable String sender, String title, String addressee, @Nullable List<String> msgs, @Nullable EmailSound sound) {
		this(group, sender, title, addressee, msgs, sound, null);
	}
	
	public static String getTime() {
		return dateFormat.format(new Date());
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		try {
			NBTTagCompound nbt = new PacketBuffer(buf).readCompoundTag();
			
			this.addresserName = nbt.getString("uid");
			this.title = nbt.getString("title");
			this.time = nbt.getString("time");
			this.group = EmailSenderGroup.getGroupByID(nbt.getInteger("sender_group"));
			if(nbt.hasKey("sender")) this.sender = nbt.getString("sender");
			if(nbt.hasKey("sound")) this.sound = EmailSound.from(nbt.getCompoundTag("sound"));
			
			if(nbt.hasKey("msgs")) {
				this.msgs = Lists.newArrayList();
				NBTTagCompound msgsNBT = nbt.getCompoundTag("msgs");
				for(int i = 0; i < msgsNBT.getSize(); i++) {
					this.msgs.add(msgsNBT.getString(Integer.toString(i)));
				}
			}
			if(nbt.hasKey("items")) {
				if(nbt.getTag("items") instanceof NBTTagString) {
					this.itemFile = nbt.getString("items");
				}else {
					this.items = Lists.newArrayList();
					NBTTagCompound itemsNBT = nbt.getCompoundTag("items");
					for(String key : itemsNBT.getKeySet()) {
						this.items.add(new ItemStack(itemsNBT.getCompoundTag(key)));
					}
				}
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		PacketBuffer pb = new PacketBuffer(buf);
		NBTTagCompound nbt = new NBTTagCompound();
		
		nbt.setString("uid", this.addresserName);
		nbt.setString("time", this.time);
		nbt.setString("title", this.title);
		nbt.setInteger("sender_group", EmailSenderGroup.getIDByGroup(this.group));
		if(this.sound!=null) nbt.setTag("sound", this.sound.toNBT());
		
		if(this.sender != null)nbt.setString("sender", this.sender);
		
		if(this.msgs != null && !this.msgs.isEmpty()) {
			NBTTagCompound msgs = new NBTTagCompound();
			for(int i = 0; i < this.msgs.size(); i++) {
				msgs.setString(Integer.toString(i), this.msgs.get(i));
			}
			nbt.setTag("msgs", msgs);
		}
		
		if(this.items != null && !this.items.isEmpty()) {
			NBTTagCompound msgs = new NBTTagCompound();
			for(int i = 0; i < this.items.size(); i++) {
				msgs.setTag(Integer.toString(i), this.items.get(i).writeToNBT(new NBTTagCompound()));
			}
			nbt.setTag("items", msgs);
		}
		
		pb.writeCompoundTag(nbt);
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
			UUID senderUID = EmailUtils.getUUID(this.sender);
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
			EmailSendEvent.Pre pre = new EmailSendEvent.Pre(EmailMain.server, this.title, this.time, sender, this.group, this.addresserName, addresserUUID, this.msgs, this.items); 
			if(MinecraftForge.EVENT_BUS.post(pre)) {
				return;
			}
			this.title = pre.getTitle();
			this.time = pre.getSendTime();
			this.sender = pre.getSender();
			
			JsonObject email = EmailUtils.getInboxJson(addresserUUID.toString());
			
			if(email == null) {
				email = new JsonObject();
			}
			JsonObject msg = new JsonObject();
			String sender = this.group.isCommandSend() ? this.sender == null ? "Console" : this.sender : EmailMain.SYSTEM;
			
			msg.addProperty("sender", sender);
			msg.addProperty("time", this.time);
			msg.addProperty("title", this.title);
			if(this.sound!=null) msg.add("sound", this.sound.toJson());
			
			if(this.msgs!=null && !this.msgs.isEmpty()) {
				JsonArray msgs = new JsonArray();
				for(int i = 0; i < this.msgs.size(); i++) {
					msgs.add(this.msgs.get(i));
				}
				msg.add("msgs", msgs);
			}
			if(this.items!=null && !this.items.isEmpty()) {
				msg.add("items", JsonToStackUtil.toJsonObject(this.items, false));
			}
			
			email.add(Integer.toString(email.size()), msg);
			
			boolean lag = EmailUtils.toJsonFile(addresserUUID.toString(), email);
			MinecraftForge.EVENT_BUS.post(new EmailSendEvent.Post(EmailMain.server, sender, this.group, this.addresserName, addresserUUID, email));
			if(lag) {
				EmailMain.log.info("{} send a email to Player: {}, UUID: {}", sender, this.addresserName, addresserUUID);
				this.writeLog(sender, this.addresserName, addresserUUID);
				EntityPlayer addresser = EmailMain.server.getEntityWorld().getPlayerEntityByUUID(addresserUUID);
				if(addresser!=null) {
					EmailMain.net.sendMessageToPlayer(new MsgUnread(EmailMain.getUn(email, "read")), (EntityPlayerMP) addresser);
					if(msg.has("items")) {
						EmailMain.net.sendMessageToPlayer(new MsgUnreceive(EmailMain.getUn(email, "accept")), (EntityPlayerMP) addresser);
					}
					addresser.sendMessage(EmailUtils.createTextComponent("info.email.from", sender));
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
			if(Email.isInBlackList(player)) {
				player.sendMessage(EmailUtils.createTextComponent("email.command.send.no_permission.black", TextFormatting.RED));
				return;
			}
		}else if(EmailConfigs.Send.Enable_Send_WhiteList) {
			if(!Email.isInWhiteList(player)) {
				player.sendMessage(EmailUtils.createTextComponent("email.command.send.no_permission.white", TextFormatting.RED));
				return;
			}
		}else if(player.getUniqueID().equals(addresserUUID) && !EmailConfigs.Send.Enable_Send_To_Self) {
			player.sendMessage(EmailUtils.createTextComponent("info.email.error.send_self", TextFormatting.RED));
			return;
		}
		
		JsonObject email = EmailUtils.getInboxJson(addresserUUID.toString());
		
		if(email == null) {
			email = new JsonObject();
		}
		JsonObject msg = new JsonObject();
		String sender = this.sender == null ? player.getName() : this.sender;
		EmailSendEvent.Pre pre = new EmailSendEvent.Pre(player.getServer(), this.title, this.time, sender, this.group, this.addresserName, addresserUUID, this.msgs, this.items); 
		if(MinecraftForge.EVENT_BUS.post(pre)) {
			return;
		}
		
		this.title = pre.getTitle();
		this.time = pre.getSendTime();
		this.sender = pre.getSender();
		
		msg.addProperty("sender", sender);
		msg.addProperty("time", this.time);
		msg.addProperty("title", this.title);
		if(this.sound!=null) msg.add("sound", this.sound.toJson());
		
		if(this.msgs!=null && !this.msgs.isEmpty()) {
			JsonArray msgs = new JsonArray();
			for(int i = 0; i < this.msgs.size(); i++) {
				msgs.add(this.msgs.get(i));
			}
			msg.add("msgs", msgs);
		}
		if(this.items!=null && !this.items.isEmpty()) {
			msg.add("items", JsonToStackUtil.toJsonObject(this.items, false));
		}
		
		email.add(Integer.toString(email.size()), msg);
		
		boolean lag = EmailUtils.toJsonFile(addresserUUID.toString(), email);
		MinecraftForge.EVENT_BUS.post(new EmailSendEvent.Post(player.getServer(), sender, this.group, this.addresserName, addresserUUID, email));
		if(lag) {
			EmailMain.log.info("{} send a email to Player: {}, UUID: {}", player.getName(), this.addresserName, addresserUUID);
			this.writeLog(player.getName(), this.addresserName, addresserUUID);
			player.sendMessage(EmailUtils.createTextComponent("info.email.send.success", TextFormatting.GREEN));
			EntityPlayer addresser = EmailMain.server.getEntityWorld().getPlayerEntityByUUID(addresserUUID);
			if(addresser!=null) {
				EmailMain.net.sendMessageToPlayer(new MsgUnread(EmailMain.getUn(email, "read")), (EntityPlayerMP) addresser);
				if(msg.has("items")) {
					EmailMain.net.sendMessageToPlayer(new MsgUnreceive(EmailMain.getUn(email, "accept")), (EntityPlayerMP) addresser);
				}
				addresser.sendMessage(EmailUtils.createTextComponent("info.email.from", sender));
			}
		}
	}
	
	private void player_to_player(EntityPlayerMP player) {
		if(EmailConfigs.Send.Enable_Send_BlackList) {
			if(Email.isInBlackList(player)) {
				player.sendMessage(EmailUtils.createTextComponent("email.command.send.no_permission.black", TextFormatting.RED));
				return;
			}
		}else if(EmailConfigs.Send.Enable_Send_WhiteList) {
			if(!Email.isInWhiteList(player)) {
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
				this.items = container.toItemList();
				String sender = this.sender == null ? player.getName() : this.sender;
				
				EmailSendEvent.Pre pre = new EmailSendEvent.Pre(player.getServer(), this.title, this.time, sender, this.group, this.addresserName, addresserUUID, this.msgs, this.items); 
				if(MinecraftForge.EVENT_BUS.post(pre)) {
					container.putStack(pre.items);
					return;
				}
				this.title = pre.getTitle();
				this.time = pre.getSendTime();
				sender = pre.getSender();
				container.putStack(pre.items);
				
				JsonObject msg = new JsonObject();
				JsonObject email = EmailUtils.getInboxJson(addresserUUID.toString());
				if(email == null) {
					email = new JsonObject();
				}
				
				msg.addProperty("sender", sender);
				msg.addProperty("time", this.time);
				msg.addProperty("title", this.title);
				if(this.sound!=null) msg.add("sound", this.sound.toJson());
				
				if(this.msgs!=null && !this.msgs.isEmpty()) {
					JsonArray msgs = new JsonArray();
					for(int i = 0; i < this.msgs.size(); i++) {
						msgs.add(this.msgs.get(i));
					}
					msg.add("msgs", msgs);
				}
				
				if(EmailUtils.isInfiniteSize()) {
					container.setLock(true);
					if(!container.isEmpty()) {
						msg.add("items", container.toItemArray(false));
					}
					
					email.add(Integer.toString(email.size()), msg);
					
					boolean lag = EmailUtils.toJsonFile(addresserUUID.toString(), email);
					MinecraftForge.EVENT_BUS.post(new EmailSendEvent.Post(player.getServer(), sender, this.group, this.addresserName, addresserUUID, email));
					if(lag) {
						EmailMain.log.info("{} send a email to Player: {}, UUID: {}", player.getName(), this.addresserName, addresserUUID);
	        			container.setLock(false);
	        			EmailMain.net.sendMessageToPlayer(new SendRenderText(Color.GREEN, EmailUtils.parseTick(5, 0), "info.email.send.success"), player);
	        			if(EmailConfigs.Send.Enable_Send_Cooling) {
	        				EmailMain.net.sendMessageToPlayer(new SendCooling(EmailUtils.getCoolingTicks()), player);
	        			}
	        			this.writeLog(player.getName(), this.addresserName, addresserUUID);
						EntityPlayer addresser = world.getPlayerEntityByUUID(addresserUUID);
						if(addresser!=null) {
							EmailMain.net.sendMessageToPlayer(new MsgUnread(EmailMain.getUn(email, "read")), (EntityPlayerMP) addresser);
							if(msg.has("items")) {
								EmailMain.net.sendMessageToPlayer(new MsgUnreceive(EmailMain.getUn(email, "accept")), (EntityPlayerMP) addresser);
							}
							addresser.sendMessage(EmailUtils.createTextComponent("info.email.from", player.getName()));
						}
					}
        		}else {
        			container.setLock(true);
        			JsonObject check_email = EmailUtils.copyJson(email);
        			JsonObject check_msg = EmailUtils.copyJson(msg);
        			if(!container.isEmpty()) {
        				check_msg.add("items", container.toItemArray(true));
					}
        			check_email.add(Integer.toString(check_email.size()), check_msg);
        			EmailSizeReport report = EmailUtils.checkEmailSize(check_email);
					
        			if(!EmailSizeReport.SUCCES.equals(report)) {
        				EmailMain.net.sendMessageToPlayer(new SendRenderText("info.email.error.send.to_big", Integer.toString(report.itemSlot), Long.toString(report.size)), player);
                		container.setLock(false);
        			}else {
        				{long size = EmailUtils.getEmailSize(check_email);
        				if(size >= 2097152L) {
    						EmailMain.net.sendMessageToPlayer(new SendRenderText("info.email.error.send.to_big.email", Long.toString(size)), player);
    	        			container.setLock(false);
    						return;
    					}}
        				
        				if(!container.isEmpty()) {
							msg.add("items", container.toItemArray(false));
						}
    					
    					email.add(Integer.toString(email.size()), msg);
    					
						boolean lag = EmailUtils.toJsonFile(addresserUUID.toString(), email);
						MinecraftForge.EVENT_BUS.post(new EmailSendEvent.Post(player.getServer(), sender, this.group, this.addresserName, addresserUUID, email));
    					if(lag) {
							EmailMain.log.info("{} send a email to Player: {}, UUID: {}", player.getName(), this.addresserName, addresserUUID);
		        			container.setLock(false);
		        			EmailMain.net.sendMessageToPlayer(new SendRenderText(Color.GREEN, EmailUtils.parseTick(5, 0), "info.email.send.success"), player);
		        			if(EmailConfigs.Send.Enable_Send_Cooling) {
		        				EmailMain.net.sendMessageToPlayer(new SendCooling(EmailUtils.getCoolingTicks()), player);
		        			}
		        			this.writeLog(player.getName(), this.addresserName, addresserUUID);
							EntityPlayer addresser = world.getPlayerEntityByUUID(addresserUUID);
							if(addresser!=null) {
								EmailMain.net.sendMessageToPlayer(new MsgUnread(EmailMain.getUn(email, "read")), (EntityPlayerMP) addresser);
								addresser.sendMessage(EmailUtils.createTextComponent("info.email.from", player.getName()));
							}
						}
        			}
        		}
			});
		}
	}
	
	public static class SendRenderText implements IMessage {
		protected String langKey;
		protected Object[] args;
		protected Color color;
		protected long renderTicks;
		
		public SendRenderText() {}
		public SendRenderText(String langKey) {
			this(langKey, new Object[0]);
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
			((ContainerEmailSend)Minecraft.getMinecraft().player.openContainer).sendCooling(this.tick);
			return null;
		}
	}
	
	static final SimpleDateFormat logDateFormat = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss] ");
	private void writeLog(String sender, String name, UUID uid) {
		File filepath = new File("./logs/email.log");

		if(!filepath.exists()) {
			try {
				filepath.createNewFile();
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		StringBuilder log = new StringBuilder()
			.append(sender)
			.append(" send a email to Player: ")
			.append(name)
			.append(", UUID: ")
			.append(uid.toString());
		
		StringBuilder s = new StringBuilder()
			.append(logDateFormat.format(new Date()))
			.append(log)
			.append("\n");
		
		try {
			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(filepath, true));
			
			out.write(s.toString());
			out.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}
