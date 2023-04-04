package cat.jiu.email.iface;

import com.google.gson.JsonObject;

import cat.jiu.email.element.InboxSound;
import cat.jiu.email.element.InboxTime;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundEvent;

public interface IInboxSound {
	SoundEvent getSound();
	IInboxSound setSound(SoundEvent sound);
	
	float getSoundVolume();
	IInboxSound setSoundVolume(float volume);
	
	float getSoundPitch();
	IInboxSound setSoundPitch(float pitch);
	
	IInboxTime getTime();
	IInboxSound setTime(IInboxTime time);
	
	@Deprecated
	default NBTTagCompound toNBT() {
		return this.write(new NBTTagCompound());
	}
	default NBTTagCompound write(NBTTagCompound nbt) {
		if(nbt==null) nbt = new NBTTagCompound();
		nbt.setInteger("sound", SoundEvent.REGISTRY.getIDForObject(this.getSound()));
		nbt.setFloat("volume", this.getSoundVolume());
		nbt.setFloat("pitch", this.getSoundPitch());
		nbt.setLong("millis", this.getTime().getAllTicks());
		return nbt;
	}
	default void read(NBTTagCompound nbt) {
		this.setTime(new InboxTime(nbt.getLong("millis")));
		this.setSound(SoundEvent.REGISTRY.getObjectById(nbt.getInteger("sound")));
		this.setSoundVolume(nbt.getFloat("volume"));
		this.setSoundPitch(nbt.getFloat("pitch"));
	}
	
	@Deprecated
	default JsonObject toJson() {
		return this.write(new JsonObject());
	}
	default JsonObject write(JsonObject json) {
		if(json==null) json = new JsonObject();
		json.addProperty("id", SoundEvent.REGISTRY.getIDForObject(this.getSound()));
		json.addProperty("pitch", this.getSoundPitch());
		json.addProperty("volume", this.getSoundVolume());
		json.addProperty("millis", this.getTime().getAllTicks());
		return json;
	}
	default void read(JsonObject json) {
		this.setTime(new InboxTime(json.get("millis").getAsLong()));
		this.setSound(SoundEvent.REGISTRY.getObjectById(json.get("id").getAsInt()));
		this.setSoundVolume(json.get("volume").getAsFloat());
		this.setSoundPitch(json.get("pitch").getAsFloat());
	}
	
	default IInboxSound copy() {
		return new InboxSound(this.write(new NBTTagCompound()));
	}
}
