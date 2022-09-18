package cat.jiu.email.net.msg;

import cat.jiu.email.ui.container.ContainerEmailSend;
import cat.jiu.email.util.EmailUtils;

import io.netty.buffer.ByteBuf;

import net.minecraft.inventory.Container;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgCloseSendGui implements IMessage {
	public MsgCloseSendGui() {}
	public void fromBytes(ByteBuf buf) {}
	public void toBytes(ByteBuf buf) {}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isServer()) {
			Container con = ctx.getServerHandler().player.openContainer;
			if(con instanceof ContainerEmailSend) {
				EmailUtils.spawnAsEntity(ctx.getServerHandler().player, ((ContainerEmailSend) con).toItemList());
			}
		}
		return null;
	}
}
