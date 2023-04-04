package cat.jiu.email.event;

import cat.jiu.email.iface.IInboxSound;
import net.minecraftforge.fml.common.eventhandler.Event;

public class InboxPlaySoundEvent extends Event {
	public final IInboxSound sound;
	public final long emailID;
	protected InboxPlaySoundEvent(IInboxSound sound, long emailID) {
		this.sound = sound;
		this.emailID = emailID;
	}
	public static class Start extends InboxPlaySoundEvent {
		public Start(IInboxSound sound, long emailID) {
			super(sound, emailID);
		}
	}
	public static class Tick extends InboxPlaySoundEvent {
		public Tick(IInboxSound sound, long emailID) {
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
