package cat.jiu.email.net.msg;

import cat.jiu.email.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailDeleteEvent;
import cat.jiu.email.util.EmailUtils;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgDeleteEmail {
	public static class Delete implements IMessage {
		protected int msgID;
		public Delete() {}
		public Delete(int msgID) {
			this.msgID = msgID;
		}
		public void fromBytes(ByteBuf buf) {this.msgID = buf.readInt();}
		public void toBytes(ByteBuf buf) {buf.writeInt(this.msgID);}
		public IMessage handler(MessageContext ctx) {
			if(ctx.side.isServer()) {
				EntityPlayerMP player = ctx.getServerHandler().player;
				WorldServer world = player.getServerWorld();
				world.addScheduledTask(()->{
					Inbox inbox = new Inbox(player.getUniqueID(), EmailUtils.getInboxJson(player.getUniqueID().toString()));
					
					if(inbox.has(this.msgID)) {
						if(!MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(inbox, this.msgID, false, false))) {
							inbox.delete(msgID);
						}
					}
					inbox.save();
					Email.sendEmailToClient(inbox, player);
					MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Post(inbox, this.msgID, false, false));
				});
			}
			return null;
		}
	}
	
	public static class AllRead implements IMessage {
		public AllRead() {}
		public void fromBytes(ByteBuf buf) {}
		public void toBytes(ByteBuf buf) {}
		
		public IMessage handler(MessageContext ctx) {
			if(ctx.side.isServer()) {
				EntityPlayerMP player = ctx.getServerHandler().player;
				WorldServer world = player.getServerWorld();
				world.addScheduledTask(()->{
					Inbox inbox = new Inbox(player.getUniqueID(), EmailUtils.getInboxJson(player.getUniqueID().toString()));
					
					for(int i = 0; i < inbox.count(); i++) {
						cat.jiu.email.element.Email email = inbox.get(i);
						if(email.isRead() && !email.hasItems()) {
							if(!MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(inbox, i, true, false))) {
								inbox.delete(i);
							}
						}
						MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Post(inbox, i, true, false));
					}
					inbox.save();
					Email.sendEmailToClient(inbox, player);
				});
			}
			return null;
		}
	}
	public static class AllReceive implements IMessage {
		public AllReceive() {}
		public void fromBytes(ByteBuf buf) {}
		public void toBytes(ByteBuf buf) {}
		
		public IMessage handler(MessageContext ctx) {
			if(ctx.side.isServer()) {
				EntityPlayerMP player = ctx.getServerHandler().player;
				WorldServer world = player.getServerWorld();
				world.addScheduledTask(()->{
					Inbox inbox = new Inbox(player.getUniqueID(), EmailUtils.getInboxJson(player.getUniqueID().toString()));
					
					for(int i = 0; i < inbox.count(); i++) {
						cat.jiu.email.element.Email email = inbox.get(i);
						if(email.isReceived() && !MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(inbox, i, false, true))) {
							inbox.delete(i);
						}
						MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Post(inbox, i, false, true));
					}
					inbox.save();
					Email.sendEmailToClient(inbox, player);
				});
			}
			return null;
		}
	}
}
