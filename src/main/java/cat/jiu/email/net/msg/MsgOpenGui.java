package cat.jiu.email.net.msg;

import com.google.gson.JsonObject;

import cat.jiu.email.Email;
import cat.jiu.email.EmailMain;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.util.EmailUtils;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MsgOpenGui implements IMessage {
	protected int guiID;
	public MsgOpenGui() {}
	public MsgOpenGui(int guiID) {
		this.guiID = guiID;
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		this.guiID = buf.readInt();
	}
	
	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(this.guiID);
	}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isClient()) {
			Minecraft.getMinecraft().player.openGui(EmailMain.MODID, this.guiID, Minecraft.getMinecraft().player.world, 0,0,0);
		}else {
			EntityPlayerMP player = ctx.getServerHandler().player;
			player.getServerWorld().addScheduledTask(()->{
				player.openGui(EmailMain.MODID, this.guiID, player.world, 0,0,0);
				if(player.openContainer instanceof ContainerEmailMain) {
					JsonObject msgs = EmailUtils.getEmail(player.getUniqueID().toString());
					if(msgs != null) {
						((ContainerEmailMain) player.openContainer).setMsgs(msgs);
						Email.sendEmailToClient(msgs, player);
					}
				}
			});
		}
		return null;
	}
}
