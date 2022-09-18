package cat.jiu.email.util;

import com.google.gson.JsonObject;

import cat.jiu.email.util.EmailSenderSndSound.Time;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundEvent;

public class EmailSound {
	protected final SoundEvent sound;
	protected final float volume;
	protected final float pitch;
	protected final Time time;
	
	public EmailSound(Time soundTime, SoundEvent sound, float volume, float pitch) {
		this.sound = sound;
		this.volume = volume;
		this.pitch = pitch;
		this.time = soundTime;
	}

	public SoundEvent getSound() {return sound;}
	public float getSoundVolume() {return this.volume;}
	public float getSoundPitch() {return this.pitch;}
	public Time getTime() {return time;}
	
	public NBTTagCompound toNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("sound", SoundEvent.REGISTRY.getIDForObject(this.sound));
		nbt.setFloat("volume", this.volume);
		nbt.setFloat("pitch", this.pitch);
		nbt.setLong("time", this.time.tick);
		return nbt;
	}
	
	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("id", SoundEvent.REGISTRY.getIDForObject(this.sound));
		json.addProperty("pitch", this.pitch);
		json.addProperty("volume", this.volume);
		json.addProperty("time", this.time.tick);
		return json;
	}
	
	public static EmailSound from(NBTTagCompound nbt) {
		if(nbt!=null) {
			return new EmailSound(new Time(nbt.getLong("time")), SoundEvent.REGISTRY.getObjectById(nbt.getInteger("sound")), nbt.getFloat("volume"), nbt.getFloat("pitch"));
		}
		return null;
	}
	public static EmailSound from(JsonObject json) {
		if(json!=null&&json.size()>0) {
			return new EmailSound(new Time(json.get("time").getAsLong()), SoundEvent.REGISTRY.getObjectById(json.get("id").getAsInt()), json.get("volume").getAsFloat(), json.get("pitch").getAsFloat());
		}
		return null;
	}
}
