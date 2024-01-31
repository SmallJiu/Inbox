package cat.jiu.email.net.msg;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailDeleteEvent;
import cat.jiu.email.net.BaseMessage;
import cat.jiu.email.util.EmailUtils;
import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgDeleteEmail {
	public static class Delete extends BaseMessage {
		protected long msgID;
		public Delete() {}
		public Delete(long msgID) {
			this.msgID = msgID;
		}
		public void fromBytes(ByteBuf buf) {this.msgID = buf.readLong();}
		public void toBytes(ByteBuf buf) {buf.writeLong(this.msgID);}
		public IMessage handler(MessageContext ctx) {
			if(ctx.side.isServer()) {
				EntityPlayerMP player = ctx.getServerHandler().player;
				WorldServer world = player.getServerWorld();
				world.addScheduledTask(()->{
					Inbox inbox = Inbox.get(player);
					
					if(inbox.hasEmail(this.msgID)) {
						if(!MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(inbox, this.msgID, false, false))) {
							inbox.deleteEmail(msgID);
						}
					}
					EmailUtils.saveInboxToDisk(inbox);
					EmailAPI.sendInboxToClient(inbox, player);
					MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Post(inbox, this.msgID, false, false));
				});
			}
			return null;
		}
	}
	
	public static class AllRead extends BaseMessage {
		public AllRead() {}
		public void fromBytes(ByteBuf buf) {}
		public void toBytes(ByteBuf buf) {}
		
		public IMessage handler(MessageContext ctx) {
			if(ctx.side.isServer()) {
				EntityPlayerMP player = ctx.getServerHandler().player;
				WorldServer world = player.getServerWorld();
				world.addScheduledTask(()->{
					Inbox inbox = Inbox.get(player);
					
					for(long i : inbox.getEmailIDs()) {
						Email email = inbox.getEmail(i);
						if(email.isRead() && !email.hasItems()) {
							if(!MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(inbox, i, true, false))) {
								inbox.deleteEmail(i);
							}
						}
						MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Post(inbox, i, true, false));
					}
					EmailUtils.saveInboxToDisk(inbox);
					EmailAPI.sendInboxToClient(inbox, player);
				});
			}
			return null;
		}
	}
	public static class AllReceive extends BaseMessage {
		public AllReceive() {}
		public void fromBytes(ByteBuf buf) {}
		public void toBytes(ByteBuf buf) {}
		
		public IMessage handler(MessageContext ctx) {
			if(ctx.side.isServer()) {
				EntityPlayerMP player = ctx.getServerHandler().player;
				WorldServer world = player.getServerWorld();
				world.addScheduledTask(()->{
					Inbox inbox = Inbox.get(player);
					
					for(long i : inbox.getEmailIDs()) {
						Email email = inbox.getEmail(i);
						if(email.isReceived() && !MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(inbox, i, false, true))) {
							inbox.deleteEmail(i);
						}
						MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Post(inbox, i, false, true));
					}
					EmailUtils.saveInboxToDisk(inbox);
					EmailAPI.sendInboxToClient(inbox, player);
				});
			}
			return null;
		}
	}
}
