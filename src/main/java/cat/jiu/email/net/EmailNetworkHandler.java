package cat.jiu.email.net;

import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.*;

import net.minecraft.entity.player.EntityPlayerMP;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class EmailNetworkHandler {
	private SimpleNetworkWrapper channel;
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
		
		this.channel.registerMessage(MsgSendInboxToClient::handler, MsgSendInboxToClient.class, nextID(), Side.CLIENT);
		
		this.channel.registerMessage(MsgSend::handler, MsgSend.class, nextID(), Side.SERVER);
		this.channel.registerMessage(MsgSend.SendRenderText::handler, MsgSend.SendRenderText.class, nextID(), Side.CLIENT);
		this.channel.registerMessage(MsgSend.SendCooling::handler, MsgSend.SendCooling.class, nextID(), Side.CLIENT);
		
		this.channel.registerMessage(MsgReadEmail::handler, MsgReadEmail.class, nextID(), Side.SERVER);
		this.channel.registerMessage(MsgReadEmail.All::handler, MsgReadEmail.All.class, nextID(), Side.SERVER);
		this.channel.registerMessage(MsgUnread::handler, MsgUnread.class, nextID(), Side.CLIENT);
		
	}
	
	/** server to client */
	public void sendMessageToPlayer(IMessage msg, EntityPlayerMP player) {
		channel.sendTo(msg, player);
	}
	
	/** client to server */
	public void sendMessageToServer(IMessage msg) {
		channel.sendToServer(msg);
	}
}
