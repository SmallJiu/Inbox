package cat.jiu.email.util;

public class TimeMillis {
	public final long millis;
	public TimeMillis(long millis) {
		this(0, 0, 0, 0, millis);
	}
	public TimeMillis(long s, long millis) {
		this(0,0,0,s,millis);
	}
	public TimeMillis(long m, long s, long millis) {
		this(0,0,m,s,millis);
	}
	public TimeMillis(long h, long m, long s, long millis) {
		this(0,h,m,s,millis);
	}
	public TimeMillis(long d, long h, long m, long s, long millis) {
		this.millis = EmailUtils.parseMillis(d,h,m,s,millis);
	}

	public long getDay() {
		return this.millis / 1000 / 60 / 60 / 24;
	}
	public long getHour() {
		return this.millis / 1000 / 60 / 60 % 24;
	}
	public long getMinute() {
		return this.millis / 1000 / 60 % 60;
	}
	public long getSecond() {
		return this.millis / 1000 % 60;
	}
	public long getTick() {
		return this.millis % 1000 / 50;
	}
}
