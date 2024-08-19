package cat.jiu.email.event;

import cat.jiu.email.element.Email;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public class InboxPlaySoundEvent extends Event {
	public final Email email;
	protected InboxPlaySoundEvent(Email email) {
		this.email = email;
	}

	@Cancelable
	public static class Start extends InboxPlaySoundEvent {
		public Start(Email email) {
			super(email);
		}
	}
	public static class Tick extends InboxPlaySoundEvent {
		private String elapse, duration;
		public Tick(Email email) {
			super(email);
		}

		public String getElapse() {
			return elapse;
		}

		public void setElapse(String elapse) {
			this.elapse = elapse;
		}

		public String getDuration() {
			return duration;
		}

		public void setDuration(String duration) {
			this.duration = duration;
		}
	}
	@Cancelable
	public static class Stop extends InboxPlaySoundEvent {
		public Stop(Email email) {
			super(email);
		}
	}
}
