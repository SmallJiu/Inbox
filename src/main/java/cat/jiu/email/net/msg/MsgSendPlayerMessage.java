package cat.jiu.email.net.msg;

import java.util.function.Supplier;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.SideProxy;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.network.NetworkEvent;

public class MsgSendPlayerMessage extends BaseMessage {
	private TextFormatting color;
	private IText text;
	
	public MsgSendPlayerMessage() {}
	public MsgSendPlayerMessage(IText text) {
		this.text = text;
	}
	public MsgSendPlayerMessage(TextFormatting color, IText text) {
		this.color = color;
		this.text = text;
	}

	@Override
	public void fromBytes(PacketBuffer buf) {
		CompoundNBT nbt = buf.readCompoundTag();
		if(nbt.contains("color")) this.color = TextFormatting.getValueByName(nbt.getString("color"));
		this.text = new Text(nbt.getCompound("text"));
	}

	@Override
	public void toBytes(PacketBuffer buf) {
		CompoundNBT nbt = new CompoundNBT();
		
		if(this.color!=null) nbt.putString("color", this.color.getFriendlyName());
		nbt.put("text", this.text.writeTo(CompoundNBT.class));

		buf.writeCompoundTag(nbt);
	}
	
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(SideProxy.isClient()) {
			ITextComponent tc = this.color!=null ?
					EmailUtils.createTextComponent(this.color, this.text.format())
				  : EmailUtils.createTextComponent(this.text.format());
			Minecraft.getInstance().player.sendStatusMessage(tc, false);
		}
		return true;
	}
}
