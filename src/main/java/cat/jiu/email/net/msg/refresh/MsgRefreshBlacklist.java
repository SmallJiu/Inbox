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
import net.minecraft.inventory.container.Container;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.PacketBuffer;

import net.minecraftforge.fml.network.NetworkEvent;

public class MsgRefreshBlacklist extends BaseMessage {
	private final List<String> senderBlacklist = Lists.newArrayList();
	public MsgRefreshBlacklist() {}
	public MsgRefreshBlacklist(Inbox inbox) {
		this.senderBlacklist.addAll(inbox.getSenderBlacklist());
	}
	public MsgRefreshBlacklist(List<String> senderBlacklist) {
		this.senderBlacklist.addAll(senderBlacklist);
	}
	
	public void fromBytes(PacketBuffer buf) {
		ListNBT blacks = buf.readCompoundTag().getList("blacks", 8);
		blacks.forEach(e -> this.senderBlacklist.add((e.getString())));
	}
	
	public void toBytes(PacketBuffer buf) {
		CompoundNBT nbt = new CompoundNBT();
		
		ListNBT blacks = new ListNBT();
		this.senderBlacklist.forEach(e -> blacks.add(StringNBT.valueOf(e)));
		nbt.put("blacks", blacks);
		
		buf.writeCompoundTag(nbt);
	}
	
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(EmailMain.proxy.isClient()) {
			Container con = Minecraft.getInstance().player.openContainer;
			if(con instanceof ContainerInboxBlacklist) {
				((ContainerInboxBlacklist) con).setBlacklist(this.senderBlacklist);
				if(Minecraft.getInstance().currentScreen instanceof GuiBlacklist){
					((GuiBlacklist)Minecraft.getInstance().currentScreen).goName(0);
				}
			}
		}
		return true;
	}

	public static class Refresh extends BaseMessage {
		public void fromBytes(PacketBuffer buf) {}
		public void toBytes(PacketBuffer buf) {}
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(ctx.get().getSender() != null) {
				EmailMain.net.sendMessageToPlayer(new MsgRefreshBlacklist(Inbox.get(ctx.get().getSender())), ctx.get().getSender());
			}
			return true;
		}
	}
}
