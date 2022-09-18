package cat.jiu.email.net.msg;

import java.util.Map.Entry;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.email.Email;
import cat.jiu.email.event.EmailDeleteEvent;
import cat.jiu.email.util.EmailUtils;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgDelete {
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
					String id = player.getUniqueID().toString();
					JsonObject email = EmailUtils.getEmail(id);
					
					if(email != null) {
						String msgID = Integer.toString(this.msgID);
						if(email.has(msgID)) {
							if(!MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(email, this.msgID, false, false))) {
								email.remove(msgID);
							}
						}
						EmailUtils.toJsonFile(id, email);
						Email.sendEmailToClient(email, player);
						MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Post(email, this.msgID, false, false));
					}
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
					JsonObject email = EmailUtils.getEmail(player.getUniqueID().toString());
					
					if(email != null && email.size() > 0) {
						for(Entry<String, JsonElement> msgs : Sets.newHashSet(email.entrySet())) {
							if(msgs.getKey().equals("dev")) continue;
							int id = Integer.parseInt(msgs.getKey());
							JsonObject msg = msgs.getValue().getAsJsonObject();
							if(msg.has("read") && !msg.has("items")) {
								if(!MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(email, id, true, false))) {
									email.remove(msgs.getKey());
								}
							}
							MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Post(email, id, true, false));
						}
						EmailUtils.toJsonFile(player.getUniqueID().toString(), email);
						Email.sendEmailToClient(email, player);
					}
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
					JsonObject email = EmailUtils.getEmail(player.getUniqueID().toString());
					
					if(email != null) {
						for(Entry<String, JsonElement> msgs : Sets.newHashSet(email.entrySet())) {
							if(msgs.getKey().equals("dev")) continue;
							int id = Integer.parseInt(msgs.getKey());
							JsonObject msg = msgs.getValue().getAsJsonObject();
							if(msg.has("accept")) {
								if(!MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Pre(email, id, false, true))) {
									email.remove(msgs.getKey());
								}
							}
							MinecraftForge.EVENT_BUS.post(new EmailDeleteEvent.Post(email, id, false, true));
						}
						EmailUtils.toJsonFile(player.getUniqueID().toString(), email);
						Email.sendEmailToClient(email, player);
					}
				});
			}
			return null;
		}
	}
}
