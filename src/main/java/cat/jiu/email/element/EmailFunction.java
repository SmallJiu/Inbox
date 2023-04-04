package cat.jiu.email.element;

import java.util.List;

import javax.annotation.Nullable;

import cat.jiu.email.iface.IInboxSound;
import cat.jiu.email.iface.IInboxText;
import net.minecraft.item.ItemStack;

public class EmailFunction {
	public IInboxText sender;
	public final String addresser;
	public final IInboxText title;
	public final List<ItemStack> items;
	public final IInboxSound sound;
	public final List<IInboxText> msgs;
	public EmailFunction(IInboxText sender, String addresser, IInboxText title, List<ItemStack> items, List<IInboxText> msgs, @Nullable IInboxSound sound) {
		this.sender = sender;
		this.addresser = addresser;
		this.title = title;
		this.items = items;
		this.msgs = msgs;
		this.sound = sound;
	}
	@Override
	public String toString() {
		return "EmailFunction [sender=" + sender + ", addresser=" + addresser + ", title=" + title + ", items=" + items + ", sound=" + sound + ", msgs=" + msgs + "]";
	}
	public Email toEmail() {
		return new Email(title, sender, sound, items, msgs);
	}
}
