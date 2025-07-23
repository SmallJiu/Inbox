package cat.jiu.email.ui;

import net.neoforged.bus.api.Event;

public class SendEmailCoolingEvent extends Event {
	public final long millis;
	public final String name;
	public SendEmailCoolingEvent(String name, long millis) {
		this.name = name;
		this.millis = millis; 
	}
}
