package cat.jiu.email.net.msg;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailMain;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgUnread extends BaseMessage {
	protected int unread;
	public MsgUnread() {}
	public MsgUnread(int unread) {
		this.unread = unread;
	}
	
	public void fromBytes(PacketBuffer buf) {this.unread = buf.readInt();}
	public void toBytes(PacketBuffer buf) {buf.writeInt(this.unread);}
	
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(EmailMain.proxy.isClient()) {
			EmailMain.setUnread(this.unread);
		}
		return true;
	}
}
