package cat.jiu.email;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import java.util.UUID;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.email.event.EmailSendEvent.EmailSenderGroup;
import cat.jiu.email.net.msg.MsgGetter;
import cat.jiu.email.net.msg.MsgSend;
import cat.jiu.email.util.EmailSound;
import cat.jiu.email.util.JsonUtil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/**
 * .查找可能存在的bug
 * .补全en_us.lang
 */
public class Email {
	public static void sendPlayerToPlayerEmail(String sender, String title, String addressee, @Nullable List<String> msgs, @Nullable EmailSound sound) {
		EmailMain.net.sendMessageToServer(new MsgSend(EmailSenderGroup.PLAYER, sender, title, addressee, msgs, sound));
	}
	
	public static void sendCMDToPlayerEmail(String sender, String title, String addressee, @Nullable List<String> msgs, @Nullable EmailSound sound, @Nullable List<ItemStack> items) {
		EmailMain.net.sendMessageToServer(new MsgSend(EmailSenderGroup.COMMAND, sender, title, addressee, msgs, sound, items));
	}
	
	public static void sendJavaToPlayerEmail(String sender, String title, String addressee, @Nullable List<String> msgs, @Nullable EmailSound sound, @Nullable List<ItemStack> items) {
		EmailMain.net.sendMessageToServer(new MsgSend(EmailSenderGroup.SYSTEM, sender, title, addressee, msgs, sound, items));
	}
	
	private static final String globalEmailListPath = "./email.json";
	
	public static boolean addToWhiteList(String name, UUID uid) {
		return addToList(name, uid, false);
	}
	public static boolean addToWhiteList(EntityPlayer player) {
		return addToWhiteList(player.getName(), player.getUniqueID());
	}
	public static boolean addToBlackList(String name, UUID uid) {
		return addToList(name, uid, true);
	}
	public static boolean addToBlackList(EntityPlayer player) {
		return addToBlackList(player.getName(), player.getUniqueID());
	}
	private static boolean addToList(String name, UUID uid, boolean black) {
		String theListName = black ? "BlackList" : "WhiteList";
		File jsonFile = new File(globalEmailListPath);
		JsonObject json = new JsonObject();;
		if(jsonFile.exists()) {
			JsonElement e = JsonUtil.parse(jsonFile);
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
		return JsonUtil.toJsonFile(globalEmailListPath, json, false);
	}

	public static boolean isInWhiteList(EntityPlayer player) {
		return isInList(player.getName(), false) || isInList(player.getUniqueID().toString(), false);
	}
	public static boolean isInWhiteList(UUID uid) {
		return isInList(uid.toString(), false);
	}
	public static boolean isInWhiteList(String name) {
		return isInList(name, false);
	}
	public static boolean isInBlackList(EntityPlayer player) {
		return isInList(player.getName(), true) || isInList(player.getUniqueID().toString(), true);
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
			JsonElement jsonO = JsonUtil.parse(jsonFile);
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
	
	public static boolean removeInWhiteList(EntityPlayer player) {
		return removeInList(player.getName(), false);
	}
	public static boolean removeInWhiteList(UUID uid) {
		return removeInList(uid.toString(), false);
	}
	public static boolean removeInWhiteList(String name) {
		return removeInList(name, false);
	}
	public static boolean removeInBlackList(EntityPlayer player) {
		return removeInList(player.getName(), true);
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
			JsonElement jsonO = JsonUtil.parse(jsonFile);
			if(jsonO != null && jsonO.isJsonObject()) {
				JsonObject json = jsonO.getAsJsonObject();
				if(json.has(inList)) {
					JsonElement e = json.get(inList);
					if(e.isJsonObject()) {
						JsonObject list = e.getAsJsonObject();
						if(list.has(name)) {
							list.remove(name);
							return JsonUtil.toJsonFile(globalEmailListPath, json, false);
						}else {
							for(Entry<String, JsonElement> names : Sets.newHashSet(list.entrySet())) {
								String listName = names.getKey();
								String listUUID = names.getValue().getAsString();
								if(listName.equalsIgnoreCase(name) || listUUID.equalsIgnoreCase(name)) {
									list.remove(listName);
									return JsonUtil.toJsonFile(globalEmailListPath, json, false);
								}
							}
						}
					}
				}
			}
		}
		return true;
	}

	public static void sendEmailToClient(JsonObject email, EntityPlayerMP player) {
		new Thread(()->{
			// for network delay, need send after
			try {Thread.sleep(100);}catch(InterruptedException e) { e.printStackTrace();}
			EmailMain.net.sendMessageToPlayer(new MsgGetter(email), player);
		}).start();
	}
}
