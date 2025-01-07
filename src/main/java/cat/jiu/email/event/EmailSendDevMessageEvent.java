package cat.jiu.email.event;

import cat.jiu.email.element.Inbox;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class EmailSendDevMessageEvent extends PlayerEvent {
	public final ServerPlayerEntity player;
	public final Inbox inbox;
	public EmailSendDevMessageEvent(ServerPlayerEntity player, Inbox inbox) {
		super(player);
		this.player = player;
		this.inbox = inbox;
	}
}
