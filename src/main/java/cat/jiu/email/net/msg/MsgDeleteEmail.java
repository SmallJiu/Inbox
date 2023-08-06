package cat.jiu.email.net.msg;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailDeleteEvent;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgDeleteEmail {
	public static class Delete extends BaseMessage {
		protected long msgID;
		public Delete() {}
		public Delete(long msgID) {
			this.msgID = msgID;
		}
		public void fromBytes(FriendlyByteBuf buf) {this.msgID = buf.readLong();}
		public void toBytes(FriendlyByteBuf buf) {buf.writeLong(this.msgID);}
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(ctx.get().getSender() != null) {
				ctx.get().enqueueWork(()->{
					ServerPlayer player = ctx.get().getSender();
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
		public void fromBytes(FriendlyByteBuf buf) {}
		public void toBytes(FriendlyByteBuf buf) {}
		
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(ctx.get().getSender() != null) {
				ctx.get().enqueueWork(()->{
					ServerPlayer player = ctx.get().getSender();
					Inbox inbox = Inbox.get(player);

					boolean changed = false;
					for(long i : inbox.getEmailIDs()) {
						Email email = inbox.getEmail(i);
						if(email.isRead() && !email.hasItems()) {
							if(!MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(inbox, i, true, false))) {
								inbox.deleteEmail(i);
								changed = true;
							}
						}
						MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Post(inbox, i, true, false));
					}
					if(changed){
						EmailUtils.saveInboxToDisk(inbox);
						EmailAPI.sendInboxToClient(inbox, player);
					}
				});
			}
			return true;
		}
	}
	public static class AllReceive extends BaseMessage {

		public void fromBytes(FriendlyByteBuf buf) {}
		public void toBytes(FriendlyByteBuf buf) {}
		
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(ctx.get().getSender() != null) {
				ctx.get().enqueueWork(()->{
					ServerPlayer player = ctx.get().getSender();
					Inbox inbox = Inbox.get(player);

					boolean changed = false;
					for(long i : inbox.getEmailIDs()) {
						Email email = inbox.getEmail(i);
						if(email.isReceived() && email.isRead() && !MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(inbox, i, false, true))) {
							inbox.deleteEmail(i);
							changed = true;
						}
						MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Post(inbox, i, false, true));
					}
					if(changed){
						EmailUtils.saveInboxToDisk(inbox);
						EmailAPI.sendInboxToClient(inbox, player);
					}
				});
			}
			return true;
		}
	}
}
