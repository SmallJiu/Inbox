package cat.jiu.email.event;

import cat.jiu.core.util.client.AudioSystem;
import net.minecraftforge.eventbus.api.Event;

import java.util.UUID;

public class InboxPlayNetworkOrLocalSoundEvent extends Event {
	public final UUID audioUUID;
	public final AudioSystem.Audio audio;
	public final long emailID;
	protected InboxPlayNetworkOrLocalSoundEvent(UUID audioUUID, AudioSystem.Audio audio, long emailID) {
		this.audio = audio;
		this.audioUUID = audioUUID;
		this.emailID = emailID;
	}
	public static class Start extends InboxPlayNetworkOrLocalSoundEvent {
		public Start(UUID sound, AudioSystem.Audio audio, long emailID) {
			super(sound, audio, emailID);
		}
	}
	public static class Tick extends InboxPlayNetworkOrLocalSoundEvent {
		public Tick(UUID sound, AudioSystem.Audio audio, long emailID) {
			super(sound, audio, emailID);
		}
	}
}
