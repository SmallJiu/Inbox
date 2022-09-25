package cat.jiu.email.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import java.util.UUID;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.EmailFunction;
import cat.jiu.email.element.EmailSound;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.element.Message;
import cat.jiu.email.net.msg.MsgSend;
import cat.jiu.email.util.EmailSenderSndSound.Time;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import net.minecraftforge.items.ItemStackHandler;

public class EmailUtils {
	private static String EmailPath = null;
	private static String EmailRootPath = null;
	public static final JsonParser parser = new JsonParser();
	public static final String typePath = "." + File.separator + "email" + File.separator + "type" + File.separator;
	public static final String exportPath = typePath + "export" + File.separator;
	
	public static boolean saveInboxToDisk(Inbox inbox, int maxRetryCount) {
		int retry = 0;
		boolean succes = false;
		while(true) {
			if(inbox.save()) {
				succes = true;
				break;
			}
			if(retry == maxRetryCount) {
				break;
			}
			retry++;
		}
		
		if(succes) {
			return true;
		}else {
			EmailMain.log.error("Can not save Inbox to Disk! Owner: {}, UUID: {}, Retry count: {}", inbox.getOwner(), inbox.getOwnerAsUUID(), retry);
			return false;
		}
	}
	
	public static List<File> getAllFiles(File dir) {
		List<File> files = Lists.newArrayList();
		for(File file : dir.listFiles()) {
			if(file.isDirectory()) {
				files.addAll(getAllFiles(file));
			}else {
				files.add(file);
			}
		}
		return files;
	}
	
	public static EmailFunction findFunction(String file) {
		File functionFile = findFunctionFile(new File(typePath), file);
		if(functionFile != null) {
			JsonElement e = JsonUtil.parse(functionFile);
			if(e != null && e.isJsonObject()) {
				JsonObject function = e.getAsJsonObject();
				List<Message> msgs = null;
				if(function.has("msgs")) {
					msgs = Lists.newArrayList();
					JsonElement msgE = function.get("msgs");
					if(msgE.isJsonPrimitive()) {
						msgs.add(new Message(msgE.getAsString()));
					}else if(msgE.isJsonArray()) {
						JsonArray msgsArray = msgE.getAsJsonArray();
						for(int i = 0; i < msgsArray.size(); i++) {
							msgs.add(new Message(msgsArray.get(i).getAsString()));
						}
					}else if(msgE.isJsonObject()) {
						for(Entry<String, JsonElement> msg : msgE.getAsJsonObject().entrySet()) {
							JsonArray argJson = msg.getValue().getAsJsonArray();
							Object[] arg = new Object[argJson.size()];
							for(int i = 0; i < arg.length; i++) {
								arg[i] = argJson.get(i).getAsString();
							}
							msgs.add(new Message(msg.getKey(), arg));
						}
					}
				}
				
				List<ItemStack> items = null;
				if(function.has("items")) {
					JsonElement itemE = function.get("items");
					if(itemE.isJsonPrimitive() && itemE.getAsJsonPrimitive().isString()) {
						ItemStack stack = JsonToStackUtil.toStack(function.get("items"));
						if(stack!=null) {
							items = Lists.newArrayList(stack);
						}else {
							String path = function.get("items").getAsString();
							JsonElement itemsE = JsonUtil.parse(exportPath + path);
							if(itemsE!=null) {
								items = JsonToStackUtil.toStacks(itemsE);
							}
						}
					}else if(itemE.isJsonObject()) {
						items = Lists.newArrayList();
						for(Entry<String, JsonElement> item : itemE.getAsJsonObject().entrySet()) {
							if(item.getValue().isJsonPrimitive()) {
								ItemStack stack = JsonToStackUtil.toStack(item.getValue());
								if(stack!=null) {
									items.add(stack);
								}else {
									JsonElement itemsE = JsonUtil.parse(exportPath + item.getValue().getAsJsonPrimitive().getAsString());
									if(itemsE!=null) {
										for(ItemStack stack0 : JsonToStackUtil.toStacks(itemsE)) {
											if(stack0!=null) {
												items.add(stack0);
											}
										}
									}
								}
							}else {
								items.add(JsonToStackUtil.toStack(item.getValue()));
							}
						}
					}else if(itemE.isJsonArray()) {
						items = Lists.newArrayList();
						for(int i = 0; i < itemE.getAsJsonArray().size(); i++) {
							JsonElement item = itemE.getAsJsonArray().get(i);
							if(item.isJsonPrimitive()) {
								ItemStack stack = JsonToStackUtil.toStack(item);
								if(stack!=null) {
									items.add(stack);
								}else {
									JsonElement itemsE = JsonUtil.parse(exportPath + item.getAsJsonPrimitive().getAsString());
									if(itemsE!=null) {
										for(ItemStack stack0 : JsonToStackUtil.toStacks(itemsE)) {
											if(stack0!=null) {
												items.add(stack0);
											}
										}
									}
								}
							}else {
								ItemStack stack = JsonToStackUtil.toStack(item);
								if(stack!=null) {
									items.add(stack);
								}
							}
						}
					}
				}
				
				EmailSound sound = null;
				if(function.has("sound")) {
					JsonObject jsonSound = function.getAsJsonObject("sound");
					sound = new EmailSound(
							new Time(jsonSound.get("time").getAsLong()), 
							SoundEvent.REGISTRY.getObject(new ResourceLocation(jsonSound.get("name").getAsString())),
							jsonSound.get("volume").getAsFloat(),
							jsonSound.get("pitch").getAsFloat());
				}
				
				Message title = Message.empty;
				JsonElement titleJson = function.get("title");
				if(titleJson.isJsonPrimitive()) {
					title = new Message(titleJson.getAsString());
				}else if(titleJson.isJsonObject()) {
					JsonObject titleObj = titleJson.getAsJsonObject();
					String key = titleObj.get("key").getAsString();
					Object[] args = MsgSend.SendRenderText.empty;
					if(titleObj.has("args")) {
						JsonArray argJson = titleObj.getAsJsonArray("args");
						args = new Object[argJson.size()];
						for(int i = 0; i < args.length; i++) {
							args[i] = argJson.get(i).getAsString();
						}
					}
					title = new Message(key, args);
				}
				
				return new EmailFunction(
						function.get("sender").getAsString(),
						function.get("addresser").getAsString(),
						title,
						items, msgs, sound);
			}
		}
		
		return null;
	}
	
