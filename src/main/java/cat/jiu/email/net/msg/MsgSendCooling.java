package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.SideProxy;
import cat.jiu.email.ui.container.ContainerEmailSend;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgSendCooling extends BaseMessage {
	protected long millis;
	
	public MsgSendCooling() {}
	public MsgSendCooling(long millis) {
		this.millis = millis;
	}
	@Override
	public void fromBytes(PacketBuffer buf) {
		this.millis = buf.readLong();
	}
	@Override
	public void toBytes(PacketBuffer buf) {
		buf.writeLong(this.millis);
	}
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(SideProxy.isClient()){
			if(Minecraft.getInstance().player.openContainer instanceof ContainerEmailSend) {
				((ContainerEmailSend) Minecraft.getInstance().player.openContainer).setCooling(this.millis);
			}
		}
		return true;
	}
}
