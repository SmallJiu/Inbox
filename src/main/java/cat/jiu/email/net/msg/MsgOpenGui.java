package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.ui.GuiHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MsgOpenGui extends BaseMessage {
	public static final Type<MsgOpenGui> TYPE = type(EmailMain.MODID);
	protected int guiID;

	public MsgOpenGui() {
		super(TYPE);
	}
	public MsgOpenGui(int guiID) {
		super(TYPE);
		this.guiID = guiID;
	}
	
	public void fromBytes(FriendlyByteBuf buf) {
		this.guiID = buf.readInt();
	}
	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(this.guiID);
	}
	
	public boolean handler(IPayloadContext ctx) {
		EmailMain.runOnServerThread(()->
				GuiHandler.openGui(this.guiID, (ServerPlayer) ctx.player())
		);
		return true;
	}
}
