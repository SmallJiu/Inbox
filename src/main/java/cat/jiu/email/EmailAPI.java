package cat.jiu.email;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import java.util.UUID;

import cat.jiu.core.util.NBTUtils;
import cat.jiu.core.util.SideProxy;
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

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.storage.FolderName;
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
	public static void sendPlayerEmail(PlayerEntity player, String addressee, Email email) {
		sendEmail(player, EmailSenderGroup.PLAYER, addressee, email);
	}
	
	public static final IText SYSTEM = new Text(EmailMain.SYSTEM);
	/**
	 * send system email to player. email sender is {@link EmailMain#SYSTEM}
	 */
	public static void sendSystemEmail(PlayerEntity player, String addressee, Email email) {
		email.setSender(SYSTEM);
		sendEmail(player, EmailSenderGroup.SYSTEM, addressee, email);
	}
	
	/**
	 * send email to player.
	 * @param group email owner group
	 */
	public static void sendEmail(PlayerEntity player, EmailSenderGroup group, String addresser, Email email) {
		if(player.getEntityWorld().isRemote()) {
			EmailMain.net.sendMessageToServer(new MsgSend(group, addresser, email));
		}else {
			sendEmail(group, addresser, email);
		}
	}
	
	public static boolean sendEmail(EmailSenderGroup group, String address, Email email) {
		EmailUtils.initNameAndUUID(SideProxy.server);
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
		
		if(SideProxy.server != null) {
			ServerPlayerEntity player;
			try {
				player = SideProxy.getServer().getPlayerList().getPlayerByUUID(UUID.fromString(address));
			}catch(Exception e) {
				player = SideProxy.getServer().getPlayerList().getPlayerByUsername(address);
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
		
		long size = inbox.getInboxSize() + EmailUtils.getSize(email.writeTo(CompoundNBT.class));
		return size < 2097152L;
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
				JsonArray list = new JsonArray();
				if(json.has("history")) {
					JsonElement element = json.get("history");
					if(element.isJsonArray()) {
						list = element.getAsJsonArray();
					}
				}
				if (list.size() >= EmailConfigs.Send.Send_History_Max_Count.get()) {
					list.remove(0);
				}
				list.add(name);
				json.add("history", list);
				JsonParser.toJsonFile(EmailAPI.globalEmailCache, json, false);
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

	public static final String ConfigPath = FMLPaths.CONFIGDIR.get() + "/jiu/inbox/";
	static String
			EmailPath = null,
			EmailRootPath = null,
			typePath = null,
			exportPath = null,
			dataPath = null,
			globalDataPath = null
	;

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
	
	public static String getSaveEmailRootPath() {
		if(EmailRootPath == null) {
			boolean root = EmailConfigs.Save_To_Minecraft_Root_Directory.get();
			if(root
					|| SideProxy.server == null) {
				EmailRootPath = String.valueOf(FMLLoader.getGamePath());
			}else {
				EmailRootPath = new File(String.valueOf(SideProxy.getServer().func_240776_a_(FolderName.LEVEL_DAT))).getParent();
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
	public static boolean addToWhiteList(PlayerEntity player) {
		return addToWhiteList(player.getName().getString(), player.getUniqueID());
	}
	public static boolean addToBlackList(String name, UUID uid) {
		return addToList(name, uid, true);
	}
	public static boolean addToBlackList(PlayerEntity player) {
		return addToBlackList(player.getName().getString(), player.getUniqueID());
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
		
		JsonObject list = new JsonObject();
		if(json.has(theListName)) {
			JsonElement e = json.get(theListName);
			if(e.isJsonObject()) {
				list = e.getAsJsonObject();
			}
		}
		
			list.addProperty(name, uid.toString());
		json.add(theListName, list);
		return JsonParser.toJsonFile(globalEmailListPath, json, false);
	}

	public static boolean isInWhiteList(PlayerEntity player) {
		return isInList(player.getName().getString(), false) || isInList(player.getUniqueID().toString(), false);
	}
	public static boolean isInWhiteList(UUID uid) {
		return isInList(uid.toString(), false);
	}
	public static boolean isInWhiteList(String name) {
		return isInList(name, false);
	}
	public static boolean isInBlackList(PlayerEntity player) {
		return isInList(player.getName().getString(), true) || isInList(player.getUniqueID().toString(), true);
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
	
	public static boolean removeInWhiteList(PlayerEntity player) {
		return removeInList(player.getName().getString(), false);
	}
	public static boolean removeInWhiteList(UUID uid) {
		return removeInList(uid.toString(), false);
	}
	public static boolean removeInWhiteList(String name) {
		return removeInList(name, false);
	}
	public static boolean removeInBlackList(PlayerEntity player) {
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
	public static void refreshScheduledEmailMap(ServerPlayerEntity player) {
		EmailMain.execute(()->{
			File dir = new File(EmailAPI.getGlobalDataPath() + "emails/");
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

	public static void sendScheduledEmailToClient(ServerPlayerEntity player) {
		EmailMain.execute(()->{
			for (ScheduledEmail email : ScheduledEmail.getScheduledEmails()) {
				boolean check = true;
				if (email.getAddressee().isCustomPlayers() && !player.getServer().getPlayerList().getOppedPlayers().hasEntry(player.getGameProfile())) {
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

	public static void sendInboxToClient(Inbox inbox, ServerPlayerEntity player) {
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
							player.sendStatusMessage(new StringTextComponent("---------------------------------------------"), false);
							player.sendStatusMessage(new TranslationTextComponent("info.inbox.error.to_big.0"), false);
							player.sendStatusMessage(new TranslationTextComponent("info.inbox.error.to_big.1", report.id, report.slot, report.size), false);
						}else {
							EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.SendEmail(id, email), player);
						}
					}
				} catch (Throwable e) {
					e.printStackTrace();
					player.sendStatusMessage(new StringTextComponent("---------------------------------------------"), false);
					player.sendStatusMessage(new StringTextComponent(String.format("Get email fail, email id: %s", id)), false);
					player.sendStatusMessage(new StringTextComponent(String.format("Exception: %s", e.getMessage())), false);
				}
			}
		}, 50);
	}
}
