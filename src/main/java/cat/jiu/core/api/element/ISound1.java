package cat.jiu.core.api.element;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.core.util.timer.Timer;
import cat.jiu.sql.SQLValues;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ISound1 extends ISerializable {
	SoundEvent getSound();
	ISound1 setSound(SoundEvent sound);
	
	float getSoundVolume();
	ISound1 setSoundVolume(float volume);
	
	float getSoundPitch();
	ISound1 setSoundPitch(float pitch);

	SoundSource getSoundSource();
	ISound1 setSoundCategory(SoundSource sc);
	
	BlockPos getPlayPosition();
	ISound1 setPlayPosition(BlockPos pos);
	
	ITimer getTime();
	ISound1 setTime(ITimer time);
	
	boolean isFollowEntity();
	ISound1 setFollowEntity(boolean isFollow);
	
	boolean isPlayed();
	ISound1 setPlayed(boolean played);
	
	ISound1 copy();
	
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
		this.setFollowEntity(json.has("followEntity") && json.get("followEntity").getAsBoolean());
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
	
	static CompoundTag writePositionNBT(BlockPos pos) {
		CompoundTag nbt = new CompoundTag();
		nbt.putInt("x", pos.getX());
		nbt.putInt("y", pos.getY());
		nbt.putInt("z", pos.getZ());
		return nbt;
	}
	static BlockPos readPosition(CompoundTag nbt) {
		return new BlockPos(nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
	}
	
	static JsonObject writePositionJson(BlockPos pos) {
		JsonObject nbt = new JsonObject();
		nbt.addProperty("x", pos.getX());
		nbt.addProperty("y", pos.getY());
		nbt.addProperty("z", pos.getZ());
		return nbt;
	}
	static BlockPos readPosition(JsonObject nbt) {
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
