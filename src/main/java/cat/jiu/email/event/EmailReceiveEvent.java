package cat.jiu.email.event;

import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;

import net.minecraft.entity.player.EntityPlayerMP;

import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public class EmailReceiveEvent {
	@Cancelable
	public static class Pre extends Event {
		public final EntityPlayerMP player;
		public final Inbox inbox;
		private Email email;
		public final boolean isReceiveAll;
		public Pre(EntityPlayerMP player, Inbox inbox, Email email, boolean isReceiveAll) {
			this.player = player;
			this.inbox = inbox;
			this.email = email;
			this.isReceiveAll = isReceiveAll;
		}
		public Email getEmail() {return email;}
		public void setMessage(Email message) {this.email = message;}
	}
	
	public static class Post extends Event {
		public final EntityPlayerMP player;
		public final Inbox inbox;
		public final Email email;
		public final boolean isReceiveAll;
		public Post(EntityPlayerMP player, Inbox inbox, Email email, boolean isReceiveAll) {
			this.player = player;
			this.inbox = inbox;
			this.email = email;
			this.isReceiveAll = isReceiveAll;
		}
	}
}
