package cat.jiu.email.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;

import static net.minecraft.util.text.TextFormatting.*;

/** from Avaritia(morph.avaritia.util.TextUtils) */
public final class TextUtils {
	/** from Avaritia(morph.avaritia.util.TextUtils#fabulousness}) */
	private static final TextFormatting[] fabulousness = new TextFormatting[]{RED, GOLD, YELLOW, GREEN, AQUA, BLUE, LIGHT_PURPLE};

	/** from Avaritia(morph.avaritia.util.TextUtils#makeFabulous) */
	public static String makeFabulous(String input) {
		return ludicrousFormatting(input, fabulousness, 80.0, 1, 1);
	}

	/** from Avaritia(morph.avaritia.util.TextUtils#ludicrousFormatting) */
	public static String ludicrousFormatting(String input, TextFormatting[] colours, double delay, int step, int posstep) {
		StringBuilder sb = new StringBuilder(input.length() * 3);
		if(delay <= 0) {
			delay = 0.001;
		}
		int offset = (int) Math.floor(Minecraft.getSystemTime() / delay) % colours.length;
		for(int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			int col = ((i * posstep) + colours.length - offset) % colours.length;
			sb.append(colours[col].toString());
			sb.append(c);
		}
		return sb.toString();
	}
}
