package cat.jiu.core.util.timer;

import com.google.gson.JsonObject;

import cat.jiu.core.api.ITimer;

import net.minecraft.nbt.NBTTagCompound;

/**
 * use {@linkplain System#currentTimeMillis()} to build timer<p>
 * you must use {@linkplain #start()} to start timer<p>
 * but you don't use {@link #update()} or {@link #update(int)} to update timer
 * @author small_jiu
 */
public class MillisTimer implements ITimer {
	protected long millis;
	protected long sysMillis;
	protected long currentMillis;
	
	public MillisTimer() {
		this(1000);
	}
	public MillisTimer(long sec, long millis) {
		this(0, sec, millis);
	}
	public MillisTimer(long min, long sec, long millis) {
		this(0, 0, min, sec, millis);
	}
	public MillisTimer(long hour, long min, long sec, long millis) {
		this(0, hour, min, sec, millis);
	}
	public MillisTimer(long day, long hour, long min, long sec, long millis) {
		this(parseMillis(day, hour, min, sec, millis));
	}
	public MillisTimer(long millis) {
		this.millis = millis;
	}
	
	protected boolean start = false;
	public boolean isStarted() {return start;}
	public MillisTimer start() {
		if(!this.isStarted()) {
			this.sysMillis = System.currentTimeMillis();
			this.currentMillis = this.sysMillis + this.millis;
			this.start = true;
		}
		return this;
	}
	
	@Override
	public boolean isDone() {
		return this.isStarted() ? System.currentTimeMillis() >= this.currentMillis : false;
	}
	
	public long getLastMillis() {
		if(!this.isStarted()) return 0;
		long ms = this.currentMillis - System.currentTimeMillis();
		if(ms <= 0) return 0; 
		return ms;
	}
	
	public void setNotStartedMillis(long millis) {
		this.millis = millis;
	}
	public long getNotStartedMillis() {
		return millis;
	}
	public long getStartedCurrentMillis() {
		return currentMillis;
	}
	public long getStartedSystemMillis() {
		return sysMillis;
	}
	
	@Override
	public long getTicks() {
		if(!this.isStarted()) return 0;
		return this.getLastMillis() / 50;
	}

	@Override
	public long getAllTicks() {
		if(!this.isStarted()) return 0;
		return this.millis / 50;
	}

	@Override
	public MillisTimer setAllTicks(long ticks) {
		this.currentMillis = this.sysMillis + (this.millis = ticks*50);
		return this;
	}

	@Override
	public MillisTimer reset() {
		this.start=false;
		return this.start();
	}

	@Override
	public MillisTimer copy() {
		return new MillisTimer(this.millis);
	}

	public int hashCode() {return (int) this.hash();}
	public boolean equals(Object obj) {return equalsTime(obj);}
	public String toString() {return toStringTime(false);}
	public MillisTimer clone() {return this.copy();}

	@Override
	public long getDay() {
		if(!this.isStarted()) return -1;
		return this.getLastMillis() / 1000 / 60 / 60 / 24;
	}
	@Override
	public long getHour() {
		if(!this.isStarted()) return -1;
		return this.getLastMillis() / 1000 / 60 / 60 % 24;
	}
	@Override
	public long getMinute() {
		if(!this.isStarted()) return -1;
		return this.getLastMillis() / 1000 / 60 % 60;
	}
	@Override
	public long getSecond() {
		if(!this.isStarted()) return -1;
		return this.getLastMillis() / 1000 % 60;
	}
	@Override
	public long getTick() {
		if(!this.isStarted()) return -1;
		return this.getLastMillis() % 1000 / 50;
	}
	
	@Override
	public NBTTagCompound write(NBTTagCompound nbt) {
		if(nbt==null) nbt = new NBTTagCompound();
		nbt.setLong("ms", this.millis);
		nbt.setLong("sysMillis", this.sysMillis);
		nbt.setLong("current", this.currentMillis);
		nbt.setBoolean("isSys", true);
		return nbt;
	}
	
	@Override
	public void read(NBTTagCompound nbt) {
		this.millis = nbt.getLong("ms");
		this.sysMillis = nbt.getLong("sysMillis");
		this.currentMillis = nbt.getLong("current");
	}
	
	@Override
	public JsonObject write(JsonObject json) {
		if(json==null) json = new JsonObject();
		json.addProperty("ms", this.millis);
		json.addProperty("sysMillis", this.sysMillis);
		json.addProperty("current", this.currentMillis);
		json.addProperty("isSys", true);
		return json;
	}
	@Override
	public void read(JsonObject json) {
		this.millis = json.get("ms").getAsLong();
		this.sysMillis = json.get("sysMillis").getAsLong();
		this.currentMillis = json.get("current").getAsLong();
	}
	
	public void format(long ms) {}
	public void update() {}
	public void update(int subtractTick) {}
	public void replace() {}
	public MillisTimer setDay(long day) {return this;}
	public MillisTimer setHour(long hour) {return this;}
	public MillisTimer setMinute(long minute) {return this;}
	public MillisTimer setSecond(long second) {return this;}
	public MillisTimer setTick(long millis) {return this;}
	public MillisTimer add(ITimer time) {return this;}
	public MillisTimer subtract(ITimer time) {return this;}
	
// static
	
	public static long parseMillis(long s, long millis) {
		return parseMillis(0, s, millis);
	}
	public static long parseMillis(long m, long s, long millis) {
		return parseMillis(0, m, s, millis);
	}
	public static long parseMillis(long h, long m, long s, long millis) {
		return parseMillis(0, h, m, s, millis);
	}
	public static long parseMillis(long day, long h, long m, long s, long millis) {
		return (((((((day*24)+h)*60)+m)*60)+s)*1000)+millis;
	}
}
