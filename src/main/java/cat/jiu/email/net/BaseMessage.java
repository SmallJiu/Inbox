package cat.jiu.email.net;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class BaseMessage implements IMessage {
	public abstract IMessage handler(MessageContext ctx);
}
