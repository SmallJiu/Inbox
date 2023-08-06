package cat.jiu.email;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import java.util.UUID;

import cat.jiu.email.element.EmailSenderGroup;
import cat.jiu.email.ui.gui.config.ConfigWriteEvent;
import cat.jiu.email.event.EmailSendEvent;
import cat.jiu.email.net.msg.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.SizeReport;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonParser;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.StringUtils;

@Mod.EventBusSubscriber
public class EmailAPI {
	/**
	 * send player email to player. sender is email sender
	 */
	public static void sendPlayerEmail(Player player, String addressee, Email email) {
		sendEmail(player, EmailSenderGroup.PLAYER, addressee, email);
	}
	
	private static final IText SYSTEM = new Text(EmailMain.SYSTEM);
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
	
	public static boolean sendEmail(EmailSenderGroup group, String addresser, Email email) {
		EmailUtils.initNameAndUUID(EmailMain.server);
		Inbox inbox = Inbox.get(addresser);
		
		if(!EmailConfigs.isInfiniteSize()
		&& email.hasItems() && !checkEmailSize(inbox, email, email.getItems())) {
			return false;
		}
		
		if(MinecraftForge.EVENT_BUS.post(new EmailSendEvent(Phase.START, group, addresser, email))) return false;
		
		inbox.addEmail(email, true);
		
		MinecraftForge.EVENT_BUS.post(new EmailSendEvent(Phase.END, group, addresser, email));
		
		if(EmailMain.server != null) {
			ServerPlayer player;
			try {
				player = EmailMain.server.getPlayerList().getPlayer(UUID.fromString(addresser));
			}catch(Exception e) {
				player = EmailMain.server.getPlayerList().getPlayerByName(addresser);
			}
			if(player != null) {
				EmailUtils.sendMessage(player, "info.email.from", email.getSender());
			}
		}
		return true;
	}
	
	private static boolean checkEmailSize(Inbox inbox, Email email, List<ItemStack> stacks) {
		SizeReport report = EmailUtils.checkEmailSize(email);
		if(!SizeReport.SUCCESS.equals(report)) {
			return false;
		}
		
		long size = inbox.getInboxSize() + EmailUtils.getSize(email.writeTo(CompoundTag.class));
		return size < 2097152L;
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
	
	static String EmailPath = null;
	static String EmailRootPath = null;
	static String typePath = null;
	static String exportPath = null;

	@SubscribeEvent
	public static void onConfigWrite(ConfigWriteEvent event){
		if(event.spec == EmailConfigs.CONFIG_MAIN){
			setRootPath();
		}
	}

	static void setRootPath(){
		clearEmailPath();
		if(EmailConfigs.Save_To_Minecraft_Root_Directory.get()){
			EmailRootPath = ".";
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
			if(EmailConfigs.Save_To_Minecraft_Root_Directory.get() || EmailMain.server == null) {
				EmailRootPath = ".";
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
	
	public static void clearEmailPath() {
		EmailRootPath = null;
		EmailPath = null;
		typePath = null;
		exportPath = null;
	}
	
	public static final String globalEmailListPath = "./email.json";
	
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
		JsonObject json = new JsonObject();;
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
	
	public static void sendInboxToClient(Inbox inbox, ServerPlayer player) {
		EmailMain.execute(()->{
			EmailMain.net.sendMessageToPlayer(new MsgInboxToClient(inbox), player);
			EmailMain.execute(()-> EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.MsgOtherToClient(inbox), player), 50);
		}, 100);
	}
}
