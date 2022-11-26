package cat.jiu.email.element;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

public class EmailFunction {
	public Text sender;
	public final String addresser;
	public final Text title;
	public final List<ItemStack> items;
	public final EmailSound sound;
	public final List<Text> msgs;
	public EmailFunction(Text sender, String addresser, Text title, List<ItemStack> items, List<Text> msgs, @Nullable EmailSound sound) {
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
