package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailReadEvent;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgReadEmail extends BaseMessage {
	protected long msgID;
	public MsgReadEmail() {}
	public MsgReadEmail(long msgID) {
		this.msgID = msgID;
	}
	public void fromBytes(PacketBuffer buf) {this.msgID = buf.readLong();}
	public void toBytes(PacketBuffer buf) {buf.writeLong(this.msgID);}
	
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(ctx.get().getSender() != null) {
			ctx.get().enqueueWork(()->{
				ServerPlayerEntity player = ctx.get().getSender();
				Inbox inbox = Inbox.get(player);
				if(inbox.isEmptyInbox()){
					EmailMain.log.error("Inbox is EMPTY! unknown bug for this. Inbox json: {}", inbox);
				}

				if(inbox.hasEmail(this.msgID)) {
					Email email = inbox.getEmail(this.msgID);
					if(!email.isRead()) {
						EmailReadEvent.Pre pre = new EmailReadEvent.Pre(player, inbox, email, false);
						if(MinecraftForge.EVENT_BUS.post(pre)) {
							inbox.saveToDisk();
							return;
						}
						email.setRead(true);

						if(inbox.isEmptyInbox()){
							EmailMain.log.error("Inbox is EMPTY! unknown bug for this. Inbox json: {}", inbox);
						}
						EmailUtils.saveInboxToDisk(inbox);
//						EmailAPI.sendInboxToClient(inbox, player);

						MinecraftForge.EVENT_BUS.post(new EmailReadEvent.Post(player, inbox, email, false));
					}
				}
			});
		}
		return true;
	}
	
	public static class All extends BaseMessage {
		public void fromBytes(PacketBuffer buf) {}
		public void toBytes(PacketBuffer buf) {}
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(ctx.get().getSender() != null) {
				ctx.get().enqueueWork(()->{
					ServerPlayerEntity player = ctx.get().getSender();
					Inbox inbox = Inbox.get(player);

					boolean changed = false;
					for(long i : inbox.getEmailIDs()) {
						Email email = inbox.getEmail(i);
						EmailReadEvent.Pre pre = new EmailReadEvent.Pre(player, inbox, email, true);
						if(MinecraftForge.EVENT_BUS.post(pre)) continue;
						email = pre.getEmail();

						if(!email.isRead()) {
							email.setRead(true);
							changed = true;
						}
						MinecraftForge.EVENT_BUS.post(new EmailReadEvent.Post(player, inbox, email, true));
					}

					if(changed){
						EmailUtils.saveInboxToDisk(inbox);
//						EmailAPI.sendInboxToClient(inbox, player);
					}
				});
			}
			return true;
		}
	}
}
