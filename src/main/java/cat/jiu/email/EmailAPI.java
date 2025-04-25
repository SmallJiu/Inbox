package cat.jiu.email;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import java.util.UUID;

import cat.jiu.core.util.NBTUtils;
import cat.jiu.email.api.IEmailStyle;
import cat.jiu.email.element.EmailSenderGroup;
import cat.jiu.email.element.ScheduledEmail;
import cat.jiu.email.net.msg.refresh.MsgRefreshScheduledEmail;
import cat.jiu.core.util.client.config.ConfigWriteEvent;
import cat.jiu.email.event.EmailSendEvent;
import cat.jiu.email.net.msg.*;
import cat.jiu.email.ui.gui.GuiInbox;
import cat.jiu.email.util.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
			EmailMain.net.sendMessageToServer(new MsgSend(group, addresser, email));
		}else {
			sendEmail(group, addresser, email);
		}
	}
	
	public static synchronized boolean sendEmail(EmailSenderGroup group, String address, Email email) {
		EmailUtils.initNameAndUUID(EmailMain.server);
		Inbox inbox = Inbox.get(address);
		
		if(!EmailConfigs.isInfiniteSize()
		&& email.hasAttachment() && !checkEmailSize(inbox, email)) {
			return false;
		}

		if (group.isPlayerSend() && inbox.isInSenderBlacklist(email.getSender().getText())) {
			EmailMain.log.warn("Failed to send the email, You have been block by the Addressee!");
			return false;
		}
		
		if(MinecraftForge.EVENT_BUS.post(new EmailSendEvent(Phase.START, group, address, email))) return false;
		
		inbox.addEmail(email, true);
		
		MinecraftForge.EVENT_BUS.post(new EmailSendEvent(Phase.END, group, address, email));
		
		if(EmailMain.server != null) {
			ServerPlayer player;
			try {
				player = EmailMain.server.getPlayerList().getPlayer(UUID.fromString(address));
			}catch(Exception e) {
				player = EmailMain.server.getPlayerList().getPlayerByName(address);
			}
			if(player != null) {
				EmailUtils.sendMessage(player, "info.inbox.from", email.getSender());
				EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.SendEmail(inbox.getEmailHistoryCount(), email), player);
			}
		}
		return true;
	}
	
	private static boolean checkEmailSize(Inbox inbox, Email email) {
		SizeReport report = EmailUtils.checkEmailSize(email);
		if(!SizeReport.SUCCESS.equals(report)) {
			return false;
		}
		
		long size = inbox.getInboxSize() + EmailUtils.getSize(email.writeTo(CompoundTag.class));
		return size < 2097152L;
	}

	public static IEmailStyle getEmailStyle() {
		JsonElement e = EmailAPI.globalEmailCache.exists() ? JsonParser.parse(EmailAPI.globalEmailCache) : new JsonObject();
		if(e != null && e.isJsonObject()) {
			return IEmailStyle.REGISTRY.get(e.getAsJsonObject());
		}
		return null;
	}
	public static void saveEmailStyle(IEmailStyle style) {
		JsonElement e = EmailAPI.globalEmailCache.exists() ? JsonParser.parse(EmailAPI.globalEmailCache) : new JsonObject();
		if(e != null && e.isJsonObject()) {
			e.getAsJsonObject().addProperty(IEmailStyle.NAME_ID, String.valueOf(style.getID()));
			JsonParser.toJsonFile(EmailAPI.globalEmailCache.getPath(), e, false);
		}
	}

	public static void addAddresseeHistory(String name) {
		JsonElement e = EmailAPI.globalEmailCache.exists() ? JsonParser.parse(EmailAPI.globalEmailCache) : new JsonObject();
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
				if (list.size() >= EmailConfigs.Send.Send_History_Max_Count.get()) {
					list.remove(0);
				}
				list.add(name);
				json.add("history", list);
				JsonParser.toJsonFile(EmailAPI.globalEmailCache.getPath(), json, false);
			}
		}
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

	@SubscribeEvent
	public static void onConfigWrite(ConfigWriteEvent event){
		if(event.spec == EmailConfigs.CONFIG_MAIN){
			setRootPath();
		}
	}

	static void setRootPath(){
		clearEmailPath();
		if(EmailConfigs.Save_To_Minecraft_Root_Directory.get()){
			EmailRootPath = String.valueOf(FMLLoader.getGamePath());
			EmailMain.log.info(String.format("Set inbox root path to: %s", EmailRootPath));
		}

		if(!StringUtils.isEmpty(EmailConfigs.Custom_Inbox_Path.get())){
			File path = new File(EmailConfigs.Custom_Inbox_Path.get());
			if(!path.exists()){
				path.mkdirs();
			}
			if(path.isDirectory()){
				EmailRootPath = EmailConfigs.Custom_Inbox_Path.get();
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
			boolean root = EmailConfigs.Save_To_Minecraft_Root_Directory.get();
			if(root
					|| EmailMain.server == null) {
				EmailRootPath = String.valueOf(FMLLoader.getGamePath());
			}else {
				EmailRootPath = new File(String.valueOf(EmailMain.server.getWorldPath(LevelResource.LEVEL_DATA_FILE))).getParent();
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
		String theListName = black ? "BlackList" : "WhiteList";
		File jsonFile = new File(globalEmailListPath);
		JsonObject json = new JsonObject();
		if(jsonFile.exists()) {
			JsonElement e = JsonParser.parse(jsonFile);
			if(e != null && e.isJsonObject()) {
				json = e.getAsJsonObject();
			}
		}
		
		JsonObject list = null;
		if(json.has(theListName) && json.get(theListName).isJsonObject()) {
			list = json.get(theListName).getAsJsonObject();
		}else {
			list = new JsonObject();
		}
		
			list.addProperty(name, uid.toString());
		json.add(theListName, list);
		return JsonParser.toJsonFile(globalEmailListPath, json, false);
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
		String inList = black ? "BlackList" : "WhiteList";
		File jsonFile = new File(globalEmailListPath);
		if(jsonFile.exists()) {
			JsonElement jsonO = JsonParser.parse(jsonFile);
			if(jsonO != null && jsonO.isJsonObject()) {
				JsonObject json = jsonO.getAsJsonObject();
				if(json.has(inList)) {
					JsonElement e = json.get(inList);
					if(e.isJsonObject()) {
						if(e.getAsJsonObject().has(str)) return true;
						for(Entry<String, JsonElement> names : e.getAsJsonObject().entrySet()) {
							String listName = names.getKey();
							String listUUID = names.getValue().getAsString();
							if(listName.equalsIgnoreCase(str) || listUUID.equalsIgnoreCase(str)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
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
		String inList = black ? "BlackList" : "WhiteList";
		File jsonFile = new File(globalEmailListPath);
		if(jsonFile.exists()) {
			JsonElement jsonO = JsonParser.parse(jsonFile);
			if(jsonO != null && jsonO.isJsonObject()) {
				JsonObject json = jsonO.getAsJsonObject();
				if(json.has(inList)) {
					JsonElement e = json.get(inList);
					if(e.isJsonObject()) {
						JsonObject list = e.getAsJsonObject();
						if(list.has(name)) {
							list.remove(name);
							return JsonParser.toJsonFile(globalEmailListPath, json, false);
						}else {
							for(Entry<String, JsonElement> names : Sets.newHashSet(list.entrySet())) {
								String listName = names.getKey();
								String listUUID = names.getValue().getAsString();
								if(listName.equalsIgnoreCase(name) || listUUID.equalsIgnoreCase(name)) {
									list.remove(listName);
									return JsonParser.toJsonFile(globalEmailListPath, json, false);
								}
							}
						}
					}
				}
			}
		}
		return true;
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
							Email email = new Email(JsonParser.parse(file).getAsJsonObject());
							EmailMain.net.sendMessageToPlayer(new MsgRefreshScheduledEmail.SendMap(path.substring(0, path.indexOf('.')), email), player);
						}catch (Exception ignored){ }
					}
				}
			}
		},50);
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
						EmailMain.net.sendMessageToPlayer(new MsgRefreshScheduledEmail.Send(email), player);
					} catch (Exception ignored) {}
				}
			}
		}, 50);
	}

	public static void sendInboxToClient(Inbox inbox, ServerPlayer player) {
//		EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.CreateInbox(inbox.getOwner()), player);
		EmailMain.execute(()-> {
			EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.SendOther(inbox), player);
			for (Long id : inbox.getEmailIDs()) {
//			for (int id = 0; id < 2560; id++) {
				if (!NBTUtils.get(player.getPersistentData(), "displayInbox", false)) {
					break;
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
							EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.SendEmail(id, email), player);
						}
					}
				} catch (Throwable e) {
					e.printStackTrace();
					player.sendSystemMessage(Component.literal("---------------------------------------------"));
					player.sendSystemMessage(Component.literal(String.format("Get email fail, email id: %s", id)));
					player.sendSystemMessage(Component.literal(String.format("Exception: %s", e.getMessage())));
				}
			}
		}, 50);
	}
}
