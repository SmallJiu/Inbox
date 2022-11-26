package cat.jiu.email;

import java.io.File;
import java.util.Map.Entry;

import java.util.UUID;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailSendEvent.EmailSenderGroup;
import cat.jiu.email.net.msg.MsgSend;
import cat.jiu.email.net.msg.MsgSendInboxToClient;
import cat.jiu.email.net.msg.MsgUnread;
import cat.jiu.email.net.msg.MsgUnreceive;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonUtil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class EmailAPI {
	public static void sendPlayerEmail(String addressee, Email email) {
		sendEmail(EmailSenderGroup.PLAYER, addressee, email);
	}
	
	public static void sendCommandEmail(String addressee, Email email) {
		sendEmail(EmailSenderGroup.COMMAND, addressee, email);
	}
	
	public static void sendSystemEmail(String addressee, Email email) {
		sendEmail(EmailSenderGroup.SYSTEM, addressee, email);
	}
	
	private static void sendEmail(EmailSenderGroup type, String addressee, Email email) {
		if(EmailMain.proxy.isClient()) {
			EmailMain.net.sendMessageToServer(new MsgSend(type, addressee, email));
		}else {
			if(EmailUtils.hasNameOrUUID(addressee)) {
				Inbox inbox = Inbox.get(EmailUtils.getUUID(addressee));
				inbox.addEmail(email);
				
				if(EmailUtils.saveInboxToDisk(inbox, 10)) {
					EmailMain.log.info("{} send a email to Player: {}, UUID: {}", email.getSender(), addressee, inbox.getOwnerAsUUID());
					MsgSend.sendLog(email.getSender().getKey(), addressee, inbox.getOwnerAsUUID());
					EntityPlayer addresser = EmailMain.server.getEntityWorld().getPlayerEntityByUUID(inbox.getOwnerAsUUID());
					if(addresser!=null) {
						EmailMain.net.sendMessageToPlayer(new MsgUnread(inbox.getUnRead()), (EntityPlayerMP) addresser);
						if(email.hasItems()) {
							EmailMain.net.sendMessageToPlayer(new MsgUnreceive(inbox.getUnReceived()), (EntityPlayerMP) addresser);
						}
						addresser.sendMessage(EmailUtils.createTextComponent("info.email.from", email.getSender()));
					}
				}
			}
		}
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

	public static void sendInboxToClient(Inbox inbox, EntityPlayerMP player) {
		EmailMain.execute(args->{
			EmailMain.net.sendMessageToPlayer(new MsgSendInboxToClient(inbox), player);
		}, 100);
	}
}
