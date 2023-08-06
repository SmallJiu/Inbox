package morph.avaritia.util;

import cat.jiu.email.EmailMain;
import net.minecraft.util.text.TextFormatting;
import static net.minecraft.util.text.TextFormatting.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextUtils {
	private static final TextFormatting[] fabulousness = new TextFormatting[]{RED, GOLD, YELLOW, GREEN, AQUA, BLUE, LIGHT_PURPLE};

	public static String makeFabulous(String input) {
		return ludicrousFormatting(input, fabulousness, 40.0, 1, 1);
	}

	public static String ludicrousFormatting(String input, TextFormatting[] colours, double delay, int step, int posstep) {
		StringBuilder sb = new StringBuilder(input.length() * 3);
		if(delay <= 0) {
			delay = 0.001;
		}
		int offset = (int) Math.floor(EmailMain.getSysTime() / delay) % colours.length;
		for(int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			int col = ((i * posstep) + colours.length - offset) % colours.length;
			sb.append(colours[col].toString());
			sb.append(c);
		}
		return sb.toString();
	}
}
