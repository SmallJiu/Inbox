package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.EmailMain;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgUnaccepted extends BaseMessage {
	protected int unread, unreceive;
	public MsgUnaccepted() {}
	public MsgUnaccepted(int unread, int unreceive) {
		this.unread = unread;
		this.unreceive = unreceive;
	}
	
	public void fromBytes(FriendlyByteBuf buf) {
		this.unread = buf.readInt();
		this.unreceive = buf.readInt();
	}
	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(this.unread);
		buf.writeInt(this.unreceive);
	}
	
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(EmailMain.proxy.isClient()) {
			EmailMain.setAccept(this.unread, this.unreceive);
		}
		return true;
	}
}
