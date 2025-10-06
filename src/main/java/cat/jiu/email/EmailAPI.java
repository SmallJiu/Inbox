package cat.jiu.email;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import java.util.Set;
import java.util.UUID;

import cat.jiu.core.api.IData;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.NBTUtils;
import cat.jiu.core.util.SideProxy;
import cat.jiu.core.util.element.data.NBTData;
import cat.jiu.email.api.IEmailStyle;
import cat.jiu.email.configs.EmailConfigClient;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.email.element.*;
import cat.jiu.email.net.msg.refresh.MsgRefreshScheduledEmail;
import cat.jiu.email.event.EmailSendEvent;
import cat.jiu.email.net.msg.*;
import cat.jiu.email.ui.gui.GuiInbox;
import cat.jiu.email.util.*;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.lang3.StringUtils;

@Mod.EventBusSubscriber
public class EmailAPI {
	/**
	 * send player email to player. sender is email sender
	 */
	public static void sendPlayerEmail(Player player, String addressee, Email email) {
		sendEmail(player, EmailSenderGroup.PLAYER, addressee, email);
	}
	
	public static final IText SYSTEM = new Text(EmailMain.SYSTEM);
	/**
	 * send system email to player. email sender is {@link EmailMain#SYSTEM}
	 */
	public static void sendSystemEmail(Player player, String addressee, Email email) {
		email.setSender(SYSTEM);
		sendEmail(player, EmailSenderGroup.SYSTEM, addressee, email);
	}
	
	/**
	 * send email to player.
	 * @param group email owner group
	 */
	public static void sendEmail(Player player, EmailSenderGroup group, String addresser, Email email) {
		if(player.level().isClientSide()) {
			EmailMain.NETWORK.sendMessageToServer(new MsgSend0(group, addresser, email));
		}else {
			sendEmail(group, addresser, email);
		}
	}
	
	public static synchronized boolean sendEmail(EmailSenderGroup group, String address, Email email) {
		EmailUtils.initNameAndUUID(SideProxy.getServer());
		Inbox inbox = Inbox.get(address);
		
		if(!EmailConfigServer.isInfiniteSize()
		&& email.hasAttachments() && !SizeReport.SUCCESS.equals(EmailUtils.checkEmailSize(email))) {
			return false;
		}

		if (group.isPlayerSend() && inbox.isInSenderBlacklist(email.getSender().getText())) {
			EmailMain.log.warn("Failed to send the email, You have been block by the Addressee!");
			return false;
		}
		
		if(MinecraftForge.EVENT_BUS.post(new EmailSendEvent(Phase.START, group, address, email))) return false;
		
		inbox.addEmail(email, true);
		
		MinecraftForge.EVENT_BUS.post(new EmailSendEvent(Phase.END, group, address, email));
		
		if(SideProxy.getServer() != null) {
			ServerPlayer player;
			try {
				player = SideProxy.getServer().getPlayerList().getPlayer(UUID.fromString(address));
			}catch(Exception e) {
				player = SideProxy.getServer().getPlayerList().getPlayerByName(address);
			}
			if(player != null) {
				player.sendSystemMessage(Component.translatable("info.inbox.from", email.getSender().toTextComponent()));
				EmailMain.NETWORK.sendMessageToPlayer(new MsgInboxToClient.SendEmail(inbox.getEmailHistoryCount(), email), player);
			}
		}
		return true;
	}

	public static IEmailStyle getEmailStyle() {
		JsonElement e = EmailAPI.globalEmailCache.exists() ? JsonUtils.parse(EmailAPI.globalEmailCache, EmailConfigServer.File_Charset.get()) : new JsonObject();
		if(e != null && e.isJsonObject()) {
			return IEmailStyle.REGISTRY.get(e.getAsJsonObject());
		}
		return null;
	}
	public static void saveEmailStyle(IEmailStyle style) {
		JsonElement e = EmailAPI.globalEmailCache.exists() ? JsonUtils.parse(EmailAPI.globalEmailCache, EmailConfigServer.File_Charset.get()) : new JsonObject();
		if(e != null && e.isJsonObject()) {
			e.getAsJsonObject().addProperty(IEmailStyle.NAME_ID, String.valueOf(style.getID()));
			JsonUtils.toJsonFile(EmailAPI.globalEmailCache, e, false, EmailConfigServer.File_Charset.get());
		}
	}

