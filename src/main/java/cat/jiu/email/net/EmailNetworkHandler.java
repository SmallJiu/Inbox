package cat.jiu.email.net;

import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.*;
import cat.jiu.email.net.msg.refresh.*;
import net.minecraft.entity.player.EntityPlayerMP;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class EmailNetworkHandler {
	private final SimpleNetworkWrapper channel;
	
	private static int ID = 0;
	private static int nextID() {
		return ID++;
	}
	
	public EmailNetworkHandler() {
		ID = 0; 
		this.channel = NetworkRegistry.INSTANCE.newSimpleChannel(EmailMain.MODID);

		this.register(MsgOpenGui.class, Side.CLIENT, Side.SERVER);
		
		this.register(MsgDeleteEmail.Delete.class, Side.SERVER);
		this.register(MsgDeleteEmail.AllRead.class, Side.SERVER);
		this.register(MsgDeleteEmail.AllReceive.class, Side.SERVER);
		
		this.register(MsgReceiveEmail.Receive.class, Side.SERVER);
		this.register(MsgReceiveEmail.All.class, Side.SERVER);
		this.register(MsgUnreceive.class, Side.CLIENT);
		
		this.register(MsgInboxToClient.class, Side.CLIENT);
		this.register(MsgInboxToClient.MsgOtherToClient.class, Side.CLIENT);
		
		this.register(MsgSend.class, Side.SERVER);
		this.register(MsgSendRenderText.class, Side.CLIENT);
		this.register(MsgAddresseeHistory.class, Side.CLIENT);
		this.register(MsgSendCooling.class, Side.CLIENT);
		this.register(MsgPlayerPermissionLevel.class, Side.CLIENT);
		
		this.register(MsgReadEmail.class, Side.SERVER);
		this.register(MsgReadEmail.All.class, Side.SERVER);
		this.register(MsgUnread.class, Side.CLIENT);
		
		this.register(MsgSendPlayerMessage.class, Side.CLIENT);
		
		this.register(MsgBlacklist.Add.class, Side.SERVER);
		this.register(MsgBlacklist.Remove.class, Side.SERVER);
		
		this.register(MsgRefreshInbox.class, Side.SERVER);
		this.register(MsgRefreshOther.class, Side.SERVER);
		this.register(MsgRefreshBlacklist.class, Side.CLIENT);
		this.register(MsgRefreshBlacklist.Refresh.class, Side.SERVER);
	}
	
	private <T extends BaseMessage> void register(Class<T> msgClass, Side... sendTo) {
		for (Side side : sendTo) {
			this.channel.registerMessage(T::handler, msgClass, nextID(), side);
		}
	}
	
	/** server to client */
	public void sendMessageToPlayer(IMessage msg, EntityPlayerMP player) {
		if(msg!=null && player!=null) channel.sendTo(msg, player);
	}
	
	/** client to server */
	public void sendMessageToServer(IMessage msg) {
		if(msg!=null) channel.sendToServer(msg);
	}
}
