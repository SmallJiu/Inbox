package cat.jiu.email;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cat.jiu.email.command.EmailCommands;
import cat.jiu.email.element.Cooling;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.net.EmailNetworkHandler;
import cat.jiu.email.net.msg.MsgUnreceive;
import cat.jiu.email.net.msg.MsgUnread;
import cat.jiu.email.proxy.ServerProxy;
import cat.jiu.email.ui.EmailGuiHandler;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailExecuteEvent;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod(
	modid = EmailMain.MODID,
	name = EmailMain.NAME,
	version = EmailMain.VERSION,
	useMetadata = true,
	guiFactory = "cat.jiu.email.util.client.ConfigGuiFactory"
)
@Mod.EventBusSubscriber
public class EmailMain {
	public static final String MODID = "email";
	public static final String NAME = "Inbox";
	public static final String OWNER = "small_jiu";
	public static final String VERSION = "1.0.3-a2";
	public static final EmailNetworkHandler net = new EmailNetworkHandler();
	public static final Logger log = LogManager.getLogger("Email");
	public static final String SYSTEM = "?????";
	public static MinecraftServer server;
	public final static boolean SQLite_INIT;
	private static boolean isServerClosed = true;
	public static boolean isServerClosed() {return isServerClosed;}
	
	private static int unread = 0;
	public static int getUnread() {return unread;}
	public static void setUnread(int unread) {
		EmailMain.unread = unread;
	}
	
	private static int unaccepted = 0;
	public static int getUnaccept() {return unaccepted;}
	public static void setAccept(int unaccepted) {
		EmailMain.unaccepted = unaccepted;
	}
	
	@SidedProxy(
		serverSide = "cat.jiu.email.proxy.ServerProxy",
		clientSide = "cat.jiu.email.proxy.ClientProxy",
		modId = EmailMain.MODID
	)
	public static ServerProxy proxy;
	static {
		boolean init = false;
		try {
			Class.forName("org.sqlite.JDBC");
			init = true;
		}catch(Exception ignored) {}
		SQLite_INIT = init;
	}
	
	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		new EmailGuiHandler();
		EmailAPI.addBlockReceiveWhitelist(SYSTEM);
		if(EmailConfigs.Send.Enable_Send_BlackList && EmailConfigs.Send.Enable_Send_WhiteList) {
			EmailConfigs.Send.Enable_Send_BlackList = false;
			EmailConfigs.Send.Enable_Send_WhiteList = false;
		}
	}
	
	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init(event);
		EmailAPI.setRootPath();
	}

	@Mod.EventHandler
	public void onServerStart(FMLServerStartingEvent event) {
		server = event.getServer();
		isServerClosed = false;
		EmailAPI.clearEmailPath();
		EmailUtils.initNameAndUUID(event.getServer());
		EmailExecuteEvent.init();
		Cooling.load();
		event.registerServerCommand(new EmailCommands());
	}

	@Mod.EventHandler
	public void onServerClosed(FMLServerStoppedEvent event) {
		server = null;
		Inbox.clearCache();
		isServerClosed = true;
		EmailAPI.setRootPath();
	}
	
	@SubscribeEvent
	public static void onServerTick(TickEvent.PlayerTickEvent event) {
		EntityPlayer player = event.player;
		if(!player.world.isRemote) {
			NBTTagCompound entityNBT = player.getEntityData();
			
			int time = entityNBT.getInteger("Email_UnAcceptTime");
			
			if(time <= 0) {
				time = (int) EmailUtils.parseTick(0,0,0,15, 0);
				Inbox inbox = Inbox.get(player);
				if(inbox != null) {
					int unread = inbox.getUnRead();
					int unreceived = inbox.getUnReceived();
					if(unread > 0) {
						if(unreceived > 0) {
							player.sendStatusMessage(new TextComponentTranslation("info.email.has_unread_and_unreceive", unread, unreceived), true);
							net.sendMessageToPlayer(new MsgUnreceive(unreceived), (EntityPlayerMP) player);
							net.sendMessageToPlayer(new MsgUnread(unread), (EntityPlayerMP) player);
						}else {
							net.sendMessageToPlayer(new MsgUnread(unread), (EntityPlayerMP) player);
							player.sendStatusMessage(new TextComponentTranslation("info.email.has_unread", unread), true);
						}
					}else if(unreceived > 0) {
						if(unread > 0) {
							net.sendMessageToPlayer(new MsgUnreceive(unreceived), (EntityPlayerMP) player);
							net.sendMessageToPlayer(new MsgUnread(unread), (EntityPlayerMP) player);
							player.sendStatusMessage(new TextComponentTranslation("info.email.has_unread_and_unreceive", unread, unreceived), true);
						}else {
							net.sendMessageToPlayer(new MsgUnreceive(unreceived), (EntityPlayerMP) player);
							player.sendStatusMessage(new TextComponentTranslation("info.email.has_unreceive", unreceived), true);
						}
					}
				}
			}else {
				time--;
			}
			entityNBT.setInteger("Email_UnAcceptTime", time);
		}
	}
	
	// for network delay, need send after
	public static void execute(Runnable function) {execute(function, 50);}
	public static void execute(Runnable function, long delay) {
		new Thread(()->{
			try {Thread.sleep(delay);}catch(InterruptedException e) { e.printStackTrace();}
			function.run();
		}).start();
	}
}
