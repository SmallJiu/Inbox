package cat.jiu.email.net.msg;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailDeleteEvent;
import cat.jiu.email.util.EmailUtils;
import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgDeleteEmail {
	public static class Delete extends BaseMessage {
		protected long msgID;
		public Delete() {}
		public Delete(long msgID) {
			this.msgID = msgID;
		}
		public void fromBytes(PacketBuffer buf) {this.msgID = buf.readLong();}
		public void toBytes(PacketBuffer buf) {buf.writeLong(this.msgID);}
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(ctx.get().getSender() != null) {
				ServerPlayerEntity player = ctx.get().getSender();
				ctx.get().enqueueWork(()->{
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
			return true;
		}
	}
	
	public static class AllRead extends BaseMessage {
		public void fromBytes(PacketBuffer buf) {}
		public void toBytes(PacketBuffer buf) {}
		
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(ctx.get().getSender() != null) {
				ServerPlayerEntity player = ctx.get().getSender();
				ctx.get().enqueueWork(()->{
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
			return true;
		}
	}
	public static class AllReceive extends BaseMessage {

		public void fromBytes(PacketBuffer buf) {}
		public void toBytes(PacketBuffer buf) {}
		
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(ctx.get().getSender() != null) {
				ServerPlayerEntity player = ctx.get().getSender();
				ctx.get().enqueueWork(()->{
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
			return true;
		}
	}
}