	public static void addAddresseeHistory(String name) {
		JsonElement e = EmailAPI.globalEmailCache.exists() ? JsonUtils.parse(EmailAPI.globalEmailCache, EmailConfigServer.File_Charset.get()) : new JsonObject();
		if(e != null && e.isJsonObject()) {
			JsonObject json = e.getAsJsonObject();
			boolean has = false;
			if (json.has("history")) {
				for (JsonElement element : json.getAsJsonArray("history")) {
					if (element.getAsString().equals(name)) {
						has = true;
						break;
					}
				}
			}
			if (!has) {
				JsonArray list = null;
				if(json.has("history") && json.get("history").isJsonArray()) {
					list = json.get("history").getAsJsonArray();
				}else {
					list = new JsonArray();
				}
				if (list.size() >= EmailConfigClient.Send_History_Max_Count.get()) {
					list.remove(0);
				}
				list.add(name);
				json.add("history", list);
				JsonUtils.toJsonFile(EmailAPI.globalEmailCache.getPath(), json, false, EmailConfigServer.File_Charset.get());
			}
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
	private static long undyingCount = 0;
	public static long getUndyingCount() {
		return undyingCount;
	}
	public static void setUndyingCount(long undyingCount) {
		EmailAPI.undyingCount = undyingCount;
	}

	private static final List<String> whitelist = Lists.newArrayList();
	public static void addBlockReceiveWhitelist(String name) {
		if(!isInBlockReceiveWhitelist(name)) {
			whitelist.add(name);
		}
	}
	
	public static boolean isInBlockReceiveWhitelist(String name) {
		return whitelist.contains(name);
	}

	static void setRootPath(){
		clearEmailPath();
		if(EmailConfigServer.Save_To_Minecraft_Root_Directory.get()){
			EmailRootPath = String.valueOf(FMLLoader.getGamePath());
			EmailMain.log.info(String.format("Set inbox root path to: %s", EmailRootPath));
		}

		if(!StringUtils.isEmpty(EmailConfigServer.Custom_Inbox_Path.get())){
			File path = new File(EmailConfigServer.Custom_Inbox_Path.get());
			if(!path.exists()){
				path.mkdirs();
			}
			if(path.isDirectory()){
				EmailRootPath = EmailConfigServer.Custom_Inbox_Path.get();
			}else if(path.isFile()){
				EmailRootPath = path.getParent();
			}
			EmailMain.log.info(String.format("Set inbox root path to: %s", EmailRootPath));
		}
	}
	public static final String ConfigPath = FMLPaths.CONFIGDIR.get() + "/jiu/inbox/";
	static String
			EmailPath = null,
			EmailRootPath = null,
			typePath = null,
			exportPath = null,
			dataPath = null,
			globalDataPath = null
	;

	public static String getSaveEmailRootPath() {
		if(EmailRootPath == null) {
			boolean root = EmailConfigServer.Save_To_Minecraft_Root_Directory.get();
			if(root
					|| SideProxy.getServer() == null) {
				EmailRootPath = String.valueOf(FMLLoader.getGamePath());
			}else {
				EmailRootPath = new File(String.valueOf(SideProxy.getServer().getWorldPath(LevelResource.LEVEL_DATA_FILE))).getParent();
			}
		}
		return EmailRootPath;
	}

	public static String getSaveInboxPath() {
		if(EmailPath == null) {
			EmailPath = getSaveEmailRootPath() + File.separator + "email" + File.separator;
		}
		return EmailPath;
	}

	public static String getTypePath() {
		if(typePath == null) {
			typePath = getSaveInboxPath() + "type" + File.separator;
		}
		return typePath;
	}
	public static String getExportPath() {
		if(exportPath == null) {
			exportPath = getTypePath() + "export" + File.separator;
		}
		return exportPath;
	}
	public static String getDataPath() {
		if(dataPath == null) {
			dataPath = getSaveInboxPath() + "data" + File.separator;
		}
		return dataPath;
	}
	public static String getGlobalDataPath() {
		if(globalDataPath == null) {
			globalDataPath = ConfigPath + "data" + File.separator;
		}
		return globalDataPath;
	}
	
	public static void clearEmailPath() {
		EmailRootPath = null;
		EmailPath = null;
		typePath = null;
		exportPath = null;
		dataPath = null;
		globalDataPath = null;
	}

	public static final File globalEmailCache = new File("./email.json");
	public static final String globalEmailListPath = EmailAPI.globalEmailCache.getPath();

	public static boolean addToWhiteList(String name, UUID uid) {
		return addToList(name, uid, false);
	}
	public static boolean addToWhiteList(Player player) {
		return addToWhiteList(player.getName().getString(), player.getUUID());
	}
	public static boolean addToBlackList(String name, UUID uid) {
		return addToList(name, uid, true);
	}
	public static boolean addToBlackList(Player player) {
		return addToBlackList(player.getName().getString(), player.getUUID());
	}
	private static boolean addToList(String name, UUID uid, boolean black) {
        try {
			BlackAndWhiteList list = new BlackAndWhiteList();
            list.add(name, String.valueOf(uid), black);
			return list.save().writeToFile(globalEmailCache, false, EmailConfigServer.File_Charset.get());
        } catch (Exception ignored) {
			return false;
        }
	}

	public static boolean isInWhiteList(Player player) {
		return isInList(player.getName().getString(), false) || isInList(player.getUUID().toString(), false);
	}
	public static boolean isInWhiteList(UUID uid) {
		return isInList(uid.toString(), false);
	}
	public static boolean isInWhiteList(String name) {
		return isInList(name, false);
	}
	public static boolean isInBlackList(Player player) {
		return isInList(player.getName().getString(), true) || isInList(player.getUUID().toString(), true);
	}
	public static boolean isInBlackList(UUID uid) {
		return isInList(uid.toString(), true);
	}
	public static boolean isInBlackList(String name) {
		return isInList(name, true);
	}
	private static boolean isInList(String str, boolean black) {
		try {
			BlackAndWhiteList list = new BlackAndWhiteList();
			return list.contains(str, true, black) || list.contains(str, false, black);
		} catch (Exception ignored) {
			return false;
		}
	}
	
	public static boolean removeInWhiteList(Player player) {
		return removeInList(player.getName().getString(), false);
	}
	public static boolean removeInWhiteList(UUID uid) {
		return removeInList(uid.toString(), false);
	}
	public static boolean removeInWhiteList(String name) {
		return removeInList(name, false);
	}
	public static boolean removeInBlackList(Player player) {
		return removeInList(player.getName().getString(), true);
	}
	public static boolean removeInBlackList(UUID uid) {
		return removeInList(uid.toString(), true);
	}
	public static boolean removeInBlackList(String name) {
		return removeInList(name, true);
	}
	private static boolean removeInList(String name, boolean black) {
		try {
			BlackAndWhiteList list = new BlackAndWhiteList();
			list.remove(name, black);
			return list.save().writeToFile(globalEmailCache, false, EmailConfigServer.File_Charset.get());
		} catch (Exception ignored) {
			return false;
		}
	}

	private static String onServerIPOrSaveName;
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
		if (Minecraft.getInstance().isLocalServer()) {
			onServerIPOrSaveName = new File(String.valueOf(SideProxy.getServer().getWorldPath(LevelResource.LEVEL_DATA_FILE))).getParentFile().getName();
		}else {
			onServerIPOrSaveName = Minecraft.getInstance().getCurrentServer().ip.replace(':', '-');
		}
        try {
            GuiInbox.INBOX.read(NBTData.readMap(new File("./inbox", onServerIPOrSaveName +".dat"), true));
        } catch (Exception e) {
			e.printStackTrace();
        }
    }
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onPlayerLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		if (onServerIPOrSaveName != null) {
			try {
				IData.IMapData<?> data = NBTData.map();
				GuiInbox.INBOX.write(data);
				data.writeToFile(new File("./inbox", onServerIPOrSaveName +".dat"), true, "UTF-8");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		EmailAPI.setAccept(0, 0);
        GuiInbox.INBOX.deleteAllEmail();
		onServerIPOrSaveName = null;
	}

	@OnlyIn(Dist.CLIENT)
	public static void openInbox() {
		GuiInbox.display();
	}

	private static final HashMap<String, Email> ScheduledEmailMap = new HashMap<>();
	public static HashMap<String, Email> getScheduledEmailMap() {
		return ScheduledEmailMap;
	}
	public static void refreshScheduledEmailMap(ServerPlayer player) {
		EmailMain.execute(()->{
			File dir = new File(EmailAPI.getGlobalDataPath()+"emails/");
			if (dir.exists()) {
				for (File file : dir.listFiles()) {
					if (file.isFile()) {
						try {
							String path = file.getName();
							Email email = new Email(JsonUtils.parse(file, EmailConfigServer.File_Charset.get()).getAsJsonObject());
							EmailMain.NETWORK.sendMessageToPlayer(new MsgRefreshScheduledEmail.SendMap(path.substring(0, path.indexOf('.')), email), player);
						}catch (Exception ignored){ }
					}
				}
			}
		});
	}

	public static void sendScheduledEmailToClient(ServerPlayer player) {
		EmailMain.execute(()->{
			for (ScheduledEmail email : ScheduledEmail.getScheduledEmails()) {
				boolean check = true;
				if (email.getAddressee().isCustomPlayers() && !player.getServer().getPlayerList().isOp(player.getGameProfile())) {
					check = email.getCustomAddressee().contains(player.getName().getString());
				}
				if (check && email.getAsEmail() != null) {
					try {
						Thread.sleep(25);
						EmailMain.NETWORK.sendMessageToPlayer(new MsgRefreshScheduledEmail.Send(email), player);
					} catch (Exception ignored) {}
				}
			}
		});
	}

	public static void sendInboxToClient(Inbox inbox, Set<Long> emailIDs, ServerPlayer player) {
//		EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.CreateInbox(inbox.getOwner()), player);
		EmailMain.execute(()-> {
			EmailMain.NETWORK.sendMessageToPlayer(new MsgInboxToClient.SendOther(inbox), player);
			for (long id : inbox.getEmailIDs()) {
//			for (int id = 0; id < 2560; id++) {
				if (!NBTUtils.get(player.getPersistentData(), "displayInbox", false)) {
					break;
				}
				if (emailIDs.contains(id)) {
					continue;
				}
				try {
					Thread.sleep(25);
					Email email = inbox.getEmail(id);
//					Email email = SendDevEmail.getDevEmail().copy();
					if (email != null) {
						SizeReport report = EmailUtils.checkEmailSize(email);
						if (!SizeReport.SUCCESS.equals(report)) {
							player.sendSystemMessage(Component.literal("---------------------------------------------"));
							player.sendSystemMessage(Component.translatable("info.inbox.error.to_big.0"));
							player.sendSystemMessage(Component.translatable("info.inbox.error.to_big.1", report.id(), report.slot(), report.size()));
						}else {
							EmailMain.NETWORK.sendMessageToPlayer(new MsgInboxToClient.SendEmail(id, email), player);
						}
					}
				} catch (Throwable e) {
					e.printStackTrace();
					player.sendSystemMessage(Component.literal("---------------------------------------------"));
					player.sendSystemMessage(Component.literal(String.format("Get email fail, email id: %s", id)));
					player.sendSystemMessage(Component.literal(String.format("Exception: %s", e.getMessage())));
				}
			}
		});
	}
}
