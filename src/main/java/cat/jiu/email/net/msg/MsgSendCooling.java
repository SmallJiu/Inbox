package cat.jiu.email.net.msg;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.ui.container.ContainerEmailSend;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.container.Container;
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
		if(EmailMain.proxy.isClient()){
			Container con = Minecraft.getInstance().player.openContainer;
			if(con instanceof ContainerEmailSend) {
				((ContainerEmailSend)con).setCooling(this.millis);
			}
		}
		return true;
	}
}
