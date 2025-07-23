package cat.jiu.email.net.msg.refresh;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MsgRefreshInbox extends BaseMessage {
	public static final Type<MsgRefreshInbox>  TYPE = type(EmailMain.MODID);
	public MsgRefreshInbox() {
		super(TYPE);
	}

	public boolean handler(IPayloadContext ctx) {
		EmailAPI.sendInboxToClient(Inbox.get(ctx.player()), (ServerPlayer) ctx.player());
		return true;
	}

	@Override
	public void toBytes(FriendlyByteBuf buffer) {}

	@Override
	public void fromBytes(FriendlyByteBuf buf) {}
}
