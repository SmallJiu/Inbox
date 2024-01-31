package cat.jiu.email.net.msg.refresh;

import cat.jiu.email.element.Inbox;
import cat.jiu.email.net.BaseMessage;
import cat.jiu.email.net.msg.MsgInboxToClient;

import io.netty.buffer.ByteBuf;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgRefreshOther extends BaseMessage {
	public void fromBytes(ByteBuf buf) {}
	public void toBytes(ByteBuf buf) {}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isServer()) {
			return new MsgInboxToClient.MsgOtherToClient(Inbox.get(ctx.getServerHandler().player));
		}
		return null;
	}
}
