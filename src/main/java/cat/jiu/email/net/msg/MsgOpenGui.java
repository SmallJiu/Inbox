package cat.jiu.email.net.msg;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.ui.GuiHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgOpenGui extends BaseMessage {
	protected int guiID;

	public MsgOpenGui() {}
	public MsgOpenGui(int guiID) {
		this.guiID = guiID;
	}
	
	public void fromBytes(FriendlyByteBuf buf) {this.guiID = buf.readInt();}
	public void toBytes(FriendlyByteBuf buf) {buf.writeInt(this.guiID);}
	
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		EmailMain.runOnServerThread(()->
			GuiHandler.openGui(this.guiID, ctx.get().getSender())
		);
//		ctx.get().getSender().getServer().doRunTask(new TickTask(0, ()->GuiHandler.openGui(this.guiID, ctx.get().getSender())));
		return true;
	}
}
