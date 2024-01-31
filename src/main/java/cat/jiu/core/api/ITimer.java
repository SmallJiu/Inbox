package cat.jiu.core.api;

import java.util.StringJoiner;

import com.google.gson.JsonObject;

import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.core.util.timer.MillisTimer;
import cat.jiu.core.util.timer.Timer;
import crafttweaker.annotations.ZenRegister;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;

@ZenRegister
@ZenClass("core.Timer")
public interface ITimer extends ISerializable {
	/**
	 * the core method, use {@code ticks} to format ticks
	 * 
	 * @param ticks
	 *            all tick
	 * @author small_jiu
	 */
	@ZenMethod
	void format(long ticks);
	
	@ZenGetter("day")
	long getDay();

	@ZenGetter("hour")
	long getHour();

	@ZenGetter("minute")
	long getMinute();

	@ZenGetter("second")
	long getSecond();

	@ZenGetter("tick")
	long getTick();

	@ZenGetter("ticks")
	long getTicks();

	@ZenGetter("allTicks")
	long getAllTicks();

	@ZenMethod("day")
	ITimer setDay(long day);

	@ZenMethod("hour")
	ITimer setHour(long hour);

	@ZenMethod("minute")
	ITimer setMinute(long minute);

	@ZenMethod("second")
	ITimer setSecond(long second);

	@ZenMethod("tick")
	ITimer setTick(long tick);

	@ZenMethod("allTicks")
	ITimer setAllTicks(long allTicks);

	@ZenMethod
	ITimer add(ITimer time);

	@ZenMethod
	ITimer subtract(ITimer time);

	@ZenMethod
	ITimer reset();

	@ZenMethod
	ITimer copy();

	@ZenMethod("allTicks")
	default ITimer setAllTicks(long s, long tick) {
		return this.setAllTicks(Timer.parseTick(s, tick));
	}

	@ZenMethod("allTicks")
	default ITimer setAllTicks(long m, long s, long tick) {
		return this.setAllTicks(Timer.parseTick(m, s, tick));
	}

	@ZenMethod("allTicks")
	default ITimer setAllTicks(long h, long m, long s, long tick) {
		return this.setAllTicks(Timer.parseTick(h, m, s, tick));
	}

	@ZenMethod("allTicks")
	default ITimer setAllTicks(long day, long h, long m, long s, long tick) {
		return this.setAllTicks(Timer.parseTick(day, h, m, s, tick));
	}

	@ZenMethod("ticks")
	default ITimer setTicks(long ticks) {
		this.format(ticks);
		return this;
	}
	
	@ZenMethod("done")
	default boolean isDone() {
		return this.getTicks() <= 0;
	}

	/**
	 * @param denominator
	 *            the denominator, like '1/5' of '5'
	 * @param numerator
	 *            the numerator, like '1/5' of '1'
	 * @return true if ticks == numerator/denominator
	 */
	@ZenMethod
	default boolean isPart(int numerator, int denominator) {
		return this.getTicks() == (this.getAllTicks() / denominator) * numerator;
	}

	@ZenMethod
	default int getPart(int denominator) {
		long part = this.getAllTicks() / denominator;
		for(int i = 1; i < denominator + 1; i++) {
			if(this.getTicks() == part * i) {
				return i;
			}
		}
		return -1;
	}

	@ZenMethod
	default float getSurplusPart() {
		return (float) (((this.getTicks() - this.getAllTicks()) * 1.0 / this.getAllTicks()) + 1);
	}

	@ZenMethod
	default void replace() {
		if(this.getTick() <= 0)
			this.setTick(0);
		if(this.getSecond() <= 0)
			this.setSecond(0);
		if(this.getMinute() <= 0)
			this.setMinute(0);
		if(this.getHour() <= 0)
			this.setHour(0);
		if(this.getDay() <= 0)
			this.setDay(0);

		this.format(Timer.parseTick(this.getDay(), this.getHour(), this.getMinute(), this.getSecond(), this.getTick()));
	}
	
	@ZenMethod("strTime")
	default String toStringTime(boolean reverse) {
		long[] str = new long[]{this.getTick(), this.getSecond(), this.getMinute(), this.getHour(), this.getDay()};
		StringJoiner sj = new StringJoiner(":");
		
		if(reverse) {
			for(int i = 0; i < str.length; i++) {
				sj.add(format(str[i], 10));
			}
		}else {
			for(int i = str.length-1; i >= 0; i--) {
				sj.add(format(str[i], 10));
			}
		}
		return sj.toString();
	}
	
	@ZenMethod("msTimestamp")
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
		
		sj.add(format(d, 10));
		sj.add(format(h, 10));
		sj.add(format(m, 10));
		sj.add(format(s, 10));
		sj.add(format(t, 10));
		
