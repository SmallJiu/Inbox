package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.SideProxy;
import cat.jiu.email.EmailMain;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class MsgUnaccepted extends BaseMessage {
	public static final Type<MsgUnaccepted> TYPE = type(EmailMain.MODID);
	protected int unread, unreceive;
	public MsgUnaccepted() {
		super(TYPE);
	}
	public MsgUnaccepted(int unread, int unreceive) {
		super(TYPE);
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
	
	public boolean handler(IPayloadContext ctx) {
		if(SideProxy.isClient()) {
			EmailMain.setAccept(this.unread, this.unreceive);
		}
		return true;
	}
}
