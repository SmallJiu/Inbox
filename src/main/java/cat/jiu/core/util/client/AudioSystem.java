package cat.jiu.core.util.client;

import cat.jiu.core.api.element.ISound;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.NBTUtils;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import jmp123.PlayBack;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(Dist.CLIENT)
public class AudioSystem {
    public static final Logger LOGGER = LogManager.getLogger("JiuCore/AudioSystem");
    static final ConcurrentMap<ResourceLocation, Audio> PRELOAD_AUDIO = Maps.newConcurrentMap();
    public static ResourceLocation preload(ResourceLocation id, Audio audio) {
        PRELOAD_AUDIO.put(id, audio);
        return id;
    }
    public static boolean hasPreloadAudio(ResourceLocation id) {
        return PRELOAD_AUDIO.containsKey(id);
    }
    public static Audio getPreloadAudio(ResourceLocation id) {
        return PRELOAD_AUDIO.get(id);
    }
    public static UUID play(ResourceLocation preloadAudioID) {
        if (hasPreloadAudio(preloadAudioID)) {
            return play(getPreloadAudio(preloadAudioID));
        }
        return null;
    }

    static final ConcurrentMap<UUID, Sound> MAP = Maps.newConcurrentMap();
    static float maxVolume = -999, minVolume = -999;

    public static UUID play(Audio audio) {
        if (audio == null) {
            return null;
        }
        UUID id = UUID.randomUUID();
        audio.uid = id;
        jmp123.output.Audio jmp_audio = new jmp123.output.Audio();
        PlayBack player = new PlayBack(jmp_audio);
        Thread thread = new Thread(()->{
                do {
                    try {
                        play(audio, id, jmp_audio, player);
                        audio.addLoopCount();
                    }catch (Exception e){
                        LOGGER.error("Unable to play audio. file: ' {} ', loop: {}, uid: {}, info: {}", audio.getFile(), audio.getLoopCount(), id, e);
                    }
                    try {
                        Thread.sleep(audio.getLoopDelay());
                    } catch (Exception ignored) {}
                }while (!audio.isClose() && audio.isCanLopping());
                stop(id);
        });
        MAP.put(id, new Sound(audio, jmp_audio, player, thread));
        thread.start();
        return id;
    }
    static void play(Audio audio, UUID id, jmp123.output.Audio jmp_audio, PlayBack player) throws IOException {
        player.open(audio.getFile(), null);
        if (maxVolume != jmp_audio.getFloatControl().getMaximum()) {
            maxVolume = jmp_audio.getFloatControl().getMaximum();
        }
        if (minVolume != jmp_audio.getFloatControl().getMinimum()) {
            minVolume = jmp_audio.getFloatControl().getMinimum();
        }

        float level = getMinecraftVolume(audio.getSoundChannel());
        Event.Played event = new Event.Played(id, audio, level);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return;
        }
        level = event.getVolume();
        setVolume(id, level);

