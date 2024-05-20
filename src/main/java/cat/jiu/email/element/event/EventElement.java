package cat.jiu.email.element.event;

public class EventElement {
	public final String emailFile;
	public final EventCondition condition;
	public EventElement(String emailFile, EventCondition condition) {
		this.emailFile = emailFile;
		this.condition = condition;
	}
}
