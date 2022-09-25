package cat.jiu.email.element;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

public class EmailFunction {
	public String sender;
	public final String addresser;
	public final Message title;
	public final List<ItemStack> items;
	public final EmailSound sound;
	public final List<Message> msgs;
	public EmailFunction(String sender, String addresser, Message title, List<ItemStack> items, List<Message> msgs, @Nullable EmailSound sound) {
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
