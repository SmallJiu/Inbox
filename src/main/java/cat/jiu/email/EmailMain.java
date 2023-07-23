package cat.jiu.email;

import cat.jiu.email.init.ContainerTypes;
import cat.jiu.email.net.EmailNetworkHandler;
import cat.jiu.email.ui.gui.*;
import cat.jiu.email.util.EmailConfigs;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EmailMain.MODID)
public class EmailMain {
    public static final Logger log = LogManager.getLogger();
    public static final String MODID = "email",
                                VERSION = "2.0.0";
    public static final String SYSTEM = "?????";
    public static EmailNetworkHandler net;
    public static MinecraftServer server;

    public EmailMain() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        ContainerTypes.CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EmailConfigs.CONFIG_MAIN, "jiu/email.toml");

        MinecraftForge.EVENT_BUS.register(this);
    }

    private static int unread = 0;
    private static int unaccepted = 0;
    public static int getUnread() {
        return unread;
    }

    public static int getUnaccepted() {
        return unaccepted;
    }

    public static void setUnread(int unRead) {
        unread = unRead;
    }

    public static void setAccept(int unReceived) {
        unaccepted = unReceived;
    }

    private void setup(final FMLCommonSetupEvent event){
        event.enqueueWork(()-> net = new EmailNetworkHandler());
    }
    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(()->{
            ScreenManager.registerFactory(ContainerTypes.container_email_send.get(), GuiEmailSend::new);
            ScreenManager.registerFactory(ContainerTypes.container_email_blacklist.get(), GuiBlacklist::new);
            ScreenManager.registerFactory(ContainerTypes.container_email_main.get(), GuiEmailMain::new);
        });
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        proxy.isServerClosed = false;
        server = event.getServer();
    }
    @SubscribeEvent
    public void onServerStopped(FMLServerStoppedEvent event) {
        proxy.isServerClosed = true;
        server = null;
    }

    public static void execute(Runnable function) {execute(function, 50);}
    public static void execute(Runnable function, long delay) {
        new Thread(()->{
            try {Thread.sleep(delay);}catch(InterruptedException e) { e.printStackTrace();}
            function.run();
        }).start();
    }

    public static class proxy {
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
    }
}
