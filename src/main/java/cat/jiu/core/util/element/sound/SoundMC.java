package cat.jiu.core.util.element.sound;

import cat.jiu.core.api.element.ISound;
import cat.jiu.core.util.client.AudioSystem;
import cat.jiu.core.util.client.FollowPosSoundInstance;
import cat.jiu.core.util.timer.MillisTimer;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

public class SoundMC extends ISound.BaseSound {
    public static final ResourceLocation ID = new ResourceLocation("jiucore", "element/sound/mc");

    @OnlyIn(Dist.CLIENT)
    protected FollowPosSoundInstance mcSound;
    protected SoundEvent soundEvent;
    protected long duration = 0,
            currentStart, currentDuration, elapse = 0;

    public SoundMC() {
        super(ID);
    }

    public SoundMC(SoundEvent soundEvent) {
        this();
        this.soundEvent = soundEvent;
    }

    public SoundEvent getSoundEvent() {
        return soundEvent;
    }

    public SoundMC setSoundEvent(SoundEvent soundEvent) {
        this.soundEvent = soundEvent;
        return this;
    }

    protected boolean followEntity;
    protected Entity entity;
    protected BlockPos pos;
    public void setFollowEntity(Entity entity) {
        this.followEntity = true;
        this.entity = entity;
    }

    public void setFollowPos(BlockPos pos) {
        this.followEntity = false;
        this.pos = pos;
    }

    @Override
    public String getAudioFile() {
        return this.getSoundEvent().getName().toString();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void play() {
        if (this.isStopped() || this.isPaused()) {
            this.mcSound = new FollowPosSoundInstance(this);
            this.mcSound.setFollowing(this.followEntity, this.entity, this.pos);

            Minecraft.getInstance().getSoundHandler().play(this.mcSound);

            this.currentStart = System.currentTimeMillis();
            this.currentDuration = this.currentStart + this.duration;
            this.pause = false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isPlayed() {
        return this.mcSound != null && !this.mcSound.isDonePlaying();
    }

    protected boolean pause = true;
    @OnlyIn(Dist.CLIENT)
    @Override
    public void pause(boolean pauseSound) {
        if (pauseSound) {
            this.pause = true;
            this.elapse = this.currentDuration - (this.currentDuration - System.currentTimeMillis());
            Minecraft.getInstance().getSoundHandler().pause();
        }else {
            this.pause = false;
            Minecraft.getInstance().getSoundHandler().resume();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isPaused() {
        return this.pause;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void stop() {
        this.pause = true;
        Minecraft.getInstance().getSoundHandler().stop(this.mcSound);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isStopped() {
        return this.mcSound==null || this.mcSound.isDonePlaying();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public float getFloatDuration() {
        return this.duration;
    }

    public SoundMC setDuration(int minutes, int seconds, int millis) {
        return this.setDuration(MillisTimer.parseMillis(minutes, seconds, millis));
    }
    public SoundMC setDuration(MillisTimer duration) {
        return this.setDuration(duration.getNotStartedMillis());
    }
    public SoundMC setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public float getFloatElapse() {
        return this.isPaused() ? this.elapse : this.currentDuration - (this.currentDuration - System.currentTimeMillis());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public float getCurrentSoundVolume() {
        return AudioSystem.getMinecraftVolume(this.getSoundChannel());
    }

    @Override
    public JsonObject write(JsonObject data) {
        super.write(data);
        data.addProperty("sound", String.valueOf(ForgeRegistries.SOUND_EVENTS.getKey(this.getSoundEvent())));
        data.addProperty("duration", this.getDuration());
        return data;
    }

    @Override
    public void read(JsonObject data) {
        super.read(data);
        this.setSoundEvent(ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(data.get("sound").getAsString())));
        this.setDuration(data.get("duration").getAsLong());
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        data.putString("sound", String.valueOf(ForgeRegistries.SOUND_EVENTS.getKey(this.getSoundEvent())));
        data.putLong("duration", this.getDuration());
        return data;
    }

    @Override
    public void read(CompoundNBT data) {
        super.read(data);
        this.setSoundEvent(ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(data.getString("sound"))));
        this.setDuration(data.getLong("duration"));
    }
}
