package cat.jiu.email.event;

import cat.jiu.email.element.Inbox;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class EmailSendDevMessageEvent extends PlayerEvent {
	public final ServerPlayer player;
	public final Inbox inbox;
	public EmailSendDevMessageEvent(ServerPlayer player, Inbox inbox) {
		super(player);
		this.player = player;
		this.inbox = inbox;
	}
}
