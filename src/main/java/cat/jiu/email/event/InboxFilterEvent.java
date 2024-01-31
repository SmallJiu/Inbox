package cat.jiu.email.event;

import java.util.function.Predicate;

import cat.jiu.email.element.Email;
import cat.jiu.email.ui.gui.GuiEmailMain;

import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class InboxFilterEvent extends Event {
	private final GuiEmailMain gui;
	public InboxFilterEvent(GuiEmailMain gui) {
		this.gui = gui;
	}
	public void addFilter(String name, Predicate<Email> predicate) {
		this.gui.addFilter(name, predicate);
	}
}
