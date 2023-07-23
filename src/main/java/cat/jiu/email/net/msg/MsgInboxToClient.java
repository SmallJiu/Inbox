package cat.jiu.email.net.msg;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import cat.jiu.core.api.BaseMessage;
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
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.container.Container;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import net.minecraftforge.fml.network.NetworkEvent;

public class MsgInboxToClient extends BaseMessage {
	protected Inbox inbox;
	protected SizeReport report = SizeReport.SUCCES;
	public MsgInboxToClient() {}
	public MsgInboxToClient(@Nonnull Inbox inbox) {
		this.inbox = inbox;
	}

	@Override
	public void fromBytes(PacketBuffer buf) {
		if(EmailConfigs.isInfiniteSize()) {
			PacketBuffer pb = new PacketBuffer(buf);
			int i = pb.readerIndex();
	        byte b0 = pb.readByte();
	        
	        if(b0 != 0) {
	        	pb.readerIndex(i);
		        try {
					CompoundNBT nbt = CompressedStreamTools.read(new ByteBufInputStream(pb), new NBTSizeTracker(Long.MAX_VALUE));
					this.inbox = Inbox.get(nbt.getUniqueId("owner"), nbt.getCompound("inbox"));
				}catch(IOException e) {
					e.printStackTrace();
				}
	        }
		}else {
			ByteBuf buf0 = buf.copy();
			PacketBuffer pb = new PacketBuffer(buf0);
			int i = pb.readerIndex();
	        byte b0 = pb.readByte();
	        
	        if(b0 != 0) {
	        	pb.readerIndex(i);
		        try {
					CompoundNBT inboxTag = CompressedStreamTools.read(new ByteBufInputStream(pb), new NBTSizeTracker(Long.MAX_VALUE));
					if (inboxTag.contains("ToBig")) {
						this.report = new SizeReport(inboxTag.getLong("id"), inboxTag.getInt("slot"), inboxTag.getInt("size"));
					} else {
						this.inbox = Inbox.get(inboxTag.getUniqueId("owner"), inboxTag.getCompound("inbox"));
					}
				}catch(IOException e) {
					e.printStackTrace();
				}
	        }
		}
	}
	
	@Override
	public void toBytes(PacketBuffer buf) {
		if(EmailConfigs.isInfiniteSize()) {
			CompoundNBT nbt = new CompoundNBT();
			
			nbt.putUniqueId("owner", this.inbox.getOwnerAsUUID());
			CompoundNBT inbox = this.inbox.writeTo(CompoundNBT.class);
			inbox.remove("historySize");
			inbox.remove("dev");
			nbt.put("inbox", inbox);
			
			buf.writeCompoundTag(nbt);
		}else {
			SizeReport report = EmailUtils.checkInboxSize(this.inbox);
			if(!SizeReport.SUCCES.equals(report)) {
				CompoundNBT nbt = new CompoundNBT();
				
				nbt.putBoolean("ToBig", true);
				nbt.putLong("id", report.id);
				nbt.putInt("slot", report.slot);
				nbt.putLong("size", report.size);
				
				buf.writeCompoundTag(nbt);
				return;
			}
			
			CompoundNBT nbt = new CompoundNBT();
			nbt.putUniqueId("owner", this.inbox.getOwnerAsUUID());
			
			CompoundNBT inbox = this.inbox.writeTo(CompoundNBT.class);
			inbox.remove("historySize");
			inbox.remove("dev");
			nbt.put("inbox", inbox);
			
			buf.writeCompoundTag(nbt);
		}
	}

	@Override
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(EmailMain.proxy.isClient()) {
			ClientPlayerEntity player = Minecraft.getInstance().player;
			Container con = player.openContainer;
			if(con instanceof ContainerEmailMain) {
				if(!SizeReport.SUCCES.equals(this.report)) {
					player.sendMessage(new StringTextComponent(TextFormatting.GRAY + "---------------------------------------------"), player.getUniqueID());
					player.sendMessage(new StringTextComponent(I18n.format("info.email.error.to_big.0")), player.getUniqueID());
					player.sendMessage(new StringTextComponent(I18n.format("info.email.error.to_big.1", this.report.id, this.report.slot, this.report.size)), player.getUniqueID());
					player.closeScreenAndDropStack();
				}else {
					EmailMain.setUnread(this.inbox.getUnRead());
					EmailMain.setAccept(this.inbox.getUnReceived());
					((ContainerEmailMain) con).setInbox(this.inbox);
					((ContainerEmailMain) con).setRefresh(false);
				}
			}
		}
		return true;
	}
	
	public static class MsgOtherToClient extends BaseMessage {
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
		public void fromBytes(PacketBuffer buf) {
			CompoundNBT nbt = buf.readCompoundTag();

			CompoundNBT values = nbt.getCompound("values");
			values.keySet().forEach(k -> this.customValue.put(k, values.getString(k)));

			ListNBT blacks = nbt.getList("blacks", 8);
			blacks.forEach(e -> this.senderBlacklist.add(e.getString()));
		}
		
		@Override
		public void toBytes(PacketBuffer buf) {
			CompoundNBT nbt = new CompoundNBT();
			
			CompoundNBT values = new CompoundNBT();
			this.customValue.forEach((k,v) -> values.putString(k, String.valueOf(v)));
			nbt.put("values", values);

			ListNBT blacks = new ListNBT();
			this.senderBlacklist.forEach(e -> blacks.add(StringNBT.valueOf(e)));
			nbt.put("blacks", blacks);
			
			buf.writeCompoundTag(nbt);
		}

		@Override
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(EmailMain.proxy.isClient()) {
				Container con = Minecraft.getInstance().player.openContainer;
				
				if(con instanceof ContainerEmailMain) {
					this.customValue.forEach((k,v) -> ((ContainerEmailMain) con).getInbox().addCustom(k, v));
					this.senderBlacklist.forEach(e -> ((ContainerEmailMain) con).getInbox().addSenderBlacklist(e));
				}
			}
			return true;
		}
	}
}
