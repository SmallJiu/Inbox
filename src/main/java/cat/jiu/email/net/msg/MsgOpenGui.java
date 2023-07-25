package cat.jiu.email.net.msg;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.ui.GuiHandler;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgOpenGui extends BaseMessage {
	protected int guiID;

	public MsgOpenGui() {}
	public MsgOpenGui(int guiID) {
		this.guiID = guiID;
	}
	
	public void fromBytes(PacketBuffer buf) {this.guiID = buf.readInt();}
	public void toBytes(PacketBuffer buf) {buf.writeInt(this.guiID);}
	
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		GuiHandler.openGui(this.guiID, ctx.get().getSender());
		return true;
	}
}
