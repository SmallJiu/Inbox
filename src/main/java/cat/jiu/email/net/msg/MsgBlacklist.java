package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;

import cat.jiu.email.net.msg.refresh.MsgRefreshBlacklist;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.util.EmailUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public abstract class MsgBlacklist extends BaseMessage {
	protected String name;

	public MsgBlacklist(Type<? extends BaseMessage> type) {
		super(type);
	}
	public MsgBlacklist(Type<? extends BaseMessage> type, String name) {
		super(type);
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
		public static final Type<Add> TYPE = type(EmailMain.MODID);
		public Add() {
			super(TYPE);
		}
		public Add(String name) {
			super(TYPE, name);
		}
		
		@Override
		public boolean handler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                Inbox inbox = Inbox.get(ctx.player());
                if (!inbox.isInSenderBlacklist(this.name)) {
                    inbox.addSenderBlacklist(this.name);
                    inbox.saveToDisk();
                    ctx.player().sendSystemMessage(EmailUtils.createTextComponent(ChatFormatting.GREEN, "info.inbox.black.add.success", name));
                    GuiHandler.openGui(GuiHandler.BLACKLIST, (ServerPlayer) ctx.player());
                }
            });
            return true;
		}
	}
	
	public static class Remove extends MsgBlacklist {
		public static final Type<Add> TYPE = type(EmailMain.MODID);
		public Remove() {
			super(TYPE);
		}
		public Remove(String name) {
			super(TYPE, name);
		}
		
		@Override
		public boolean handler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                Inbox inbox = Inbox.get(ctx.player());
                inbox.removeSenderBlacklist(this.name);
                inbox.saveToDisk();
                EmailMain.net.sendMessageToPlayer(new MsgRefreshBlacklist(inbox.getSenderBlacklist()), (ServerPlayer) ctx.player());
                ctx.player().sendSystemMessage(EmailUtils.createTextComponent(ChatFormatting.GREEN, "info.inbox.black.remove.success", this.name));
            });
            return true;
		}
	}
}
