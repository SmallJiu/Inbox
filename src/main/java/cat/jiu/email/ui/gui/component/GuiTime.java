package cat.jiu.email.ui.gui.component;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import cat.jiu.email.EmailMain;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class GuiTime extends Gui {
	private boolean isEnable = false;
	protected final FontRenderer fontRenderer;
	private final boolean isHorizontal;
	private final GuiTextField[] times = new GuiFilterTextField[6];
	public static final List<Integer> CTRL_KEY = Collections.unmodifiableList(Arrays.asList(Keyboard.KEY_ESCAPE, Keyboard.KEY_RSHIFT, Keyboard.KEY_RSHIFT, Keyboard.KEY_BACK, Keyboard.KEY_UP, Keyboard.KEY_DOWN, Keyboard.KEY_RIGHT, Keyboard.KEY_LEFT));
	
	public GuiTime(boolean isHorizontal) {
		this.fontRenderer = Minecraft.getMinecraft().fontRenderer;
		this.isHorizontal = isHorizontal;
		
		Predicate<Character> charFilter = typedChar ->
			"0123456789".contains(String.valueOf(typedChar));
		Predicate<Integer> keyFilter = CTRL_KEY::contains;
		
		int width = this.fontRenderer.getCharWidth('8') * 3 + 7;
		
		for(int i = 0; i < this.times.length; i++) {
			GuiFilterTextField tf = new GuiFilterTextField(0, "0", this.fontRenderer, 0, 0, width, this.fontRenderer.FONT_HEIGHT + 1)
					.setTypedCharFilter(charFilter)
					.setKeyCodeFilter(keyFilter);
			tf.setEnableBackgroundDrawing(false);
			tf.setMaxStringLength(4);
			this.times[i] = tf;
		}
	}
	
	public void drawTimeBox(int x, int y) {
		if(!this.isEnable) {
			return;
		}
		String[] times = {
			I18n.format("email.config.time.day"),
			I18n.format("email.config.time.hour"),
			I18n.format("email.config.time.minute"),
			I18n.format("email.config.time.second"),
			I18n.format("email.config.time.tick"),
			I18n.format("email.config.time.millis")
		};
		
		int width = 0,
			tfWidth = this.times[0].width,
			tfHeight = this.times[0].height;
		for(GuiTextField tf : this.times) {
			width = Math.max(width, tf.width);
		}
		if(this.isHorizontal) {
			this.drawGradientRect(x - 2, y - 3, x + tfWidth - 3 + width + 2 + 5, y + tfHeight + 3 - 1, Color.BLACK.getRGB(), Color.BLACK.getRGB());
			this.drawGradientRect(x - 4, y - 3, x + tfWidth - 3 + width + 3 + 5, y + tfHeight + 3 - 1, Color.BLACK.getRGB(), Color.BLACK.getRGB());
			this.drawGradientRect(x - 2, y - 4, x + tfWidth - 3 + width + 2 + 5, y + tfHeight + 3, Color.BLACK.getRGB(), Color.BLACK.getRGB());
			
			this.drawHorizontalLine(x - 2, x + tfWidth - 3 + width + 5, y - 2, 1347420415);
			this.drawHorizontalLine(x - 2, x + tfWidth - 3 + width + 5, y + tfHeight + 3 - 3, 1347420415);
			
			this.drawVerticalLine(x-2, y - 2, y + tfHeight + 3 - 3, 1347420415);
			this.drawVerticalLine(x + tfWidth - 3 + width + 5, y - 2, y + tfHeight + 3 - 3, 1347420415);
			x+=2;
			
			for(int i = 0; i < this.times.length; i++) {
				GuiTextField tf = this.times[i];
				tf.x = x;
				tf.y = y;
				tf.drawTextBox();
				x += tf.width;
				
				this.fontRenderer.drawString(times[i]+",", x, y, Color.GREEN.getRGB());
				x += this.fontRenderer.getStringWidth(times[i]) + 5;
			}
		}else {
			this.drawGradientRect(x - 2, y - 3, x + tfWidth - 3 + width + 2 + 5, y + (tfHeight + 3) * 6 - 1, Color.BLACK.getRGB(), Color.BLACK.getRGB());
			this.drawGradientRect(x - 4, y - 3, x + tfWidth - 3 + width + 3 + 5, y + (tfHeight + 3) * 6 - 1, Color.BLACK.getRGB(), Color.BLACK.getRGB());
			this.drawGradientRect(x - 2, y - 4, x + tfWidth - 3 + width + 2 + 5, y + (tfHeight + 3) * 6, Color.BLACK.getRGB(), Color.BLACK.getRGB());
			
			this.drawHorizontalLine(x - 2, x + tfWidth - 3 + width + 5, y - 2, 1347420415);
			this.drawHorizontalLine(x - 2, x + tfWidth - 3 + width + 5, y + (tfHeight + 3) * 6 - 3, 1347420415);
			
			this.drawVerticalLine(x-2, y - 2, y + (tfHeight + 3) * 6 - 3, 1347420415);
			this.drawVerticalLine(x + tfWidth - 3 + width + 5, y - 2, y + (tfHeight + 3) * 6 - 3, 1347420415);
			x++;
			
			for(int i = 0; i < this.times.length; i++) {
				GuiTextField tf = this.times[i];
				tf.x = x;
				tf.y = y;
				tf.drawTextBox();
				this.fontRenderer.drawString(times[i], x + tf.width, y, Color.GREEN.getRGB());
				y += tf.height + 3;
			}
		}
	}
	
	public boolean mouseClick(int mouseX, int mouseY, int mouseButton) {
		if(!this.isEnable()) {
			return false;
		}
		for(GuiTextField tf : this.times) {
			if(tf.mouseClicked(mouseX, mouseY, mouseButton)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean keyTyped(char typedChar, int keyCode) {
		if(!this.isEnable()) {
			return false;
		}
		for(GuiTextField tf : this.times) {
			if(tf.textboxKeyTyped(typedChar, keyCode)) {
				return true;
			}
		}
		return false;
	}

	public boolean isEnable() {
		return isEnable;
	}
	public GuiTime setEnable(boolean isEnable) {
		this.isEnable = isEnable;
		for(GuiTextField tf : this.times) {
			tf.setVisible(isEnable);
		}
		return this;
	}
	
	public int getDay() {
		try {
			return Integer.parseInt(this.times[0].getText());
		}catch(NumberFormatException e) {
			return 0;
		}
	}

	public int getHour() {
		try {
			return Integer.parseInt(this.times[1].getText());
		}catch(NumberFormatException e) {
			return 0;
		}
	}

	public int getMinute() {
		try {
			return Integer.parseInt(this.times[2].getText());
		}catch(NumberFormatException e) {
			return 0;
		}
	}

	public int getSecond() {
		try {
			return Integer.parseInt(this.times[3].getText());
		}catch(NumberFormatException e) {
			return 0;
		}
	}

	public int getTick() {
		try {
			return Integer.parseInt(this.times[4].getText());
		}catch(NumberFormatException e) {
			return 0;
		}
	}

	public int getMillis() {
		try {
			return Integer.parseInt(this.times[5].getText());
		}catch(NumberFormatException e) {
			return 0;
		}
	}

	public long getTimeOfMillis() {
		return EmailUtils.parseMillis(this.getDay(), this.getHour(), this.getMinute(), this.getSecond(), this.getTick(), this.getMillis());
	}
	
	public long getTimeOfTicks() {
		return this.getTimeOfMillis() / 50;
	}

	public static class GuiFilterTextField extends GuiTextField {
		private Predicate<Character> typedCharFilter;
		private Predicate<Integer> keyCodeFilter;
		private final String defaultText;
		public GuiFilterTextField(int componentId, String defaultText, FontRenderer fontrendererObj, int x, int y, int par5Width, int par6Height) {
			super(componentId, fontrendererObj, x, y, par5Width, par6Height);
			this.setText(defaultText);
			this.defaultText = defaultText;
		}
		
		public GuiFilterTextField setTypedCharFilter(Predicate<Character> filter) {
			this.typedCharFilter = filter;
			return this;
		}
		public GuiFilterTextField setKeyCodeFilter(Predicate<Integer> keyCodeFilter) {
			this.keyCodeFilter = keyCodeFilter;
			return this;
		}
		
		@Override
		public boolean textboxKeyTyped(char typedChar, int keyCode) {
			boolean typedCharTest = this.typedCharFilter != null && this.typedCharFilter.test(typedChar);
			boolean keyCodeTest = this.keyCodeFilter != null && this.keyCodeFilter.test(keyCode);
			
			if(typedCharTest || keyCodeTest) {
				if(this.getText().isEmpty()) {
					this.setText(this.defaultText);
				}
				return super.textboxKeyTyped(typedChar, keyCode);
			}else {
				return false;
			}
		}
	}
}
