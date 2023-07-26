package cat.jiu.email.util;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.Nullable;

import java.util.UUID;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicLong;

import cat.jiu.email.EmailAPI;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Sound;
import cat.jiu.core.util.element.Text;
import cat.jiu.core.util.timer.Timer;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.EmailFunction;
import cat.jiu.email.element.Inbox;

import com.mojang.blaze3d.matrix.MatrixStack;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemStackHandler;

public class EmailUtils {
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	public static String getTime() {
		return dateFormat.format(new Date());
	}


	@OnlyIn(Dist.CLIENT)
	public static boolean isKeyDown(int key) {
		return InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), key);
	}

	public static String formatTimestamp(long time) {
		StringJoiner sj = new StringJoiner(":");
		long t = time / 50;
		long s = t / 20;
		t %= 20;
		long m = s / 60;
		s %= 60;
		long h = m / 60;
		m %= 60;
		long d = h / 24;
		h %= 24;
		
		sj.add(ITimer.format(d, 10));
		sj.add(ITimer.format(h, 10));
		sj.add(ITimer.format(m, 10));
		sj.add(ITimer.format(s, 10));
		sj.add(ITimer.format(t, 10));
		
		return sj.toString();
	}
	
	public static boolean isOP(PlayerEntity player) {
		boolean isOP;
		if(player.getServer().isDedicatedServer()) {
			isOP = player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null;
		}else {
			isOP = Minecraft.getInstance().getIntegratedServer().getPublic() && Minecraft.getInstance().player.hasPermissionLevel(1);
		}
		return isOP;
	}
	
	public static void sendMessage(PlayerEntity player, TextFormatting color, String key, Object... args) {
		player.sendMessage(new Text(key, formatArgsTextToComponent(args)).toTextComponent(color), player.getUniqueID());
	}
	
	public static void sendMessage(PlayerEntity player, String key, Object... args) {
		player.sendMessage(new Text(key, formatArgsTextToComponent(args)).toTextComponent(), player.getUniqueID());
	}

	public static Object[] formatArgsTextToComponent(Object... args) {
		for (int i = 0; i < args.length; i++) {
			if(args[i] instanceof IText){
				args[i] = ((IText)args[i]).toTextComponent();
			}
		}
		return args;
	}
	
	public static boolean saveInboxToDisk(Inbox inbox) {
		return saveInboxToDisk(inbox, 10);
	}
	public static boolean saveInboxToDisk(Inbox inbox, int maxRetryCount) {
		if(EmailMain.proxy.isClient()
		&& !Minecraft.getInstance().isIntegratedServerRunning()) {
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
	
	public static void getAllFiles(List<File> files, File dir) {
		for(File file : dir.listFiles()) {
			if(file.isDirectory()) {
				getAllFiles(files, file);
			}else {
				files.add(file);
			}
		}
	}

	@Deprecated
	public static EmailFunction findFunction(String file) {
		/*
		File functionFile = findFunctionFile(new File(EmailAPI.getTypePath()), file);
		if(functionFile != null) {
			JsonElement e = JsonParser.parse(functionFile);
			if(e != null && e.isJsonObject()) {
				JsonObject function = e.getAsJsonObject();
				List<IText> msgs = null;
				if(function.has("msgs")) {
					msgs = Lists.newArrayList();
					JsonElement msgE = function.get("msgs");
					if(msgE.isJsonPrimitive()) {
						msgs.add(new Text(msgE.getAsString()));
					}else if(msgE.isJsonArray()) {
						JsonArray msgsArray = msgE.getAsJsonArray();
						for(int i = 0; i < msgsArray.size(); i++) {
							msgs.add(new Text(msgsArray.get(i).getAsString()));
						}
					}else if(msgE.isJsonObject()) {
						for(Entry<String, JsonElement> msg : msgE.getAsJsonObject().entrySet()) {
							JsonArray argJson = msg.getValue().getAsJsonArray();
							Object[] arg = new Object[argJson.size()];
							for(int i = 0; i < arg.length; i++) {
								arg[i] = argJson.get(i).getAsString();
							}
							msgs.add(new Text(msg.getKey(), arg));
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
							JsonElement itemsE = JsonParser.parse(EmailAPI.getExportPath() + path);
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
									JsonElement itemsE = JsonParser.parse(EmailAPI.getExportPath() + item.getValue().getAsJsonPrimitive().getAsString());
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
									JsonElement itemsE = JsonParser.parse(EmailAPI.getExportPath() + item.getAsJsonPrimitive().getAsString());
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
				
				Sound sound = null;
				if(function.has("sound")) {
					JsonObject jsonSound = function.getAsJsonObject("sound");
					sound = new Sound(
								new Timer(jsonSound.get("millis").getAsLong()), 
								Registry.SOUND_EVENT.get(new ResourceLocation(jsonSound.get("name").getAsString())),
								jsonSound.get("volume").getAsFloat(),
								jsonSound.get("pitch").getAsFloat(), SoundCategory.PLAYERS
							);
				}
				
				return new EmailFunction(
						new Text(function.getAsJsonObject("sender")),
						function.get("addresser").getAsString(),
						new Text(function.getAsJsonObject("title")),
						items, msgs, sound);
			}
		}
		 */
		
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
	
	public static TranslationTextComponent createTextComponent(String arg, Object... objs) {
		return new TranslationTextComponent(arg, objs);
	}
	
	public static TranslationTextComponent createTextComponent(TextFormatting color, String arg, Object... objs) {
		TranslationTextComponent text = new TranslationTextComponent(arg, objs);
		return (TranslationTextComponent) text.setStyle(text.getStyle().setColor(Color.fromTextFormatting(color)));
	}
	
	public static SizeReport checkEmailSize(Email email) {
		if(email == null) return SizeReport.SUCCES;
		if(email.hasItems()) {
			List<ItemStack> items = email.getItems();
			for(int slot = 0; slot < items.size(); slot++) {
				long size = getSize(items.get(slot).serializeNBT());
				if(size >= 2097152L) {
					return new SizeReport(-1, slot, size);
				}
			}
		}
		long size = getSize(email.writeTo(CompoundNBT.class));
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
			long emailSize = getSize(email.writeTo(CompoundNBT.class));
			if(emailSize >= 2097152L) {
				return new SizeReport(id, -1, emailSize);
			}
			
			if(email.hasItems()) {
				List<ItemStack> items = email.getItems();
				for(int slot = 0; slot < items.size(); slot++) {
					long size = getSize(items.get(slot).serializeNBT());
					if(size >= 2097152L) {
						return new SizeReport(id, slot, size);
					}
				}
			}
		}
		
		long size = inbox.getInboxSize();
		return size >= 2097152L ? new SizeReport(-1, -1, size) : SizeReport.SUCCES;
	}
	
	public static long getSize(CompoundNBT nbt) {
		if(nbt == null) return 0;
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		pb.writeCompoundTag(nbt);

		NBTSizeTracker tracker = new NBTSizeTracker(0) {
			@Override
			public void read(long bits) {
				this.read += bits / 8L;
			}
		};

		pb.func_244272_a(tracker);
		return tracker.read;
	}
	
	public static boolean saveInboxToDB(Inbox inbox) {
		/*
		if(EmailMain.SQLite_INIT) {
			SQLDatabase db = null;
			try {
				synchronized(Inbox.class) {
					db = new SQLDatabase(org.sqlite.JDBC.PREFIX + EmailAPI.getSaveEmailRootPath() + File.separator + "inbox.db");
					db.prepared.createTable("inboxs", new SQLTableKey()
							.put("uuid", db.createKey(JDBCType.VARCHAR)
									.setNotNull(true)
									.setPrimaryKey(true))
							.put("inbox", db.createKey(JDBCType.VARCHAR)
									.setNotNull(true)));
					
					db.prepared.delete("inboxs", new SQLSelect(SQLSelectType.WHERE, "'" + inbox.getOwner() + "'")
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
		 */
		return false;
	}
	
	public static JsonObject getInboxJson(String uid) {
		/*
		if(EmailConfigs.Save_Inbox_To_SQL && EmailMain.SQLite_INIT) {
			SQLDatabase db = null;
			try {
				synchronized(Email.class) {
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
						rs.close();
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
		}else { */
			File email = new File(EmailAPI.getSaveInboxPath() + uid + ".json");
			if(email.exists()) {
				JsonElement file = JsonParser.parse(email);
				if(file != null && file.isJsonObject()) {
					return file.getAsJsonObject();
				}else {
					return new JsonObject();
				}
			}
//		}

		return new JsonObject();
	}
	
	private static final HashMap<String, UUID> NameToUUID = Maps.newHashMap();
	private static final HashMap<UUID, String> UUIDToName = Maps.newHashMap();

	public static void initNameAndUUID(@Nullable MinecraftServer server) {
		if(EmailMain.proxy.isClient()
		&& !Minecraft.getInstance().isIntegratedServerRunning()) {
			return;
		}
		if(server != null) {
			server.getPlayerProfileCache().save();
		}
		if(MinecraftServer.USER_CACHE_FILE.exists()) {
			try(FileInputStream in = new FileInputStream(MinecraftServer.USER_CACHE_FILE)) {
				JsonArray array = JsonParser.parse(in);
				NameToUUID.clear();
				UUIDToName.clear();
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
				if(stackA.getDamage() != stackB.getDamage()) {
					return false;
				}
			}
			if(checkAmout) {
				if(stackA.getCount() != stackB.getCount()) {
					return false;
				}
			}
			if(checkNBT) {
				if(stackA.getTag() != null && stackB.getTag() != null) {
					if(!stackA.getTag().equals(stackB.getTag())) {
						return false;
					}
				}else {
					return false;
				}
			}
		}
		return true;
	}
	
	public static void spawnAsEntity(PlayerEntity player, List<ItemStack> stacks) {
		if(stacks == null || stacks.size() == 0) return;
		World worldIn = player.getEntityWorld();
		if (!worldIn.isRemote() && !worldIn.restoringBlockSnapshots) {
			for(ItemStack stack : stacks) {
				spawnAsEntity(worldIn, player.getEyePosition(0), stack);
			}
        }
	}

	public static void spawnAsEntity(PlayerEntity player, ItemStackHandler handler) {
		if(handler==null || handler.getSlots()==0) return;
		World world = player.getEntityWorld();
		if (!world.isRemote() && !world.restoringBlockSnapshots) {
			for(int i = 0; i < handler.getSlots(); i++) {
				spawnAsEntity(world, player.getEyePosition(0), handler.getStackInSlot(i));
			}
		}
	}
	public static void spawnAsEntity(World world, Vector3d pos, ItemStack stack){
		if(!stack.isEmpty()){
			ItemEntity item = new ItemEntity(world, pos.x+0.5F, pos.y+0.5F, pos.z+0.5F, stack.copy());
			item.prevPosX = 0;
			item.prevPosY = 0;
			item.prevPosZ = 0;
			item.setNoPickupDelay();
			item.setNoDespawn();
			world.addEntity(item);
		}
	}
	
	public static long getCoolingMillis() {
		EmailConfigs.Send.Cooling cooling = EmailConfigs.Send.cooling;
		return parseTick(cooling.Day.get(), cooling.Hour.get(), cooling.Minute.get(), cooling.Second.get(), cooling.Tick.get()) * 50 + cooling.Millis.get();
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

    public static List<String> splitString(String text, int textMaxLength) {
        FontRenderer fr = Minecraft.getInstance().fontRenderer;
        List<String> texts = Lists.newArrayList();
        if(fr.getStringWidth(text) >= textMaxLength) {
            StringBuilder s = new StringBuilder();
            for(int i = 0; i < text.length(); i++) {
                String str = s.toString();
                if(fr.getStringWidth(str) >= textMaxLength) {
                    texts.add(str);
                    s.setLength(0);
                }
                s.append(text.charAt(i));
            }
            if(s.length() > 0) {
                texts.add(s.toString());
            }
        }else {
            texts.add(text);
        }
        return texts;
    }

    public static boolean isInRange(double mouseX, double mouseY, int x, int y, int width, int height) {
        int maxX = x + width;
        int maxY = y + height;
        return (mouseX >= x && mouseY >= y) && (mouseX <= maxX && mouseY <= maxY);
    }

    public static void drawAlignRightString(MatrixStack matrix, String text, int x, int y, int color, boolean drawShadow) {
        FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
        for(int i = text.length(); i > 0; i--) {
            if('§' == text.charAt(i-1)) {
                continue;
            }
            if(i-2>=0 && '§' == text.charAt(i-2)) {
                continue;
            }

            String c = String.valueOf(text.charAt(i-1));

            float width = fontRenderer.getStringWidth(c);

            if(i-2 > 0) {
                boolean isColor;
                String s = text.charAt(i-3)+""+text.charAt(i-2);
                for(TextFormatting format : TextFormatting.values()) {
                    isColor = format.toString().equals(s);
                    if(isColor) {
                        c = s + c;
                        width = fontRenderer.getStringWidth(c);
                        break;
                    }
                }
            }

            x -= width;
            if(drawShadow){
                fontRenderer.drawStringWithShadow(matrix, c, x, y, color);
            }else {
                fontRenderer.drawString(matrix, c, x, y, color);
            }
        }
    }
}
