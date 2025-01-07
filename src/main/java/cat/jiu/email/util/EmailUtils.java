package cat.jiu.email.util;

import java.sql.JDBCType;
import java.sql.ResultSet;
import java.util.*;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import cat.jiu.core.util.SideProxy;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailAPI;
import cat.jiu.sql.*;
import cat.jiu.sql.select.Where;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;

import com.mojang.blaze3d.matrix.MatrixStack;
import io.netty.buffer.Unpooled;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;

import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemStackHandler;

public class EmailUtils {
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	public static final SimpleDateFormat dateFormat_1 = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	public static String getTime() {
		return dateFormat.format(new Date());
	}

	public static ServerPlayerEntity getPlayer(MinecraftServer server, String name){
		EmailUtils.initNameAndUUID(server);
		ServerPlayerEntity player = server.getPlayerList().getPlayerByUsername(name);
		if (player == null) {
			try {
				player = server.getPlayerList().getPlayerByUUID(EmailUtils.getTrueUUID(name));
			}catch (Exception ignored){}
		}
		return player;
	}

	@OnlyIn(Dist.CLIENT)
	public static boolean isKeyDown(int key) {
		return InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), key);
	}

	public static String formatTimestamp(long ms) {
		long t = ms / 50;
		long s = t / 20;
		t %= 20;
		long m = s / 60;
		s %= 60;
		long h = m / 60;
		m %= 60;
		long d = h / 24;
		h %= 24;

		if (d > 0) {
			return d + " " + I18n.format("inbox.config.time.day");
		}
		if (h > 0) {
			return h + " " + I18n.format("inbox.config.time.hour");
		}
		if (m > 0) {
			return m + " " + I18n.format("inbox.config.time.minute");
		}
		if (s > 0) {
			return s + " " + I18n.format("inbox.config.time.second");
		}
		if (t > 0) {
			return t + " " + I18n.format("inbox.config.time.tick");
		}
		return "";
	}

	public static String getExpirationTime(Email email) {
		if (!email.isExpiration()) {
			return formatTimestamp(email.getExpirationTimeAsTimestamp() - System.currentTimeMillis());
		}
		return I18n.format("inbox.config.expiration.ed");
	}
	
	public static boolean isOP(PlayerEntity player) {
		boolean isOP;
		if(!SideProxy.isClient()) {
			isOP = player.getServer().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null;
		}else {
			if (Minecraft.getInstance().isIntegratedServerRunning()) {
				isOP = Minecraft.getInstance().getIntegratedServer().getPublic() && Minecraft.getInstance().player.hasPermissionLevel(4);
			}else {
				isOP = Minecraft.getInstance().player.hasPermissionLevel(4);
			}
		}
		return isOP;
	}

	
	public static void sendMessage(PlayerEntity player, TextFormatting color, String key, Object... args) {
		player.sendStatusMessage(new Text(key, formatArgsTextToComponent(args)).toTextComponent(color), false);
	}
	
	public static void sendMessage(PlayerEntity player, String key, Object... args) {
		player.sendStatusMessage(new Text(key, formatArgsTextToComponent(args)).toTextComponent(), false);
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
		if(SideProxy.isClient()
		&& !Minecraft.getInstance().isIntegratedServerRunning()) {
			EmailMain.log.error("Client can not save inbox to Server!");
			return false;
		}
		int retry = 0;
		for(; retry < maxRetryCount; retry++) {
			if(inbox.saveToDisk()) {
				return true;
			}
		}
		EmailMain.log.error("Can not save Inbox to Disk! Owner: {}, UUID: {}, Retry count: {}", inbox.getOwner(), inbox.getOwnerAsUUID(), retry);
		return false;
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
	
	public static ITextComponent createTextComponent(String arg, Object... objs) {
		return new TranslationTextComponent(arg, objs);
	}
	
	public static ITextComponent createTextComponent(TextFormatting color, String arg, Object... objs) {
		ITextComponent text = new TranslationTextComponent(arg, objs);
		return text.copyRaw().setStyle(text.getStyle().setFormatting(color));
	}
	
	public static SizeReport checkEmailSize(Email email) {
		if(email == null) return SizeReport.SUCCESS;
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
		return size >= 2097152L ? new SizeReport(-1, -1, size) : SizeReport.SUCCESS;
	}
	
	public static SizeReport checkInboxSize(Inbox inbox) {
		if(inbox == null || inbox.isEmptyInbox()) return SizeReport.SUCCESS;
		
		for(long id : inbox.getEmailIDs()) {
			Email email = inbox.getEmail(id);
			if(email == null) {
				inbox.deleteEmail(id);
				continue;
			}
			long emailSize = getSize(email.writeTo(CompoundNBT.class));
			if(emailSize >= 2097152L) {
				SizeReport report = checkEmailSize(email);
				if(report.slot>=0){
					return new SizeReport(id, report.slot, report.size);
				}else {
					return new SizeReport(id, -1, emailSize);
				}
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
		return size >= 2097152L ? new SizeReport(-1, -1, size) : SizeReport.SUCCESS;
	}
	
	public static long getSize(CompoundNBT nbt) {
		if(nbt == null) return 0;
		PacketBuffer pb = new PacketBuffer(Unpooled.buffer());
		AtomicLong size = new AtomicLong();

		pb.writeCompoundTag(nbt);
		pb.func_244272_a(new NBTSizeTracker(0) {
			@Override
			public void read(long pBytes) {
				size.addAndGet(pBytes / 8L);
			}
		});
		return size.get();
	}

	public static JsonObject getInboxJson(String uid) {
		if(EmailConfigs.Save_Inbox_To_SQL.get() && EmailMain.SQLite_INIT) {
			return DBParser.getInboxJson(DBParser.DB_URL, uid);
		}else {
			File email = new File(EmailAPI.getSaveInboxPath() + uid + ".json");
			if(email.exists()) {
				JsonElement file = JsonParser.parse(email);
				if(file != null && file.isJsonObject()) {
					return file.getAsJsonObject();
				}
			}
		}
		return new JsonObject();
	}
	
	private static final HashMap<String, UUID> NameToUUID = Maps.newHashMap();
	private static final HashMap<UUID, String> UUIDToName = Maps.newHashMap();
	public static final File USERID_CACHE_FILE = MinecraftServer.USER_CACHE_FILE;

	public static void initNameAndUUID(@Nullable MinecraftServer server) {
		if(SideProxy.isClient()
		&& !Minecraft.getInstance().isIntegratedServerRunning()) {
			return;
		}
		if(server != null) {
			server.getPlayerProfileCache().save();
		}

		if(USERID_CACHE_FILE.exists()) {
			try(FileInputStream in = new FileInputStream(USERID_CACHE_FILE)) {
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

	public static UUID getTrueUUID(String id){
		UUID uid = null;
		try{
			UUID uuid = UUID.fromString(id);
			if(EmailUtils.hasUUID(uuid)){
				uid = uuid;
			}
		}catch (Exception e){
			if(EmailUtils.hasName(id)){
				uid = EmailUtils.getUUID(id);
			}
		}
		return uid;
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
					return stackA.getTag().equals(stackB.getTag());
				}else {
					return false;
				}
			}
		}
		return true;
	}
	
	public static void spawnAsEntity(PlayerEntity player, List<ItemStack> stacks) {
		if(stacks == null || stacks.size() == 0) return;
		World world = player.getEntityWorld();
		if (!world.isRemote() && !world.restoringBlockSnapshots) {
			for(ItemStack stack : stacks) {
				spawnAsEntity(world, player.getPosition(), stack);
			}
        }
	}

	public static void spawnAsEntity(PlayerEntity player, ItemStackHandler handler) {
		if(handler==null || handler.getSlots()==0) return;
		World world = player.getEntityWorld();
		if (!world.isRemote() && !world.restoringBlockSnapshots) {
			for(int i = 0; i < handler.getSlots(); i++) {
				spawnAsEntity(world, player.getPosition(), handler.getStackInSlot(i));
			}
		}
	}
	public static void spawnAsEntity(World world, Vector3i pos, ItemStack stack){
		if(!stack.isEmpty()){
			ItemEntity item = new ItemEntity(world, pos.getX() +0.5F, pos.getY() +0.5F, pos.getZ() +0.5F, stack.copy());
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

	@OnlyIn(Dist.CLIENT)
    public static void drawAlignRightString(MatrixStack stack, String text, int x, int y, int color, boolean drawShadow) {
		RenderUtils.drawRightString(stack, text, x, y, color, drawShadow);
    }

	static Map<String, SoundCategory> SOUND_CATEGORIES;
	public static SoundCategory getSoundCategory(String name) {
		if (SOUND_CATEGORIES == null) {
			SOUND_CATEGORIES = Arrays.stream(SoundCategory.values()).collect(Collectors.toMap(SoundCategory::getName, Function.identity()));
		}
		return SOUND_CATEGORIES.get(name);
	}
}
