package cat.jiu.email.event;

import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public class EmailReceiveEvent {
	@Cancelable
	public static class Pre extends PlayerEvent {
		public final ServerPlayer player;
		public final Inbox inbox;
		private Email email;
		public final boolean isReceiveAll;
		public Pre(ServerPlayer player, Inbox inbox, Email email, boolean isReceiveAll) {
			super(player);
			this.player = player;
			this.inbox = inbox;
			this.email = email;
			this.isReceiveAll = isReceiveAll;
		}
		public Email getEmail() {return email;}
		public void setMessage(Email message) {this.email = message;}
	}
	
	public static class Post extends PlayerEvent {
		public final ServerPlayer player;
		public final Inbox inbox;
		public final Email email;
		public final boolean isReceiveAll;
		public Post(ServerPlayer player, Inbox inbox, Email email, boolean isReceiveAll) {
			super(player);
			this.player = player;
			this.inbox = inbox;
			this.email = email;
			this.isReceiveAll = isReceiveAll;
		}
	}
}
