package cat.jiu.email.net;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.*;
import cat.jiu.email.net.msg.refresh.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.HashSet;

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
				EmailMain.VERSION::toString,
				EmailMain.VERSION::equals,
				EmailMain.VERSION::equals
		);

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
				.register(MsgSendCooling.class, NetworkDirection.PLAY_TO_CLIENT)
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

				.register(MsgScheduledEmail.Add.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgScheduledEmail.Remove.class, NetworkDirection.PLAY_TO_SERVER)

				.register(MsgRefreshScheduledEmail.Refresh.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgRefreshScheduledEmail.Send.class, NetworkDirection.PLAY_TO_CLIENT)
				.register(MsgRefreshScheduledEmail.RefreshMap.class, NetworkDirection.PLAY_TO_SERVER)
				.register(MsgRefreshScheduledEmail.SendMap.class, NetworkDirection.PLAY_TO_CLIENT)
		;
	}

	private static final HashMap<Class<? extends BaseMessage>, HashSet<NetworkDirection>> REGISTRY = new HashMap<>();
	private <T extends BaseMessage> EmailNetworkHandler register(Class<T> msgClass, NetworkDirection... sendTo) {
		for (NetworkDirection side : sendTo) {
			if (!REGISTRY.containsKey(msgClass) || !REGISTRY.get(msgClass).contains(side)) {
				this.channel.messageBuilder(msgClass, nextID(), side)
						.encoder(T::toBytes)
						.decoder(buf ->{
							try {
								T instance = msgClass.getDeclaredConstructor().newInstance();
								instance.fromBytes(buf);
								return instance;
							} catch (Exception e) {
								EmailMain.log.error("Can not read message from network. class: {}, side: {}", msgClass, side);
								e.printStackTrace();
								return null;
							}
						})
						.consumerNetworkThread(T::handler)
						.add();
				if (!REGISTRY.containsKey(msgClass)) {
					REGISTRY.put(msgClass, new HashSet<>());
				}
				REGISTRY.get(msgClass).add(side);
			}
		}
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
