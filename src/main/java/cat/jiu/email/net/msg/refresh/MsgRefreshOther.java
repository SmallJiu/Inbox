package cat.jiu.email.net.msg.refresh;

import cat.jiu.core.api.BaseMessage;
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
		if(!EmailMain.proxy.isClient()) {
			Inbox inbox = Inbox.get(ctx.get().getSender());
			EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.MsgOtherToClient(inbox.getCustomValue(), inbox.getSenderBlacklist()), ctx.get().getSender());
		}
		return true;
	}
}
