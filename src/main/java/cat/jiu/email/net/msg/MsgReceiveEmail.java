package cat.jiu.email.net.msg;

import java.util.function.Supplier;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailReceiveEvent;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MsgReceiveEmail  {
	public static class Receive extends BaseMessage {
		public static final Type<Receive> TYPE = type(EmailMain.MODID);
		protected long msgID;
		public Receive() {
			super(TYPE);
		}
		public Receive(long msgID) {
			super(TYPE);
			this.msgID = msgID;
		}
		public void fromBytes(FriendlyByteBuf buf) {this.msgID = buf.readLong();}
		public void toBytes(FriendlyByteBuf buf) {buf.writeLong(this.msgID);}
		public boolean handler(IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = (ServerPlayer) ctx.player();
                Inbox inbox = Inbox.get(player);

                if (inbox != null && inbox.hasEmail(this.msgID)) {
                    Email email = inbox.getEmail(msgID);
                    EmailReceiveEvent.Pre pre = new EmailReceiveEvent.Pre(player, inbox, email, false);
                    if (NeoForge.EVENT_BUS.post(pre).isCanceled()) {
                        return;
                    }
                    email = pre.getEmail();
                    inbox.setEmail(msgID, email);

                    if (!email.isReceived()) {
//							已经，不需要检查邮箱大小了
//							if(inbox.getInboxSize()+55 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
//								return;
//							}

                        email.receive(player);

                        EmailUtils.saveInboxToDisk(inbox);
//							EmailAPI.sendInboxToClient(inbox, player);
                        NeoForge.EVENT_BUS.post(new EmailReceiveEvent.Post(player, inbox, email, false));
                    }
                }
            });
            return true;
		}
	}
	
	public static class All extends BaseMessage {
		public static final Type<All> TYPE = type(EmailMain.MODID);
		public All() {
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
                    EmailReceiveEvent.Pre pre = new EmailReceiveEvent.Pre(player, inbox, email, true);
                    if (NeoForge.EVENT_BUS.post(pre).isCanceled()) continue;
                    email = pre.getEmail();
                    inbox.setEmail(i, email);

                    if (!email.isReceived()) {
//							已经，不需要检查邮箱大小了
//							if(inbox.getInboxSize()+55 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
//								return;
//							}
                        email.receive(player);
                        changed = true;
                    }
                    NeoForge.EVENT_BUS.post(new EmailReceiveEvent.Post(player, inbox, email, true));
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
