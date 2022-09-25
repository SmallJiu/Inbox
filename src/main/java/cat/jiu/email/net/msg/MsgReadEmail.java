package cat.jiu.email.net.msg;

import cat.jiu.email.Email;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailReadEvent;
import cat.jiu.email.ui.container.ContainerEmailMain;

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
				
				if(inbox.has(this.msgID)) {
					cat.jiu.email.element.Email email = inbox.get(this.msgID);
					
					EmailReadEvent.Pre pre = new EmailReadEvent.Pre(player, inbox, email, false);
					if(MinecraftForge.EVENT_BUS.post(pre)) {
						return;
					}
					email = pre.getMessage();
					inbox.set(this.msgID, email);
					
					if(!email.isRead()) {
						email.setRead(true);
						EmailMain.net.sendMessageToPlayer(new MsgUnread(inbox.getUnRead()), (EntityPlayerMP) player);
						
						((ContainerEmailMain) player.openContainer).setMsgs(inbox);
						Email.sendEmailToClient(inbox, player);
						inbox.save();
						
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
					
					for(int i = 0; i < inbox.count(); i++) {
						cat.jiu.email.element.Email email = inbox.get(i);
						EmailReadEvent.Pre pre = new EmailReadEvent.Pre(player, inbox, email, true);
						if(MinecraftForge.EVENT_BUS.post(pre)) continue;
						email = pre.getMessage();
						
						if(!email.isRead()) {
							email.setRead(true);
						}
						MinecraftForge.EVENT_BUS.post(new EmailReadEvent.Post(player, inbox, email, true));
					}
					
					((ContainerEmailMain) player.openContainer).setMsgs(inbox);
					inbox.save();;
					Email.sendEmailToClient(inbox, player);
					EmailMain.net.sendMessageToPlayer(new MsgUnread(inbox.getUnRead()), (EntityPlayerMP) player);
				});
			}
			return null;
		}
	}
}
