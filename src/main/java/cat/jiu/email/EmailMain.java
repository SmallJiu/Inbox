package cat.jiu.email;

import cat.jiu.email.api.IAttachment;
import cat.jiu.email.command.EmailCommands;
import cat.jiu.email.command.EmailEventType;
import cat.jiu.email.command.EmailFileType;
import cat.jiu.email.configs.EmailConfigClient;
import cat.jiu.email.element.*;
import cat.jiu.email.element.attachment.*;
import cat.jiu.email.net.EmailNetworkHandler;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.KeyBinds;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.SendDevEmail;
import com.tterrag.registrate.Registrate;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod(EmailMain.MODID)
public class EmailMain {
    public static final Logger log = LogManager.getLogger();
    public static final String MODID = "email",
                                VERSION = "1.21.1-1.0.0";
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
    public static MinecraftServer server;
    private static final long initTime = System.currentTimeMillis();
    @OnlyIn(Dist.CLIENT)
    public static long getSysTime() {
        return System.currentTimeMillis() - initTime;
    }

    private final Registrate registrate = Registrate.create(MODID).defaultCreativeTab((ResourceKey)null);
    private static ModContainer modContainer;
    private static IEventBus modBus;
    private static EmailMain instance;

    public static ModContainer container() {
        return modContainer;
    }
    public static IEventBus bus() {
        return modBus;
    }
    public static Registrate registrate() {
        return getInstance().registrate;
    }
    public static EmailMain getInstance() {
        return instance;
    }

    public EmailMain(IEventBus modBus, ModContainer container) {
        instance = this;
        EmailMain.modBus = modBus;
        EmailMain.modContainer = container;
        bus().addListener(this::setup);
//        bus().addListener(this::onConfigLoading);
        bus().addListener((RegisterPayloadHandlersEvent event) -> {
            net = EmailNetworkHandler.getInstance();
            net.registerChannel(event);
        });

        if(FMLLoader.getDist().isClient()) {
            bus().addListener(this::onClientSetup);
            bus().addListener(this::onRegisterBindings);
        }

        container().registerConfig(ModConfig.Type.SERVER, EmailConfigServer.CONFIG_MAIN, "jiu/inbox/configs-server.toml");
        container().registerConfig(ModConfig.Type.CLIENT, EmailConfigClient.CONFIG_MAIN, "jiu/inbox/configs-client.toml");
        NeoForge.EVENT_BUS.register(this);
    }

    void onConfigLoading(ModConfigEvent.Loading event){
        if(MODID.equalsIgnoreCase(event.getConfig().getModId()) && event.getConfig().getType() == ModConfig.Type.SERVER) {
            EmailAPI.setRootPath();
        }
    }
//    @SubscribeEvent
//    public void onConfigReload(ConfigWriteEvent event) {
//        if(event.file.contains("jiu/inbox/config-server.toml")){
//            EmailAPI.setRootPath();
//        }
//    }

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

        IAttachment.REGISTRY.register(AttachmentItem.ID, AttachmentItem::new, AttachmentItem::new);
        IAttachment.REGISTRY.register(AttachmentCommand.ID, AttachmentCommand::new, AttachmentCommand::new);
        IAttachment.REGISTRY.register(AttachmentXP.ID, AttachmentXP::new, AttachmentXP::new);
        IAttachment.REGISTRY.register(AttachmentMaxHealth.ID, AttachmentMaxHealth::new, AttachmentMaxHealth::new);
        IAttachment.REGISTRY.register(AttachmentEffect.ID, AttachmentEffect::new, AttachmentEffect::new);
        IAttachment.REGISTRY.register(AttachmentAttribute.ID, AttachmentAttribute::new, AttachmentAttribute::new);

        AttachmentCommand.registerParameterParser("player", true, (key, cmd, player) -> cmd.replace(key, player.getName().getString()));
//        NeoForge.EVENT_BUS.register(AttachmentAttribute.class);
//        NeoForge.EVENT_BUS.register(ScheduledEmail.class);
//        NeoForge.EVENT_BUS.register(SendDevEmail.class);
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
        server = event.getServer();
        EmailUtils.initNameAndUUID(event.getServer());
        Cooling.load();
        EventEmail.load();
        ScheduledEmail.initScheduledEmail();
    }
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        server = null;
        Inbox.clearCache();
        ScheduledEmail.saveScheduledEmail();
        EmailAPI.clearEmailPath();
        EventEmail.save();
    }

    private static final List<Runnable> TASK = new ArrayList<>();
    public static void runOnServerThread(Runnable runnable) {
        TASK.add(runnable);
    }
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Pre event) {
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
