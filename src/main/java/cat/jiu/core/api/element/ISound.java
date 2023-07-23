package cat.jiu.core.api.element;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.JsonObject;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.core.util.timer.Timer;
import cat.jiu.sql.SQLValues;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

public interface ISound extends ISerializable {
	SoundEvent getSound();
	ISound setSound(SoundEvent sound);
	
	float getSoundVolume();
	ISound setSoundVolume(float volume);
	
	float getSoundPitch();
	ISound setSoundPitch(float pitch);
	
	SoundCategory getSoundCategory();
	ISound setSoundCategory(SoundCategory sc);
	
	BlockPos getPlayPosition();
	ISound setPlayPosition(BlockPos pos);
	
	ITimer getTime();
	ISound setTime(ITimer time);
	
	boolean isFollowEntity();
	ISound setFollowEntity(boolean isFollow);
	
	boolean isPlayed();
	ISound setPlayed(boolean played);
	
	ISound copy();
	
	default CompoundNBT write(CompoundNBT nbt) {
		if(nbt==null) nbt = new CompoundNBT();
		nbt.putString("sound", Registry.SOUND_EVENT.getKey(this.getSound()).toString());
		nbt.putFloat("volume", this.getSoundVolume());
		nbt.putFloat("pitch", this.getSoundPitch());
		nbt.putLong("millis", this.getTime().getAllTicks());
		nbt.putString("category", this.getSoundCategory().getName());
		nbt.putBoolean("followEntity", this.isFollowEntity());
		if(!BlockPos.ZERO.equals(this.getPlayPosition())) {
			nbt.put("playPosition", writePositionNBT(this.getPlayPosition()));
		}
		return nbt;
	}
	default void read(CompoundNBT nbt) {
		this.setTime(new Timer(nbt.getLong("millis")));
		this.setSound(Registry.SOUND_EVENT.getOptional(new ResourceLocation(nbt.getString("sound"))).get());
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
		json.addProperty("id", Registry.SOUND_EVENT.getKey(this.getSound()).toString());
		json.addProperty("pitch", this.getSoundPitch());
		json.addProperty("volume", this.getSoundVolume());
		json.addProperty("millis", this.getTime().getAllTicks());
		json.addProperty("category", this.getSoundCategory().getName());
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
			sound = Registry.SOUND_EVENT.getOptional(new ResourceLocation(json.get("id").getAsString())).get();
		}else if(json.getAsJsonPrimitive("id").isNumber()) {
			sound = Registry.SOUND_EVENT.getByValue(json.get("id").getAsInt());
		}
		this.setSound(sound);
		
		this.setSoundVolume(json.get("volume").getAsFloat());
		this.setSoundPitch(json.get("pitch").getAsFloat());
		this.setSoundCategory(json.has("category") ? getSoundCategoryByName(json.get("category").getAsString()) : SoundCategory.PLAYERS);
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
	
	public static CompoundNBT writePositionNBT(BlockPos pos) {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putInt("x", pos.getX());
		nbt.putInt("y", pos.getY());
		nbt.putInt("z", pos.getZ());
		return nbt;
	}
	public static BlockPos readPosition(CompoundNBT nbt) {
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

	static SoundCategory getSoundCategoryByName(String name){
		for (SoundCategory value : SoundCategory.values()) {
			if(value.getName().contentEquals(name)){
				return value;
			}
		}
		return null;
	}
}
