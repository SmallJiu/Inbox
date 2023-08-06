package cat.jiu.core.api.element;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.JsonObject;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.core.util.timer.Timer;
import cat.jiu.sql.SQLValues;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public interface ISound extends ISerializable {
	SoundEvent getSound();
	ISound setSound(SoundEvent sound);
	
	float getSoundVolume();
	ISound setSoundVolume(float volume);
	
	float getSoundPitch();
	ISound setSoundPitch(float pitch);

	SoundSource getSoundSource();
	ISound setSoundCategory(SoundSource sc);
	
	BlockPos getPlayPosition();
	ISound setPlayPosition(BlockPos pos);
	
	ITimer getTime();
	ISound setTime(ITimer time);
	
	boolean isFollowEntity();
	ISound setFollowEntity(boolean isFollow);
	
	boolean isPlayed();
	ISound setPlayed(boolean played);
	
	ISound copy();
	
	default CompoundTag write(CompoundTag nbt) {
		if(nbt==null) nbt = new CompoundTag();
		nbt.putString("sound", BuiltInRegistries.SOUND_EVENT.getKey(this.getSound()).toString());
		nbt.putFloat("volume", this.getSoundVolume());
		nbt.putFloat("pitch", this.getSoundPitch());
		nbt.putLong("millis", this.getTime().getAllTicks());
		nbt.putString("category", this.getSoundSource().getName());
		nbt.putBoolean("followEntity", this.isFollowEntity());
		if(!BlockPos.ZERO.equals(this.getPlayPosition())) {
			nbt.put("playPosition", writePositionNBT(this.getPlayPosition()));
		}
		return nbt;
	}
	default void read(CompoundTag nbt) {
		this.setTime(new Timer(nbt.getLong("millis")));
		this.setSound(BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(nbt.getString("sound"))));
		this.setSoundVolume(nbt.getFloat("volume"));
		this.setSoundPitch(nbt.getFloat("pitch"));
		this.setSoundCategory(getSoundCategoryByName(nbt.getString("category")));
		this.setFollowEntity(nbt.getBoolean("followEntity"));
		if(nbt.contains("playPosition")) {
			this.setPlayPosition(readPosition(nbt.getCompound("playPosition")));
		}
	}
	
	default JsonObject write(JsonObject json) {
		if(json==null) json = new JsonObject();
		json.addProperty("id", BuiltInRegistries.SOUND_EVENT.getKey(this.getSound()).toString());
		json.addProperty("pitch", this.getSoundPitch());
		json.addProperty("volume", this.getSoundVolume());
		json.addProperty("millis", this.getTime().getAllTicks());
		json.addProperty("category", this.getSoundSource().getName());
		json.addProperty("followEntity", this.isFollowEntity());
		if(!BlockPos.ZERO.equals(this.getPlayPosition())) {
			json.add("playPosition", writePositionJson(this.getPlayPosition()));
		}
		return json;
	}
	default void read(JsonObject json) {
		this.setTime(new Timer(json.get("millis").getAsLong()));
		
		SoundEvent sound = null;
		if(json.getAsJsonPrimitive("id").isString()) {
			sound = BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation(json.get("id").getAsString()));
		}else if(json.getAsJsonPrimitive("id").isNumber()) {
			sound = BuiltInRegistries.SOUND_EVENT.getHolder(json.get("id").getAsInt()).get().get();
		}
		this.setSound(sound);
		
		this.setSoundVolume(json.get("volume").getAsFloat());
		this.setSoundPitch(json.get("pitch").getAsFloat());
		this.setSoundCategory(json.has("category") ? getSoundCategoryByName(json.get("category").getAsString()) : SoundSource.PLAYERS);
		this.setFollowEntity(json.has("followEntity") ? json.get("followEntity").getAsBoolean() : false);
		if(json.has("playPosition")) {
			this.setPlayPosition(readPosition(json.getAsJsonObject("playPosition")));
		}
	}
	
	@Override
	default SQLValues write(SQLValues value) {
		return value;
	}
	@Override
	default void read(ResultSet result) throws SQLException {}
	
	public static CompoundTag writePositionNBT(BlockPos pos) {
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("x", pos.getX());
		nbt.putInt("y", pos.getY());
		nbt.putInt("z", pos.getZ());
		return nbt;
	}
	public static BlockPos readPosition(CompoundTag nbt) {
		return new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
	}
	
	public static JsonObject writePositionJson(BlockPos pos) {
		JsonObject nbt = new JsonObject();
		nbt.addProperty("x", pos.getX());
		nbt.addProperty("y", pos.getY());
		nbt.addProperty("z", pos.getZ());
		return nbt;
	}
	public static BlockPos readPosition(JsonObject nbt) {
		return new BlockPos(nbt.get("x").getAsInt(), nbt.get("y").getAsInt(), nbt.get("z").getAsInt());
	}

	static SoundSource getSoundCategoryByName(String name){
		for (SoundSource value : SoundSource.values()) {
			if(value.getName().contentEquals(name)){
				return value;
			}
		}
		return null;
	}
}
