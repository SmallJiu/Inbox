package cat.jiu.email.element;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.ui.SendEmailCoolingEvent;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonUtil;
import cat.jiu.email.util.TimeMillis;

import net.minecraftforge.common.MinecraftForge;

public class Cooling {
	private static final HashMap<String, Long> coolings = Maps.newHashMap();
	
	public static void cooling(String name) {
		cooling(name, EmailUtils.getCoolingMillis());
	}
	public static void cooling(String name, TimeMillis time) {
		cooling(name, time.millis);
	}
	public static void cooling(String name, long millis) {
		long m = System.currentTimeMillis() + millis;
		coolings.put(name, m);
		MinecraftForge.EVENT_BUS.post(new SendEmailCoolingEvent(name, m));
		save();
	}
	
	public static boolean isCooling(String name) {
		if(coolings.containsKey(name)) {
			return coolings.get(name) > System.currentTimeMillis();
		}
		return false;
	}
	public static long getCoolingTimeMillis(String name) {
		if(isCooling(name)) {
			return coolings.get(name);
		}
		return 0;
	}
	public static long getLastCoolingTimeMillis(String name) {
		if(isCooling(name)) {
			return coolings.get(name) - System.currentTimeMillis();
		}
		return 0;
	}
	public static IText getLastCoolingTimeText(String name) {
		if(isCooling(name)) {
			long last = getLastCoolingTimeMillis(name);
			long t_t = last % 1000;
			 long t_s = last / 1000;
			 long t_m = 0;
			 if(t_s >= 60) {
				 t_m = t_s / 60;
				 t_s %= 60;
			 }
			 long t_h = 0;
			 if(t_m >= 60) {
				 t_h = t_m / 60;
				 t_m %= 60;
			 }
			 long t_d = 0;
			 if(t_h >= 24) {
				 t_d = t_h / 24;
				 t_h %= 24;
			 }
			 String d = t_d < 10 ? "0" + t_d : Long.toString(t_d);
			 String h = t_h < 10 ? "0" + t_h : Long.toString(t_h);
			 String m = t_m < 10 ? "0" + t_m : Long.toString(t_m);
			 String s = t_s < 10 ? "0" + t_s : Long.toString(t_s);
			 String t = t_t < 10 ? "0" + t_t : Long.toString(t_t);
			 return new Text("info.email.cooling", d, h, m, s, t);
		}
		return Text.empty;
	}
	
	public static void save() {
		File jsonFile = new File(EmailAPI.globalEmailListPath);
		JsonObject json = new JsonObject();
		if(jsonFile.exists()) {
			JsonElement e = JsonUtil.parse(jsonFile);
			if(e != null && e.isJsonObject()) {
				json = e.getAsJsonObject();
			}
		}
		
		JsonObject list = new JsonObject();
		if(json.has("Cooling")) {
			JsonElement e = json.get("Cooling");
			if(e.isJsonObject()) {
				list = e.getAsJsonObject();
			}
		}
		
		for(Entry<String, Long> cooling : coolings.entrySet()) {
			list.addProperty(cooling.getKey(), cooling.getValue());
		}
		
		json.add("Cooling", list);
		JsonUtil.toJsonFile(EmailAPI.globalEmailListPath, json, false);
	}
	
	public static void load() {
		coolings.clear();
		File jsonFile = new File(EmailAPI.globalEmailListPath);
		if(jsonFile.exists()) {
			JsonElement e = JsonUtil.parse(jsonFile);
			if(e != null && e.isJsonObject() && e.getAsJsonObject().has("Cooling")) {
				for(Entry<String, JsonElement> cooling : e.getAsJsonObject().getAsJsonObject("Cooling").entrySet()) {
					coolings.put(cooling.getKey(), cooling.getValue().getAsLong());
				}
			}
		}
	}
	
	@Deprecated
	public static class CoolingMillis extends TimeMillis {
		public CoolingMillis(long millis) {
			this(0,0,millis);
		}
		public CoolingMillis(long m, long s, long millis) {
			super(m,s,millis);
		}
	}
}
