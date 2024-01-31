package cat.jiu.email.net.msg.refresh;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

import cat.jiu.email.element.Inbox;
import cat.jiu.email.net.BaseMessage;
import cat.jiu.email.ui.container.ContainerInboxBlacklist;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgRefreshBlacklist extends BaseMessage {
	private final List<String> senderBlacklist = Lists.newArrayList();
	public MsgRefreshBlacklist() {}
	public MsgRefreshBlacklist(Inbox inbox) {
		this.senderBlacklist.addAll(inbox.getSenderBlacklist());
	}
	public MsgRefreshBlacklist(List<String> senderBlacklist) {
		this.senderBlacklist.addAll(senderBlacklist);
	}
	
	public void fromBytes(ByteBuf buf) {
		try {
			NBTTagCompound nbt = new PacketBuffer(buf).readCompoundTag();
			
			NBTTagList blacks = nbt.getTagList("blacks", 8);
			blacks.forEach(e -> this.senderBlacklist.add(((NBTTagString)e).getString()));
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void toBytes(ByteBuf buf) {
		NBTTagCompound nbt = new NBTTagCompound();
		
		NBTTagList blacks = new NBTTagList();
		this.senderBlacklist.forEach(e -> blacks.appendTag(new NBTTagString(e)));
		nbt.setTag("blacks", blacks);
		
		new PacketBuffer(buf).writeCompoundTag(nbt);
	}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isClient()) {
			Container con = Minecraft.getMinecraft().player.openContainer;
			
			if(con instanceof ContainerInboxBlacklist) {
				this.senderBlacklist.forEach(e -> ((ContainerInboxBlacklist) con).addBlacklist(e));
			}
		}
		return null;
	}
	
	public static class Refresh extends BaseMessage {
		public void fromBytes(ByteBuf buf) {}
		public void toBytes(ByteBuf buf) {}
		public IMessage handler(MessageContext ctx) {
			if(ctx.side.isServer()) {
				return new MsgRefreshBlacklist(Inbox.get(ctx.getServerHandler().player));
			}
			return null;
		}
	}
}
