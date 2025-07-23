package cat.jiu.email.util;

import java.util.Objects;

public record SizeReport(long id, int slot, long size) {
	public static final SizeReport SUCCESS = new SizeReport(-1, -1, 0);

	@Override
	public int hashCode() {
		return Objects.hash(id, slot, size);
	}

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
