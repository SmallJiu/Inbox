package cat.jiu.email.element.event;

public class EventCondition {
	public final String name;
	public final Object defaultValue;
	public final Object after;
	private int arg;
	public EventCondition(String name, Object value, Object after) {
		this.name = name;
		this.defaultValue = value;
		this.after = after;
	}
	public void setArg(int arg1) {
		this.arg = arg1;
	}
	public int getArg() {
		return this.arg;
	}
}
