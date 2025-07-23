package cat.jiu.email.event;

import java.util.function.Predicate;

import cat.jiu.email.element.Email;

import cat.jiu.email.ui.gui.GuiInbox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.Event;

@OnlyIn(Dist.CLIENT)
public class InboxFilterEvent extends Event {
	private final GuiInbox gui;
	public InboxFilterEvent(GuiInbox gui) {
		this.gui = gui;
	}
	public void addFilter(String name, Predicate<Email> predicate) {
		this.gui.addFilter(name, predicate);
	}
}
