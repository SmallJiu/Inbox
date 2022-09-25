package cat.jiu.email.util;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import cat.jiu.email.element.EmailFunction;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.element.event.EventCondition;
import cat.jiu.email.element.event.EventElement;
import cat.jiu.email.event.EmailSendDevMessageEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

@Mod.EventBusSubscriber
public class EmailExecuteEvent {
	private static final Map<String, List<EventElement>> events = Maps.newHashMap();
	private static final Map<String, Object> defaultValue = Maps.newHashMap();

	@SubscribeEvent
	public static void onPlayerJoinWorld(EntityJoinWorldEvent event) {
		if(!event.getEntity().world.isRemote && event.getEntity() instanceof EntityPlayer) {
			List<EventElement> elements = events.get("PlayerJoinWorld");
			if(elements != null && !elements.isEmpty()) {
				Inbox inbox = Inbox.get((EntityPlayer) event.getEntity());
				for(EventElement e : elements) {
					if(e.condition != null) {
						Object defaultValue = inbox.getCustom(e.condition.name);
						Object conValue = e.condition.defaultValue;
						if(conValue != null && check(defaultValue, conValue)) {
							if((Integer) e.condition.getArg() == null || e.condition.getArg() == event.getEntity().dimension) {
								inbox.addCustom(e.condition.name, e.condition.after);
								EmailFunction function = EmailUtils.findFunction(e.emailFile);
								if(function != null) {
									inbox.add(function.toEmail());
								}
								EmailUtils.saveInboxToDisk(inbox, 10);
							}
						}
					}else {
						EmailFunction function = EmailUtils.findFunction(e.emailFile);
						if(function != null) {
							inbox.add(function.toEmail());
						}
						EmailUtils.saveInboxToDisk(inbox, 10);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerDeath(LivingDeathEvent event) {
		if(!event.getEntity().world.isRemote && event.getEntity() instanceof EntityPlayer) {
			List<EventElement> elements = events.get("PlayerDeath");
			if(elements != null && !elements.isEmpty()) {
				EntityPlayer player = (EntityPlayer) event.getEntityLiving();
				int deathCount = player.world.getMinecraftServer().getPlayerList().getPlayerStatsFile(player).readStat(StatList.DEATHS) + 1;
				Inbox inbox = Inbox.get(player);
				for(EventElement e : elements) {
					if(e.condition != null) {
						Object defaultValue = inbox.getCustom(e.condition.name);
						Object conValue = e.condition.defaultValue;
						if(conValue != null && check(defaultValue, conValue)) {
							if((Integer) e.condition.getArg() == null || e.condition.getArg() == deathCount) {
								inbox.addCustom(e.condition.name, e.condition.after);
								EmailFunction function = EmailUtils.findFunction(e.emailFile);
								if(function != null) {
									inbox.add(function.toEmail());
								}
								EmailUtils.saveInboxToDisk(inbox, 10);
							}
						}
					}else {
						EmailFunction function = EmailUtils.findFunction(e.emailFile);
						if(function != null) {
							inbox.add(function.toEmail());
						}
						EmailUtils.saveInboxToDisk(inbox, 10);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerBreakBlock(BlockEvent.BreakEvent event) {

	}

	@SubscribeEvent
	public static void onPlayerCraftItem(PlayerEvent.ItemCraftedEvent event) {

	}

	@SubscribeEvent
	public static void onPlayerSmeltedItem(PlayerEvent.ItemSmeltedEvent event) {

	}

	@SubscribeEvent
	public static void onPlayerPlaceBlock(BlockEvent.EntityPlaceEvent event) {
		if(event.getEntity() instanceof EntityPlayer) {

		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(EmailSendDevMessageEvent event) {
		List<EventElement> elements = events.get("PlayerFirstLogin");
		if(elements != null && !elements.isEmpty()) {
			Inbox inbox = event.inbox;
			for(EventElement e : elements) {
				if(e.condition != null) {
					Object value = inbox.getCustom(e.condition.name);
					Object defaultValue = e.condition.defaultValue;
					if(defaultValue != null && check(value, defaultValue)) {
						inbox.addCustom(e.condition.name, e.condition.after);
						EmailFunction function = EmailUtils.findFunction(e.emailFile);
						if(function != null) {
							inbox.add(function.toEmail());
						}
					}
				}else {
					EmailFunction function = EmailUtils.findFunction(e.emailFile);
					if(function != null) {
						inbox.add(function.toEmail());
					}
				}
			}
		}
	}

	public static void initDefaultCustomValue(Inbox inbox) {
		for(Entry<String, Object> defaultValue : defaultValue.entrySet()) {
			String key = defaultValue.getKey();
			Object value = defaultValue.getValue();
			if(!inbox.hasCustomValue(key)) {
				inbox.addCustom(key, value);
			}
		}
	}

	private static boolean check(Object value, Object condition) {
		if(value instanceof Boolean && condition instanceof Boolean) {
			return (boolean) value == (boolean) condition;
		}
		if(value instanceof Integer && condition instanceof Integer) {
			return (int) value == (int) condition;
		}
		if(value instanceof ItemStack && condition instanceof ItemStack) {
			return EmailUtils.equalsStack((ItemStack) value, (ItemStack) condition, true, true, true);
		}
		return false;
	}

	public static void init() {
		events.clear();
		defaultValue.clear();
		File dir = new File(EmailUtils.typePath + "event" + File.separator);
		if(dir.exists() && dir.isDirectory()) {
			List<File> eventFiles = EmailUtils.getAllFiles(dir);
			for(File file : eventFiles) {
				JsonElement e = JsonUtil.parse(file);
				if(e != null && e.isJsonObject()) {
					for(Entry<String, JsonElement> events : e.getAsJsonObject().entrySet()) {
						List<EventElement> elements = Lists.newArrayList();
						JsonArray eventEntrys = events.getValue().getAsJsonArray();
						for(int i = 0; i < eventEntrys.size(); i++) {
							JsonObject function = eventEntrys.get(i).getAsJsonObject();
							EventCondition condition = null;
							if(function.has("condition")) {
								JsonObject conditionJson = function.getAsJsonObject("condition");
								Object defaultValue = null;
								{
									JsonElement valueElement = conditionJson.get("default");
									if(valueElement.isJsonPrimitive()) {
										JsonPrimitive pri = valueElement.getAsJsonPrimitive();
										if(pri.isNumber()) {
											defaultValue = pri.getAsNumber();
										}else if(pri.isBoolean()) {
											defaultValue = pri.getAsBoolean();
										}else {
											defaultValue = pri.getAsString();
										}
									}
									EmailExecuteEvent.defaultValue.put(conditionJson.get("name").getAsString(), defaultValue);
								}

								Object after = null;
								{
									JsonElement afterElement = conditionJson.get("after");
									if(afterElement.isJsonPrimitive()) {
										JsonPrimitive pri = afterElement.getAsJsonPrimitive();
										if(pri.isNumber()) {
											after = pri.getAsNumber();
										}else if(pri.isBoolean()) {
											after = pri.getAsBoolean();
										}else {
											after = pri.getAsString();
										}
									}
								}
								condition = new EventCondition(conditionJson.get("name").getAsString(), defaultValue, after);
								if(conditionJson.has("arg")) {
									condition.setArg(conditionJson.getAsJsonPrimitive("arg").getAsInt());
								}
							}
							elements.add(new EventElement(function.get("email").getAsString(), condition));
						}
						EmailExecuteEvent.events.put(events.getKey(), elements);
					}
				}
			}
		}
	}
}
