package cat.jiu.email.net.msg;

import cat.jiu.email.net.BaseMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgPlayerPermissionLevel extends BaseMessage {
	private int level;
	public MsgPlayerPermissionLevel() {}
	public MsgPlayerPermissionLevel(int level) {
		this.level = level;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		this.level = buf.readInt();
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.level);
	}
	
	public IMessage handler(MessageContext context) {
		if(context.side.isClient()) {
			if(Minecraft.getMinecraft().player!=null) Minecraft.getMinecraft().player.setPermissionLevel(this.level);
		}
		return null;
	}
}
