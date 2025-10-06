package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.SideProxy;

import cat.jiu.email.ui.gui.GuiInbox;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgSendCooling extends BaseMessage {
	protected long millis;
	
	public MsgSendCooling() {}
	public MsgSendCooling(long millis) {
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
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(SideProxy.isClient()){
			GuiInbox.INBOX.setSendCooling(this.millis);
		}
		return true;
	}
}
