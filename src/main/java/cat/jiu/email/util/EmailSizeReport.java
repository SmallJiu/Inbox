package cat.jiu.email.util;

public class EmailSizeReport {
	public static final EmailSizeReport SUCCES = new EmailSizeReport(-1, -1, 0);
	public final int msgID;
	public final int itemSlot;
	public final long size;
	public EmailSizeReport(int msgID, int itemID, long size) {
		this.msgID = msgID;
		this.itemSlot = itemID;
		this.size = size;
	}
	public boolean equals(Object obj) {
		if(obj == this) {
			return true;
		}
		if(obj instanceof EmailSizeReport) {
			EmailSizeReport other = (EmailSizeReport) obj;
			return this.msgID == other.msgID && this.itemSlot == other.itemSlot && this.size == other.size;
		}
		return false;
	}
	public String toString() {
		return "MessageID: " + this.msgID + ", ItemSlot: " + this.itemSlot + ", Size: " + this.size;
	}
}
