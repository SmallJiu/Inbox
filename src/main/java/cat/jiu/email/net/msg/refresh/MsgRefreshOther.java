package cat.jiu.email.net.msg.refresh;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.SideProxy;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.net.msg.MsgInboxToClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MsgRefreshOther extends BaseMessage {
	public static final Type<MsgRefreshOther> TYPE = type(EmailMain.MODID);
	public MsgRefreshOther() {
		super(TYPE);
	}

	public void fromBytes(FriendlyByteBuf buf) {}
	public void toBytes(FriendlyByteBuf buf) {}
	
	public boolean handler(IPayloadContext ctx) {
		if(!SideProxy.isClient()) {
			Inbox inbox = Inbox.get(ctx.player());
			EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.SendOther(inbox.getCustomValue(), inbox.getSenderBlacklist()), (ServerPlayer) ctx.player());
		}
		return true;
	}
}
