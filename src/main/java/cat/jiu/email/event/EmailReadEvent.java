package cat.jiu.email.event;

import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public class EmailReadEvent {
	@Cancelable
	public static class Pre extends Event {
		public final ServerPlayerEntity player;
		public final Inbox inbox;
		private Email email;
		public final boolean isReadAll;
		public Pre(ServerPlayerEntity player, Inbox inbox, Email email, boolean isReadAll) {
			this.player = player;
			this.inbox = inbox;
			this.email = email;
			this.isReadAll = isReadAll;
		}
		public Email getEmail() {return email;}
		public void setMessage(Email message) {this.email = message;}
	}
	
	public static class Post extends Event {
		public final ServerPlayerEntity player;
		public final Inbox inbox;
		public final Email email;
		public final boolean isReadAll;
		public Post(ServerPlayerEntity player, Inbox inbox, Email email, boolean isReadAll) {
			this.player = player;
			this.inbox = inbox;
			this.email = email;
			this.isReadAll = isReadAll;
		}
	}
}
