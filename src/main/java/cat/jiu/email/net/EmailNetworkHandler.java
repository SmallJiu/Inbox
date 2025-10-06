package cat.jiu.email.net;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.net.BaseNetworkHandler;
import cat.jiu.core.util.Utils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.*;
import cat.jiu.email.net.msg.refresh.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;

public class EmailNetworkHandler extends BaseNetworkHandler {
	private static EmailNetworkHandler instance;
	public static EmailNetworkHandler getInstance() {
		if(instance==null)
			instance = new EmailNetworkHandler();
		return instance;
	}
	
	EmailNetworkHandler() {
		super(Utils.location(EmailMain.MODID, "main_network"), EmailMain.VERSION);

		this
				.register(MsgOpenGui.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgUnaccepted.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgToast.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgSendPlayerMessage.class, NetworkDirection.PLAY_TO_CLIENT)
//				.register(MsgSend.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgSendRenderText.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgDisplayInbox.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgUndying.class, NetworkDirection.PLAY_TO_CLIENT)

				.register(MsgDeleteEmail.Delete.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgDeleteEmail.AllRead.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgDeleteEmail.AllReceive.class, NetworkDirection.PLAY_TO_SERVER)

				.register(MsgReceiveEmail.Receive.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgReceiveEmail.All.class, NetworkDirection.PLAY_TO_SERVER)

				.register(MsgInboxToClient.SendEmail.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgInboxToClient.SendOther.class, NetworkDirection.PLAY_TO_CLIENT)

				.register(MsgSendCooling.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgSendCooling.class, NetworkDirection.PLAY_TO_CLIENT)

				.register(MsgReadEmail.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgReadEmail.All.class, NetworkDirection.PLAY_TO_SERVER)

				.register(MsgBlacklist.Add.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgBlacklist.Remove.class, NetworkDirection.PLAY_TO_SERVER)

				.register(MsgRefreshInbox.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgRefreshOther.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgRefreshBlacklist.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgRefreshBlacklist.Refresh.class, NetworkDirection.PLAY_TO_SERVER)

				.register(MsgScheduledEmail.Add.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgScheduledEmail.Remove.class, NetworkDirection.PLAY_TO_SERVER)

				.register(MsgRefreshScheduledEmail.Refresh.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgRefreshScheduledEmail.Send.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgRefreshScheduledEmail.RefreshMap.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgRefreshScheduledEmail.SendMap.class, NetworkDirection.PLAY_TO_CLIENT)

				.register(MsgSend0.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgSend0.MsgLock.class, NetworkDirection.PLAY_TO_CLIENT)
		;
	}

	@Override
	protected <T extends BaseMessage> EmailNetworkHandler register(T msg, NetworkDirection... sendTo) {
		super.register(msg, sendTo);
		return this;
	}

	@Override
	protected <T extends BaseMessage> EmailNetworkHandler register(Class<T> msgClass, Function<FriendlyByteBuf, T> decoder, NetworkDirection... sendTo) {
		super.register(msgClass, decoder, sendTo);
		return this;
	}

	@Override
	protected <T extends BaseMessage> EmailNetworkHandler register(Class<T> msgClass, NetworkDirection... sendTo) {
		super.register(msgClass, sendTo);
		return this;
	}

	/** server to client */
	public void sendMessageToPlayer(BaseMessage msg, ServerPlayer player) {
		if(msg!=null && player!=null)
			channel.send(PacketDistributor.PLAYER.with(()->player), msg);
	}
	
	/** client to server */
	public void sendMessageToServer(BaseMessage msg) {
		if(msg!=null) channel.sendToServer(msg);
	}
}
