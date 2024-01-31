package cat.jiu.email.net.msg;

import cat.jiu.email.EmailMain;
import cat.jiu.email.net.BaseMessage;
import io.netty.buffer.ByteBuf;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgUnread extends BaseMessage {
	protected int unread;
	public MsgUnread() {}
	public MsgUnread(int unread) {
		this.unread = unread;
	}
	
	public void fromBytes(ByteBuf buf) {this.unread = buf.readInt();}
	public void toBytes(ByteBuf buf) {buf.writeInt(this.unread);}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isClient()) {
			EmailMain.setUnread(this.unread);
		}
		return null;
	}
}
