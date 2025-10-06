package cat.jiu.email.net.msg;

import java.util.function.Supplier;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.SideProxy;
import cat.jiu.core.util.element.Text;
import cat.jiu.core.util.element.data.NBTData;
import cat.jiu.email.ui.gui.GuiGenerateEmail;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class MsgSendRenderText extends BaseMessage {
	public static final Object[] empty = new Object[0];
	protected IText text;
	protected Object[] args;
	protected long renderTicks;
	
	public MsgSendRenderText() {}
	public MsgSendRenderText(IText text) {
		this(EmailUtils.parseTick(0,0,0,5, 0), text);
	}
	public MsgSendRenderText(long renderTicks, IText text) {
		this.text = text;
		this.renderTicks = renderTicks;
	}
	public void fromBytes(FriendlyByteBuf buf) {
		CompoundTag nbt = buf.readNbt();

		this.text = new Text(NBTData.map(nbt.getCompound("text")));
		this.renderTicks = nbt.getLong("ticks");
	}

	public void toBytes(FriendlyByteBuf buf) {
		CompoundTag nbt = new CompoundTag();
		
		nbt.put("text", (CompoundTag) this.text.write(NBTData.map()).getData());
		nbt.putLong("ticks", this.renderTicks);
		
		buf.writeNbt(nbt);
	}
	
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(SideProxy.isClient()) {
			if(Minecraft.getInstance().screen instanceof GuiGenerateEmail){
				((GuiGenerateEmail) Minecraft.getInstance().screen).setMessage(this.text.toTextComponent(), this.renderTicks * 50);
			}
		}
		return true;
	}
}
