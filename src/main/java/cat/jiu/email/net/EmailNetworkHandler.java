package cat.jiu.email.net;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.*;
import cat.jiu.email.net.msg.refresh.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class EmailNetworkHandler {
	private final SimpleChannel channel;
	
	private static int ID = 0;
	private static int nextID() {
		return ID++;
	}
	
	public EmailNetworkHandler() {
		ID = 0; 
		this.channel = NetworkRegistry.newSimpleChannel(
				new ResourceLocation(EmailMain.MODID, "main_network"),
				() -> EmailMain.VERSION,
				(version) -> version.equals(EmailMain.VERSION),
				(version) -> version.equals(EmailMain.VERSION)
		);

		this.register(MsgOpenGui.class, NetworkDirection.PLAY_TO_SERVER);

		this.register(MsgDeleteEmail.Delete.class, NetworkDirection.PLAY_TO_SERVER);
		this.register(MsgDeleteEmail.AllRead.class, NetworkDirection.PLAY_TO_SERVER);
		this.register(MsgDeleteEmail.AllReceive.class, NetworkDirection.PLAY_TO_SERVER);

		this.register(MsgReceiveEmail.Receive.class, NetworkDirection.PLAY_TO_SERVER);
		this.register(MsgReceiveEmail.All.class, NetworkDirection.PLAY_TO_SERVER);
		this.register(MsgUnreceived.class, NetworkDirection.PLAY_TO_CLIENT);

		this.register(MsgInboxToClient.class, NetworkDirection.PLAY_TO_CLIENT);
		this.register(MsgInboxToClient.MsgOtherToClient.class, NetworkDirection.PLAY_TO_CLIENT);

		this.register(MsgSend.class, NetworkDirection.PLAY_TO_SERVER);
		this.register(MsgSendRenderText.class, NetworkDirection.PLAY_TO_CLIENT);
		this.register(MsgSendCooling.class, NetworkDirection.PLAY_TO_SERVER);
		this.register(MsgSendCooling.class, NetworkDirection.PLAY_TO_CLIENT);
		this.register(MsgPlayerPermissionLevel.class, NetworkDirection.PLAY_TO_CLIENT);

		this.register(MsgReadEmail.class, NetworkDirection.PLAY_TO_SERVER);
		this.register(MsgReadEmail.All.class, NetworkDirection.PLAY_TO_SERVER);
		this.register(MsgUnread.class, NetworkDirection.PLAY_TO_CLIENT);

		this.register(MsgSendPlayerMessage.class, NetworkDirection.PLAY_TO_CLIENT);

		this.register(MsgBlacklist.Add.class, NetworkDirection.PLAY_TO_SERVER);
		this.register(MsgBlacklist.Remove.class, NetworkDirection.PLAY_TO_SERVER);

		this.register(MsgRefreshInbox.class, NetworkDirection.PLAY_TO_SERVER);
		this.register(MsgRefreshOther.class, NetworkDirection.PLAY_TO_SERVER);
		this.register(MsgRefreshBlacklist.class, NetworkDirection.PLAY_TO_CLIENT);
		this.register(MsgRefreshBlacklist.Refresh.class, NetworkDirection.PLAY_TO_SERVER);
	}

	private <T extends BaseMessage> void register(Class<T> msgClass, NetworkDirection sendTo) {
		this.channel.messageBuilder(msgClass, nextID(), sendTo)
				.encoder(T::toBytes)
				.decoder(buf ->{
					try {
						T instance = msgClass.newInstance();
						instance.fromBytes(buf);
						return instance;
					} catch (Exception e) {
						return null;
					}
				})
				.consumerNetworkThread(T::handler)
				.add();
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
