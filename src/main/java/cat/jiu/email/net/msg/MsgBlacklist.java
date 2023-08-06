package cat.jiu.email.net.msg;

import java.util.function.Supplier;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;

import cat.jiu.email.net.msg.refresh.MsgRefreshBlacklist;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.util.EmailUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public abstract class MsgBlacklist extends BaseMessage {
	protected String name;
	public MsgBlacklist() {}
	public MsgBlacklist(String name) {
		this.name = name;
	}
	
	@Override
	public void fromBytes(FriendlyByteBuf buf) {
		this.name = buf.readNbt().getString("name");
	}

	@Override
	public void toBytes(FriendlyByteBuf buf) {
		CompoundTag nbt = new CompoundTag();
		nbt.putString("name", name);
		new FriendlyByteBuf(buf).writeNbt(nbt);
	}
	
	public static class Add extends MsgBlacklist {
		public Add() {}
		public Add(String name) {
			super(name);
		}
		
		@Override
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(ctx.get().getSender() != null) {
				ctx.get().enqueueWork(()->{
					Inbox inbox = Inbox.get(ctx.get().getSender());
					if(!inbox.isInSenderBlacklist(this.name)) {
						inbox.addSenderBlacklist(this.name);
						inbox.saveToDisk();
						ctx.get().getSender().sendSystemMessage(EmailUtils.createTextComponent(ChatFormatting.GREEN, "info.email.black.add.success", name));
						GuiHandler.openGui(GuiHandler.EMAIL_BLACKLIST, ctx.get().getSender());
					}
				});
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
				ctx.get().enqueueWork(()->{
					Inbox inbox = Inbox.get(ctx.get().getSender());
					inbox.removeSenderBlacklist(this.name);
					inbox.saveToDisk();
					EmailMain.net.sendMessageToPlayer(new MsgRefreshBlacklist(inbox.getSenderBlacklist()), ctx.get().getSender());
					ctx.get().getSender().sendSystemMessage(EmailUtils.createTextComponent(ChatFormatting.GREEN, "info.email.black.remove.success", this.name));
				});
			}
			return true;
		}
	}
}
