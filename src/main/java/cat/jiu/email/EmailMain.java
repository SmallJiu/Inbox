package cat.jiu.email;

import cat.jiu.core.util.client.config.ConfigWriteEvent;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.command.EmailCommands;
import cat.jiu.email.command.EmailEventType;
import cat.jiu.email.command.EmailFileType;
import cat.jiu.email.configs.EmailConfigClient;
import cat.jiu.email.element.Cooling;
import cat.jiu.email.element.EventEmail;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.element.ScheduledEmail;
import cat.jiu.email.element.attachment.*;
import cat.jiu.email.net.EmailNetworkHandler;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.KeyBinds;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.email.util.EmailUtils;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod(EmailMain.MODID)
public class EmailMain {
    public static final Logger log = LogManager.getLogger();
    public static final String MODID = "email",
                                VERSION = "1.20.1-1.1.3-a3";
    public static final String SYSTEM = "?????";
    public static final boolean SQLite_INIT;
    static {
        boolean init = false;
        try {
            Class.forName("org.sqlite.JDBC");
            init = true;
        }catch(Exception ignored) {}
        SQLite_INIT = init;
    }
    public static EmailNetworkHandler net;
    private static final long initTime = System.currentTimeMillis();
    @OnlyIn(Dist.CLIENT)
    public static long getSysTime() {
        return System.currentTimeMillis() - initTime;
    }

    public EmailMain() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::setup);
//        bus.addListener(this::onConfigLoading);
        if (FMLLoader.getDist().isClient()) {
            bus.addListener(this::onClientSetup);
            bus.addListener(this::onRegisterBindings);
        }
        GuiHandler.MENU_TYPE_REGISTER.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, EmailConfigServer.CONFIG_MAIN, "jiu/inbox/configs-server.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, EmailConfigClient.CONFIG_MAIN, "jiu/inbox/configs-client.toml");
        MinecraftForge.EVENT_BUS.register(this);
    }

    void onConfigLoading(ModConfigEvent.Loading event){
        if(MODID.equalsIgnoreCase(event.getConfig().getModId())){
            EmailAPI.setRootPath();
        }
    }
    @SubscribeEvent
    public void onConfigReload(ConfigWriteEvent event) {
        if(event.file.contains("jiu/inbox/config-server.toml")){
            EmailAPI.setRootPath();
        }
    }

    private static int unread = 0;
    private static int unaccepted = 0;
    public static int getUnread() {
        return unread;
    }
    public static int getUnaccepted() {
        return unaccepted;
    }

    public static void setAccept(int unRead, int unReceived) {
        unread = unRead;
        unaccepted = unReceived;
    }

    private void setup(final FMLCommonSetupEvent event){
        ArgumentTypeInfos.registerByClass(EmailFileType.class, SingletonArgumentInfo.contextFree(() -> new EmailFileType(EmailAPI.getGlobalDataPath() + "emails/")));
        ArgumentTypeInfos.registerByClass(EmailEventType.class, SingletonArgumentInfo.contextFree(EmailEventType::new));
        event.enqueueWork(()-> net = new EmailNetworkHandler());

        IAttachment.REGISTRY.register(AttachmentItem.ID, AttachmentItem::new, AttachmentItem::new);
        IAttachment.REGISTRY.register(AttachmentCommand.ID, AttachmentCommand::new, AttachmentCommand::new);
        IAttachment.REGISTRY.register(AttachmentXP.ID, AttachmentXP::new, AttachmentXP::new);
        IAttachment.REGISTRY.register(AttachmentMaxHealth.ID, AttachmentMaxHealth::new, AttachmentMaxHealth::new);
        IAttachment.REGISTRY.register(AttachmentEffect.ID, AttachmentEffect::new, AttachmentEffect::new);
        IAttachment.REGISTRY.register(AttachmentAttribute.ID, AttachmentAttribute::new, AttachmentAttribute::new);

        AttachmentCommand.registerParameterParser("player", true, (key, cmd, player) -> cmd.replace(key, player.getName().getString()));
    }

    @OnlyIn(Dist.CLIENT)
    void onClientSetup(final FMLClientSetupEvent event) {
        GuiHandler.registerScreen();
    }
    @OnlyIn(Dist.CLIENT)
    void onRegisterBindings(RegisterKeyMappingsEvent event) {
        KeyBinds.KEY_DELETE_EMAIL.register(event);
        KeyBinds.KEY_ACCEPT_EMAIL.register(event);
        KeyBinds.KEY_BUTTON_DRAGGING.register(event);
    }
    @SubscribeEvent
    public void onServerStarting(ServerStartedEvent event) {
        EmailUtils.initNameAndUUID(event.getServer());
        Cooling.load();
        EventEmail.load();
        ScheduledEmail.initScheduledEmail();
    }
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        Inbox.clearCache();
        ScheduledEmail.saveScheduledEmail();
        EmailAPI.setRootPath();
        EventEmail.save();
    }

    private static final List<Runnable> TASK = new ArrayList<>();
    public static void runOnServerThread(Runnable runnable) {
        TASK.add(runnable);
    }
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (!TASK.isEmpty()) {
            for (int i = 0; i < TASK.size(); i++) {
                if (TASK.get(i) != null) {
                    TASK.remove(i).run();
                }
            }
        }
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
}
