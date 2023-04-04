package cat.jiu.email.event;

import java.util.UUID;

import cat.jiu.email.element.Email;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

@Cancelable
public class EmailSendEvent extends Event {
	public final Phase phase;
	public final EmailSenderGroup group;
	public final String addresser;
	public final Email email;
	
	public EmailSendEvent(Phase phase, EmailSenderGroup group, String addresser, Email email) {
		this.phase = phase;
		this.group = group;
		this.addresser = addresser;
		this.email = email;
	}
	@Override
	public final void setCanceled(boolean cancel) {
		if(this.phase == Phase.START) {
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
	
	public static enum EmailSenderGroup {
		SYSTEM, PLAYER;
		public static EmailSenderGroup getGroupByID(int id) {
			switch(id) {
				case 1: return PLAYER;
				default: return SYSTEM;
			}
		}
		public static int getIDByGroup(EmailSenderGroup sender) {
			switch(sender) {
				case PLAYER: return 1;
				default: return 0;
			}
		}
		public boolean isPlayerSend() {
			return this == PLAYER;
		}
		public boolean isSystemSend() {
			return this == SYSTEM;
		}
	}
}
