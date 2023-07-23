package cat.jiu.email.event;

import cat.jiu.email.element.Inbox;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public class EmailDeleteEvent {
	@Cancelable
	public static class Pre extends Event {
		public final Inbox email;
		public final long message;
		public final boolean isDeleteAllRead;
		public final boolean isDeleteAllReceive;
		public Pre(Inbox email, long messageID, boolean isDeleteAllRead, boolean isDeleteAllReceive) {
			this.email = email;
			this.message = messageID;
			this.isDeleteAllRead = isDeleteAllRead;
			this.isDeleteAllReceive = isDeleteAllReceive;
		}
	}
	
	public static class Post extends Event {
		public final Inbox email;
		public final long messageID;
		public final boolean isDeleteAllRead;
		public final boolean isDeleteAllReceive;
		public Post(Inbox email, long messageID, boolean isDeleteAllRead, boolean isDeleteAllReceive) {
			this.email = email;
			this.messageID = messageID;
			this.isDeleteAllRead = isDeleteAllRead;
			this.isDeleteAllReceive = isDeleteAllReceive;
		}
	}
}
