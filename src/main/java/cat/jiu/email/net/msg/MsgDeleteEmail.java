package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailDeleteEvent;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MsgDeleteEmail {
	public static class Delete extends BaseMessage {
		public static final Type<Delete> TYPE = type(EmailMain.MODID);
		protected long msgID;
		public Delete() {
			super(TYPE);
		}
		public Delete(long msgID) {
			super(TYPE);
			this.msgID = msgID;
		}
		public void fromBytes(FriendlyByteBuf buf) {this.msgID = buf.readLong();}
		public void toBytes(FriendlyByteBuf buf) {buf.writeLong(this.msgID);}
		public boolean handler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) ctx.player();
                Inbox inbox = Inbox.get(player);

                if (inbox.hasEmail(this.msgID)) {
                    if (!NeoForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(inbox, this.msgID, false, false)).isCanceled()) {
                        if (!inbox.getEmail(this.msgID).isReceived()) {
                            inbox.getEmail(this.msgID).receive(player);
                        }
                        inbox.deleteEmail(msgID);
                    }
                }
                EmailUtils.saveInboxToDisk(inbox);
//					EmailAPI.sendInboxToClient(inbox, player);
                NeoForge.EVENT_BUS.post(new EmailDeleteEvent.Post(inbox, this.msgID, false, false));
            });
            return true;
		}
	}
	
	public static class AllRead extends BaseMessage {
		public static final Type<AllRead> TYPE = type(EmailMain.MODID);
		public AllRead() {
			super(TYPE);
		}

		public void fromBytes(FriendlyByteBuf buf) {}
		public void toBytes(FriendlyByteBuf buf) {}
		
		public boolean handler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) ctx.player();
                Inbox inbox = Inbox.get(player);

                boolean changed = false;
                for (long i : inbox.getEmailIDs()) {
                    Email email = inbox.getEmail(i);
                    if (email.isRead() && !email.hasAttachment()) {
                        if (!NeoForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(inbox, i, true, false)).isCanceled()) {
                            inbox.deleteEmail(i);
                            changed = true;
                        }
                    }
                    NeoForge.EVENT_BUS.post(new EmailDeleteEvent.Post(inbox, i, true, false));
                }
                if (changed) {
                    EmailUtils.saveInboxToDisk(inbox);
//						EmailAPI.sendInboxToClient(inbox, player);
                }
            });
            return true;
		}
	}
	public static class AllReceive extends BaseMessage {
		public static final Type<AllReceive> TYPE = type(EmailMain.MODID);
		public AllReceive() {
			super(TYPE);
		}
		public void fromBytes(FriendlyByteBuf buf) {}
		public void toBytes(FriendlyByteBuf buf) {}
		
		public boolean handler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) ctx.player();
                Inbox inbox = Inbox.get(player);

                boolean changed = false;
                for (long i : inbox.getEmailIDs()) {
                    Email email = inbox.getEmail(i);
                    if (!NeoForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(inbox, i, false, true)).isCanceled()) {
                        if (!email.isReceived()) {
                            email.receive(player);
                        }
                        inbox.deleteEmail(i);
                        changed = true;
                    }
                    NeoForge.EVENT_BUS.post(new EmailDeleteEvent.Post(inbox, i, false, true));
                }
                if (changed) {
                    EmailUtils.saveInboxToDisk(inbox);
//						EmailAPI.sendInboxToClient(inbox, player);
                }
            });
            return true;
		}
	}
}
