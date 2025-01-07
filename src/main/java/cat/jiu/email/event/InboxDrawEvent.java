package cat.jiu.email.event;

import cat.jiu.email.element.Inbox;

import cat.jiu.email.ui.gui.GuiInbox;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.Event;

@OnlyIn(Dist.CLIENT)
public class InboxDrawEvent extends Event {
	public enum Type {
		INBOX, CURRENT, CANDIDATE, SOUND
    }
	
	public final GuiInbox gui;
	public final Type type;
	public final Phase phase;
	public final Inbox inbox;
	public final long emailID;
	public final int mouseX, mouseY;
	public InboxDrawEvent(GuiInbox gui, Type type, Phase phase, Inbox inbox, long id, int mouseX, int mouseY) {
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
