package cat.jiu.email.util;

public class TimeMillis {
	public final long millis;
	public TimeMillis(long millis) {
		this(0,0,millis);
	}
	public TimeMillis(long m, long s, long millis) {
		this.millis = EmailUtils.parseMillis(0,0,m,s,millis);
	}
}
