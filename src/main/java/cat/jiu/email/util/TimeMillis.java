package cat.jiu.email.util;

public class TimeMillis {
	public final long millis;
	public TimeMillis(long millis) {
		this.millis = EmailUtils.parseMillis(0,0,0,0,millis);
	}
	public TimeMillis(long s, long millis) {
		this.millis = EmailUtils.parseMillis(0,0,0,s,millis);
	}
	public TimeMillis(long m, long s, long millis) {
		this.millis = EmailUtils.parseMillis(0,0,m,s,millis);
	}
	public TimeMillis(long h, long m, long s, long millis) {
		this.millis = EmailUtils.parseMillis(0,h,m,s,millis);
	}
	public TimeMillis(long d, long h, long m, long s, long millis) {
		this.millis = EmailUtils.parseMillis(d,h,m,s,millis);
	}
}
