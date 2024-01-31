package cat.jiu.email.net.msg;

import java.io.IOException;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.net.BaseMessage;
import cat.jiu.email.util.EmailUtils;

import io.netty.buffer.ByteBuf;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

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
	public void fromBytes(ByteBuf buf) {
		try {
			NBTTagCompound nbt = new PacketBuffer(buf).readCompoundTag();
			
			if(nbt.hasKey("color")) this.color = TextFormatting.getValueByName(nbt.getString("color"));
			this.text = new Text(nbt.getCompoundTag("text"));
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		NBTTagCompound nbt = new NBTTagCompound();
		
		if(this.color!=null) nbt.setString("color", this.color.name());
		nbt.setTag("text", this.text.writeTo(NBTTagCompound.class));
		
		new PacketBuffer(buf).writeCompoundTag(nbt);
	}
	
	public IMessage handler(MessageContext ctx) {
		if(ctx.side.isClient()) {
			ITextComponent tc = this.color!=null ? 
					EmailUtils.createTextComponent(this.color, this.text.format())
				  : EmailUtils.createTextComponent(this.text.format());
			Minecraft.getMinecraft().player.sendMessage(tc);
		}
		return null;
	}
}
