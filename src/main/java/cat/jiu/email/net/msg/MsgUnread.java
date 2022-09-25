package cat.jiu.email.net.msg;

import cat.jiu.email.EmailMain;

import io.netty.buffer.ByteBuf;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgUnread implements IMessage {
	protected int unread;
	public MsgUnread() {}
	public MsgUnread(int unread) {
		this.unread = unread;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		this.unread = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.unread);
	}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isClient()) {
			EmailMain.execute(args->{
				EmailMain.setUnread(this.unread);
			}, 100);
		}
		return null;
	}
}
