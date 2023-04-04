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
		this.channel = NetworkRegistry.INSTANCE.newSimpleChannel(EmailMain.MODID);

		this.channel.registerMessage(MsgOpenGui::handler, MsgOpenGui.class, nextID(), Side.CLIENT);
		this.channel.registerMessage(MsgOpenGui::handler, MsgOpenGui.class, nextID(), Side.SERVER);
		
		this.channel.registerMessage(MsgDeleteEmail.Delete::handler, MsgDeleteEmail.Delete.class, nextID(), Side.SERVER);
		this.channel.registerMessage(MsgDeleteEmail.AllRead::handler, MsgDeleteEmail.AllRead.class, nextID(), Side.SERVER);
		this.channel.registerMessage(MsgDeleteEmail.AllReceive::handler, MsgDeleteEmail.AllReceive.class, nextID(), Side.SERVER);
		
		this.channel.registerMessage(MsgReceiveEmail.Receive::handler, MsgReceiveEmail.Receive.class, nextID(), Side.SERVER);
		this.channel.registerMessage(MsgReceiveEmail.All::handler, MsgReceiveEmail.All.class, nextID(), Side.SERVER);
		this.channel.registerMessage(MsgUnreceive::handler, MsgUnreceive.class, nextID(), Side.CLIENT);
		
		this.channel.registerMessage(MsgInboxToClient::handler, MsgInboxToClient.class, nextID(), Side.CLIENT);
		this.channel.registerMessage(MsgInboxToClient.MsgOtherToClient::handler, MsgInboxToClient.MsgOtherToClient.class, nextID(), Side.CLIENT);
		
		this.channel.registerMessage(MsgSend::handler, MsgSend.class, nextID(), Side.SERVER);
		this.channel.registerMessage(SendRenderText::handler, SendRenderText.class, nextID(), Side.CLIENT);
		this.channel.registerMessage(SendCooling::handler, SendCooling.class, nextID(), Side.CLIENT);
		
		this.channel.registerMessage(MsgReadEmail::handler, MsgReadEmail.class, nextID(), Side.SERVER);
		this.channel.registerMessage(MsgReadEmail.All::handler, MsgReadEmail.All.class, nextID(), Side.SERVER);
		this.channel.registerMessage(MsgUnread::handler, MsgUnread.class, nextID(), Side.CLIENT);
		
		this.channel.registerMessage(MsgSendPlayerMessage::handler, MsgSendPlayerMessage.class, nextID(), Side.CLIENT);
		
		this.channel.registerMessage(MsgBlacklist.Add::handler, MsgBlacklist.Add.class, nextID(), Side.SERVER);
		this.channel.registerMessage(MsgBlacklist.Remove::handler, MsgBlacklist.Remove.class, nextID(), Side.SERVER);
		
		this.channel.registerMessage(MsgRefreshInbox::handler, MsgRefreshInbox.class, nextID(), Side.SERVER);
		this.channel.registerMessage(MsgRefreshOther::handler, MsgRefreshOther.class, nextID(), Side.SERVER);
		this.channel.registerMessage(MsgRefreshBlacklist::handler, MsgRefreshBlacklist.class, nextID(), Side.CLIENT);
		this.channel.registerMessage(MsgRefreshBlacklist.Refresh::handler, MsgRefreshBlacklist.Refresh.class, nextID(), Side.SERVER);
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
