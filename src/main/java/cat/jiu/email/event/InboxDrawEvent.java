package cat.jiu.email.event;

import cat.jiu.email.element.Inbox;
import cat.jiu.email.ui.gui.GuiEmailMain;

import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class InboxDrawEvent extends Event {
	public static enum Type {
		INBOX, CURRENT, CANDIDATE, SOUND;
	}
	
	public final GuiEmailMain gui;
	public final Type type;
	public final Phase phase;
	public final Inbox inbox;
	public final long emailID;
	public final int mouseX, mouseY;
	public InboxDrawEvent(GuiEmailMain gui, Type type, Phase phase, Inbox inbox, long id, int mouseX, int mouseY) {
		this.gui = gui;
		this.type = type;
		this.phase = phase;
		this.inbox = inbox;
		this.emailID = id;
		this.mouseX = mouseX;
		this.mouseY = mouseY;
	}
	@Override
	public String toString() {
		return "InboxDrawEvent [gui=" + gui + ", type=" + type + ", phase=" + phase + ", emailID=" + emailID + ", mouseX=" + mouseX + ", mouseY=" + mouseY + "]";
	}
}
