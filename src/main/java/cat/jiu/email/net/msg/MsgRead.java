package cat.jiu.email.net.msg;

import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.email.Email;
import cat.jiu.email.EmailMain;
import cat.jiu.email.event.EmailReadEvent;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.util.EmailUtils;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgRead implements IMessage {
	protected int msgID;
	public MsgRead() {}
	public MsgRead(int msgID) {
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
				String msgID = Integer.toString(this.msgID);
				
				if(email != null) {
					if(email.has(msgID)) {
						JsonObject msg = email.get(msgID).getAsJsonObject();
						
						EmailReadEvent.Pre pre = new EmailReadEvent.Pre(player, EmailUtils.copyJson(email), msg, false);
						if(MinecraftForge.EVENT_BUS.post(pre)) {
							return;
						}
						msg = pre.getMessage();
						
						if(!msg.has("read")) {
							msg.addProperty("read", true);
							EmailMain.net.sendMessageToPlayer(new MsgUnread(EmailMain.getUn(email, "read")), (EntityPlayerMP) player);
							
							((ContainerEmailMain) player.openContainer).setMsgs(email);
							Email.sendEmailToClient(email, player);
							EmailUtils.toJsonFile(id, email);
							
							MinecraftForge.EVENT_BUS.post(new EmailReadEvent.Post(player, EmailUtils.copyJson(email), msg, false));
						}
					}
				}
			});
		}
		return null;
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
							
							EmailReadEvent.Pre pre = new EmailReadEvent.Pre(player, EmailUtils.copyJson(email), msg, true);
							if(MinecraftForge.EVENT_BUS.post(pre)) continue;
							msg = pre.getMessage();
							
							if(!msg.has("read")) {
								msg.addProperty("read", true);
							}
							MinecraftForge.EVENT_BUS.post(new EmailReadEvent.Post(player, EmailUtils.copyJson(email), msg, true));
						}
						((ContainerEmailMain) player.openContainer).setMsgs(email);
						EmailUtils.toJsonFile(id, email);
						Email.sendEmailToClient(email, player);
						EmailMain.net.sendMessageToPlayer(new MsgUnread(EmailMain.getUn(email, "read")), (EntityPlayerMP) player);
					}
				});
			}
			return null;
		}
	}
}