	private static File findFunctionFile(File dir, String name) {
		if(dir==null || !dir.exists()) return null;
		for(File subFile : dir.listFiles()) {
			if(subFile.isDirectory() && !"export".equals(subFile.getName()) && !"event".equals(subFile.getName())) {
				File f = findFunctionFile(subFile, name);
				if(f != null) return f;
			}else if(subFile.getName().equals(name)) {
				return subFile;
			}
		}
		return null;
	}
	
	public static ITextComponent createTextComponent(String arg, Object... objs) {
		return new TextComponentTranslation(arg, objs);
	}
	
	public static ITextComponent createTextComponent(String arg, TextFormatting color, Object... objs) {
		ITextComponent text = new TextComponentTranslation(arg, objs);
		return text.setStyle(text.getStyle().setColor(color)); 
	}
	
	public static EmailSizeReport checkInboxSize(Inbox inbox) {
		if(inbox == null || inbox.isEmpty()) return EmailSizeReport.SUCCES;
		
		for(int id = 0; id < inbox.count(); id++) {
			cat.jiu.email.element.Email email = inbox.get(id);
			if(email.hasItems()) {
				List<ItemStack> items = email.getItems();
				for(int slot = 0; slot < items.size(); slot++) {
					long size = getSize(items.get(slot).writeToNBT(new NBTTagCompound()));
					if(size >= 2097152L) {
						return new EmailSizeReport(id, slot, size);
					}
				}
			}
		}
		
		long size = inbox.getInboxSize();
		return size >= 2097152L ? new EmailSizeReport(-1, -1, size) : EmailSizeReport.SUCCES;
	}
	
