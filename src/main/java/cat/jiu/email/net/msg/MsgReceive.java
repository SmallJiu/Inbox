package cat.jiu.email.net.msg;

import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.email.Email;
import cat.jiu.email.event.EmailReceiveEvent;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonToStackUtil;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgReceive  {
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
					JsonObject email = EmailUtils.getEmail(player.getUniqueID().toString());
					
					String msgID = Integer.toString(this.msgID);
					
					if(email!=null && email.has(msgID)) {
						JsonObject msg = email.get(msgID).getAsJsonObject();
						EmailReceiveEvent.Pre pre = new EmailReceiveEvent.Pre(player, EmailUtils.copyJson(email), msg, false);
						if(MinecraftForge.EVENT_BUS.post(pre)) {
							return;
						}
						msg = pre.getMessage();
						
						if(!msg.has("accept") && msg.has("items")) {
							if(EmailUtils.getEmailSize(email)+55 >= 2097152L && !EmailUtils.isInfiniteSize()) {
								
							}
							msg.addProperty("accept", true);
							JsonObject items = msg.get("items").getAsJsonObject();
							if(items.size() > 0) {
								EmailUtils.spawnAsEntity(player, JsonToStackUtil.toStacks(items));
							}
							
							
							((ContainerEmailMain) player.openContainer).setMsgs(email);
							Email.sendEmailToClient(email, player);
							MinecraftForge.EVENT_BUS.post(new EmailReceiveEvent.Post(player, EmailUtils.copyJson(email), msg, false));
							EmailUtils.toJsonFile(player.getUniqueID().toString(), email);
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
					String id = player.getUniqueID().toString();
					JsonObject email = EmailUtils.getEmail(id);
					
					if(email != null) {
						for(Entry<String, JsonElement> msgs : email.entrySet()) {
							if(msgs.getKey().equals("dev")) continue;
							JsonObject msg = msgs.getValue().getAsJsonObject();
							
							EmailReceiveEvent.Pre pre = new EmailReceiveEvent.Pre(player, EmailUtils.copyJson(email), msg, true);
							if(MinecraftForge.EVENT_BUS.post(pre)) {
								continue;
							}
							msg = pre.getMessage();
							
							if(!msg.has("accept") && msg.has("items")) {
								msg.addProperty("accept", true);
								JsonElement e = msg.get("items");
								if(e.isJsonObject()) {
									JsonObject items = e.getAsJsonObject();
									if(items.size() > 0) {
										EmailUtils.spawnAsEntity(player, JsonToStackUtil.toStacks(items));
									}
								}else if(e.isJsonArray()) {
									JsonArray items = e.getAsJsonArray();
									if(items.size() > 0) {
										EmailUtils.spawnAsEntity(player, JsonToStackUtil.toStacks(items));
									}
								}
							}
							MinecraftForge.EVENT_BUS.post(new EmailReceiveEvent.Post(player, EmailUtils.copyJson(email), msg, true));
						}
						((ContainerEmailMain) player.openContainer).setMsgs(email);
						Email.sendEmailToClient(email, player);
						EmailUtils.toJsonFile(id, email);
					}
				});
			}
			return null;
		}
	}
}
