package cat.jiu.email.net.msg;

import java.util.List;
import java.util.Map;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.SideProxy;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.ui.gui.GuiInbox;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cat.jiu.email.element.Inbox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MsgInboxToClient {
	public static class SendEmail extends BaseMessage {
		public static final Type<SendEmail> TYPE = type(EmailMain.MODID);
		private long id;
		private Email email;

		public SendEmail() {
			super(TYPE);
		}

		public SendEmail(long id, Email email) {
			super(TYPE);
			this.id = id;
			this.email = email;
		}

		@Override
		public void toBytes(FriendlyByteBuf buffer) {
			buffer.writeLong(this.id);
			buffer.writeNbt(this.email.write(new CompoundTag()));
		}

		@Override
		public void fromBytes(FriendlyByteBuf buf) {
			this.id = buf.readLong();
			this.email = new Email(buf.readNbt());
		}

		@Override
		public boolean handler(IPayloadContext context) {
			if(SideProxy.isClient()) {
				if(Minecraft.getInstance().screen instanceof GuiInbox gui){
					gui.addEmail(this.id, this.email);
				}
			}
			return true;
		}
	}

	public static class SendOther extends BaseMessage {
		public static final Type<SendOther> TYPE = type(EmailMain.MODID);
		private final Map<String, Object> customValue = Maps.newHashMap();
		private final List<String> senderBlacklist = Lists.newArrayList();
		public SendOther() {
			super(TYPE);
		}
		
		public SendOther(Inbox inbox) {
			this(inbox.getCustomValue(), inbox.getSenderBlacklist());
		}
		public SendOther(Map<String, Object> customValue, List<String> senderBlacklist) {
			super(TYPE);
			this.customValue.putAll(customValue);
			this.senderBlacklist.addAll(senderBlacklist);
		}
		
		@Override
		public void fromBytes(FriendlyByteBuf buf) {
			CompoundTag nbt = buf.readNbt();

			CompoundTag values = nbt.getCompound("values");
			values.getAllKeys().forEach(k -> this.customValue.put(k, values.getString(k)));

			ListTag blacks = nbt.getList("blacks", 8);
			blacks.forEach(e -> this.senderBlacklist.add(e.getAsString()));
		}
		
		@Override
		public void toBytes(FriendlyByteBuf buf) {
			CompoundTag nbt = new CompoundTag();
			
			CompoundTag values = new CompoundTag();
			this.customValue.forEach((k,v) -> values.putString(k, String.valueOf(v)));
			nbt.put("values", values);

			ListTag blacks = new ListTag();
			this.senderBlacklist.forEach(e -> blacks.add(StringTag.valueOf(e)));
			nbt.put("blacks", blacks);
			
			buf.writeNbt(nbt);
		}

		@Override
		public boolean handler(IPayloadContext ctx) {
			if(SideProxy.isClient()) {
				Screen gui = Minecraft.getInstance().screen;

				if(gui instanceof GuiInbox && ((GuiInbox) gui).getInbox() != null) {
					this.customValue.forEach((k,v) -> ((GuiInbox) gui).getInbox().addCustom(k, v));
					this.senderBlacklist.forEach(e -> ((GuiInbox) gui).getInbox().addSenderBlacklist(e));
				}
			}
			return true;
		}
	}
}
