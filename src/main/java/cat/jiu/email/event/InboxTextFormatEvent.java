package cat.jiu.email.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class InboxTextFormatEvent extends Event {
	private String result;
	public final String key;
	public final Object[] args;
	public InboxTextFormatEvent(String key, Object[] args) {
		this.key = key;
		this.args = args;
	}
	public String getFormatResult() {
		return result;
	}
	public void setFormatResult(String result) {
		this.result = result;
	}
}
