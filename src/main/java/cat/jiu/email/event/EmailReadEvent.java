package cat.jiu.email.event;

import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class EmailReadEvent extends PlayerEvent {
	public final Inbox inbox;
	public final boolean isReadAll;

	public EmailReadEvent(Player player, Inbox inbox, boolean isReadAll) {
		super(player);
		this.inbox = inbox;
		this.isReadAll = isReadAll;
	}

	public static class Pre extends EmailReadEvent implements ICancellableEvent {
		private Email email;
		public Pre(Player player, Inbox inbox, Email email, boolean isReadAll) {
			super(player, inbox, isReadAll);
			this.email = email;
		}
		public Email getEmail() {return email;}
		public void setEmail(Email email) {
			this.email = email;
		}
	}
	
	public static class Post extends EmailReadEvent {
		public final Email email;
		public Post(Player player, Inbox inbox, Email email, boolean isReadAll) {
			super(player, inbox, isReadAll);
			this.email = email;
		}
	}
}
