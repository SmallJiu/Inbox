package cat.jiu.core.util.element;

import com.google.gson.JsonObject;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.ISound;
import cat.jiu.core.util.timer.Timer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;

public class Sound implements ISound {
	public static final ITimer EMPTY_PLAY_TYIME = new Timer(0);
	protected SoundEvent sound;
	protected float volume;
	protected float pitch;
	protected ITimer time;
	protected SoundCategory sc;
	protected boolean followEntity;
	protected boolean played;
	protected BlockPos playPosition = BlockPos.ORIGIN;
	
	public Sound(NBTTagCompound nbt) {
		this.read(nbt);
	}
	
	public Sound(JsonObject json) {
		this.read(json);
	}
	
	public Sound(ITimer soundTime, SoundEvent sound, float volume, float pitch, SoundCategory sc) {
		this.setSound(sound);
		this.setSoundVolume(volume);
		this.setSoundPitch(pitch);
		this.setTime(soundTime);
		this.setSoundCategory(sc);
	}

	public SoundEvent getSound() { return sound; }
	public Sound setSound(SoundEvent sound) { this.sound = sound; return this; }
	
	public float getSoundVolume() { return this.volume; }
	public Sound setSoundVolume(float volume) { this.volume = volume; return this; }
	
	public float getSoundPitch() { return this.pitch; }
	public Sound setSoundPitch(float pitch) { this.pitch = pitch; return this; }
	
	public SoundCategory getSoundCategory() {return this.sc;}
	public Sound setSoundCategory(SoundCategory sc) { this.sc = sc; return this; }
	
	public BlockPos getPlayPosition() { return this.playPosition; }
	public Sound setPlayPosition(BlockPos pos) { this.playPosition = pos; return this; }
	
	public ITimer getTime() {return time;}
	public Sound setTime(ITimer time) { this.time = time; return this; }
	
	public boolean isFollowEntity() { return this.followEntity; }
	public Sound setFollowEntity(boolean isFollow) { this.followEntity = isFollow; return this; }
	
	public boolean isPlayed() { return this.played; }
	public Sound setPlayed(boolean played) { this.played = played; return this; }
	
	public Sound copy() {return new Sound(this.writeTo(NBTTagCompound.class));}
}
