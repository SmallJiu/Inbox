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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + (int) (size ^ (size >>> 32));
		result = prime * result + slot;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		if(obj instanceof SizeReport) {
			SizeReport other = (SizeReport) obj;
			return this.id == other.id && this.slot == other.slot && this.size == other.size;
		}
		return false;
	}
	@Override
	public String toString() {
		return "Report [emailID=" + id + ", Slot=" + slot + ", Size=" + size + "]";
	}

	public static class ToBigException extends RuntimeException {
		public ToBigException() {}
		public ToBigException(SizeReport report) {
			this(String.format("To big! this is report. Email ID: %s, Slot: %s, Size: %s / §72097152 Bytes", report.id, report.slot, report.size));
		}

		public ToBigException(String message) {
			super(message);
		}
	}
}
