package cat.jiu.email.net.msg;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.SizeReport;
import cat.jiu.email.util.EmailUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgInboxToClient implements IMessage {
	protected Inbox inbox;
	protected SizeReport report = SizeReport.SUCCES;
	public MsgInboxToClient() {}
	public MsgInboxToClient(@Nonnull Inbox inbox) {
		this.inbox = inbox;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		if(EmailConfigs.isInfiniteSize()) {
			PacketBuffer pb = new PacketBuffer(buf);
			int i = pb.readerIndex();
	        byte b0 = pb.readByte();
	        
	        if(b0 != 0) {
	        	pb.readerIndex(i);
		        try {
					NBTTagCompound nbt = CompressedStreamTools.read(new ByteBufInputStream(pb), new NBTSizeTracker(Long.MAX_VALUE));
					if(nbt != null) {
						this.inbox = Inbox.get(nbt.getUniqueId("owner"), nbt.getCompoundTag("inbox"));
					}
		        }catch(IOException e) {
					e.printStackTrace();
				}
	        }
		}else {
			ByteBuf bufc = buf.copy();
			PacketBuffer pb = new PacketBuffer(bufc);
			int i = pb.readerIndex();
	        byte b0 = pb.readByte();
	        
	        if(b0 != 0) {
	        	pb.readerIndex(i);
		        try {
					NBTTagCompound inboxTag = CompressedStreamTools.read(new ByteBufInputStream(pb), new NBTSizeTracker(Long.MAX_VALUE));
					if(inboxTag != null) {
						if(inboxTag.hasKey("ToBig")) {
							this.report = new SizeReport(inboxTag.getLong("id"), inboxTag.getInteger("slot"), inboxTag.getInteger("size"));
						}else {
							this.inbox = Inbox.get(inboxTag.getUniqueId("owner"), inboxTag.getCompoundTag("inbox"));
						}
					}
				}catch(IOException e) {
					e.printStackTrace();
				}
	        }
		}
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		PacketBuffer pb = new PacketBuffer(buf);
		
		if(EmailConfigs.isInfiniteSize()) {
			NBTTagCompound nbt = new NBTTagCompound();
			
			nbt.setUniqueId("owner", this.inbox.getOwnerAsUUID());
			NBTTagCompound inbox = this.inbox.writeTo(NBTTagCompound.class);
			inbox.removeTag("blacklist");
			inbox.removeTag("historySize");
			inbox.removeTag("dev");
			nbt.setTag("inbox", inbox);
			
			pb.writeCompoundTag(nbt);
		}else {
			SizeReport report = EmailUtils.checkInboxSize(this.inbox);
			if(!SizeReport.SUCCES.equals(report)) {
				NBTTagCompound nbt = new NBTTagCompound();
				
				nbt.setBoolean("ToBig", true);
				nbt.setLong("id", report.id);
				nbt.setInteger("slot", report.slot);
				nbt.setLong("size", report.size);
				
				pb.writeCompoundTag(nbt);
				return;
			}
			
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setUniqueId("owner", this.inbox.getOwnerAsUUID());
			
			NBTTagCompound inbox = this.inbox.writeTo(NBTTagCompound.class);
			inbox.removeTag("blacklist");
			inbox.removeTag("historySize");
			inbox.removeTag("dev");
			nbt.setTag("inbox", inbox);
			
			pb.writeCompoundTag(nbt);
		}
	}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isClient()) {
			EntityPlayerSP player = Minecraft.getMinecraft().player;
			Container con = player.openContainer;
			if(con instanceof ContainerEmailMain) {
				if(!SizeReport.SUCCES.equals(this.report)) {
					player.sendMessage(new TextComponentString(TextFormatting.GRAY + "---------------------------------------------"));
					player.sendMessage(new TextComponentString(I18n.format("info.email.error.to_big.0")));
					player.sendMessage(new TextComponentString(I18n.format("info.email.error.to_big.1", this.report.id, this.report.slot, this.report.size)));
					player.closeScreen();
				}else {
					EmailMain.setUnread(this.inbox.getUnRead());
					EmailMain.setAccept(this.inbox.getUnReceived());
					((ContainerEmailMain) con).setInbox(this.inbox);
					((ContainerEmailMain) con).setRefresh(false);
				}
			}
		}
		return null;
	}
	
	public static class MsgOtherToClient implements IMessage {
		private final Map<String, Object> customValue = Maps.newHashMap();
		private final List<String> senderBlacklist = Lists.newArrayList();
		public MsgOtherToClient() {}
		
		public MsgOtherToClient(Inbox inbox) {
			this(inbox.getCustomValue(), inbox.getSenderBlacklist());
		}
		public MsgOtherToClient(Map<String, Object> customValue, List<String> senderBlacklist) {
			this.customValue.putAll(customValue);
			this.senderBlacklist.addAll(senderBlacklist);
		}
		
		@Override
		public void fromBytes(ByteBuf buf) {
			try {
				NBTTagCompound nbt = new PacketBuffer(buf).readCompoundTag();
				
				NBTTagCompound values = nbt.getCompoundTag("values");
				values.getKeySet().forEach(k -> this.customValue.put(k, values.getString(k)));
				
				NBTTagList blacks = nbt.getTagList("blacks", 8);
				blacks.forEach(e -> this.senderBlacklist.add(((NBTTagString)e).getString()));
				
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void toBytes(ByteBuf buf) {
			NBTTagCompound nbt = new NBTTagCompound();
			
			NBTTagCompound values = new NBTTagCompound();
			this.customValue.forEach((k,v) -> values.setString(k, String.valueOf(v)));
			nbt.setTag("values", values);
			
			NBTTagList blacks = new NBTTagList();
			this.senderBlacklist.forEach(e -> blacks.appendTag(new NBTTagString(e)));
			nbt.setTag("blacks", blacks);
			
			new PacketBuffer(buf).writeCompoundTag(nbt);
		}
		
		public IMessage handler(MessageContext ctx) {
			if(ctx.side.isClient()) {
				Container con = Minecraft.getMinecraft().player.openContainer;
				
				if(con instanceof ContainerEmailMain) {
					this.customValue.forEach((k,v) -> ((ContainerEmailMain) con).getInbox().addCustom(k, v));
					this.senderBlacklist.forEach(e -> ((ContainerEmailMain) con).getInbox().addSenderBlacklist(e));
				}
			}
			return null;
		}
	}
}
