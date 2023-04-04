package cat.jiu.email.net.msg;

import cat.jiu.email.ui.container.ContainerEmailSend;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SendCooling implements IMessage {
	protected long millis;
	
	public SendCooling() {}
	public SendCooling(long millis) {
		this.millis = millis;
	}
	@Override
	public void fromBytes(ByteBuf buf) {
		this.millis = buf.readLong();
	}
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(this.millis);
	}
	public IMessage handler(MessageContext ctx) {
		Container con = Minecraft.getMinecraft().player.openContainer;
		if(con instanceof ContainerEmailSend) {
			((ContainerEmailSend)con).setCooling(this.millis);
		}
		return null;
	}
}
