package cat.jiu.email.event;

import java.util.UUID;

import cat.jiu.email.element.Email;

import cat.jiu.email.element.EmailSenderGroup;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class EmailSendEvent extends Event {
	public final TickEvent.Phase phase;
	public final EmailSenderGroup group;
	public final String addresser;
	public final Email email;
	
	public EmailSendEvent(TickEvent.Phase phase, EmailSenderGroup group, String addresser, Email email) {
		this.phase = phase;
		this.group = group;
		this.addresser = addresser;
		this.email = email;
	}
	@Override
	public final void setCanceled(boolean cancel) {
		if(this.phase == TickEvent.Phase.START) {
			super.setCanceled(cancel);
		}
	}
	
	@Deprecated
	@Cancelable
	public static class Pre extends Event {
		public final MinecraftServer server;
		public final EmailSenderGroup group;
		public final String addresserName;
		public final UUID addresserUID;
		public final Email email;
		
		public Pre(MinecraftServer server, EmailSenderGroup group, String addresserName, UUID addresserUID, Email email) {
			this.server = server;
			this.group = group;
			this.addresserName = addresserName;
			this.addresserUID = addresserUID;
			this.email = email;
		}
	}

	@Deprecated
	public static class Post extends Event {
		public final MinecraftServer server;
		public final EmailSenderGroup group;
		public final UUID addresser;
		public final Email email;
		
		public Post(MinecraftServer server, EmailSenderGroup group, UUID addresser, Email email) {
			this.server = server;
			this.group = group;
			this.addresser = addresser;
			this.email = email;
		}
	}
}
