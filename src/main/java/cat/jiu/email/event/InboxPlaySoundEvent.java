package cat.jiu.email.event;

import cat.jiu.email.element.Email;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class InboxPlaySoundEvent extends Event {
	public final Email email;
	protected InboxPlaySoundEvent(Email email) {
		this.email = email;
	}

	public static class Start extends InboxPlaySoundEvent implements ICancellableEvent {
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
	public static class Stop extends InboxPlaySoundEvent implements ICancellableEvent {
		public Stop(Email email) {
			super(email);
		}
	}
}
