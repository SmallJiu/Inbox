package cat.jiu.email.net.msg.refresh;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.SideProxy;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.net.msg.MsgInboxToClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgRefreshOther extends BaseMessage {
	public void fromBytes(FriendlyByteBuf buf) {}
	public void toBytes(FriendlyByteBuf buf) {}
	
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(!SideProxy.isClient()) {
			Inbox inbox = Inbox.get(ctx.get().getSender());
			EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.SendOther(inbox.getCustomValue(), inbox.getSenderBlacklist()), ctx.get().getSender());
		}
		return true;
	}
}
