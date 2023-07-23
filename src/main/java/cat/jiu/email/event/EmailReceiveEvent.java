package cat.jiu.email.event;

import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public class EmailReceiveEvent {
	@Cancelable
	public static class Pre extends Event {
		public final ServerPlayerEntity player;
		public final Inbox inbox;
		private Email email;
		public final boolean isReceiveAll;
		public Pre(ServerPlayerEntity player, Inbox inbox, Email email, boolean isReceiveAll) {
			this.player = player;
			this.inbox = inbox;
			this.email = email;
			this.isReceiveAll = isReceiveAll;
		}
		public Email getEmail() {return email;}
		public void setMessage(Email message) {this.email = message;}
	}
	
	public static class Post extends Event {
		public final ServerPlayerEntity player;
		public final Inbox inbox;
		public final Email email;
		public final boolean isReceiveAll;
		public Post(ServerPlayerEntity player, Inbox inbox, Email email, boolean isReceiveAll) {
			this.player = player;
			this.inbox = inbox;
			this.email = email;
			this.isReceiveAll = isReceiveAll;
		}
	}
}
