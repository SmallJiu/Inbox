package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.SideProxy;
import cat.jiu.email.EmailMain;
import cat.jiu.email.ui.container.ContainerEmailSend;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MsgSendCooling extends BaseMessage {
	public static final Type<MsgSendCooling> TYPE = type(EmailMain.MODID);
	protected long millis;
	public MsgSendCooling() {
		super(TYPE);
	}
	public MsgSendCooling(long millis) {
		super(TYPE);
		this.millis = millis;
	}
	@Override
	public void fromBytes(FriendlyByteBuf buf) {
		this.millis = buf.readLong();
	}
	@Override
	public void toBytes(FriendlyByteBuf buf) {
		buf.writeLong(this.millis);
	}
	public boolean handler(IPayloadContext ctx) {
		if(SideProxy.isClient()){
			if(ctx.player().containerMenu instanceof ContainerEmailSend container) {
				container.setCooling(this.millis);
			}
		}
		return true;
	}
}
