package cat.jiu.email.element;

import java.util.List;

import javax.annotation.Nullable;

import cat.jiu.core.api.element.ISound;
import cat.jiu.core.api.element.IText;
import net.minecraft.world.item.ItemStack;

public class EmailFunction {
	public IText sender;
	public final String addresser;
	public final IText title;
	public final List<ItemStack> items;
	public final ISound sound;
	public final List<IText> msgs;
	public EmailFunction(IText sender, String addresser, IText title, List<ItemStack> items, List<IText> msgs, @Nullable ISound sound) {
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
