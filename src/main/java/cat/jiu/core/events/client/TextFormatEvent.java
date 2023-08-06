package cat.jiu.core.events.client;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class TextFormatEvent extends Event {
	private String result;
	public final String key;
	public final Object[] args;
	public TextFormatEvent(String key, Object[] args) {
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
