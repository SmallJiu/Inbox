package cat.jiu.core.util.timer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import cat.jiu.core.api.ITimer;

public class Timer implements ITimer {
	protected long day;
	protected long hour;
	protected long minute;
	protected long second;
	protected long tick;
	protected long ticks;
	protected long allTicks;
	
	public Timer() {
		this(0);
	}
	public Timer(long sec, long tick) {
		this(0, sec, tick);
	}
	public Timer(long min, long sec, long tick) {
		this(0, 0, min, sec, tick);
	}
	public Timer(long hour, long min, long sec, long tick) {
		this(0, hour, min, sec, tick);
	}
	public Timer(long day, long hour, long min, long sec, long tick) {
		this(parseTick(day, hour, min, sec, tick));
	}
	public Timer(long ticks) {
		this.setTicks(ticks);
		this.setAllTicks(ticks);
	}
	
	public Timer subtractDay(long day) {
		this.day -= day;
		this.replace();
		return this;
	}
	public Timer subtractHour(long hour) {
		this.hour -= hour;
		this.replace();
		return this;
	}
	public Timer subtractMinute(long minute) {
		this.minute -= minute;
		this.replace();
		return this;
	}
	public Timer subtractSecond(long second) {
		this.second -= second;
		this.replace();
		return this;
	}
	public Timer subtractTick(long tick) {
		this.tick -= tick;
		this.replace();
		return this;
	}
	
	public Timer addDay(long day) {
		this.day += day;
		this.replace();
		return this;
	}
	public Timer addHour(long hour) {
		this.hour += hour;
		this.replace();
		return this;
	}
	public Timer addMinute(long minute) {
		this.minute += minute;
		this.replace();
		return this;
	}
	public Timer addSecond(long second) {
		this.second += second;
		this.replace();
		return this;
	}
	public Timer addTick(long tick) {
		this.tick += tick;
		this.replace();
		return this;
	}
	
	public Timer setDay(long day) {
		this.day = day;
		this.replace();
		return this;
	}
	public Timer setHour(long hour) {
		this.hour = hour;
		this.replace();
		return this;
	}
	public Timer setMinute(long minute) {
		this.minute = minute;
		this.replace();
		return this;
	}
	public Timer setSecond(long second) {
		this.second = second;
		this.replace();
		return this;
	}
	public Timer setTick(long tick) {
		this.tick = tick;
		this.replace();
		return this;
	}
	
	public Timer setAllTicks(long allTicks) {
		this.allTicks = allTicks;
		return this;
	}
	
	@Override
	public Timer add(ITimer time) {
		long ticks = 0;
		
		ticks = this.ticks + time.getTicks();
		
		this.format(ticks);
		return this;
	}
	
	@Override
	public Timer subtract(ITimer time) {
		long ticks = 0;
		
		ticks = this.ticks - time.getTicks();
		
		this.format(ticks);
		return this;
	}
	
	@Override
	public Timer reset() {
		this.format(this.allTicks);
		return this;
	}
	
	public void format(long ticks) {
		if(ticks <= 0) {
			this.day = 0;
			this.hour = 0;
			this.minute = 0;
			this.second = 0;
			this.tick = 0;
			this.ticks = 0;
			return;
		}
		
		long day = 0;
		long hour = 0;
		long min = 0;
		long sec = 0;
		long tick = 0;
		
		if(ticks >= 20) {
			tick = ticks % 20;
			sec += ticks / 20;
		}else {
			tick = ticks;
		}
		if(sec >= 60) {
			min = sec / 60;
			sec %= 60;
		}
		if(min >= 60) {
			hour = min / 60;
			min %= 60;
		}
		if(hour >= 24) {
			day = hour / 24;
			hour %= 24;
		}
		this.day = day;
		this.hour = hour;
		this.minute = min;
		this.second = sec;
		this.tick = tick;
		this.ticks = ticks;
	}
	
	public long getDay() {return day;}
	public long getHour() {return hour;}
	public long getMinute() {return minute;}
	public long getSecond() {return second;}
	public long getTick() {return tick;}
	public long getTicks() {return ticks;}
	public long getAllTicks() {return allTicks;}
	
	public int hashCode() {return (int) this.hash();}
	public boolean equals(Object obj) {return equalsTime(obj);}
	public String toString() {return toStringTime(false);}
	public Timer clone() {return this.copy();}

	public Timer copy() {
		Timer time = new Timer(ticks);
		time.setAllTicks(allTicks);
		return time;
	}
	
// static
	
	public static Timer getTime(JsonElement e) {
		if(e.isJsonObject()) {
			return getTime(e.getAsJsonObject());
		}else if(e.isJsonPrimitive()) {
			return getTime(e.getAsJsonPrimitive());
		}
		return new Timer();
	}
	
	public static Timer getTime(JsonPrimitive json) {
		long day = 0;
		long hour = 0;
		long min = 0;
		long sec = 0;
		long tick = 0;
		if(json.isString()) {
			return getTime(json.getAsString());
		}else {
			tick = json.getAsLong();
		}
		
		return new Timer(day, hour, min, sec, tick);
	}
	
	public static Timer getTime(String time) {
		long day = 0;
		long hour = 0;
		long min = 0;
		long sec = 0;
		long tick = 0;
		if(time.contains(":")) {
			String[] times = time.split("\\:");
			switch(times.length) {
				case 5: day = Long.parseLong(times[4]);
				case 4: hour = Long.parseLong(times[3]);
				case 3: min = Long.parseLong(times[2]);
				case 2: sec = Long.parseLong(times[1]);
				default:
					tick = Long.parseLong(times[0]);
					break;
			}
		}else {
			tick = Long.parseLong(time);
		}
		return new Timer(day, hour, min, sec, tick);
	}
	
	public static Timer getTime(JsonObject obj) {
		return new Timer(
						time(obj, "d", "ds", "format_day",   "days"),
						time(obj, "h", "hs", "format_hour",   "hours"),
						time(obj, "m", "ms", "format_minute", "minutes"),
						time(obj, "s", "ss", "sec",    "secs", "format_second", "seconds"),
						time(obj, "t", "ts", "tick")
					);
	}
	
	private static long time(JsonObject obj, String... keys) {
		JsonPrimitive pri = getElement(JsonPrimitive.class, obj, keys);
		if(pri != null && pri.isNumber()) {
			return pri.getAsLong();
		}
		return 0;
	}
	
	@SuppressWarnings({"unchecked"})
	public static <T extends JsonElement> T getElement(Class<T> type, JsonObject obj, String... keys) {
		JsonType jsonType = JsonType.getType(type);
		T result = null;
		
		lable: for(String key : keys) {
			if(obj.has(key)) {
				JsonElement e = obj.get(key);
				if(e != null) {
					switch(jsonType) {
						case Object:
							if(e.isJsonObject()) {
								result = (T) e.getAsJsonObject();
								break lable;
							}
						case Array:
							if(e.isJsonArray()) {
								result = (T) e.getAsJsonArray();
								break lable;
							}
						case Primitive:
							if(e.isJsonPrimitive()) {
								result = (T) e.getAsJsonPrimitive();
								break lable;
							}
						case Element:
								result = (T) e;
								break lable;
					}
				}
			}
		}
		return result;
	}
	
	static enum JsonType {
		Object, Array, Primitive, Element;
		static <T extends JsonElement> JsonType getType(Class<T> type) {
			if(type == JsonObject.class) {
				return JsonType.Object;
			}else if(type == JsonArray.class) {
				return JsonType.Array;
			}else if(type == JsonPrimitive.class) {
				return JsonType.Primitive;
			}
			return JsonType.Element;
		}
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
}
