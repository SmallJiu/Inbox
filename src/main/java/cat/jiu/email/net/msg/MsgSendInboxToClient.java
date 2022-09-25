package cat.jiu.email.net.msg;

import java.io.IOException;

import javax.annotation.Nonnull;

import com.google.gson.JsonObject;

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
	protected Inbox email;
	protected EmailSizeReport report = EmailSizeReport.SUCCES;
	public MsgSendInboxToClient() {}
	public MsgSendInboxToClient(@Nonnull Inbox email) {
		this.email = email;
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
						this.email = new Inbox(nbt.getUniqueId("owner"), nbt.getCompoundTag("inbox"));
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
					NBTTagCompound nbt = CompressedStreamTools.read(new ByteBufInputStream(pb), new NBTSizeTracker(Long.MAX_VALUE));
					if(nbt != null) {
						if(nbt.hasKey("ToBig")) {
							this.report = new EmailSizeReport(nbt.getInteger("msgID"), nbt.getInteger("slot"), nbt.getInteger("size"));
							return;
						}else {
							NBTTagCompound inboxTag = new PacketBuffer(buf).readCompoundTag();
							this.email = new Inbox(inboxTag.getUniqueId("owner"), inboxTag.getCompoundTag("inbox"));
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
			pb.writeCompoundTag(this.email.write(new NBTTagCompound()));
		}else {
			EmailSizeReport report = EmailUtils.checkEmailSize(this.email.write(new JsonObject()));
			if(!EmailSizeReport.SUCCES.equals(report)) {
				NBTTagCompound nbt = new NBTTagCompound();
				
				nbt.setBoolean("ToBig", true);
				nbt.setInteger("msgID", report.msgID);
				nbt.setInteger("slot", report.itemSlot);
				nbt.setLong("size", report.size);
				
				pb.writeCompoundTag(nbt);
				return;
			}
			NBTTagCompound inbox = new NBTTagCompound();
			inbox.setTag("inbox", this.email.write(new NBTTagCompound()));
			inbox.setUniqueId("owner", Minecraft.getMinecraft().player.getUniqueID());
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
					player.sendMessage(new TextComponentString(I18n.format("info.email.error.to_big.1", this.report.msgID, this.report.itemSlot, this.report.size)));
					player.closeScreen();
				}else {
					EmailMain.setUnread(this.email.getUnRead());
					EmailMain.setAccept(this.email.getUnReceived());
					((ContainerEmailMain) con).setMsgs(this.email);
				}
			}
		}
		return null;
	}
}
