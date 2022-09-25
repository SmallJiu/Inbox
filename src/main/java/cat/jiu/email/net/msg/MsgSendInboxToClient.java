package cat.jiu.email.net.msg;

import java.io.IOException;

import javax.annotation.Nonnull;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.util.EmailSizeReport;
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
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgSendInboxToClient implements IMessage {
	protected Inbox inbox;
	protected EmailSizeReport report = EmailSizeReport.SUCCES;
	public MsgSendInboxToClient() {}
	public MsgSendInboxToClient(@Nonnull Inbox inbox) {
		this.inbox = inbox;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		if(EmailUtils.isInfiniteSize()) {
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
							this.report = new EmailSizeReport(inboxTag.getInteger("id"), inboxTag.getInteger("slot"), inboxTag.getInteger("size"));
							return;
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
		
		if(EmailUtils.isInfiniteSize()) {
			NBTTagCompound inbox = new NBTTagCompound();
			inbox.setUniqueId("owner", this.inbox.getOwnerAsUUID());
			inbox.setTag("inbox", this.inbox.write(new NBTTagCompound()));
			pb.writeCompoundTag(inbox);
		}else {
			EmailSizeReport report = EmailUtils.checkInboxSize(this.inbox);
			if(!EmailSizeReport.SUCCES.equals(report)) {
				NBTTagCompound nbt = new NBTTagCompound();
				
				nbt.setBoolean("ToBig", true);
				nbt.setInteger("id", report.id);
				nbt.setInteger("slot", report.slot);
				nbt.setLong("size", report.size);
				
				pb.writeCompoundTag(nbt);
				return;
			}
			NBTTagCompound inbox = new NBTTagCompound();
			inbox.setUniqueId("owner", this.inbox.getOwnerAsUUID());
			inbox.setTag("inbox", this.inbox.write(new NBTTagCompound()));
			pb.writeCompoundTag(inbox);
		}
	}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isClient()) {
			EntityPlayerSP player = Minecraft.getMinecraft().player;
			Container con = player.openContainer;
			if(con instanceof ContainerEmailMain) {
				if(!EmailSizeReport.SUCCES.equals(this.report)) {
					player.sendMessage(new TextComponentString(TextFormatting.GRAY + "---------------------------------------------"));
					player.sendMessage(new TextComponentString(I18n.format("info.email.error.to_big.0")));
					player.sendMessage(new TextComponentString(I18n.format("info.email.error.to_big.1", this.report.id, this.report.slot, this.report.size)));
					player.closeScreen();
				}else {
					EmailMain.setUnread(this.inbox.getUnRead());
					EmailMain.setAccept(this.inbox.getUnReceived());
					((ContainerEmailMain) con).setInbox(this.inbox);
				}
			}
		}
		return null;
	}
}
