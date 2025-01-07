package cat.jiu.email.util;

public class SizeReport {
	public static final SizeReport SUCCESS = new SizeReport(-1, -1, 0);
	public final long id;
	public final int slot;
	public final long size;

	public SizeReport(long id, int slot, long size) {
		this.id = id;
		this.slot = slot;
		this.size = size;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof SizeReport) {
			SizeReport other = (SizeReport) obj;
			return this.id == other.id && this.slot == other.slot && this.size == other.size;
		}
		return false;
	}
}
