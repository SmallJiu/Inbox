package cat.jiu.email.event;

import cat.jiu.core.api.element.ISound;
import net.minecraftforge.eventbus.api.Event;

public class InboxPlaySoundEvent extends Event {
	public final ISound sound;
	public final long emailID;
	protected InboxPlaySoundEvent(ISound sound, long emailID) {
		this.sound = sound;
		this.emailID = emailID;
	}
	public static class Start extends InboxPlaySoundEvent {
		public Start(ISound sound, long emailID) {
			super(sound, emailID);
		}
	}
	public static class Tick extends InboxPlaySoundEvent {
		public Tick(ISound sound, long emailID) {
			super(sound, emailID);
		}
	}
	public static class Stop extends Event {
		public final long emailID;
		public Stop(long emailID) {
			this.emailID = emailID;
		}
	}
}
