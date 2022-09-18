package cat.jiu.email.event;

import com.google.gson.JsonObject;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public class EmailDeleteEvent {
	@Cancelable
	public static class Pre extends Event {
		public final JsonObject email;
		public final int message;
		public final boolean isDeleteAllRead;
		public final boolean isDeleteAllReceive;
		public Pre(JsonObject email, int messageID, boolean isDeleteAllRead, boolean isDeleteAllReceive) {
			this.email = email;
			this.message = messageID;
			this.isDeleteAllRead = isDeleteAllRead;
			this.isDeleteAllReceive = isDeleteAllReceive;
		}
	}
	
	public static class Post extends Event {
		public final JsonObject email;
		public final int messageID;
		public final boolean isDeleteAllRead;
		public final boolean isDeleteAllReceive;
		public Post(JsonObject email, int messageID, boolean isDeleteAllRead, boolean isDeleteAllReceive) {
			this.email = email;
			this.messageID = messageID;
			this.isDeleteAllRead = isDeleteAllRead;
			this.isDeleteAllReceive = isDeleteAllReceive;
		}
	}
}
