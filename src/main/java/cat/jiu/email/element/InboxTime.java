package cat.jiu.email.element;

import cat.jiu.email.iface.IInboxTime;

public class InboxTime implements IInboxTime {
	private final long original;
	private long tick;
	public InboxTime(long tick) {
		this(0,0,tick);
	}
	public InboxTime(long m, long s, long tick) {
		this.tick = parseTick(0,0,m,s,tick);
		this.original = this.tick;
	}
	public static long parseTick(long day, long h, long m, long s, long tick) {
		return (((((((day*24)+h)*60)+m)*60)+s)*20)+tick;
	}
	
	@Override
	public long getAllTicks() {
		return this.original;
	}
	@Override
	public void update() {
		this.tick--;
	}
	@Override
	public InboxTime reset() {
		this.tick = this.original;
		return this;
	}
	
	@Override
	public long getDay() {
		return this.tick / 20 / 60 / 60 / 24;
	}
	@Override
	public long getHour() {
		return this.tick / 20 / 60 / 60 % 24;
	}
	@Override
	public long getMinute() {
		return this.tick / 20 / 60 % 60;
	}
	@Override
	public long getSecond() {
		return this.tick / 20 % 60;
	}
	@Override
	public long getTick() {
		return this.tick % 20;
	}
}
