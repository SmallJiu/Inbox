package cat.jiu.core.util.element.sound;

import cat.jiu.core.api.element.ISound;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.NBTUtils;
import cat.jiu.core.util.client.AudioSystem;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

public class SoundJmp123 extends ISound.BaseSound {
    public static final ResourceLocation ID = new ResourceLocation("jiucore", "element/sound/jmp123");

    private AudioSystem.Audio audio;
    private UUID uid;
    public SoundJmp123() {
        super(ID);
    }

    public SoundJmp123(AudioSystem.Audio audio) {
        this();
        this.audio = audio;
        this.setSoundChannel(audio.getSoundChannel());
    }

    public AudioSystem.Audio getAudio() {
        return audio;
    }

    public SoundJmp123 setAudio(AudioSystem.Audio audio) {
        this.audio = audio;
        if (audio!=null) this.setSoundChannel(audio.getSoundChannel());
        return this;
    }

    @Override
    public String getAudioFile() {
        return this.audio != null ? this.audio.getFile() : "";
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void play() {
        if (this.isStopped()) {
            this.uid = AudioSystem.play(this.getAudio());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isPlayed() {
        return !this.isStopped();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void pause(boolean pauseSound) {
        if (this.isPlayed()) {
            AudioSystem.pause(this.uid, pauseSound);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isPaused() {
        return AudioSystem.isPaused(this.uid);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void stop() {
        if (this.isPlayed()) {
            AudioSystem.stop(this.uid);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isStopped() {
        return this.uid == null || AudioSystem.isClose(this.uid);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public float getFloatDuration() {
        return this.isPlayed() ? AudioSystem.getFloatDuration(this.uid) : 0;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public float getFloatElapse() {
        return this.isPlayed() ? AudioSystem.getFloatElapse(this.uid) : 0;
    }

    @Override
    public float getCurrentSoundVolume() {
        return this.isPlayed() ? AudioSystem.getVolume(this.uid) : -40;
    }

    @Override
    public float getFullSoundVolume() {
        return 80f;
    }

    @Override
    public ISound setSoundLooping(boolean isLooping) {
        this.getAudio().setCanLopping(isLooping);
        return super.setSoundLooping(isLooping);
    }

    @Override
    public boolean isSoundLooping() {
        return this.getAudio().isCanLopping();
    }

    @Override
    public SoundCategory getSoundChannel() {
        return this.getAudio() != null ? this.getAudio().getSoundChannel() : super.getSoundChannel();
    }

    @Override
    public JsonObject write(JsonObject data) {
        super.write(data);
        data.add("audio", this.audio.write(new JsonObject()));
        return data;
    }

    @Override
    public void read(JsonObject data) {
        super.read(data);
        this.setAudio(AudioSystem.Audio.create(JsonUtils.get(data, "audio", new JsonObject())));
    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        super.write(data);
        data.put("file", this.audio.write(new CompoundNBT()));
        return data;
    }

    @Override
    public void read(CompoundNBT data) {
        super.read(data);
        this.setAudio(AudioSystem.Audio.create(NBTUtils.get(data, "audio", new CompoundNBT())));
    }
}
