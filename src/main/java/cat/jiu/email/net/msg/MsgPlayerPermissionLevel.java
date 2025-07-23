package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.SideProxy;

import cat.jiu.email.EmailMain;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MsgPlayerPermissionLevel extends BaseMessage {
	public static final Type<MsgPlayerPermissionLevel> TYPE = type(EmailMain.MODID);
	private int level;
	public MsgPlayerPermissionLevel() {
		super(TYPE);
	}
	public MsgPlayerPermissionLevel(int level) {
		super(TYPE);
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
	
	public boolean handler(IPayloadContext context) {
		if(SideProxy.isClient()) {
			if(Minecraft.getInstance().player!=null) Minecraft.getInstance().player.setPermissionLevel(this.level);
		}
		return true;
	}
}
