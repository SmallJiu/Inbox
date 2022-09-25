package cat.jiu.email.event;

import cat.jiu.email.element.Inbox;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

public class EmailSendDevMessageEvent extends Event {
	public final EntityPlayer player;
	public final Inbox inbox;
	public EmailSendDevMessageEvent(EntityPlayer player, Inbox inbox) {
		this.player = player;
		this.inbox = inbox;
	}
}
