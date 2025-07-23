package cat.jiu.email.event;

import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class EmailReceiveEvent extends PlayerEvent {
	public final Inbox inbox;
	public final boolean isReceiveAll;

	protected EmailReceiveEvent(Player player, Inbox inbox, boolean isReceiveAll) {
		super(player);
		this.inbox = inbox;
		this.isReceiveAll = isReceiveAll;
	}

	public static class Pre extends EmailReceiveEvent implements ICancellableEvent {
		private Email email;
		public Pre(Player player, Inbox inbox, Email email, boolean isReceiveAll) {
			super(player, inbox, isReceiveAll);
			this.email = email;
		}
		public Email getEmail() {
			return email;
		}
		public void setEmail(Email email) {
			this.email = email;
		}
	}
	
	public static class Post extends EmailReceiveEvent {
		public final Email email;
		public Post(Player player, Inbox inbox, Email email, boolean isReceiveAll) {
			super(player, inbox, isReceiveAll);
			this.email = email;
		}
	}
}
