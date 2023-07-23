package cat.jiu.email.ui.gui.component;

import cat.jiu.email.element.Email;

public class EmailType {
	public final long id;
	public final Email email;
	public EmailType(long id, Email email) {
		this.id = id;
		this.email = email;
	}
}
