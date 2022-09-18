package cat.jiu.email.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import java.util.UUID;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cat.jiu.email.EmailMain;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
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
	
	public static ITextComponent createTextComponent(String arg, Object... objs) {
		return new TextComponentTranslation(arg, objs);
	}
	
	public static ITextComponent createTextComponent(String arg, TextFormatting color, Object... objs) {
		ITextComponent text = new TextComponentTranslation(arg, objs);
		return text.setStyle(text.getStyle().setColor(color)); 
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends JsonElement> T copyJson(T json) {
		if(json==null) return null;
		
		try {
			Method method = json.getClass().getDeclaredMethod("deepCopy");
			method.setAccessible(true);
			return (T) method.invoke(json);
		}catch(NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e0) {
			e0.printStackTrace();
			throw new RuntimeException(e0);
		}
	}
	
	public static long getEmailSize(JsonObject email) {
		if(email == null) {
			return 0;
		}
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeCompoundTag(EmailUtils.toNBT(email));
		
		int i = pb.readerIndex();
        byte b0 = pb.readByte();
        
        if(b0 == 0) {
        	return 0;
        }
        
        pb.readerIndex(i);
        
        NBTSizeTracker tracker = new NBTSizeTracker(0) {
        	public void read(long bits) {
        		this.read += bits / 8L;
        	}
        };
        
        try {
			CompressedStreamTools.read(new ByteBufInputStream(pb), tracker);
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return tracker.read;
	}
	
	public static EmailSizeReport checkEmailSize(JsonObject email) {
		if(email == null || email.entrySet().isEmpty()) {
			return EmailSizeReport.SUCCES;
		}
		for(Entry<String, JsonElement> msgs : email.entrySet()) {
			if(msgs.getValue().isJsonObject()) {
				String msgID = msgs.getKey();
				JsonObject msg = msgs.getValue().getAsJsonObject();
				if(msg.has("items")) {
					JsonObject items = msg.get("items").getAsJsonObject();
					for(Entry<String, JsonElement> slot : items.entrySet()) {
						long size = getNBTSize(slot.getValue());
						if(size >= 2097152L) {
							return new EmailSizeReport(Integer.parseInt(msgID), Integer.parseInt(slot.getKey()), size);
						}
					}
				}
			}
		}
		{
			long size = getEmailSize(email);
			if(size >= 2097152L) {
				return new EmailSizeReport(-1, -1, size);
			}
		}
		return EmailSizeReport.SUCCES;
	}
	
	private static long getNBTSize(JsonElement item) {
		ItemStack stack = JsonToStackUtil.toStack(item);
		if(stack != null && stack.hasTagCompound()) {
			PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
			pb.writeCompoundTag(stack.getTagCompound());
			
			int i = pb.readerIndex();
	        byte b0 = pb.readByte();
	        
	        if(b0 == 0) {
	        	return 0;
	        }
	        
	        pb.readerIndex(i);
	        
	        NBTSizeTracker tracker = new NBTSizeTracker(0) {
	        	public void read(long bits) {
	        		this.read += bits / 8L;
	        	}
	        };
	        
	        try {
				CompressedStreamTools.read(new ByteBufInputStream(pb), tracker);
			}catch(IOException e) {
				e.printStackTrace();
			}
			
			return tracker.read;
		}
		return -1;
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
			return NameToUUID.get(name);
		}
		return null;
	}

	public static String getName(UUID uid) {
		if(hasNameOrUUID(uid)) {
			return UUIDToName.get(uid);
		}
		return null;
	}
	
	public static boolean toJsonFile(String id, JsonObject json) {
		JsonObject email = new JsonObject();
		int index = 0;
		for(Entry<String, JsonElement> msgs : json.entrySet()) {
			if(msgs.getKey().equals("dev")) {
				email.addProperty("dev", true);
				continue;
			}
			email.add(Integer.toString(index), msgs.getValue());
			index++; 
		}
		return JsonUtil.toJsonFile(getSaveEmailPath() + id + ".json", email, false);
	}
	
	public static boolean deleteEmail(String uid, int msgID) {
		JsonObject email = getEmail(uid);
		if(email != null && email.has(Integer.toString(msgID))) {
			email.remove(Integer.toString(msgID));
		}else {
			return false;
		}
		return toJsonFile(uid, email);
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
	
	public static void resetPath() {
		EmailRootPath = null;
		EmailPath = null;
	}
	
	public static JsonObject getEmail(String uid) {
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
