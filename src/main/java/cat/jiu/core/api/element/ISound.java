package cat.jiu.core.api.element;

import com.google.gson.JsonObject;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.core.util.element.Sound;
import cat.jiu.core.util.timer.Timer;
import crafttweaker.annotations.ZenRegister;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;

import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("core.Sound")
public interface ISound extends ISerializable {
	@ZenGetter("sound")
	SoundEvent getSound();
	@ZenGetter("sound_str")
	default String getSoundAsString() {
		if(this.getSound() != null && this.getSound().getRegistryName()!=null) {
			return this.getSound().getRegistryName().toString();
		}
		return "null";
	}
	@ZenMethod("sound")
	ISound setSound(SoundEvent sound);
	@ZenMethod("sound")
	default ISound setSound(String sound) {
		return this.setSound(SoundEvent.REGISTRY.getObject(new ResourceLocation(sound)));
	}
	@ZenMethod("sound")
	default ISound setSound(int sound) {
		return this.setSound(SoundEvent.REGISTRY.getObjectById(sound));
	}

	@ZenGetter("volume")
	float getSoundVolume();
	@ZenMethod("volume")
	ISound setSoundVolume(float volume);

	@ZenGetter("pitch")
	float getSoundPitch();
	@ZenMethod("pitch")
	ISound setSoundPitch(float pitch);

	SoundCategory getSoundCategory();
	@ZenGetter("category")
	default String getSoundCategoryAsString() {
		return String.valueOf(this.getSoundCategory()).toLowerCase();
	}
	
	ISound setSoundCategory(SoundCategory sc);
	@ZenMethod("category")
	default ISound setSoundCategory(String category) {
		SoundCategory category_ =  SoundCategory.getByName(category);
		if(category_ != null) {
			return this.setSoundCategory(category_);
		}else {
			return this;
		}
	}
	@ZenMethod("category")
	default ISound setSoundCategory(int category) {
		SoundCategory category_ =  SoundCategory.values()[category];
		if(category_ != null) {
			return this.setSoundCategory(category_);
		}else {
			return this;
		}
	}

	BlockPos getPlayPosition();
	
	@ZenGetter("pos")
	default Pos getPlayPos() {
		return new Pos(this.getPlayPosition());
	}
	@ZenMethod("pos")
	ISound setPlayPosition(BlockPos pos);
	@ZenMethod("pos")
	default ISound setPlayPosition(int x, int y, int z) {
		return this.setPlayPosition(new BlockPos(x, y, z));
	}
	@ZenMethod("pos")
	default ISound setPlayPosition(Pos pos) {
		return this.setPlayPosition(pos.toBlockPos());
	}

	@ZenGetter("time")
	ITimer getTime();
	@ZenMethod("time")
	ISound setTime(ITimer time);

	@ZenGetter("follow")
	boolean isFollowEntity();
	@ZenMethod("follow")
	ISound setFollowEntity(boolean isFollow);

	@ZenGetter("played")
	boolean isPlayed();
	@ZenMethod("played")
	ISound setPlayed(boolean played);
	
	@ZenMethod
	ISound copy();
	
	@ZenMethod
	default NBTTagCompound write(NBTTagCompound nbt) {
		if(nbt==null) nbt = new NBTTagCompound();
		nbt.setString("sound", this.getSound().soundName.toString());
		nbt.setFloat("volume", this.getSoundVolume());
		nbt.setFloat("pitch", this.getSoundPitch());
		nbt.setLong("millis", this.getTime().getAllTicks());
		nbt.setString("category", this.getSoundCategory().getName());
		nbt.setBoolean("followEntity", this.isFollowEntity());
		if(!BlockPos.ORIGIN.equals(this.getPlayPosition())) {
			nbt.setTag("playPosition", writePositionNBT(this.getPlayPosition()));
		}
		return nbt;
	}
	@ZenMethod
	default void read(NBTTagCompound nbt) {
		this.setTime(new Timer(nbt.getLong("millis")));
		this.setSound(SoundEvent.REGISTRY.getObject(new ResourceLocation(nbt.getString("sound"))));
		this.setSoundVolume(nbt.getFloat("volume"));
		this.setSoundPitch(nbt.getFloat("pitch"));
		this.setSoundCategory(SoundCategory.getByName(nbt.getString("category")));
		this.setFollowEntity(nbt.getBoolean("followEntity"));
		if(nbt.hasKey("playPosition")) {
			this.setPlayPosition(readPosition(nbt.getCompoundTag("playPosition")));
		}
	}

