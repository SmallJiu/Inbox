package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.SideProxy;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgPlayerPermissionLevel extends BaseMessage {
	private int level;
	public MsgPlayerPermissionLevel() {}
	public MsgPlayerPermissionLevel(int level) {
		this.level = level;
	}
	
	@Override
	public void fromBytes(FriendlyByteBuf buf) {
		this.level = buf.readInt();
	}
	
	@Override
	public void toBytes(FriendlyByteBuf buf) {
		buf.writeInt(this.level);
	}
	
	public boolean handler(Supplier<NetworkEvent.Context> context) {
		if(SideProxy.isClient()) {
			if(Minecraft.getInstance().player!=null) Minecraft.getInstance().player.setPermissionLevel(this.level);
		}
		return true;
	}
}
