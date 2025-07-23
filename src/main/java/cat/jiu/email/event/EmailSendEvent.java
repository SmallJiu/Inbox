package cat.jiu.email.event;

import cat.jiu.email.element.Email;

import cat.jiu.email.element.EmailSenderGroup;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class EmailSendEvent extends Event implements ICancellableEvent {
	public final boolean pre;
	public final EmailSenderGroup group;
	public final String addresser;
	public final Email email;
	
	public EmailSendEvent(boolean pre, EmailSenderGroup group, String addresser, Email email) {
		this.pre = pre;
		this.group = group;
		this.addresser = addresser;
		this.email = email;
	}
	@Override
	public void setCanceled(boolean cancel) {
		if (this.pre) {
			ICancellableEvent.super.setCanceled(cancel);
		}
	}
}