        player.start(false);
    }

    public static void pause(UUID id){
        if (id!=null && MAP.containsKey(id) && !getPlayer(id).isPaused()) {
            getPlayer(id).pause();
            MinecraftForge.EVENT_BUS.post(new Event.Pause(id, MAP.get(id).audio, true));
        }
    }
    public static boolean isPaused(UUID id){
        if (id!=null && MAP.containsKey(id)) {
            return getPlayer(id).isPaused();
        }
        return true;
    }
    public static void start(UUID id){
        if (id!=null && MAP.containsKey(id) && getPlayer(id).isPaused()) {
            getPlayer(id).pause();
            MinecraftForge.EVENT_BUS.post(new Event.Pause(id, MAP.get(id).audio, false));
        }
    }
    public static void stop(UUID id){
        pause(id);
        if (id!=null && MAP.containsKey(id)) {
            Sound sound = MAP.get(id);
            getPlayer(id).stop();
            getPlayer(id).close();
            sound.close = true;
            sound.audio.uid = null;
            MinecraftForge.EVENT_BUS.post(new Event.Stopped(id, sound.audio));
            MAP.remove(id);
        }
    }

    public static void pause(UUID id, boolean pause) {
        if (pause) {
            pause(id);
        }else {
            start(id);
        }
    }

    public static float getVolume(UUID id){
        if (id!=null && MAP.containsKey(id)) {
            return MAP.get(id).jmpAudio.getFloatControl().getValue();
        }
        return -40;
    }
    public static void setVolume(UUID id, float volume){
        float level = getAudioVolume(volume);
        if (id!=null && MAP.containsKey(id)) {

            Event.SetVolume event = new Event.SetVolume(id, MAP.get(id).audio, level);
            if (MinecraftForge.EVENT_BUS.post(event)) return;
            level = event.getVolume();

            getPlayer(id).setVolume(level);
            MAP.get(id).jmpAudio.setLineGain(level);
        }
    }

    public static float getAudioVolume(float volume) {
        return -40f + (40f * volume);
    }

    /**
     * current audio elapse
     * @return millis
     */
    public static long getElapse(UUID id){
        return (long) getFloatElapse(id);
    }

    public static float getFloatElapse(UUID id){
        if (id!=null && MAP.containsKey(id)) {
            return (getPlayer(id).getHeader().getFrames() * getPlayer(id).getHeader().getFrameDuration()) * 1000;
        }
        return 0;
    }

    /**
     *  current audio duration
     * @return millis
     */
    public static long getDuration(UUID id){
        return (long) getFloatDuration(id);
    }
    public static float getFloatDuration(UUID id){
        if (id!=null && MAP.containsKey(id)) {
            return getPlayer(id).getHeader().getDuration() * 1000;
        }
        return 0;
    }

    public static float getSurplusPart(UUID id) {
        if (id!=null && MAP.containsKey(id)) {
            return 1.0f - (float) (((getElapse(id) - getDuration(id)) * 1.0 / getDuration(id)) + 1);
        }
        return 0.0F;
    }

    public static boolean isClose(UUID id) {
        return id!=null && MAP.containsKey(id) && MAP.get(id).close;
    }
    public static boolean isPlayed(UUID id) {
        return id!=null && MAP.containsKey(id) && !MAP.get(id).close;
    }

    static PlayBack getPlayer(UUID id){
        return MAP.get(id).player;
    }

    public static void pauseAll(){
        for (UUID uuid : MAP.keySet()) {
            pause(uuid);
        }
    }
    public static void startAll(){
        for (UUID uuid : MAP.keySet()) {
            start(uuid);
        }
    }
    public static void stopAll(){
        for (UUID uuid : MAP.keySet()) {
            stop(uuid);
        }
    }
    public static void setAllVolume(float volume){
        for (UUID uuid : MAP.keySet()) {
            setVolume(uuid, volume);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static float getMinecraftVolume(SoundSource sc){
        return Minecraft.getInstance().options.getSoundSourceVolume(sc) * Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiOpen(ScreenEvent.Opening event){
        if (event.getScreen().isPauseScreen()) {
            AudioSystem.pauseAll();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onGuiClose(ScreenEvent.Closing event){
        if (event.getScreen().isPauseScreen()) {
            AudioSystem.startAll();
//            MAP.forEach((key, value) -> setVolume(key, getMinecraftVolume(value.audio.getSoundChannel())));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onExitServer(ClientPlayerNetworkEvent.LoggingOut event) {
        AudioSystem.stopAll();
    }

    static class Sound {
        final Audio audio;
        final jmp123.output.Audio jmpAudio;
        final PlayBack player;
        final Thread thread;
        boolean close = false;
        public Sound(Audio audio, jmp123.output.Audio jmpAudio, PlayBack player, Thread thread) {
            this.audio = audio;
            this.jmpAudio = jmpAudio;
            this.player = player;
            this.thread = thread;
        }
    }

    public static class Audio {
        private UUID uid;
        private String file;
        private SoundSource soundChannel;
        private boolean canLopping = false, infiniteLopping = false;
        private int maxLoopCount = 5, loopCount = 0;
        private long loopDelay = 500;

        public Audio(File file, SoundSource soundChannel) {
            this(file.getPath(), soundChannel);
        }
        public Audio(String file, SoundSource soundChannel) {
            this.file = file;
            this.soundChannel = soundChannel;
        }

        public Audio(String fileName, InputStream audioStream, SoundSource soundChannel) {
            this(streamToFile(fileName, audioStream), soundChannel);
        }

        /**
         * @return stared will set to actual id, stopped will set to null.
         */
        public UUID getUUID() {
            return uid;
        }

        public UUID play() {
            return AudioSystem.play(this);
        }

        public void pause(boolean pause) {
            AudioSystem.pause(this.getUUID(), pause);
        }
        public void pause(){
            AudioSystem.pause(this.getUUID());
        }
        public void start(){
            AudioSystem.start(this.getUUID());
        }
        public void stop(){
            AudioSystem.stop(this.getUUID());
        }
        public boolean isPause(){
            return AudioSystem.isPaused(this.getUUID());
        }
        public boolean isClose(){
            return AudioSystem.isClose(this.getUUID());
        }
        public boolean isPlayed(){
            return AudioSystem.isPlayed(this.getUUID());
        }
        public float getVolume(){
            return AudioSystem.getVolume(this.getUUID());
        }
        public void setVolume(float volume) {
            AudioSystem.setVolume(this.getUUID(), volume);
        }
        public long getElapse(){
            return AudioSystem.getElapse(this.getUUID());
        }
        public long getDuration(){
            return AudioSystem.getDuration(this.getUUID());
        }

        public static File streamToFile(String fileName, InputStream audioStream) {
            File file = new File("./audio_tmp/", fileName);
            if (!file.exists() && audioStream != null) {
                try {
                    IOUtils.copy(audioStream, new FileOutputStream(file));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return file;
        }

        public String getFile() {
            return file;
        }
        File fileObj;
        public File getFileObject(){
            if (this.fileObj == null) {
                this.fileObj = new File(this.getFile());
            }
            return this.fileObj;
        }

        @Deprecated
        public SoundSource getCategory() {
            return this.getSoundChannel();
        }
        public SoundSource getSoundChannel() {
            return soundChannel!=null ? soundChannel : SoundSource.PLAYERS;
        }
        public Audio setSoundChannel(SoundSource soundChannel) {
            this.soundChannel = soundChannel;
            return this;
        }

        public boolean isInfiniteLoop() {
            return infiniteLopping;
        }

        public Audio setInfiniteLopping(boolean infiniteLopping) {
            this.infiniteLopping = infiniteLopping;
            return this;
        }

        boolean stopLopping;
        public Audio stopLopping() {
            this.stopLopping = true;
            return this;
        }

        public int getLoopCount() {
            return loopCount;
        }

        public void addLoopCount() {
            this.loopCount++;
        }

        public boolean isCanLopping() {
            boolean loop = this.isInfiniteLoop();
            if (this.getMaxLoopCount() > 1){
                loop = this.getLoopCount() <= this.getMaxLoopCount();
            }
            return this.canLopping && loop && !this.stopLopping;
        }

        public Audio setCanLopping(boolean canLopping) {
            this.canLopping = canLopping;
            return this;
        }

        public int getMaxLoopCount() {
            return maxLoopCount;
        }

        public Audio setMaxLoopCount(int maxLoopCount) {
            this.maxLoopCount = maxLoopCount;
            return this;
        }

        public long getLoopDelay() {
            return loopDelay;
        }

        /**
         * @param loopDelay millis
         */
        public Audio setLoopDelay(long loopDelay) {
            this.loopDelay = loopDelay;
            return this;
        }

        public CompoundTag write(CompoundTag data) {
            data.putString("file", this.getFile());
            data.putString("channel", this.getSoundChannel().getName());
            if (this.isCanLopping()) {
                data.putBoolean("canLoop", this.isCanLopping());
                data.putLong("loopDelay", this.getLoopDelay());
                if (this.isInfiniteLoop()) {
                    data.putBoolean("infiniteLoop", this.isInfiniteLoop());
                }
                if (this.getMaxLoopCount() > 1){
                    data.putInt("maxLoopCount", this.getMaxLoopCount());
                }
            }
            return data;
        }
        public void read(CompoundTag data) {
            this.file = NBTUtils.get(data, "file", "file_not_found");
            this.soundChannel = ISound.getSoundChannelByName(NBTUtils.get(data, "channel", SoundSource.PLAYERS.getName()));
            this.setCanLopping(NBTUtils.get(data, "canLoop", false));
            this.setLoopDelay(NBTUtils.get(data, "loopDelay", 0L));
            this.setInfiniteLopping(NBTUtils.get(data, "infiniteLoop", false));
            this.setMaxLoopCount(NBTUtils.get(data, "maxLoopCount", 0));
        }
        public JsonObject write(JsonObject data) {
            data.addProperty("file", this.getFile());
            data.addProperty("channel", this.getSoundChannel().getName());
            if (this.isCanLopping()) {
                data.addProperty("canLoop", this.isCanLopping());
                data.addProperty("loopDelay", this.getLoopDelay());
                if (this.isInfiniteLoop()) {
                    data.addProperty("infiniteLoop", this.isInfiniteLoop());
                }
                if (this.getMaxLoopCount() > 1) {
                    data.addProperty("maxLoopCount", this.getMaxLoopCount());
                }
            }
            return data;
        }
        public void read(JsonObject data) {
            this.file = JsonUtils.get(data, "file", "file_not_found"); // 音频文件路径
            this.soundChannel = ISound.getSoundChannelByName(JsonUtils.get(data, "channel", SoundSource.PLAYERS.getName())); // 调节通道
            this.setCanLopping(JsonUtils.get(data, "canLoop", false)); // 是否可以循环播放
            this.setLoopDelay(JsonUtils.get(data, "loopDelay", 0L)); // 循环一次的间隙
            this.setInfiniteLopping(JsonUtils.get(data, "infiniteLoop", false)); // 是否可以无限循环
            this.setMaxLoopCount(JsonUtils.get(data, "maxLoopCount", 0)); // 最大循环次数
        }

        public static Audio create(CompoundTag data) {
            Audio instance = new Audio("file_not_found", SoundSource.PLAYERS);
            instance.read(data);
            return instance;
        }
        public static Audio create(JsonObject data) {
            Audio instance = new Audio("file_not_found", SoundSource.PLAYERS);
            instance.read(data);
            return instance;
        }
    }

    public static class Event extends net.minecraftforge.eventbus.api.Event {
        public final UUID uid;
        public final Audio audio;
        protected Event(UUID uid, Audio audio) {
            this.uid = uid;
            this.audio = audio;
        }
        @Cancelable
        public static class Played extends Event {
            protected float volume;
            public Played(UUID uid, Audio audio, float volume) {
                super(uid, audio);
                this.volume = volume;
            }
            public float getVolume() {
                return volume;
            }
            public void setVolume(float volume) {
                this.volume = volume;
            }
        }
        public static class Stopped extends Event {
            public Stopped(UUID uid, Audio audio) {
                super(uid, audio);
            }
        }
        public static class Pause extends Event {
            public final boolean pause;
            public Pause(UUID uid, Audio audio, boolean pause) {
                super(uid, audio);
                this.pause = pause;
            }
        }
        @Cancelable
        public static class SetVolume extends Event {
            protected float volume;
            public SetVolume(UUID uid, Audio audio, float volume) {
                super(uid, audio);
                this.volume = volume;
            }
            public float getVolume() {
                return volume;
            }
            public void setVolume(float volume) {
                this.volume = volume;
            }
        }
    }
}
