package cat.jiu.email.net.msg;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.element.Email;
import cat.jiu.email.ui.gui.GuiInbox;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class MsgInboxToClient /* extends BaseMessage */ {
	/*
	protected Inbox inbox;
	protected SizeReport report = SizeReport.SUCCESS;
	public MsgInboxToClient() {}
	public MsgInboxToClient(@Nonnull Inbox inbox) {
		this.inbox = inbox;
	}

	@Override
	public void fromBytes(FriendlyByteBuf buf) {
		if(EmailConfigs.isInfiniteSize()) {
			CompoundTag nbt = buf.readNbt(NbtAccounter.UNLIMITED);
			this.inbox = Inbox.get(nbt.getUUID("owner"), nbt.getCompound("inbox"));
		}else {
			CompoundTag inboxTag = buf.readNbt(NbtAccounter.UNLIMITED);
			if (inboxTag!=null && inboxTag.contains("ToBig")) {
				this.report = new SizeReport(inboxTag.getLong("id"), inboxTag.getInt("slot"), inboxTag.getInt("size"));
			} else {
				this.inbox = Inbox.get(inboxTag.getUUID("owner"), inboxTag.getCompound("inbox"));
			}
		}
	}
	
	@Override
	public void toBytes(FriendlyByteBuf buf) {
		if(EmailConfigs.isInfiniteSize()) {
			CompoundTag nbt = new CompoundTag();
			
			nbt.putUUID("owner", this.inbox.getOwnerAsUUID());
			CompoundTag inbox = this.inbox.writeTo(CompoundTag.class);
			inbox.remove("historySize");
			inbox.remove("dev");
			nbt.put("inbox", inbox);
			
			buf.writeNbt(nbt);
		}else {
			SizeReport report = EmailUtils.checkInboxSize(this.inbox);
			if(!SizeReport.SUCCESS.equals(report)) {
				CompoundTag nbt = new CompoundTag();
				
				nbt.putBoolean("ToBig", true);
				nbt.putLong("id", report.id());
				nbt.putInt("slot", report.slot());
				nbt.putLong("size", report.size());
				
				buf.writeNbt(nbt);
				return;
			}
			
			CompoundTag nbt = new CompoundTag();
			nbt.putUUID("owner", this.inbox.getOwnerAsUUID());
			
			CompoundTag inbox = this.inbox.writeTo(CompoundTag.class);
			inbox.remove("historySize");
			inbox.remove("dev");
			inbox.remove("blacklist");
			nbt.put("inbox", inbox);
			
			buf.writeNbt(nbt);
		}
	}

	@Override
	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(EmailMain.proxy.isClient()) {
			LocalPlayer player = Minecraft.getInstance().player;
			AbstractContainerMenu con = player.containerMenu;
			if(con instanceof ContainerEmailMain) {
				if(!SizeReport.SUCCESS.equals(this.report)) {
					player.sendSystemMessage(Component.nullToEmpty(ChatFormatting.GRAY + "---------------------------------------------"));
					player.sendSystemMessage(Component.translatable("info.email.error.to_big.0"));
					player.sendSystemMessage(Component.translatable("info.email.error.to_big.1", this.report.id(), this.report.slot(), this.report.size()));
					player.closeContainer();
				}else {
					if(Minecraft.getInstance().screen instanceof GuiEmailMain gui){
						EmailMain.setUnread(this.inbox.getUnRead());
						EmailMain.setAccept(this.inbox.getUnReceived());
						gui.setInbox(this.inbox);
					}
				}
			}
		}
		return true;
	}
	 */

	public static class SendEmail extends BaseMessage {
		private long id;
		private Email email;

		public SendEmail() {}

		public SendEmail(long id, Email email) {
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
		public boolean handler(Supplier<NetworkEvent.Context> context) {
			if(EmailMain.proxy.isClient()) {
				if(Minecraft.getInstance().screen instanceof GuiInbox gui){
					gui.addEmail(this.id, this.email);
				}
			}
			return true;
		}
	}

	public static class SendOther extends BaseMessage {
		private final Map<String, Object> customValue = Maps.newHashMap();
		private final List<String> senderBlacklist = Lists.newArrayList();
		public SendOther() {}
		
		public SendOther(Inbox inbox) {
			this(inbox.getCustomValue(), inbox.getSenderBlacklist());
		}
		public SendOther(Map<String, Object> customValue, List<String> senderBlacklist) {
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
		public boolean handler(Supplier<NetworkEvent.Context> ctx) {
			if(EmailMain.proxy.isClient()) {
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
