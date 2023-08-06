package cat.jiu.email.net.msg;

import java.util.function.Supplier;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailMain;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import net.minecraftforge.network.NetworkEvent;

public class MsgSendPlayerMessage extends BaseMessage {
	private ChatFormatting color;
	private IText text;
	
	public MsgSendPlayerMessage() {}
	public MsgSendPlayerMessage(IText text) {
		this.text = text;
	}
	public MsgSendPlayerMessage(ChatFormatting color, IText text) {
		this.color = color;
		this.text = text;
	}

	@Override
	public void fromBytes(FriendlyByteBuf buf) {
		CompoundTag nbt = buf.readNbt();
		if(nbt.contains("color")) this.color = ChatFormatting.getByName(nbt.getString("color"));
		this.text = new Text(nbt.getCompound("text"));
	}

	@Override
	public void toBytes(FriendlyByteBuf buf) {
		CompoundTag nbt = new CompoundTag();
		
		if(this.color!=null) nbt.putString("color", this.color.name());
		nbt.put("text", this.text.writeTo(CompoundTag.class));

		buf.writeNbt(nbt);
	}
	
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(EmailMain.proxy.isClient()) {
			Component tc = this.color!=null ?
					EmailUtils.createTextComponent(this.color, this.text.format())
				  : EmailUtils.createTextComponent(this.text.format());
			Minecraft.getInstance().player.sendSystemMessage(tc);
		}
		return true;
	}
}
