package cat.jiu.email.net.msg;

import java.io.IOException;

import cat.jiu.email.element.Inbox;
import io.netty.buffer.ByteBuf;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class MsgBlacklist implements IMessage {
	protected String name;
	public MsgBlacklist() {}
	public MsgBlacklist(String name) {
		this.name = name;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		try {
			this.name = new PacketBuffer(buf).readCompoundTag().getString("name");
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("name", name);
		new PacketBuffer(buf).writeCompoundTag(nbt);
	}
	
	public abstract IMessage handler(MessageContext ctx);
	
	public static class Add extends MsgBlacklist {
		public Add() {}
		public Add(String name) {
			super(name);
		}
		
		@Override
		public IMessage handler(MessageContext ctx) {
			if(ctx.side.isServer()) {
				Inbox inbox = Inbox.get(ctx.getServerHandler().player);
				if(!inbox.isInSenderBlacklist(this.name)) {
					inbox.addSenderBlacklist(this.name);
					inbox.saveToDisk();
				}
			}
			return null;
		}
	}
	
	public static class Remove extends MsgBlacklist {
		public Remove() {}
		public Remove(String name) {
			super(name);
		}
		
		@Override
		public IMessage handler(MessageContext ctx) {
			if(ctx.side.isServer()) {
				Inbox inbox = Inbox.get(ctx.getServerHandler().player);
				inbox.removeSenderBlacklist(this.name);
				inbox.saveToDisk();
			}
			return null;
		}
	}
}
