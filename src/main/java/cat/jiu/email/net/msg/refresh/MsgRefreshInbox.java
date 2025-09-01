package cat.jiu.email.net.msg.refresh;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.ArrayUtils;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.net.msg.MsgInboxToClient;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class MsgRefreshInbox extends BaseMessage {
	private Set<Long> emailIDs;
	public MsgRefreshInbox() {}
	public MsgRefreshInbox(Set<Long> emailIDs) {
		this.emailIDs = emailIDs;
	}

	public boolean handler(Supplier<NetworkEvent.Context> ctx) {
		if(ctx.get().getSender() != null) {
			EmailAPI.sendInboxToClient(Inbox.get(ctx.get().getSender()), this.emailIDs, ctx.get().getSender());
			return true;
		}
		return false;
	}

	@Override
	public void toBytes(FriendlyByteBuf buffer) {
		buffer.writeLongArray(this.emailIDs.stream().mapToLong(Long::longValue).toArray());
	}

	@Override
	public void fromBytes(FriendlyByteBuf buf) {
		this.emailIDs = new HashSet<>(List.of(ArrayUtils.toArray(buf.readLongArray())));
	}
}
