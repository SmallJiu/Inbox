package cat.jiu.email.util;

import java.util.Date;
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
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.EmailFunction;
import cat.jiu.email.element.InboxSound;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.element.InboxText;
import cat.jiu.email.element.InboxTime;
import cat.jiu.email.iface.IInboxText;
import cat.jiu.sql.SQLDatabase;
import cat.jiu.sql.SQLOperator;
import cat.jiu.sql.SQLSelect;
import cat.jiu.sql.SQLSelectType;
import cat.jiu.sql.SQLTableKey;
import cat.jiu.sql.SQLValues;
import cat.jiu.sql.select.Where;
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
	public static final JsonParser parser = new JsonParser();
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	public static String getTime() {
		return dateFormat.format(new Date());
	}
	
	public static void sendMessage(EntityPlayer player, TextFormatting color, String key, Object... args) {
		player.sendMessage(createTextComponent(color, key, args));
	}
	public static void sendMessage(EntityPlayer player, String key, Object... args) {
		player.sendMessage(createTextComponent(key, args));
	}
	
	public static boolean saveInboxToDisk(Inbox inbox) {
		return saveInboxToDisk(inbox, 10);
	}
	public static boolean saveInboxToDisk(Inbox inbox, int maxRetryCount) {
		if(EmailMain.proxy.isClient()
		&& !Minecraft.getMinecraft().isIntegratedServerRunning()) {
			EmailMain.log.error("Client can not save inbox to Server!");
			return false;
		}
		int retry = 0;
		boolean succes = false;
		for(; retry < maxRetryCount; retry++) {
			if(inbox.saveToDisk()) {
				succes = true;
				break;
			}
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
		File functionFile = findFunctionFile(new File(EmailAPI.getTypePath()), file);
		if(functionFile != null) {
			JsonElement e = JsonUtil.parse(functionFile);
			if(e != null && e.isJsonObject()) {
				JsonObject function = e.getAsJsonObject();
				List<IInboxText> msgs = null;
				if(function.has("msgs")) {
					msgs = Lists.newArrayList();
					JsonElement msgE = function.get("msgs");
					if(msgE.isJsonPrimitive()) {
						msgs.add(new InboxText(msgE.getAsString()));
					}else if(msgE.isJsonArray()) {
						JsonArray msgsArray = msgE.getAsJsonArray();
						for(int i = 0; i < msgsArray.size(); i++) {
							msgs.add(new InboxText(msgsArray.get(i).getAsString()));
						}
					}else if(msgE.isJsonObject()) {
						for(Entry<String, JsonElement> msg : msgE.getAsJsonObject().entrySet()) {
							JsonArray argJson = msg.getValue().getAsJsonArray();
							Object[] arg = new Object[argJson.size()];
							for(int i = 0; i < arg.length; i++) {
								arg[i] = argJson.get(i).getAsString();
							}
							msgs.add(new InboxText(msg.getKey(), arg));
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
							JsonElement itemsE = JsonUtil.parse(EmailAPI.getExportPath() + path);
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
									JsonElement itemsE = JsonUtil.parse(EmailAPI.getExportPath() + item.getValue().getAsJsonPrimitive().getAsString());
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
									JsonElement itemsE = JsonUtil.parse(EmailAPI.getExportPath() + item.getAsJsonPrimitive().getAsString());
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
				
				InboxSound sound = null;
				if(function.has("sound")) {
					JsonObject jsonSound = function.getAsJsonObject("sound");
					sound = new InboxSound(
							new InboxTime(jsonSound.get("millis").getAsLong()), 
							SoundEvent.REGISTRY.getObject(new ResourceLocation(jsonSound.get("name").getAsString())),
							jsonSound.get("volume").getAsFloat(),
							jsonSound.get("pitch").getAsFloat());
				}
				
				return new EmailFunction(
						new InboxText(function.get("sender")),
						function.get("addresser").getAsString(),
						new InboxText(function.get("title")),
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
	
	public static ITextComponent createTextComponent(TextFormatting color, String arg, Object... objs) {
		ITextComponent text = new TextComponentTranslation(arg, objs);
		return text.setStyle(text.getStyle().setColor(color)); 
	}
	
	public static SizeReport checkEmailSize(Email email) {
		if(email == null) return SizeReport.SUCCES;
		if(email.hasItems()) {
			List<ItemStack> items = email.getItems();
			for(int slot = 0; slot < items.size(); slot++) {
				long size = getSize(items.get(slot).writeToNBT(new NBTTagCompound()));
				if(size >= 2097152L) {
					return new SizeReport(-1, slot, size);
				}
			}
		}
		long size = getSize(email.writeTo(NBTTagCompound.class));
		return size >= 2097152L ? new SizeReport(-1, -1, size) : SizeReport.SUCCES;
	}
	
	public static SizeReport checkInboxSize(Inbox inbox) {
		if(inbox == null || inbox.isEmptyInbox()) return SizeReport.SUCCES;
		
		for(long id : inbox.getEmailIDs()) {
			Email email = inbox.getEmail(id);
			if(email == null) {
				inbox.deleteEmail(id);
				continue;
			}
			long emailSize = getSize(email.writeTo(NBTTagCompound.class));
			if(emailSize >= 2097152L) {
				return new SizeReport(id, -1, emailSize);
			}
			
			if(email.hasItems()) {
				List<ItemStack> items = email.getItems();
				for(int slot = 0; slot < items.size(); slot++) {
					long size = getSize(items.get(slot).writeToNBT(new NBTTagCompound()));
					if(size >= 2097152L) {
						return new SizeReport(id, slot, size);
					}
				}
			}
		}
		
		long size = inbox.getInboxSize();
		return size >= 2097152L ? new SizeReport(-1, -1, size) : SizeReport.SUCCES;
	}
	
	public static long getSize(NBTTagCompound nbt) {
		if(nbt == null) return 0;
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeCompoundTag(nbt);
		
		int index = pb.readerIndex();
        if(pb.readByte() == 0) return 0;
        pb.readerIndex(index);
        
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
	
	public static boolean saveInboxToDB(Inbox inbox) {
		if(EmailMain.SQLite_INIT) {
			SQLDatabase db = null;
			try {
				synchronized(EmailUtils.class) {
					db = new SQLDatabase(org.sqlite.JDBC.PREFIX + EmailAPI.getSaveEmailRootPath() + File.separator + "inbox.db");
					db.prepared.createTable("inboxs", new SQLTableKey()
							.put("uuid", db.createKey(JDBCType.VARCHAR)
									.setNotNull(true)
									.setPrimaryKey(true))
							.put("inbox", db.createKey(JDBCType.VARCHAR)
									.setNotNull(true)));
					
					db.prepared.delete("inboxs", new SQLSelect(SQLSelectType.WHERE, inbox.getOwner())
							.add(new Where("uuid", SQLOperator.EQUAL, "?", null)));
					
					db.prepared.insert("inboxs", inbox.writeTo(SQLValues.class));
					try {
						db.close();
					}catch(SQLException e) {}
				}
				return true;
			}catch(Exception e) {
				EmailMain.log.error("{}", e.getMessage());
				return false;
			}finally {
				if(db!=null) {
					try {
						db.close();
					}catch(SQLException e1) {}
				}
			}
		}
		return false;
	}
	
	public static JsonObject getInboxJson(String uid) {
		if(EmailConfigs.Save_Inbox_To_SQL) {
			if(EmailMain.SQLite_INIT) {
				SQLDatabase db = null;
				try {
					synchronized(EmailUtils.class) {
						db = new SQLDatabase(org.sqlite.JDBC.PREFIX + EmailAPI.getSaveEmailRootPath() + File.separator + "inbox.db"); 
						ResultSet rs = db.prepared.select("inboxs", new SQLSelect(SQLSelectType.WHERE, uid)
								.add(new Where("uuid", SQLOperator.EQUAL, "?", null)));
						JsonObject json = null;
						while(rs.next()) {
							String str = rs.getString("inbox");
							if(str.isEmpty()) {
								json = new JsonObject();
							}else {
								json = JsonUtil.parser.parse(str).getAsJsonObject();
							}
							break;
						}
						try {
							db.close();
						}catch(SQLException e) {}
						return json;
					}
				}catch(Exception e) {
					EmailMain.log.warn("found inbox data has error: {}", e.getLocalizedMessage());
				}finally {
					if(db!=null) {
						try {
							db.close();
						}catch(SQLException e1) {}
					}
				}
			}
		}else {
			File email = new File(EmailAPI.getSaveInboxPath() + uid + ".json");
			if(email.exists()) {
				JsonElement file = JsonUtil.parse(email);
				if(file != null && file.isJsonObject()) {
					return file.getAsJsonObject();
				}else {
					return new JsonObject();
				}
			}
		}
		return new JsonObject();
	}
	
	private static final HashMap<String, UUID> NameToUUID = Maps.newHashMap();
	private static final HashMap<UUID, String> UUIDToName = Maps.newHashMap();

	public static void initNameAndUUID(@Nullable MinecraftServer server) {
		if(EmailMain.proxy.isClient()
		&& !Minecraft.getMinecraft().isIntegratedServerRunning()) {
			return;
		}
		if(server != null) {
			server.getPlayerProfileCache().save();
			server.getPlayerProfileCache().load();
		}
		File file = new File("./usercache.json");
		if(file.exists()) {
			NameToUUID.clear();
			UUIDToName.clear();
			try(FileInputStream in = new FileInputStream(file)) {
				JsonArray array = parser.parse(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonArray();
				for(int i = 0; i < array.size(); i++) {
					JsonObject player = array.get(i).getAsJsonObject();
					
					String name = player.get("name").getAsString();
					UUID uid = UUID.fromString(player.get("uuid").getAsString());
					NameToUUID.put(name, uid);
					UUIDToName.put(uid, name);
				}
			}catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static boolean hasName(String name) {
		return NameToUUID.containsKey(name) && UUIDToName.containsValue(name);
	}

	public static boolean hasUUID(UUID uid) {
		return UUIDToName.containsKey(uid) && NameToUUID.containsValue(uid);
	}

	public static UUID getUUID(String name) {
		if(hasName(name)) {
			UUID uid = NameToUUID.get(name);
			if(uid==null) {
				for(Entry<UUID, String> uuid : UUIDToName.entrySet()) {
					if(uuid.getValue().equals(name)) {
						uid = uuid.getKey();
						NameToUUID.put(name, uid);
						break;
					}
				}
			}
			return uid;
		}
		return null;
	}

	public static String getName(UUID uid) {
		if(hasUUID(uid)) {
			String name = UUIDToName.get(uid);
			if(name==null) {
				for(Entry<String, UUID> names : NameToUUID.entrySet()) {
					if(names.getValue().equals(uid)) {
						name = names.getKey();
						UUIDToName.put(uid, name);
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
	
	public static long getCoolingMillis() {
		EmailConfigs.Send.Cooling cooling = EmailConfigs.Send.cooling;
		return parseTick(cooling.Day, cooling.Hour, cooling.Minute, cooling.Second, cooling.Tick) * 50 + cooling.Millis;
	}
	
	public static long parseTick(long day, long h, long m, long s, long tick) {
		return (((((((day*24)+h)*60)+m)*60)+s)*20)+tick;
	}
	public static long parseMillis(long day, long h, long m, long s, long tick, long ms) {
		return parseMillis(day, h, m, s, tick*50 + ms);
	}
	public static long parseMillis(long day, long h, long m, long s, long ms) {
		return (((((((day*24)+h)*60)+m)*60)+s)*1000)+ms;
	}

	@Deprecated
	public static boolean isInfiniteSize() {
		return EmailConfigs.isInfiniteSize();
	}
}
