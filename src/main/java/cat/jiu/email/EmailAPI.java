package cat.jiu.email;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;

import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailSendEvent;
import cat.jiu.email.event.EmailSendEvent.EmailSenderGroup;
import cat.jiu.email.net.msg.MsgSend;
import cat.jiu.email.net.msg.MsgInboxToClient;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.SizeReport;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonUtil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StringUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

@EventBusSubscriber
public class EmailAPI {
	/**
	 * send player email to player. sender is email sender
	 * @param player the sender
	 * @param addressee send to who
	 * @param email email object
	 */
	public static void sendPlayerEmail(EntityPlayer player, String addressee, Email email) {
		sendEmail(player, EmailSenderGroup.PLAYER, addressee, email);
	}
	
	public static final IText SYSTEM = new Text(EmailMain.SYSTEM) {
		@Override
		public Text setText(String key) {
			return this;
		}
	};
	/**
	 * send system email to player. email sender is {@link EmailMain#SYSTEM}
	 * @param player the player object
	 * @param addressee send to who
	 * @param email email object
	 */
	public static void sendSystemEmail(EntityPlayer player, String addressee, Email email) {
		email.setSender(SYSTEM);
		sendEmail(player, EmailSenderGroup.SYSTEM, addressee, email);
	}
	
	/**
	 * send email to player.
	 * @param player the player object
	 * @param group email owner group
	 * @param addressee send to who
	 * @param email email object
	 */
	public static void sendEmail(EntityPlayer player, EmailSenderGroup group, String addressee, Email email) {
		if(player.world.isRemote) {
			EmailMain.net.sendMessageToServer(new MsgSend(group, addressee, email));
		}else {
			sendEmail(group, addressee, email);
		}
	}
	
	public static boolean sendEmail(EmailSenderGroup group, String addressee, Email email) {
		EmailUtils.initNameAndUUID(EmailMain.server);
		Inbox inbox = Inbox.get(addressee);
		
		if(!EmailConfigs.isInfiniteSize()
		&& email.hasItems() && !checkEmailSize(inbox, email, email.getItems())) {
			return false;
		}
		
		if(MinecraftForge.EVENT_BUS.post(new EmailSendEvent(Phase.START, group, addressee, email))) return false;
		
		inbox.addEmail(email, true);
		
		MinecraftForge.EVENT_BUS.post(new EmailSendEvent(Phase.END, group, addressee, email));
		
		if(EmailMain.server != null) {
			EntityPlayerMP player;
			try {
				player = EmailMain.server.getPlayerList().getPlayerByUUID(UUID.fromString(addressee));
			}catch(Exception e) {
				player = EmailMain.server.getPlayerList().getPlayerByUsername(addressee);
			}
			if(player != null) {
				EmailUtils.sendMessage(player, "info.email.from", email.getSender());
			}
		}
		return true;
	}

	public static void addAddresseeHistory(String name) {
		File jsonFile = new File(EmailAPI.globalEmailListPath);
		if(jsonFile.exists()) {
			JsonElement e = JsonUtil.parse(jsonFile);
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
					if (list.size() >= EmailConfigs.Send.Send_History_Max_Count) {
						list.remove(0);
					}
					list.add(name);
					json.add("history", list);
					JsonUtil.toJsonFile(EmailAPI.globalEmailListPath, json, false);
				}
			}
		}
	}
	
	private static boolean checkEmailSize(Inbox inbox, Email email, List<ItemStack> stacks) {
		SizeReport report = EmailUtils.checkEmailSize(email);
		if(!SizeReport.SUCCESS.equals(report)) {
			return false;
		}
		
		long size = inbox.getInboxSize() + EmailUtils.getSize(email.writeTo(NBTTagCompound.class));
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
	public static void onConfigChange(ConfigChangedEvent.PostConfigChangedEvent event) {
		if(EmailMain.MODID.equalsIgnoreCase(event.getModID())) {
			setRootPath();
		}
	}
	
	static boolean setRootPath() {
		clearEmailPath();
		if(!StringUtils.isNullOrEmpty(EmailConfigs.Custom_Inbox_Path)){
			File path = new File(EmailConfigs.Custom_Inbox_Path);
			if(!path.exists()){
				path.mkdirs();
			}
			if(path.isDirectory()){
				EmailRootPath = EmailConfigs.Custom_Inbox_Path;
			}else if(path.isFile()){
				EmailRootPath = path.getParent();
			}
			EmailMain.log.info(String.format("Set inbox root path to: %s", EmailRootPath));
			return true;
		}
		return false;
	}
	
	public static String getSaveEmailRootPath() {
		if(EmailRootPath == null) {
			if(!setRootPath()) {
				if(EmailConfigs.Save_To_Minecraft_Root_Directory || EmailMain.server == null) {
					EmailRootPath = ".";
				}else {
					EmailRootPath = EmailMain.server.getEntityWorld().getSaveHandler().getWorldDirectory().toString();
				}
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
			exportPath = typePath + "export" + File.separator;
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
		EmailMain.execute(()->{
			EmailMain.net.sendMessageToPlayer(new MsgInboxToClient(inbox), player);
			EmailMain.execute(()->
				EmailMain.net.sendMessageToPlayer(new MsgInboxToClient.MsgOtherToClient(inbox), player)
			, 50);
		}, 100);
	}
}
