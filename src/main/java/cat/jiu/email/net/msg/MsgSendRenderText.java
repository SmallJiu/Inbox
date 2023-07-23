package cat.jiu.email.net.msg;

import java.awt.Color;
import java.util.function.Supplier;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailMain;
import cat.jiu.email.ui.container.ContainerEmailSend;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public class MsgSendRenderText extends BaseMessage {
	public static final Object[] empty = new Object[0];
	protected IText text;
	protected Object[] args;
	protected Color color;
	protected long renderTicks;
	
	public MsgSendRenderText() {}
	public MsgSendRenderText(IText text) {
		this(Color.RED, EmailUtils.parseTick(0,0,0,15, 0), text);
	}
	public MsgSendRenderText(Color color, IText text) {
		this(color, EmailUtils.parseTick(0,0,0,15, 0), text);
	}
	public MsgSendRenderText(Color color, long renderTicks, IText text) {
		this.text = text;
		this.color = color;
		this.renderTicks = renderTicks;
	}
	public void fromBytes(PacketBuffer buf) {
		CompoundNBT nbt = buf.readCompoundTag();

		this.text = new Text(nbt.getCompound("text"));
		this.color = new Color(nbt.getInt("color"));
		this.renderTicks = nbt.getLong("ticks");
	}

	public void toBytes(PacketBuffer buf) {
		CompoundNBT nbt = new CompoundNBT();
		
		nbt.put("text", this.text.writeTo(CompoundNBT.class));
		nbt.putInt("color", this.color.getRGB());
		nbt.putLong("ticks", this.renderTicks);
		
		buf.writeCompoundTag(nbt);
	}
	
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(EmailMain.proxy.isClient()) {
			((ContainerEmailSend)Minecraft.getInstance().player.openContainer)
				.setRenderText(this.text.format(), this.color, this.renderTicks);
		}
		return true;
	}
}
