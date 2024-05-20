package cat.jiu.formless.utils.client;

import com.google.common.collect.Maps;
import jmp123.PlayBack;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class AudioSystem {
    static final ConcurrentMap<UUID, Sound> MAP = Maps.newConcurrentMap();
    public static UUID play(Audio audio) {
        UUID id = UUID.randomUUID();
        jmp123.output.Audio jmp_audio = new jmp123.output.Audio();
        PlayBack player = new PlayBack(jmp_audio);
        Thread thread = new Thread(()->{
            try {
                play(audio, id, jmp_audio, player);
            }catch (Exception ignored){}
        });
        MAP.put(id, new Sound(audio, jmp_audio, player, thread));
        thread.start();
        return id;
    }
    static void play(Audio audio, UUID id, jmp123.output.Audio jmp_audio, PlayBack player) throws IOException {
        float level = getMinecraftVolume(audio.category());

        Event.Played event = new Event.Played(id, MAP.get(id).audio, level);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            stop(id);
            return;
        }
        level = event.getVolume();

        player.open(audio.file(), null);
        setVolume(id, level);
        player.start(false);
        stop(id);
    }
    public static void pause(UUID id){
        if (MAP.containsKey(id) && !getPlayer(id).isPaused()) {
            getPlayer(id).pause();
            MinecraftForge.EVENT_BUS.post(new Event.Pause(id, MAP.get(id).audio, true));
        }
    }
    public static boolean isPaused(UUID id){
        if (MAP.containsKey(id)) {
            return getPlayer(id).isPaused();
        }
        return true;
    }
    public static void start(UUID id){
        if (MAP.containsKey(id) && getPlayer(id).isPaused()) {
            getPlayer(id).pause();
            MinecraftForge.EVENT_BUS.post(new Event.Pause(id, MAP.get(id).audio, false));
        }
    }
    public static void stop(UUID id){
        pause(id);
        if (MAP.containsKey(id)) {
            Sound sound = MAP.get(id);
            getPlayer(id).stop();
            getPlayer(id).close();
            sound.close = true;
            MinecraftForge.EVENT_BUS.post(new Event.Stopped(id, sound.audio));
            MAP.remove(id);
        }
    }

    public static void setVolume(UUID id, float volume){
        float level = -40 + (40 * volume);
        if (MAP.containsKey(id)) {

            Event.SetVolume event = new Event.SetVolume(id, MAP.get(id).audio, level);
            if (MinecraftForge.EVENT_BUS.post(event)) return;
            level = event.getVolume();

            getPlayer(id).setVolume(level);
            MAP.get(id).jmpAudio.setLineGain(level);
        }
    }

    /**
     * current audio elapse
     * @return millis
     */
    public static long getElapse(UUID id){
        return (long) getFloatElapse(id);
    }

    public static float getFloatElapse(UUID id){
        if (MAP.containsKey(id)) {
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
        if (MAP.containsKey(id)) {
            return getPlayer(id).getHeader().getDuration() * 1000;
        }
        return 0;
    }

    public static float getSurplusPart(UUID id) {
        if (MAP.containsKey(id)) {
            return 1.0f - (float) (((getElapse(id) - getDuration(id)) * 1.0 / getDuration(id)) + 1);
        }
        return 0.0F;
    }

    public static boolean isClose(UUID id) {
        return MAP.containsKey(id) && MAP.get(id).close;
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

    public static float getMinecraftVolume(SoundSource sc){
        return Minecraft.getInstance().options.getSoundSourceVolume(sc) * Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER);
    }

    @SubscribeEvent
    public static void onGuiOpen(ScreenEvent.InitScreenEvent event){
        if (event.getScreen().isPauseScreen()) {
            AudioSystem.pauseAll();
        }
    }

    @SubscribeEvent
    public static void onKeyboardInput(ScreenEvent.KeyboardKeyPressedEvent.Post event){
        if (event.getScreen().isPauseScreen()
                && event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE || event.getKeyCode() == GLFW.GLFW_KEY_E) {
            AudioSystem.startAll();
            MAP.forEach((key, value) -> setVolume(key, getMinecraftVolume(value.audio.category())));
        }
    }

    @SubscribeEvent
    public static void onExitServer(ScreenEvent.MouseClickedEvent.Post event){
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.PauseScreen) {
            int index = event.getScreen().children().indexOf(event.getScreen().getChildAt(event.getMouseX(), event.getMouseY()).orElse(null));
            switch (index) {
                case 7 -> AudioSystem.stopAll();
                case 0 -> {
                    AudioSystem.startAll();
                    MAP.forEach((key, value) -> setVolume(key, getMinecraftVolume(value.audio.category())));
                }
            }
        }
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

    public record Audio(String file, SoundSource category) {
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
