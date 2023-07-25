package cat.jiu.email.net.msg.refresh;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.net.msg.MsgInboxToClient;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgRefreshInbox extends BaseMessage {
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(ctx.get().getSender() != null) {
			Inbox inbox = Inbox.get(ctx.get().getSender());
			EmailMain.net.sendMessageToPlayer(new MsgInboxToClient(inbox), ctx.get().getSender());
			EmailMain.execute(()->
				EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.MsgOtherToClient(inbox), ctx.get().getSender())
			, 50);
		}
		return true;
	}

	@Override
	public void toBytes(PacketBuffer buffer) {}

	@Override
	public void fromBytes(PacketBuffer buf) {}
}
