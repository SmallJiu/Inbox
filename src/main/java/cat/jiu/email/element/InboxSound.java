package cat.jiu.email.element;

import com.google.gson.JsonObject;

import cat.jiu.email.iface.IInboxSound;
import cat.jiu.email.iface.IInboxTime;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundEvent;

public class InboxSound implements IInboxSound {
	protected SoundEvent sound;
	protected float volume;
	protected float pitch;
	protected IInboxTime time;
	
	public InboxSound(NBTTagCompound nbt) {
		this.read(nbt);
	}
	
	public InboxSound(JsonObject json) {
		this.read(json);
	}
	
	public InboxSound(IInboxTime soundTime, SoundEvent sound, float volume, float pitch) {
		this.setSound(sound);
		this.setSoundVolume(volume);
		this.setSoundPitch(pitch);
		this.setTime(soundTime);
	}

	public SoundEvent getSound() {return sound;}
	public InboxSound setSound(SoundEvent sound) { this.sound = sound; return this; }
	public float getSoundVolume() {return this.volume;}
	public InboxSound setSoundVolume(float volume) { this.volume = volume; return this; }
	
	public float getSoundPitch() {return this.pitch;}
	public InboxSound setSoundPitch(float pitch) { this.pitch = pitch; return this; }
	
	public IInboxTime getTime() {return time;}
	public IInboxSound setTime(IInboxTime time) { this.time = time; return this; }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(pitch);
		result = prime * result + ((sound == null) ? 0 : sound.hashCode());
		result = prime * result + ((time == null) ? 0 : time.hashCode());
		result = prime * result + Float.floatToIntBits(volume);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		InboxSound other = (InboxSound) obj;
		if(Float.floatToIntBits(pitch) != Float.floatToIntBits(other.pitch))
			return false;
		if(sound == null) {
			if(other.sound != null)
				return false;
		}else if(!sound.equals(other.sound))
			return false;
		if(time == null) {
			if(other.time != null)
				return false;
		}else if(!time.equals(other.time))
			return false;
		if(Float.floatToIntBits(volume) != Float.floatToIntBits(other.volume))
			return false;
		return true;
	}

	@Deprecated
	public static InboxSound from(NBTTagCompound nbt) {
		if(nbt!=null) {
			return new InboxSound(new InboxTime(nbt.getLong("millis")), SoundEvent.REGISTRY.getObjectById(nbt.getInteger("sound")), nbt.getFloat("volume"), nbt.getFloat("pitch"));
		}
		return null;
	}
	@Deprecated
	public static InboxSound from(JsonObject json) {
		if(json!=null&&json.size()>0) {
			return new InboxSound(new InboxTime(json.get("millis").getAsLong()), SoundEvent.REGISTRY.getObjectById(json.get("id").getAsInt()), json.get("volume").getAsFloat(), json.get("pitch").getAsFloat());
		}
		return null;
	}
}
