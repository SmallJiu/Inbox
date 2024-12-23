package cat.jiu.core.util;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod.EventBusSubscriber
public final class SideProxy {
    static boolean isServerClosed = true;
    static MinecraftServer server;

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
    public static void onServerStarting(ServerStartedEvent event) {
        isServerClosed = false;
        server = event.getServer();
    }
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        isServerClosed = true;
        server = null;
    }
}
