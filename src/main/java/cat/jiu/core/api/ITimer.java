package cat.jiu.core.api;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringJoiner;

import com.google.gson.JsonObject;

import cat.jiu.core.api.handler.ISerializable;
import cat.jiu.core.util.timer.MillisTimer;
import cat.jiu.core.util.timer.Timer;
import cat.jiu.sql.SQLValues;

import net.minecraft.nbt.CompoundNBT;

public interface ITimer extends ISerializable {
	/**
	 * the core method, use {@code ticks} to format ticks
	 * 
	 * @param ticks
	 *            all tick
	 * @author small_jiu
	 */
	void format(long ticks);

	long getDay();

	long getHour();

	long getMinute();

	long getSecond();

	long getTick();

	long getTicks();

	long getAllTicks();

	ITimer setDay(long day);

	ITimer setHour(long hour);

	ITimer setMinute(long minute);

	ITimer setSecond(long second);

	ITimer setTick(long tick);

	ITimer setAllTicks(long allTicks);

	ITimer add(ITimer time);

	ITimer subtract(ITimer time);

	ITimer reset();

	ITimer copy();

	default ITimer setAllTicks(long s, long tick) {
		return this.setAllTicks(Timer.parseTick(s, tick));
	}

	default ITimer setAllTicks(long m, long s, long tick) {
		return this.setAllTicks(Timer.parseTick(m, s, tick));
	}

	default ITimer setAllTicks(long h, long m, long s, long tick) {
		return this.setAllTicks(Timer.parseTick(h, m, s, tick));
	}

	default ITimer setAllTicks(long day, long h, long m, long s, long tick) {
		return this.setAllTicks(Timer.parseTick(day, h, m, s, tick));
	}

	default ITimer setTicks(long ticks) {
		this.format(ticks);
		return this;
	}

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
	default boolean isPart(int numerator, int denominator) {
		return this.getTicks() == (this.getAllTicks() / denominator) * numerator;
	}

	default int getPart(int denominator) {
		long part = this.getAllTicks() / denominator;
		for(int i = 1; i < denominator + 1; i++) {
			if(this.getTicks() == part * i) {
				return i;
			}
		}
		return -1;
	}

	default float getSurplusPart() {
		return (float) (((this.getTicks() - this.getAllTicks()) * 1.0 / this.getAllTicks()) + 1);
	}

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

	default long hash() {
		return this.getAllTicks() >> 9;
	}

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

	default boolean isStarted() {
		return false;
	}

	default ITimer start() {
		return this;
	};

	/**
	 * like {@link net.minecraft.tileentity.TileEntity}
	 * 
	 * @param subtractTick
	 *            Per tick to subtract lastMillis ticks
	 */
	default void update(int subtractTick) {
		this.format(this.getTicks() - subtractTick);
	}

	default void update() {
		this.update(1);
	}

	@Override
	default JsonObject write(JsonObject json) {
		if(json == null)
			json = new JsonObject();
		json.addProperty("ticks", this.getTicks());
		json.addProperty("allTicks", this.getAllTicks());
		json.addProperty("isSys", this instanceof MillisTimer);
		return json;
	}

	@Override
	default CompoundNBT write(CompoundNBT nbt) {
		if(nbt == null)
			nbt = new CompoundNBT();
		nbt.putLong("ticks", this.getTicks());
		nbt.putLong("allTicks", this.getAllTicks());
		nbt.putBoolean("isSys", this instanceof MillisTimer);
		return nbt;
	}

	@Override
	default SQLValues write(SQLValues value) {
		if(value == null)
			value = new SQLValues();
		value.put("ticks", this.getTicks());
		value.put("allTicks", this.getAllTicks());
		value.put("isSys", this instanceof MillisTimer);
		return value;
	}

	@Override
	default void read(JsonObject json) {
		this.format(json.get("ticks").getAsLong());
		this.setAllTicks(json.get("allTicks").getAsLong());
	}

	@Override
	default void read(CompoundNBT nbt) {
		this.format(nbt.getLong("ticks"));
		this.setAllTicks(nbt.getLong("allTicks"));
	}

	@Override
	default void read(ResultSet result) throws SQLException {
		this.format(result.getLong("ticks"));
		this.setAllTicks(result.getLong("allTicks"));
	}

	public static ITimer from(CompoundNBT nbt) {
		ITimer time = null;
		if(nbt.contains("isSys") && nbt.getBoolean("isSys")) {
			time = new MillisTimer();
		}else {
			time = new Timer();
		}
		time.readFrom(nbt);
		return time;
	}

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
}
