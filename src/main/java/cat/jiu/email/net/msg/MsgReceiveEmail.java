package cat.jiu.email.net.msg;

import java.util.List;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailReceiveEvent;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.util.EmailUtils;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgReceiveEmail  {
	public static class Receive implements IMessage {
		protected int msgID;
		public Receive() {}
		public Receive(int msgID) {
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
					
					if(inbox!=null && inbox.has(this.msgID)) {
						cat.jiu.email.element.Email email = inbox.get(msgID);
						EmailReceiveEvent.Pre pre = new EmailReceiveEvent.Pre(player, inbox, email, false);
						if(MinecraftForge.EVENT_BUS.post(pre)) {
							return;
						}
						email = pre.getEmail();
						inbox.set(msgID, email);
						
						if(!email.isReceived() && email.hasItems()) {
							if(inbox.getInboxSize()+55 >= 2097152L && !EmailUtils.isInfiniteSize()) {
								return;
							}
							email.setAccept(true);
							List<ItemStack> items = email.getItems();
							if(items.size() > 0) {
								EmailUtils.spawnAsEntity(player, items);
							}
							
							((ContainerEmailMain) player.openContainer).setInbox(inbox);
							EmailAPI.sendInboxToClient(inbox, player);
							MinecraftForge.EVENT_BUS.post(new EmailReceiveEvent.Post(player, inbox, email, false));
							EmailUtils.saveInboxToDisk(inbox, 10);
						}
					}
				});
			}
			return null;
		}
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
						EmailReceiveEvent.Pre pre = new EmailReceiveEvent.Pre(player, inbox, email, true);
						if(MinecraftForge.EVENT_BUS.post(pre)) continue;
						email = pre.getEmail();
						inbox.set(i, email);
						
						if(!email.isReceived() && email.hasItems()) {
							if(inbox.getInboxSize()+55 >= 2097152L && !EmailUtils.isInfiniteSize()) {
								return;
							}
							email.setAccept(true);
							List<ItemStack> items = email.getItems();
							if(items.size() > 0) {
								EmailUtils.spawnAsEntity(player, items);
							}
						}
						MinecraftForge.EVENT_BUS.post(new EmailReceiveEvent.Post(player, inbox, email, true));
					}
					((ContainerEmailMain) player.openContainer).setInbox(inbox);
					EmailAPI.sendInboxToClient(inbox, player);
					EmailUtils.saveInboxToDisk(inbox, 10);
				});
			}
			return null;
		}
	}
}
