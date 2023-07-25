package cat.jiu.email;

import cat.jiu.email.command.EmailCommands;
import cat.jiu.email.element.Cooling;
import cat.jiu.email.net.EmailNetworkHandler;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegisterCommandsEvent;
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
    private static long sysTime = 0;
    @OnlyIn(Dist.CLIENT)
    public static long getSysTime() {
        return sysTime;
    }
    static {
        new Thread(()->{
            while (proxy.isClient()) {
                try {
                    Thread.sleep(1);
                    sysTime++;
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    public EmailMain() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        GuiHandler.CONTAINER_TYPE_REGISTER.register(FMLJavaModLoadingContext.get().getModEventBus());
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
        event.enqueueWork(GuiHandler::registerScreen);
//        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, ()->(mc, parent)->
//            new cat.jiu.email.ui.gui.config.GuiConfig("/config/jiu/email.toml", parent, EmailConfigs.CONFIG_MAIN)
//        );
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        proxy.isServerClosed = false;
        Cooling.load();
        EmailUtils.initNameAndUUID(event.getServer());
        server = event.getServer();
    }
    @SubscribeEvent
    public void onServerStopped(FMLServerStoppedEvent event) {
        proxy.isServerClosed = true;
        server = null;
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        new EmailCommands().register(event.getDispatcher());
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