		return sj.toString();
	}
	
	@ZenMethod
	public static StringBuilder format(long num, long f) {
		StringBuilder s = new StringBuilder();
		if(num < 10)
			s.append("0");
		for(int i = 0; i < Long.toString(f).length() - 2; i++) {
			s.append(0);
		}
		s.append(num);
		return s;
	}

	@ZenMethod("hashCode")
	default long hash() {
		return this.getAllTicks() >> 9;
	}
	
	@ZenMethod("equals")
	default boolean equalsTime(Object obj) {
		if(obj == this)
			return true;
		if(obj instanceof ITimer) {
			ITimer other = (ITimer) obj;
			other.replace();
			this.replace();
			return this.hash() == other.hash() && this.getTicks() == other.getTicks();
		}

		return false;
	}
	
	@ZenMethod("isStarted")
	default boolean isStarted() {
		return false;
	}

	@ZenMethod("start")
	default ITimer start() {
		return this;
	};

	/**
	 * like {@link net.minecraft.util.ITickable}
	 * 
	 * @param subtractTick
	 *            Per tick to subtract lastMillis ticks
	 */
	@ZenMethod
	default void update(int subtractTick) {
		this.format(this.getTicks() - subtractTick);
	}

	@ZenMethod
	default void update() {
		this.update(1);
	}

	@ZenMethod
	@Override
	default JsonObject write(JsonObject json) {
		if(json == null)
			json = new JsonObject();
		json.addProperty("ticks", this.getTicks());
		json.addProperty("allTicks", this.getAllTicks());
		json.addProperty("isSys", this instanceof MillisTimer);
		return json;
	}

	@ZenMethod
	@Override
	default NBTTagCompound write(NBTTagCompound nbt) {
		if(nbt == null)
			nbt = new NBTTagCompound();
		nbt.setLong("ticks", this.getTicks());
		nbt.setLong("allTicks", this.getAllTicks());
		nbt.setBoolean("isSys", this instanceof MillisTimer);
		return nbt;
	}

	@ZenMethod
	@Override
	default void read(JsonObject json) {
		this.format(json.get("ticks").getAsLong());
		this.setAllTicks(json.get("allTicks").getAsLong());
	}

	@ZenMethod
	@Override
	default void read(NBTTagCompound nbt) {
		this.format(nbt.getLong("ticks"));
		this.setAllTicks(nbt.getLong("allTicks"));
	}

	@ZenMethod
	public static ITimer from(NBTTagCompound nbt) {
		ITimer time = null;
		if(nbt.hasKey("isSys") && nbt.getBoolean("isSys")) {
			time = new MillisTimer();
		}else {
			time = new Timer();
		}
		time.readFrom(nbt);
		return time;
	}

	@ZenMethod
	public static ITimer from(JsonObject obj) {
		ITimer time = null;
		if(obj.has("isSys") && obj.get("isSys").getAsBoolean()) {
			time = new MillisTimer();
		}else {
			time = new Timer();
		}
		time.readFrom(obj);
		return time;
	}
	
	/**
	 * var instance = core.Timer.from(true)
	 */
	@ZenMethod
	public static ITimer from(boolean isSysTimer) {
		if(isSysTimer){
			return new MillisTimer();
		}else {
			return new Timer();
		}
	}
	@ZenMethod
	public static ITimer from(boolean isSysTimer, long ms) {
		if(isSysTimer){
			return new MillisTimer(ms);
		}else {
			return new Timer(ms / 50);
		}
	}
	@ZenMethod
	public static ITimer from(boolean isSysTimer, long tick, long ms) {
		if(isSysTimer){
			return new MillisTimer((tick * 50) + ms);
		}else {
			return new Timer((ms / 50) + tick);
		}
	}
	@ZenMethod
	public static ITimer from(boolean isSysTimer, long sec, long tick, long ms) {
		if(isSysTimer){
			return new MillisTimer(sec, (tick * 50) + ms);
		}else {
			return new Timer(sec, (ms / 50) + tick);
		}
	}
	@ZenMethod
	public static ITimer from(boolean isSysTimer, long min, long sec, long tick, long ms) {
		if(isSysTimer){
			return new MillisTimer(min, sec, (tick * 50) + ms);
		}else {
			return new Timer(min, sec, (ms / 50) + tick);
		}
	}
	@ZenMethod
	public static ITimer from(boolean isSysTimer, long hour, long min, long sec, long tick, long ms) {
		if(isSysTimer){
			return new MillisTimer(hour, min, sec, (tick * 50) + ms);
		}else {
			return new Timer(hour, min, sec, (ms / 50) + tick);
		}
	}
	@ZenMethod
	public static ITimer from(boolean isSysTimer, long day, long hour, long min, long sec, long tick, long ms) {
		if(isSysTimer){
			return new MillisTimer(day, hour, min, sec, (tick * 50) + ms);
		}else {
			return new Timer(day, hour, min, sec, (ms / 50) + tick);
		}
	}
}
