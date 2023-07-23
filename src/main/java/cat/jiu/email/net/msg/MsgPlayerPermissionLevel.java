package cat.jiu.email.net.msg;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailMain;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgPlayerPermissionLevel extends BaseMessage {
	private int level;
	public MsgPlayerPermissionLevel() {}
	public MsgPlayerPermissionLevel(int level) {
		this.level = level;
	}
	
	@Override
	public void fromBytes(PacketBuffer buf) {
		this.level = buf.readInt();
	}
	
	@Override
	public void toBytes(PacketBuffer buf) {
		buf.writeInt(this.level);
	}
	
	public boolean handler(Supplier<NetworkEvent.Context> context) {
		if(EmailMain.proxy.isClient()) {
			if(Minecraft.getInstance().player!=null) Minecraft.getInstance().player.setPermissionLevel(this.level);
		}
		return true;
	}
}
