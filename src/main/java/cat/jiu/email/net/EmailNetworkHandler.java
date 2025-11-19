package cat.jiu.email.net;

import cat.jiu.core.net.BaseNetworkHandler;
import cat.jiu.core.util.Utils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.*;
import cat.jiu.email.net.msg.refresh.*;

public class EmailNetworkHandler extends BaseNetworkHandler {
	public EmailNetworkHandler() {
		super(Utils.location(EmailMain.MODID, "main_network"), EmailMain.VERSION);
		this
				.register(MsgOpenGui.class)
				.register(MsgUnaccepted.class)
				.register(MsgToast.class)
				.register(MsgSendPlayerMessage.class)
				.register(MsgSendRenderText.class)
				.register(MsgDisplayInbox.class)
				.register(MsgGenerateEmail.class)
				.register(MsgUndying.class)
				.register(MsgSendCooling.class)
				.register(MsgEmailFiles.class)

				.register(MsgDeleteEmail.Delete.class)
				.register(MsgDeleteEmail.AllRead.class)
				.register(MsgDeleteEmail.AllReceive.class)

				.register(MsgReceiveEmail.Receive.class)
				.register(MsgReceiveEmail.All.class)

				.register(MsgInboxToClient.SendEmail.class)
				.register(MsgInboxToClient.SendOther.class)

				.register(MsgReadEmail.class)
				.register(MsgReadEmail.All.class)

				.register(MsgBlacklist.Add.class)
				.register(MsgBlacklist.Remove.class)

				.register(MsgRefreshInbox.class)
				.register(MsgRefreshOther.class)
				.register(MsgRefreshBlacklist.class)
				.register(MsgRefreshBlacklist.Refresh.class)

				.register(MsgScheduledEmail.Add.class)
				.register(MsgScheduledEmail.Remove.class)

				.register(MsgSend.class)
				.register(MsgSend.MsgLock.class)

				.register(MsgRefreshScheduledEmail.Refresh.class)
				.register(MsgRefreshScheduledEmail.Send.class)
				.register(MsgRefreshScheduledEmail.RefreshMap.class)
				.register(MsgRefreshScheduledEmail.SendMap.class)
		;
	}
}
