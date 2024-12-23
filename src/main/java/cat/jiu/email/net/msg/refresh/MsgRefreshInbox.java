package cat.jiu.email.net.msg.refresh;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.net.msg.MsgInboxToClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgRefreshInbox extends BaseMessage {
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(ctx.get().getSender() != null) {
			EmailAPI.sendInboxToClient(Inbox.get(ctx.get().getSender()), ctx.get().getSender());
			return true;
		}
		return false;
	}

	@Override
	public void toBytes(FriendlyByteBuf buffer) {}

	@Override
	public void fromBytes(FriendlyByteBuf buf) {}
}
