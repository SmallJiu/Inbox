package cat.jiu.email.event;

import cat.jiu.email.element.Inbox;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.eventbus.api.Event;

public class EmailSendDevMessageEvent extends Event {
	public final ServerPlayerEntity player;
	public final Inbox inbox;
	public EmailSendDevMessageEvent(ServerPlayerEntity player, Inbox inbox) {
		this.player = player;
		this.inbox = inbox;
	}
}
