package cat.jiu.email.event;

import java.util.function.Predicate;

import cat.jiu.email.element.Email;

import cat.jiu.email.ui.gui.GuiEmailMain;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Event;

@OnlyIn(Dist.CLIENT)
public class InboxFilterEvent extends Event {
	private final GuiEmailMain gui;
	public InboxFilterEvent(GuiEmailMain gui) {
		this.gui = gui;
	}
	public void addFilter(String name, Predicate<Email> predicate) {
		this.gui.addFilter(name, predicate);
	}
}
