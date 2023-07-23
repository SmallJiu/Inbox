package cat.jiu.email.net.msg;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.ui.EmailGuiHandler;
import cat.jiu.email.ui.container.ContainerEmailMain;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;

import net.minecraft.entity.player.ServerPlayerEntity;
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
		EmailGuiHandler.openGui(this.guiID, ctx.get().getSender());
		return true;
	}
}
