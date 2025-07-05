package cat.jiu.email.util;

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
import cat.jiu.email.configs.EmailConfigClient;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.email.element.StorageType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;

import com.mojang.blaze3d.platform.InputConstants;
import io.netty.buffer.Unpooled;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import net.minecraft.server.Services;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.ItemStackHandler;

public class EmailUtils {
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	public static final SimpleDateFormat dateFormat_1 = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	public static String getTime() {
		return dateFormat.format(new Date());
	}

	public static ServerPlayer getPlayer(MinecraftServer server, String name){
		EmailUtils.initNameAndUUID(server);
		ServerPlayer player = server.getPlayerList().getPlayerByName(name);
		if (player == null) {
			try {
				player = server.getPlayerList().getPlayer(EmailUtils.getTrueUUID(name));
			}catch (Exception ignored){}
		}
		return player;
	}

	@OnlyIn(Dist.CLIENT)
	public static boolean isKeyDown(int key) {
		return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
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
			return d + " " + I18n.get("inbox.config.time.day");
		}
		if (h > 0) {
			return h + " " + I18n.get("inbox.config.time.hour");
		}
		if (m > 0) {
			return m + " " + I18n.get("inbox.config.time.minute");
		}
		if (s > 0) {
			return s + " " + I18n.get("inbox.config.time.second");
		}
		if (t > 0) {
			return t + " " + I18n.get("inbox.config.time.tick");
		}
		return "";
	}

	public static String getExpirationTime(Email email) {
		if (!email.isExpiration()) {
			return formatTimestamp(email.getExpirationTimeAsTimestamp() - System.currentTimeMillis());
		}
		return I18n.get("inbox.config.expiration.ed");
	}
	
	public static boolean isOP(Player player) {
		boolean isOP;
		if(!SideProxy.isClient()) {
			isOP = player.getServer().getPlayerList().getOps().get(player.getGameProfile()) != null;
		}else {
			if (Minecraft.getInstance().isLocalServer()) {
				isOP = Minecraft.getInstance().getSingleplayerServer().isPublished() && Minecraft.getInstance().player.hasPermissions(4);
			}else {
				isOP = Minecraft.getInstance().player.hasPermissions(4);
			}
		}
		return isOP;
	}

	
	public static void sendMessage(Player player, ChatFormatting color, String key, Object... args) {
		player.sendSystemMessage(new Text(key, formatArgsTextToComponent(args)).toTextComponent(color));
	}
	
	public static void sendMessage(Player player, String key, Object... args) {
		player.sendSystemMessage(new Text(key, formatArgsTextToComponent(args)).toTextComponent());
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
		&& !Minecraft.getInstance().isLocalServer()) {
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

	public static JsonObject getInboxJson(String uid) {
		try {
			StorageType type = StorageType.getInstance();
			if (type==null) {
				return new JsonObject();
			}
			return type.read.apply(uid);
		} catch (Exception e) {
			EmailMain.log.error(e);
		}
		return new JsonObject();
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
	
	public static Component createTextComponent(String arg, Object... objs) {
		return Component.translatable(arg, objs);
	}
	
	public static Component createTextComponent(ChatFormatting color, String arg, Object... objs) {
		Component text = Component.translatable(arg, objs);
		return text.copy().setStyle(text.getStyle().withColor(color));
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
		long size = getSize(email.writeTo(CompoundTag.class));
		return size >= 2097152L ? new SizeReport(-1, -1, size) : SizeReport.SUCCESS;
	}

	@Deprecated
	public static SizeReport checkInboxSize(Inbox inbox) {
		if(inbox == null || inbox.isEmptyInbox()) return SizeReport.SUCCESS;
		
		for(long id : inbox.getEmailIDs()) {
			Email email = inbox.getEmail(id);
			if(email == null) {
				inbox.deleteEmail(id);
				continue;
			}
			long emailSize = getSize(email.writeTo(CompoundTag.class));
			if(emailSize >= 2097152L) {
				SizeReport report = checkEmailSize(email);
				if(report.slot()>=0){
					return new SizeReport(id, report.slot(), report.size());
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
	
	public static long getSize(CompoundTag nbt) {
		if(nbt == null) return 0;
		FriendlyByteBuf pb = new FriendlyByteBuf(Unpooled.buffer());
		AtomicLong size = new AtomicLong();

		pb.writeNbt(nbt);
		pb.readNbt(new NbtAccounter(0) {
			@Override
			public void accountBytes(long pBytes) {
				size.addAndGet(pBytes);
			}
		});
		return size.get();
	}
	
	private static final HashMap<String, UUID> NameToUUID = Maps.newHashMap();
	private static final HashMap<UUID, String> UUIDToName = Maps.newHashMap();
	public static final File USERID_CACHE_FILE = new File("./", Services.USERID_CACHE_FILE);

	public static void initNameAndUUID(@Nullable MinecraftServer server) {
		if(SideProxy.isClient()
		&& !Minecraft.getInstance().isLocalServer()) {
			return;
		}
		if(server != null) {
			server.getProfileCache().save();
			server.getProfileCache().load();
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
				if(stackA.getDamageValue() != stackB.getDamageValue()) {
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
	
	public static void spawnAsEntity(Player player, List<ItemStack> stacks) {
		if(stacks == null || stacks.size() == 0) return;
		Level world = player.level();
		if (!world.isClientSide() && !world.restoringBlockSnapshots) {
			for(ItemStack stack : stacks) {
				spawnAsEntity(world, player.getEyePosition(), stack);
			}
        }
	}

	public static void spawnAsEntity(Player player, ItemStackHandler handler) {
		if(handler==null || handler.getSlots()==0) return;
		Level world = player.level();
		if (!world.isClientSide() && !world.restoringBlockSnapshots) {
			for(int i = 0; i < handler.getSlots(); i++) {
				spawnAsEntity(world, player.getEyePosition(), handler.getStackInSlot(i));
			}
		}
	}
	public static void spawnAsEntity(Level world, Vec3 pos, ItemStack stack){
		if(!stack.isEmpty()){
			ItemEntity item = new ItemEntity(world, pos.x+0.5F, pos.y+0.5F, pos.z+0.5F, stack.copy());
			item.setPickUpDelay(1);
			world.addFreshEntity(item);
		}
	}

	@Deprecated
	public static long getCoolingMillis() {
		return EmailConfigServer.Send.cooling.getTicks();
	}
	@Deprecated
	public static long getPromptTicks() {
		return EmailConfigClient.Prompt_Email.getTicks();
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
		return EmailConfigServer.isInfiniteSize();
	}

    public static List<String> splitString(String text, int textMaxLength) {
        Font fr = Minecraft.getInstance().font;
        List<String> texts = Lists.newArrayList();
        if(fr.width(text) >= textMaxLength) {
            StringBuilder s = new StringBuilder();
            for(int i = 0; i < text.length(); i++) {
                String str = s.toString();
                if(fr.width(str) >= textMaxLength) {
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
    public static void drawAlignRightString(GuiGraphics graphics, String text, int x, int y, int color, boolean drawShadow) {
        drawAlignRightString(graphics, Minecraft.getInstance().font, text, x, y, color, drawShadow);
    }

	@OnlyIn(Dist.CLIENT)
    public static void drawAlignRightString(GuiGraphics graphics, Font fr, String text, int x, int y, int color, boolean drawShadow) {
        graphics.drawString(fr, text, x - fr.width(text), y, color, drawShadow);
    }
	@OnlyIn(Dist.CLIENT)
    public static void drawAlignRightString(GuiGraphics graphics, Component text, int x, int y, int color, boolean drawShadow) {
        drawAlignRightString(graphics, Minecraft.getInstance().font, text, x, y, color, drawShadow);
    }

	@OnlyIn(Dist.CLIENT)
    public static void drawAlignRightString(GuiGraphics graphics, Font fr, Component text, int x, int y, int color, boolean drawShadow) {
        graphics.drawString(fr, text, x - fr.width(text), y, color, drawShadow);
    }

	@Deprecated
	public static void hLineGradient(GuiGraphics graphics, boolean anti, int pX1, int pY1, int pX2, int pY2, int pColorFrom, int pColorTo) {
		RenderUtils.hLineGradient(graphics, anti, pX1, pY1, pX2, pY2, pColorFrom, pColorTo);
	}

	static Map<String, SoundSource> SOUND_CATEGORIES;
	public static SoundSource getSoundSource(String name) {
		if (SOUND_CATEGORIES == null) {
			SOUND_CATEGORIES = Arrays.stream(SoundSource.values()).collect(Collectors.toMap(SoundSource::getName, Function.identity()));
		}
		return SOUND_CATEGORIES.get(name);
	}
}
