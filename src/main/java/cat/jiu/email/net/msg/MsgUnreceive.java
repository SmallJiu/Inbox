package cat.jiu.email.net.msg;

import cat.jiu.email.EmailMain;

import io.netty.buffer.ByteBuf;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgUnreceive implements IMessage {
	protected int accept;
	public MsgUnreceive() {}
	public MsgUnreceive(int accept) {
		this.accept = accept;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		this.accept = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.accept);
	}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isClient()) {
			EmailMain.execute(args->{
				EmailMain.setAccept(this.accept);
			}, 100);
		}
		return null;
	}
}