	public static long getSize(NBTTagCompound nbt) {
		if(nbt == null) return 0;
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeCompoundTag(nbt);
		
		int index = pb.readerIndex();
        if(pb.readByte() == 0) return 0;
        pb.readerIndex(index);
        
        Tracker tracker = new Tracker();
        try {
        	CompressedStreamTools.read(new ByteBufInputStream(pb), tracker);
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return tracker.read;
	}
	
	private static class Tracker extends NBTSizeTracker {
		public Tracker() {
			super(0);
		}
		public void read(long bits) {
    		this.read += bits / 8L;
    	}
	}
	
	private static final HashMap<String, UUID> NameToUUID = Maps.newHashMap();
	private static final HashMap<UUID, String> UUIDToName = Maps.newHashMap();

	public static void initNameAndUUID(@Nullable MinecraftServer server) {
		if(server != null) {
			server.getPlayerProfileCache().save();
			server.getPlayerProfileCache().load();
		}
		File file = new File("./usernamecache.json");
		if(file.exists()) {
			NameToUUID.clear();
			UUIDToName.clear();
			try(FileInputStream in = new FileInputStream(file)) {
				JsonObject obj = parser.parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
				for(Entry<String, JsonElement> cache : obj.entrySet()) {
					String name = cache.getValue().getAsString();
					UUID uid = UUID.fromString(cache.getKey());
					NameToUUID.put(name, uid);
					UUIDToName.put(uid, name);
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static boolean hasNameOrUUID(String name) {
		return (!NameToUUID.isEmpty() && !UUIDToName.isEmpty()) ? NameToUUID.containsKey(name) && UUIDToName.containsValue(name) : false;
	}

	public static boolean hasNameOrUUID(UUID uid) {
		return (!NameToUUID.isEmpty() && !UUIDToName.isEmpty()) ? UUIDToName.containsKey(uid) && NameToUUID.containsValue(uid) : false;
	}

	public static UUID getUUID(String name) {
		if(hasNameOrUUID(name)) {
			UUID uid = NameToUUID.get(name);
			if(uid==null) {
				for(Entry<UUID, String> uuid : UUIDToName.entrySet()) {
					if(uuid.getValue().equals(name)) {
						uid = uuid.getKey();
						break;
					}
				}
			}
			return uid;
		}
		return null;
	}

	public static String getName(UUID uid) {
		if(hasNameOrUUID(uid)) {
			String name = UUIDToName.get(uid);
			if(name==null) {
				for(Entry<String, UUID> names : NameToUUID.entrySet()) {
					if(names.getValue().equals(uid)) {
						name = names.getKey();
						break;
					}
				}
			}
			return name;
		}
		return null;
	}
	
	public static Set<UUID> getAllUUID(){
		return Sets.newHashSet(UUIDToName.keySet());
	}
	public static Set<String> getAllName(){
		return Sets.newHashSet(NameToUUID.keySet());
	}
	
	public static String getSaveEmailRootPath() {
		if(EmailRootPath == null) {
			if(EmailConfigs.Save_To_Minecraft_Root_Directory) {
				EmailRootPath = ".";
			}else {
				EmailRootPath = EmailMain.server.getEntityWorld().getSaveHandler().getWorldDirectory().toString();
			}
		}
		
		return EmailRootPath;
	}
	
	public static String getSaveEmailPath() {
		if(EmailPath == null) {
			EmailPath = getSaveEmailRootPath() + File.separator + "email" + File.separator;
		}
		return EmailPath;
	}
	
	public static void clearEmailPath() {
		EmailRootPath = null;
		EmailPath = null;
	}
	
	public static JsonObject getInboxJson(String uid) {
		File email = new File(getSaveEmailPath() + uid + ".json");
		if(email.exists()) {
			JsonElement file = JsonUtil.parse(email);
			if(file != null && file.isJsonObject()) {
				return file.getAsJsonObject();
			}else {
				return new JsonObject();
			}
		}
		return null;
	}
	
	public static JsonObject toJson(NBTTagCompound nbt) {
		JsonObject obj = new JsonObject();
		for(String nbtKey : nbt.getKeySet()) {
			if(nbtKey.equals("dev")) {
				obj.addProperty("dev", true);
				continue;
			}
			if(nbtKey.equals("custom")) {
				JsonObject customJson = new JsonObject();
				for(String valueKey : nbt.getCompoundTag(nbtKey).getKeySet()) {
					NBTBase value = nbt.getCompoundTag(nbtKey).getTag(valueKey);
					if(value instanceof NBTTagByte) {
						customJson.addProperty(valueKey, ((NBTTagByte)value).getByte() == 1);
					}else if(value instanceof NBTTagInt) {
						customJson.addProperty(valueKey, ((NBTTagInt)value).getInt());
					}else {
						customJson.addProperty(valueKey, value.toString());
					}
				}
				obj.add("custom", customJson);
				continue;
			}
			JsonObject msg = new JsonObject();
			NBTTagCompound msgNBT = nbt.getCompoundTag(nbtKey);
			
			msg.addProperty("time", msgNBT.getString("time"));
			msg.addProperty("sender", msgNBT.getString("sender"));
			msg.addProperty("title", msgNBT.getString("title"));
			if(msgNBT.hasKey("accept")) {
				msg.addProperty("accept", true);
			}
			if(msgNBT.hasKey("read")) {
				msg.addProperty("read", true);
			}
			
			if(msgNBT.hasKey("msgs")) {
				JsonArray texts = new JsonArray();
				NBTTagList textNBT = msgNBT.getTagList("msgs", 8);
				textNBT.forEach(text->{
					texts.add(((NBTTagString)text).getString());
				});
				msg.add("msgs", texts);
			}
			
			if(msgNBT.hasKey("items")) {
				NBTTagList itemsNBT = msgNBT.getTagList("items", 10);
				JsonObject items = new JsonObject();
				itemsNBT.forEach(nbts->{
					NBTTagCompound itemNBT = (NBTTagCompound)nbts;
					items.add(itemNBT.getString("slot"), JsonToStackUtil.toJson(new ItemStack((NBTTagCompound)itemNBT)));
				});
				msg.add("items", items);
			}
			obj.add(nbtKey, msg);
		}
		
		return obj;
	}
	
	public static NBTTagCompound toNBT(JsonObject json) {
		NBTTagCompound nbt = new NBTTagCompound();
		for(Entry<String, JsonElement> msgs : json.entrySet()) {
			if(msgs.getKey().equals("dev")) {
				nbt.setBoolean("dev", true);
				continue;
			}else if(msgs.getKey().equals("custom")) {
				JsonObject customValue = msgs.getValue().getAsJsonObject();
				NBTTagCompound valueTag = new NBTTagCompound();
				for(Entry<String, JsonElement> values : customValue.entrySet()) {
					JsonPrimitive value = values.getValue().getAsJsonPrimitive();
					if(value.isBoolean()) {
						valueTag.setBoolean(values.getKey(), value.getAsBoolean());
					}else if(value.isNumber()){
						valueTag.setInteger(values.getKey(), value.getAsInt());
					}else {
						valueTag.setString(values.getKey(), value.getAsString());
					}
				}
				nbt.setTag("custom", valueTag);
				continue;
			}else if(msgs.getValue().isJsonObject()) {
				NBTTagCompound field = new NBTTagCompound();
				JsonObject msg = msgs.getValue().getAsJsonObject();
				
				field.setString("time", msg.get("time").getAsString());
				field.setString("sender", msg.get("sender").getAsString());
				field.setString("title", msg.get("title").getAsString());
				if(msg.has("accept")) {
					field.setByte("accept", (byte) 0);
				}
				if(msg.has("read")) {
					field.setByte("read", (byte) 0);
				}
				
				if(msg.has("msgs")) {
					NBTTagList texts = new NBTTagList();
					JsonArray textArray = msg.get("msgs").getAsJsonArray();
					
					for(int i = 0; i < textArray.size(); i++) {
						texts.appendTag(new NBTTagString(textArray.get(i).getAsString()));
					}
					field.setTag("msgs", texts);
				}
				
				if(msg.has("items")) {
					NBTTagList items = new NBTTagList();
					JsonObject itemObject = msg.get("items").getAsJsonObject();
					for(Entry<String, JsonElement> item : itemObject.entrySet()) {
						ItemStack stack = JsonToStackUtil.toStack(item.getValue());
						if(stack==null) continue;
						NBTTagCompound itemNBT = stack.writeToNBT(new NBTTagCompound()); 
						itemNBT.setString("slot", item.getKey());
						items.appendTag(itemNBT);
					}
					field.setTag("items", items);
				}
				nbt.setTag(msgs.getKey(), field);
			}
		}
		return nbt;
	}
	
	public static boolean equalsStack(ItemStack stackA, ItemStack stackB, boolean checkDamage, boolean checkAmout, boolean checkNBT) {
		if(stackA == null || stackB == null) {
			return false;
		}
		if(stackA.isEmpty() || stackB.isEmpty()) {
			return false;
		}
		if(stackA == stackB) {
			return true;
		}else {
			if(stackA.getItem() != stackB.getItem()) {
				return false;
			}
			if(checkDamage) {
				if(stackA.getMetadata() != stackB.getMetadata()) {
					return false;
				}
			}
			if(checkAmout) {
				if(stackA.getCount() != stackB.getCount()) {
					return false;
				}
			}
			if(checkNBT) {
				if(stackA.getTagCompound() != null && stackB.getTagCompound() != null) {
					if(!stackA.getTagCompound().equals(stackB.getTagCompound())) {
						return false;
					}
				}else {
					return false;
				}
			}
		}
		return true;
	}
	
	public static void spawnAsEntity(EntityPlayer player, List<ItemStack> stacks) {
		if(stacks == null || stacks.size() == 0) return;
		World worldIn = player.getEntityWorld();
		BlockPos pos = player.getPosition();
		if (!worldIn.isRemote && !worldIn.restoringBlockSnapshots) {
			for(ItemStack stack : stacks) {
				if(stack!=null) {
					EntityItem item = new EntityItem(worldIn, pos.getX()+0.5F, pos.getY()+0.5F, pos.getZ()+0.5F, stack.copy());
					item.motionX = 0;
					item.motionY = 0;
					item.motionZ = 0;
					item.setNoPickupDelay();
					item.setNoDespawn();
					worldIn.spawnEntity(item);
				}
			}
        }
	}

	public static void spawnAsEntity(EntityPlayer player, ItemStackHandler handler) {
		if(handler==null || handler.getSlots()==0) return;
		World world = player.getEntityWorld();
		Vec3d pos = player.getPositionVector();
		if (!world.isRemote && !world.restoringBlockSnapshots) {
			for(int i = 0; i < handler.getSlots(); i++) {
				ItemStack stack = handler.getStackInSlot(i);
				if(!stack.isEmpty()){
					EntityItem item = new EntityItem(world, pos.x, pos.y, pos.z, stack);
					item.motionX = 0;
					item.motionY = 0;
					item.motionZ = 0;
					item.setNoPickupDelay();
					item.setNoDespawn();
					world.spawnEntity(item);
				}
			}
		}
	}
	
	public static long getCoolingTicks() {
		EmailConfigs.Send.Cooling cooling = EmailConfigs.Send.cooling;
		return parseTick(cooling.Day, cooling.Hour, cooling.Minute, cooling.Second, cooling.Tick);
	}
	
	public static long parseTick(long s) {
		return parseTick(s, 0);
	}
	public static long parseTick(long s, long tick) {
		return parseTick(0, s, tick);
	}
	public static long parseTick(long m, long s, long tick) {
		return parseTick(0, m, s, tick);
	}
	public static long parseTick(long h, long m, long s, long tick) {
		return parseTick(0, h, m, s, tick);
	}
	public static long parseTick(long day, long h, long m, long s, long tick) {
		return (((((((day*24)+h)*60)+m)*60)+s)*20)+tick;
	}

	public static boolean isInfiniteSize() {
		if(EmailMain.proxy.isClient()) {
			return EmailConfigs.Enable_MailBox_Infinite_Storage_Cache && Minecraft.getMinecraft().isSingleplayer();
		}
		return false;
	}
}
