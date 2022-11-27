package cat.jiu.email.util;

public class EmailSizeReport {
	public static final EmailSizeReport SUCCES = new EmailSizeReport(-1, -1, 0);
	public final long id;
	public final int slot;
	public final long size;
	public EmailSizeReport(long id, int slot, long size) {
		this.id = id;
		this.slot = slot;
		this.size = size;
	}
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		if(obj instanceof EmailSizeReport) {
			EmailSizeReport other = (EmailSizeReport) obj;
			return this.id == other.id && this.slot == other.slot && this.size == other.size;
		}
		return false;
	}
	@Override
	public String toString() {
		return "EmailSizeReport [emailID=" + id + ", Slot=" + slot + ", Size=" + size + "]";
	}
}
