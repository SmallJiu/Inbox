package cat.jiu.email.event;

import com.google.gson.JsonObject;

import net.minecraft.entity.player.EntityPlayerMP;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public class EmailReadEvent {
	@Cancelable
	public static class Pre extends Event {
		public final EntityPlayerMP player;
		public final JsonObject email;
		private JsonObject message;
		public final boolean isReadAll;
		public Pre(EntityPlayerMP player, JsonObject email, JsonObject message, boolean isReadAll) {
			this.player = player;
			this.email = email;
			this.message = message;
			this.isReadAll = isReadAll;
		}
		public JsonObject getMessage() {return message;}
		public void setMessage(JsonObject message) {this.message = message;}
	}
	
	public static class Post extends Event {
		public final EntityPlayerMP player;
		public final JsonObject email;
		public final JsonObject message;
		public final boolean isReadAll;
		public Post(EntityPlayerMP player, JsonObject email, JsonObject message, boolean isReadAll) {
			this.player = player;
			this.email = email;
			this.message = message;
			this.isReadAll = isReadAll;
		}
	}
}