package cat.jiu.email.net.msg;

import java.awt.Color;
import java.io.IOException;

import cat.jiu.email.element.InboxText;
import cat.jiu.email.iface.IInboxText;
import cat.jiu.email.ui.container.ContainerEmailSend;
import cat.jiu.email.util.EmailUtils;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class SendRenderText implements IMessage {
	public static final Object[] empty = new Object[0];
	protected IInboxText text;
	protected Object[] args;
	protected Color color;
	protected long renderTicks;
	
	public SendRenderText() {}
	public SendRenderText(IInboxText text) {
		this(Color.RED, EmailUtils.parseTick(0,0,0,15, 0), text);
	}
	public SendRenderText(Color color, IInboxText text) {
		this(color, EmailUtils.parseTick(0,0,0,15, 0), text);
	}
	public SendRenderText(Color color, long renderTicks, IInboxText text) {
		this.text = text;
		this.color = color;
		this.renderTicks = renderTicks;
	}
	public void fromBytes(ByteBuf buf) {
		try {
			NBTTagCompound nbt = new PacketBuffer(buf).readCompoundTag();
			
			this.text = new InboxText(nbt.getTag("text"));
			this.color = new Color(nbt.getInteger("color"));
			this.renderTicks = nbt.getLong("ticks");
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	public void toBytes(ByteBuf buf) {
		NBTTagCompound nbt = new NBTTagCompound();
		
		nbt.setTag("text", this.text.writeToNBT());
		nbt.setInteger("color", this.color.getRGB());
		nbt.setLong("ticks", this.renderTicks);
		
		new PacketBuffer(buf).writeCompoundTag(nbt);
	}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isClient()) {
			((ContainerEmailSend)Minecraft.getMinecraft().player.openContainer)
				.setRenderText(this.text.format(), this.color, this.renderTicks);
		}
		return null;
	}
}
