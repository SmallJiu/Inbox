package cat.jiu.email.event;

import cat.jiu.email.element.Inbox;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class EmailSendDevMessageEvent extends PlayerEvent {
	public final Player player;
	public final Inbox inbox;
	public EmailSendDevMessageEvent(Player player, Inbox inbox) {
		super(player);
		this.player = player;
		this.inbox = inbox;
	}
}
