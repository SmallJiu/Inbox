package cat.jiu.email.net.msg.refresh;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.net.BaseMessage;
import cat.jiu.email.net.msg.MsgInboxToClient;

import io.netty.buffer.ByteBuf;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgRefreshInbox extends BaseMessage {
	public void fromBytes(ByteBuf buf) {}
	public void toBytes(ByteBuf buf) {}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isServer()) {
			Inbox inbox = Inbox.get(ctx.getServerHandler().player);
			EmailMain.execute(()->
				EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.MsgOtherToClient(inbox), ctx.getServerHandler().player)
			, 50);
			return new MsgInboxToClient(inbox);
		}
		return null;
	}
}
