package cat.jiu.core.util;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod.EventBusSubscriber
public final class SideProxy {
    public static MinecraftServer server;
    static boolean isServerClosed = true;

    public static Dist getSide() {
        return FMLLoader.getDist();
    }
    public static boolean isClient() {
        return getSide().isClient();
    }

    public static boolean isServerClosed() {
        return isServerClosed;
    }
    public static MinecraftServer getServer() {
        return server;
    }

    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event) {
        isServerClosed = false;
        server = event.getServer();
    }
    @SubscribeEvent
    public static void onServerStopped(FMLServerStoppedEvent event) {
        isServerClosed = true;
        server = null;
    }
}
