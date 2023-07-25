package cat.jiu.email.net.msg;

import java.util.function.Supplier;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;

import cat.jiu.email.net.msg.refresh.MsgRefreshBlacklist;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.util.EmailUtils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.NetworkEvent;

public abstract class MsgBlacklist extends BaseMessage {
	protected String name;
	public MsgBlacklist() {}
	public MsgBlacklist(String name) {
		this.name = name;
	}
	
	@Override
	public void fromBytes(PacketBuffer buf) {
		this.name = buf.readCompoundTag().getString("name");
	}

	@Override
	public void toBytes(PacketBuffer buf) {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putString("name", name);
		new PacketBuffer(buf).writeCompoundTag(nbt);
	}
	
	public static class Add extends MsgBlacklist {
		public Add() {}
		public Add(String name) {
			super(name);
		}
		
		@Override
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(ctx.get().getSender() != null) {
				Inbox inbox = Inbox.get(ctx.get().getSender());
				if(!inbox.isInSenderBlacklist(this.name)) {
					inbox.addSenderBlacklist(this.name);
					inbox.saveToDisk();
					ctx.get().getSender().sendMessage(EmailUtils.createTextComponent(TextFormatting.GREEN, "info.email.black.add.success", name), ctx.get().getSender().getUniqueID());
					GuiHandler.openGui(GuiHandler.EMAIL_BLACKLIST, ctx.get().getSender());
				}
			}
			return true;
		}
	}
	
	public static class Remove extends MsgBlacklist {
		public Remove() {}
		public Remove(String name) {
			super(name);
		}
		
		@Override
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(ctx.get().getSender() != null) {
				Inbox inbox = Inbox.get(ctx.get().getSender());
				inbox.removeSenderBlacklist(this.name);
				inbox.saveToDisk();
				EmailMain.net.sendMessageToPlayer(new MsgRefreshBlacklist(inbox.getSenderBlacklist()), ctx.get().getSender());
				ctx.get().getSender().sendMessage(EmailUtils.createTextComponent(TextFormatting.GREEN, "info.email.black.remove.success", this.name), ctx.get().getSender().getUniqueID());
			}
			return true;
		}
	}
}
