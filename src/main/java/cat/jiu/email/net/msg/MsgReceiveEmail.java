package cat.jiu.email.net.msg;

import java.util.List;
import java.util.function.Supplier;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailReceiveEvent;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

public class MsgReceiveEmail  {
	public static class Receive extends BaseMessage {
		protected long msgID;
		public Receive() {}
		public Receive(long msgID) {
			this.msgID = msgID;
		}
		public void fromBytes(FriendlyByteBuf buf) {this.msgID = buf.readLong();}
		public void toBytes(FriendlyByteBuf buf) {buf.writeLong(this.msgID);}
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(ctx.get().getSender() != null) {
				ctx.get().enqueueWork(()->{
					ServerPlayer player = ctx.get().getSender();
					Inbox inbox = Inbox.get(player);

					if(inbox!=null && inbox.hasEmail(this.msgID)) {
						Email email = inbox.getEmail(msgID);
						EmailReceiveEvent.Pre pre = new EmailReceiveEvent.Pre(player, inbox, email, false);
						if(MinecraftForge.EVENT_BUS.post(pre)) {
							return;
						}
						email = pre.getEmail();
						inbox.setEmail(msgID, email);

						if(!email.isReceived() && email.hasItems()) {
							if(inbox.getInboxSize()+55 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
								return;
							}
							email.setAccept(true);
							List<ItemStack> items = email.getItems();
							if(items.size() > 0) {
								EmailUtils.spawnAsEntity(player, items);
							}

							if(player.containerMenu instanceof ContainerEmailMain container) {
								container.setInbox(inbox);
							}
							EmailUtils.saveInboxToDisk(inbox);
							EmailAPI.sendInboxToClient(inbox, player);
							MinecraftForge.EVENT_BUS.post(new EmailReceiveEvent.Post(player, inbox, email, false));
						}
					}
				});
			}
			return true;
		}
	}
	
	public static class All extends BaseMessage {
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
						EmailReceiveEvent.Pre pre = new EmailReceiveEvent.Pre(player, inbox, email, true);
						if(MinecraftForge.EVENT_BUS.post(pre)) continue;
						email = pre.getEmail();
						inbox.setEmail(i, email);

						if(!email.isReceived() && email.hasItems()) {
							if(inbox.getInboxSize()+55 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
								return;
							}
							email.setAccept(true);
							EmailUtils.spawnAsEntity(player, email.getItems());
							changed = true;
						}
						MinecraftForge.EVENT_BUS.post(new EmailReceiveEvent.Post(player, inbox, email, true));
					}
					if(changed){
						if(player.containerMenu instanceof ContainerEmailMain container) {
							container.setInbox(inbox);
						}
						EmailUtils.saveInboxToDisk(inbox);
						EmailAPI.sendInboxToClient(inbox, player);
					}
				});
			}
			return true;
		}
	}
}
