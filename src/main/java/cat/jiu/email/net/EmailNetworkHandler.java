package cat.jiu.email.net;

import cat.jiu.core.net.BaseNetworkHandler;
import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.*;
import cat.jiu.email.net.msg.refresh.*;
import net.minecraft.resources.ResourceLocation;

public class EmailNetworkHandler extends BaseNetworkHandler {
	private static EmailNetworkHandler INSTANCE;
	public static EmailNetworkHandler getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new EmailNetworkHandler();
		}
		return INSTANCE;
	}

	EmailNetworkHandler() {
		super(ResourceLocation.fromNamespaceAndPath(EmailMain.MODID, "main_network"));
	}

	@Override
	protected void registerMessage() {
		this
				.register(MsgOpenGui.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgDeleteEmail.Delete.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgDeleteEmail.AllRead.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgDeleteEmail.AllReceive.class, NetworkDirection.PLAY_TO_SERVER)

				.register(MsgReceiveEmail.Receive.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgReceiveEmail.All.class, NetworkDirection.PLAY_TO_SERVER)

				.register(MsgUnaccepted.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgToast.class, NetworkDirection.PLAY_TO_CLIENT)

				.register(MsgInboxToClient.SendEmail.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgInboxToClient.SendOther.class, NetworkDirection.PLAY_TO_CLIENT)

				.register(MsgSend.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgSendRenderText.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgSendCooling.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgPlayerPermissionLevel.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgReadEmail.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgReadEmail.All.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgSendPlayerMessage.class, NetworkDirection.PLAY_TO_CLIENT)

				.register(MsgBlacklist.Add.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgBlacklist.Remove.class, NetworkDirection.PLAY_TO_SERVER)

				.register(MsgRefreshInbox.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgRefreshOther.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgRefreshBlacklist.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgRefreshBlacklist.Refresh.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgDisplayInbox.class, NetworkDirection.PLAY_TO_SERVER)

				.register(MsgScheduledEmail.Add.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgScheduledEmail.Remove.class, NetworkDirection.PLAY_TO_SERVER)

				.register(MsgRefreshScheduledEmail.Refresh.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgRefreshScheduledEmail.Send.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgRefreshScheduledEmail.RefreshMap.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgRefreshScheduledEmail.SendMap.class, NetworkDirection.PLAY_TO_CLIENT)
		;
	}
}
