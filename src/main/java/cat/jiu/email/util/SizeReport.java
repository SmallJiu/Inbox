package cat.jiu.email.util;

public record SizeReport(long id, int slot, long size) {
	public static final SizeReport SUCCESS = new SizeReport(-1, -1, 0);

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof SizeReport other) {
			return this.id == other.id && this.slot == other.slot && this.size == other.size;
		}
		return false;
	}
}
