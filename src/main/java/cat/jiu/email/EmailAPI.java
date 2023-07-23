package cat.jiu.email;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import java.util.UUID;

import cat.jiu.email.element.EmailSenderGroup;
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

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.Phase;

public class EmailAPI {
	/**
	 * send player email to player. sender is email sender
	 * @param player
	 * @param addressee
	 * @param email
	 */
	public static void sendPlayerEmail(PlayerEntity player, String addressee, Email email) {
		sendEmail(player, EmailSenderGroup.PLAYER, addressee, email);
	}
	
	private static final IText SYSTEM = new Text(EmailMain.SYSTEM);
	/**
	 * send system email to player. email sender is {@link EmailMain#SYSTEM}
	 * @param player
	 * @param addressee
	 * @param email
	 */
	public static void sendSystemEmail(PlayerEntity player, String addressee, Email email) {
		email.setSender(SYSTEM);
		sendEmail(player, EmailSenderGroup.SYSTEM, addressee, email);
	}
	
	/**
	 * send a email to player. 
	 * @param player
	 * @param group email owner group
	 * @param addresser
	 * @param email
	 */
	public static void sendEmail(PlayerEntity player, EmailSenderGroup group, String addresser, Email email) {
		if(player.getEntityWorld().isRemote()) {
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
			ServerPlayerEntity player;
			try {
				player = EmailMain.server.getPlayerList().getPlayerByUUID(UUID.fromString(addresser));
			}catch(Exception e) {
				player = EmailMain.server.getPlayerList().getPlayerByUsername(addresser);
			}
			if(player != null) {
				EmailUtils.sendMessage(player, "info.email.from", email.getSender());
			}
		}
		return true;
	}
	
	private static boolean checkEmailSize(Inbox inbox, Email email, List<ItemStack> stacks) {
		SizeReport report = EmailUtils.checkEmailSize(email);
		if(!SizeReport.SUCCES.equals(report)) {
			return false;
		}
		
		long size = inbox.getInboxSize() + EmailUtils.getSize(email.writeTo(CompoundNBT.class));
		if(size >= 2097152L) {
			return false;
		}
		return true;
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
	static String typePath = getSaveEmailRootPath() + File.separator + "email" + File.separator + "type" + File.separator;
	static String exportPath = typePath + "export" + File.separator;
	
	public static String getSaveEmailRootPath() {
		if(EmailRootPath == null) {
			if(EmailConfigs.Save_To_Minecraft_Root_Directory.get() || EmailMain.server == null) {
				EmailRootPath = ".";
			}else {
				EmailRootPath = new File(String.valueOf(EmailMain.server.func_240776_a_(FolderName.LEVEL_DAT))).getParent();
			}
		}
		
		return EmailRootPath;
	}

	public static String getTypePath() {
		if(typePath == null) {
			typePath = getSaveEmailRootPath() + File.separator + "email" + File.separator + "type" + File.separator;
		}
		return typePath;
	}
	public static String getExportPath() {
		if(exportPath == null) {
			exportPath = typePath + "export" + File.separator;
		}
		return exportPath;
	}
	
	public static String getSaveInboxPath() {
		if(EmailPath == null) {
			EmailPath = getSaveEmailRootPath() + File.separator + "email" + File.separator;
		}
		return EmailPath;
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
	
	public static void sendInboxToClient(Inbox inbox, ServerPlayerEntity player) {
		EmailMain.execute(()->{
			EmailMain.net.sendMessageToPlayer(new MsgInboxToClient(inbox), player);
			EmailMain.execute(()->{
				EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.MsgOtherToClient(inbox), player);
			}, 50);
		}, 100);
	}
}
