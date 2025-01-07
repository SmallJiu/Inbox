package cat.jiu.core.net;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.HashSet;

public class BaseNetworkHandler {

	private final SimpleChannel channel;
	
	private int ID = 0;
	private int nextID() {
		return ID++;
	}
	
	protected BaseNetworkHandler(ResourceLocation id, String version) {
		this(id, version, version, version);
	}
	protected BaseNetworkHandler(ResourceLocation id, String networkVersion, String clientVersion, String serverVersion) {
		ID = 0;
		this.channel = NetworkRegistry.newSimpleChannel(
				id,
				networkVersion::toString,
				clientVersion::equals,
				serverVersion::equals
		);
	}

	private final HashMap<Class<? extends BaseMessage>, HashSet<NetworkDirection>> REGISTRY = new HashMap<>();
	public   <T extends BaseMessage> BaseNetworkHandler register(Class<T> msgClass, NetworkDirection... sendTo) {
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
								return null;
							}
						})
						.consumer(T::handler)
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
	public void sendMessageToPlayer(BaseMessage msg, ServerPlayerEntity player) {
		if(msg!=null && player!=null)
			channel.send(PacketDistributor.PLAYER.with(()->player), msg);
	}

	/** client to server */
	public void sendMessageToServer(BaseMessage msg) {
		if(msg!=null) channel.sendToServer(msg);
	}
}
