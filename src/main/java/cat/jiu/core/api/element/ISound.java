package cat.jiu.core.api.element;

import cat.jiu.core.api.handler.IJsonSerializable;
import cat.jiu.core.api.handler.INBTSerializable;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.NBTUtils;
import cat.jiu.core.util.registry.DynamicRegistry;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

public interface ISound extends IJsonSerializable, INBTSerializable, Supplier<ResourceLocation> {
    public static final String ID_NAME = "id";
    DynamicRegistry<ResourceLocation, ISound> REGISTRY = new DynamicRegistry<ResourceLocation, ISound>("jiucore", "element/sound")
            .setKeyGetter(
                    data->new ResourceLocation(NBTUtils.get(data, ID_NAME, "")),
                    data->new ResourceLocation(JsonUtils.get(data, ID_NAME, ""))
            );

    ResourceLocation getSoundID();
    @Override
    default ResourceLocation get(){
        return this.getSoundID();
    }

    String getAudioFile();

    @OnlyIn(Dist.CLIENT)
    void play();
    @OnlyIn(Dist.CLIENT)
    boolean isPlayed();

    @OnlyIn(Dist.CLIENT)
    void pause(boolean pauseSound);
    @OnlyIn(Dist.CLIENT)
    boolean isPaused();

    @OnlyIn(Dist.CLIENT)
    void stop();
    @OnlyIn(Dist.CLIENT)
    boolean isStopped();

    float getSoundVolume();
    ISound setSoundVolume(float volume);
    default float getFullSoundVolume() {
        return 1f;
    }

    float getSoundPitch();
    ISound setSoundPitch(float pitch);

    boolean isSoundLooping();
    ISound setSoundLooping(boolean isLooping);

    SoundCategory getSoundChannel();
    ISound setSoundChannel(SoundCategory source);
    static SoundCategory getSoundChannelByName(String name){
        for (SoundCategory value : SoundCategory.values()) {
            if(value.getName().contentEquals(name)){
                return value;
            }
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    float getFloatDuration();
    @OnlyIn(Dist.CLIENT)
    default long getDuration() {
        return (long) this.getFloatDuration();
    }

    @OnlyIn(Dist.CLIENT)
    float getFloatElapse();
    @OnlyIn(Dist.CLIENT)
    default long getElapse() {
        return (long)this.getFloatElapse();
    }

    @OnlyIn(Dist.CLIENT)
    default float getSurplusPart() {
        return 1.0f - ((getFloatElapse() - getFloatDuration()) * 1.0f / getFloatDuration() + 1f);
    }

    default ISound copy() {
        return this;
    }

    public static abstract class BaseSound implements ISound {
        public final ResourceLocation id;
        private float volume = 1f, pitch = 1f;
        private boolean canLooping = false;
        private SoundCategory channel = SoundCategory.PLAYERS;

        public BaseSound(String mod, String id) {
            this(new ResourceLocation(mod, id));
        }
        public BaseSound(String id) {
            this(new ResourceLocation(id));
        }
        public BaseSound(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public ResourceLocation getSoundID() {
            return this.id;
        }

        @Override
        public float getSoundVolume() {
            float fullVol = this.getFullSoundVolume();
            float vol = this.getCurrentSoundVolume();
            if (vol == 0) {
                vol += 0.000001f;
            }
            return vol / fullVol;
        }

        public abstract float getCurrentSoundVolume();

        @Override
        public ISound setSoundVolume(float volume) {
            this.volume = volume;
            return this;
        }

        @Override
        public float getSoundPitch() {
            return this.pitch;
        }

        @Override
        public ISound setSoundPitch(float pitch) {
            this.pitch = pitch;
            return this;
        }

        @Override
        public boolean isSoundLooping() {
            return this.canLooping;
        }

        @Override
        public ISound setSoundLooping(boolean isLooping) {
            this.canLooping = isLooping;
            return this;
        }

        @Override
        public SoundCategory getSoundChannel() {
            return this.channel;
        }

        @Override
        public ISound setSoundChannel(SoundCategory channel) {
            this.channel = channel;
            return this;
        }

        @Override
        public JsonObject write(JsonObject data) {
            data.addProperty(ID_NAME, String.valueOf(this.getSoundID()));
            data.addProperty("channel", this.getSoundChannel().getName());
            data.addProperty("looping", this.isSoundLooping());
            data.addProperty("volume", this.getCurrentSoundVolume());
            data.addProperty("pitch", this.getSoundPitch());
            return data;
        }

        @Override
        public void read(JsonObject data) {
            this.setSoundChannel(data.has("channel") ? getSoundChannelByName(data.get("channel").getAsString()) : SoundCategory.PLAYERS);
            this.setSoundLooping(data.has("looping") && data.get("looping").getAsBoolean());
            this.setSoundVolume(data.has("volume") ? data.get("volume").getAsFloat() : 1f);
            this.setSoundPitch(data.has("pitch") ? data.get("pitch").getAsFloat() : 1f);
        }

        @Override
        public CompoundNBT write(CompoundNBT data) {
            data.putString(ID_NAME, String.valueOf(this.getSoundID()));
            data.putString("channel", this.getSoundChannel().getName());
            data.putBoolean("looping", this.isSoundLooping());
            data.putFloat("volume", this.getCurrentSoundVolume());
            data.putFloat("pitch", this.getSoundPitch());
            return data;
        }

        @Override
        public void read(CompoundNBT data) {
            this.setSoundChannel(getSoundChannelByName(data.getString("channel")));
            this.setSoundLooping(data.getBoolean("looping"));
            this.setSoundVolume(data.getFloat("volume"));
            this.setSoundPitch(data.getFloat("pitch"));
        }
    }
}
