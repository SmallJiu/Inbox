package cat.jiu.email.net.msg;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailReadEvent;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.util.EmailUtils;
import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgReadEmail implements IMessage {
	protected int msgID;
	public MsgReadEmail() {}
	public MsgReadEmail(int msgID) {
		this.msgID = msgID;
	}
	public void fromBytes(ByteBuf buf) {this.msgID = buf.readInt();}
	public void toBytes(ByteBuf buf) {buf.writeInt(this.msgID);}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isServer()) {
			EntityPlayerMP player = ctx.getServerHandler().player;
			WorldServer world = player.getServerWorld();
			world.addScheduledTask(()->{
				Inbox inbox = Inbox.get(player);
				
				if(inbox.hasEmail(this.msgID)) {
					Email email = inbox.getEmail(this.msgID);
					
					if(!email.isRead()) {
						EmailReadEvent.Pre pre = new EmailReadEvent.Pre(player, inbox, email, false);
						if(MinecraftForge.EVENT_BUS.post(pre)) {
							inbox.save();
							return;
						}
						email = pre.getEmail();
						inbox.setEmail(this.msgID, email);
						
						email.setRead(true);
						EmailMain.net.sendMessageToPlayer(new MsgUnread(inbox.getUnRead()), (EntityPlayerMP) player);
						
						((ContainerEmailMain) player.openContainer).setInbox(inbox);
						EmailAPI.sendInboxToClient(inbox, player);
						EmailUtils.saveInboxToDisk(inbox, 10);
						
						MinecraftForge.EVENT_BUS.post(new EmailReadEvent.Post(player, inbox, email, false));
					}
				}
			});
		}
		return null;
	}
	
	public static class All implements IMessage {
		public void fromBytes(ByteBuf buf) {}
		public void toBytes(ByteBuf buf) {}
		public IMessage handler(MessageContext ctx) {
			if(ctx.side.isServer()) {
				EntityPlayerMP player = ctx.getServerHandler().player;
				WorldServer world = player.getServerWorld();
				world.addScheduledTask(()->{
					Inbox inbox = Inbox.get(player);
					
					for(int i = 0; i < inbox.emailCount(); i++) {
						Email email = inbox.getEmail(i);
						EmailReadEvent.Pre pre = new EmailReadEvent.Pre(player, inbox, email, true);
						if(MinecraftForge.EVENT_BUS.post(pre)) continue;
						email = pre.getEmail();
						
						if(!email.isRead()) {
							email.setRead(true);
						}
						MinecraftForge.EVENT_BUS.post(new EmailReadEvent.Post(player, inbox, email, true));
					}
					
					((ContainerEmailMain) player.openContainer).setInbox(inbox);
					EmailUtils.saveInboxToDisk(inbox, 10);
					EmailAPI.sendInboxToClient(inbox, player);
					EmailMain.net.sendMessageToPlayer(new MsgUnread(inbox.getUnRead()), (EntityPlayerMP) player);
				});
			}
			return null;
		}
	}
}
