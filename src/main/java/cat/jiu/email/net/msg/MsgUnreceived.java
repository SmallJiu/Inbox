package cat.jiu.email.net.msg;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailMain;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgUnreceived extends BaseMessage {
	protected int accept;
	public MsgUnreceived() {}
	public MsgUnreceived(int accept) {
		this.accept = accept;
	}
	
	public void fromBytes(FriendlyByteBuf buf) {this.accept = buf.readInt();}
	public void toBytes(FriendlyByteBuf buf) {buf.writeInt(this.accept);}
	
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(EmailMain.proxy.isClient()) {
			EmailMain.setAccept(this.accept);
		}
		return true;
	}
}
