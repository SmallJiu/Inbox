package cat.jiu.email.net.msg.refresh;

import java.util.List;
import java.util.function.Supplier;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.ui.gui.GuiBlacklist;
import com.google.common.collect.Lists;

import cat.jiu.email.element.Inbox;
import cat.jiu.email.ui.container.ContainerInboxBlacklist;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

public class MsgRefreshBlacklist extends BaseMessage {
	private final List<String> senderBlacklist = Lists.newArrayList();
	public MsgRefreshBlacklist() {}
	public MsgRefreshBlacklist(Inbox inbox) {
		this.senderBlacklist.addAll(inbox.getSenderBlacklist());
	}
	public MsgRefreshBlacklist(List<String> senderBlacklist) {
		this.senderBlacklist.addAll(senderBlacklist);
	}
	
	public void fromBytes(FriendlyByteBuf buf) {
		ListTag blacks = buf.readNbt().getList("blacks", 8);
		blacks.forEach(e -> this.senderBlacklist.add((e.getAsString())));
	}
	
	public void toBytes(FriendlyByteBuf buf) {
		CompoundTag nbt = new CompoundTag();
		
		ListTag blacks = new ListTag();
		this.senderBlacklist.forEach(e -> blacks.add(StringTag.valueOf(e)));
		nbt.put("blacks", blacks);
		
		buf.writeNbt(nbt);
	}
	
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(EmailMain.proxy.isClient()) {
			AbstractContainerMenu con = Minecraft.getInstance().player.containerMenu;
			if(con instanceof ContainerInboxBlacklist) {
				((ContainerInboxBlacklist) con).setBlacklist(this.senderBlacklist);
				if(Minecraft.getInstance().screen instanceof GuiBlacklist){
					((GuiBlacklist)Minecraft.getInstance().screen).goName(0);
				}
			}
		}
		return true;
	}

	public static class Refresh extends BaseMessage {
		public void fromBytes(FriendlyByteBuf buf) {}
		public void toBytes(FriendlyByteBuf buf) {}
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(ctx.get().getSender() != null) {
				EmailMain.net.sendMessageToPlayer(new MsgRefreshBlacklist(Inbox.get(ctx.get().getSender())), ctx.get().getSender());
			}
			return true;
		}
	}
}