	@ZenMethod
	default JsonObject write(JsonObject json) {
		if(json==null) json = new JsonObject();
		json.addProperty("id", this.getSound().soundName.toString());
		json.addProperty("pitch", this.getSoundPitch());
		json.addProperty("volume", this.getSoundVolume());
		json.addProperty("millis", this.getTime().getAllTicks());
		json.addProperty("category", this.getSoundCategory().getName());
		json.addProperty("followEntity", this.isFollowEntity());
		if(!BlockPos.ORIGIN.equals(this.getPlayPosition())) {
			json.add("playPosition", writePositionJson(this.getPlayPosition()));
		}
		return json;
	}
	@ZenMethod
	default void read(JsonObject json) {
		this.setTime(new Timer(json.get("millis").getAsLong()));
		
		SoundEvent sound = null;
		if(json.getAsJsonPrimitive("id").isString()) {
			sound = SoundEvent.REGISTRY.getObject(new ResourceLocation(json.get("id").getAsString()));
			if(sound == null) {
				sound = new SoundEvent(new ResourceLocation(json.get("id").getAsString()));
			}
		}else if(json.getAsJsonPrimitive("id").isNumber()) {
			sound = SoundEvent.REGISTRY.getObjectById(json.get("id").getAsInt());
		}
		this.setSound(sound);
		
		this.setSoundVolume(json.get("volume").getAsFloat());
		this.setSoundPitch(json.get("pitch").getAsFloat());
		this.setSoundCategory(SoundCategory.getByName(json.get("category").getAsString()));
		this.setFollowEntity(json.get("followEntity").getAsBoolean());
		if(json.has("playPosition")) {
			this.setPlayPosition(readPosition(json.getAsJsonObject("playPosition")));
		}
	}
	
	@Deprecated
	public static NBTTagCompound writePositionNBT(BlockPos pos) {
		return NBTUtil.createPosTag(pos);
	}
	@Deprecated
	public static BlockPos readPosition(NBTTagCompound nbt) {
		return NBTUtil.getPosFromTag(nbt);
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
	
	@ZenMethod
	static ISound from(ITimer time, String sound, float volume, float pitch, String category) {
		return new Sound(time, SoundEvent.REGISTRY.getObject(new ResourceLocation(sound)), volume, pitch, SoundCategory.getByName(category));
	}
	
	@ZenRegister
	@ZenClass("core.sound.Builder")
	public static class Builder {
		protected SoundEvent sound;
		protected float volume;
		protected float pitch;
		protected ITimer time;
		protected SoundCategory sc;
		protected boolean followEntity;
		protected BlockPos playPosition = BlockPos.ORIGIN;
		
		public Builder setSound(SoundEvent sound) {
			this.sound = sound;
			return this;
		}
		@ZenMethod("sound")
		public Builder setSound(String sound) {
			this.sound = SoundEvent.REGISTRY.getObject(new ResourceLocation(sound));
			return this;
		}
		@ZenMethod("volume")
		public Builder setVolume(float volume) {
			this.volume = volume;
			return this;
		}
		@ZenMethod("pitch")
		public Builder setPitch(float pitch) {
			this.pitch = pitch;
			return this;
		}
		@ZenMethod("time")
		public Builder setTime(ITimer time) {
			this.time = time;
			return this;
		}
		public Builder setSoundCategory(SoundCategory sc) {
			this.sc = sc;
			return this;
		}
		@ZenMethod("category")
		public Builder setSoundCategory(String sc) {
			this.sc = SoundCategory.getByName(sc);
			return this;
		}
		@ZenMethod("followEntity")
		public Builder setFollowEntity(boolean followEntity) {
			this.followEntity = followEntity;
			return this;
		}
		@ZenMethod("playPos")
		public Builder setPlayPosition(BlockPos playPosition) {
			this.playPosition = playPosition;
			return this;
		}
		
		@ZenMethod
		public ISound build() {
			ISound sound = new Sound(time, this.sound, volume, pitch, sc);
			sound.setFollowEntity(followEntity);
			sound.setPlayPosition(playPosition);
			return sound;
		}
	}
}
