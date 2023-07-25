package cat.jiu.email.ui.gui.component;

import java.awt.Color;
import java.util.List;
import java.util.function.Predicate;

import cat.jiu.email.util.EmailUtils;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class GuiTime extends Screen {
	protected final Screen parent;
	private boolean isEnable = false;
	protected final FontRenderer fontRenderer;
	private final boolean isHorizontal;
	private final List<TextFieldWidget> fields = Lists.newArrayList();
	
	public GuiTime(Screen parent, boolean isHorizontal) {
		super(ITextComponent.getTextComponentOrEmpty(null));
		this.fontRenderer = Minecraft.getInstance().fontRenderer;
		this.parent = parent;
		this.isHorizontal = isHorizontal;
		
		Predicate<Character> charFilter = typedChar ->
			"0123456789".contains(String.valueOf(typedChar));
		
		Predicate<Integer> keyFilter = keyCode -> {
			switch(keyCode) {
				case GLFW.GLFW_GAMEPAD_BUTTON_BACK:
				case GLFW.GLFW_KEY_LEFT:
				case GLFW.GLFW_KEY_RIGHT:
				case GLFW.GLFW_KEY_DELETE:
				case GLFW.GLFW_KEY_INSERT:
				case GLFW.GLFW_KEY_HOME:
				case GLFW.GLFW_KEY_END:
					return true;
				default:
					return false;
			}
		};
		
		int width = this.fontRenderer.getStringWidth("8") * 3 + 7;
		for (int i = 0; i < 6; i++) {
			GuiFilterTextField field = new GuiFilterTextField("0", this.fontRenderer, 0, 0, width, this.fontRenderer.FONT_HEIGHT + 1).setTypedCharFilter(charFilter).setKeyCodeFilter(keyFilter);
			field.setEnableBackgroundDrawing(false);
			field.setMaxStringLength(4);
			this.fields.add(this.addListener(field));
		}
	}

	@Override
	public void render(MatrixStack matrix, int x, int y, float p_230430_4_) {
		if(!this.isEnable) return;

		final String[] times = {
				I18n.format("email.config.time.day"),
				I18n.format("email.config.time.hour"),
				I18n.format("email.config.time.minute"),
				I18n.format("email.config.time.second"),
				I18n.format("email.config.time.tick"),
				I18n.format("email.config.time.millis")
		};

		int width = 0;
		for (String time : times) {
			width = Math.max(width, this.fontRenderer.getStringWidth(time));
		}
		int weightWidth = this.fields.get(0).getWidth(),
			weightHeight = this.fields.get(0).getHeight();

		if(this.isHorizontal) {
			fill(matrix, x - 2, y - 3, x + weightWidth - 3 + width + 2 + 5, y + weightHeight + 3 - 1, Color.BLACK.getRGB());
			fill(matrix, x - 4, y - 3, x + weightWidth - 3 + width + 3 + 5, y + weightHeight + 3 - 1, Color.BLACK.getRGB());
			fill(matrix, x - 2, y - 4, x + weightWidth - 3 + width + 2 + 5, y + weightHeight + 3, Color.BLACK.getRGB());
			
			this.hLine(matrix, x - 2, x + weightWidth - 3 + width + 5, y - 2, 1347420415);
			this.hLine(matrix, x - 2, x + weightWidth - 3 + width + 5, y + weightHeight + 3 - 3, 1347420415);
			
			this.vLine(matrix, x-2, y - 2, y + weightHeight + 3 - 3, 1347420415);
			this.vLine(matrix, x + weightWidth - 3 + width + 5, y - 2, y + weightHeight + 3 - 3, 1347420415);
			x+=2;

			for (int i = 0; i < this.fields.size(); i++) {
				TextFieldWidget field = this.fields.get(i);
				field.x = x;
				field.y = y;
				field.renderWidget(matrix, x, y, p_230430_4_);
				x += field.getWidth();

				this.fontRenderer.drawStringWithShadow(matrix, times[i]+",", x, y, Color.GREEN.getRGB());
				x += this.fontRenderer.getStringWidth(times[i]) + 5;
			}
		}else {
			fill(matrix, x - 2, y - 3, x + weightWidth - 3 + width + 2 + 5, y + (weightHeight + 3) * 6 - 1, Color.BLACK.getRGB());
			fill(matrix, x - 4, y - 3, x + weightWidth - 3 + width + 3 + 5, y + (weightHeight + 3) * 6 - 1, Color.BLACK.getRGB());
			fill(matrix, x - 2, y - 4, x + weightWidth - 3 + width + 2 + 5, y + (weightHeight + 3) * 6, Color.BLACK.getRGB());
			
			this.hLine(matrix, x - 2, x + weightWidth - 3 + width + 5, y - 2, 1347420415);
			this.hLine(matrix, x - 2, x + weightWidth - 3 + width + 5, y + (weightHeight + 3) * 6 - 3, 1347420415);
			
			this.vLine(matrix, x - 3, y - 2, y + (weightHeight + 3) * 6 - 3, 1347420415);
			this.vLine(matrix, x + weightWidth + 3 + width, y - 2, y + (weightHeight + 3) * 6 - 3, 1347420415);
			x++;

			for (int i = 0; i < this.fields.size(); i++) {
				TextFieldWidget field = this.fields.get(i);
				field.x = x;
				field.y = y;
				field.renderWidget(matrix, x, y, p_230430_4_);
				this.fontRenderer.drawStringWithShadow(matrix, times[i], x + weightWidth, y, Color.GREEN.getRGB());
				y += field.getHeight() + 3;
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if(this.isEnable){
			for (TextFieldWidget field : this.fields) {
				if(field.mouseClicked(mouseX, mouseY, mouseButton)){
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		if(this.isEnable) {
			for (TextFieldWidget field : this.fields) {
				if(field.isFocused() && field.charTyped(typedChar, keyCode)){
					return true;
				}
			}
		}
		return super.charTyped(typedChar, keyCode);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(this.isEnable){
			for (TextFieldWidget field : this.fields) {
				if(field.isFocused() && field.keyPressed(keyCode, scanCode, modifiers)){
					return true;
				}
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	public boolean isEnable() {
		return isEnable;
	}
	public GuiTime setEnable(boolean isEnable) {
		this.isEnable = isEnable;
		this.fields.forEach(field -> field.setVisible(isEnable));
		return this;
	}

	/**
	 * @param index
	 * 				0 = Day,<p>
	 * 				1 = Hour,<p>
	 * 				2 = Minute,<p>
	 * 				3 = Second,<p>
	 * 				4 = Tick,<p>
	 * 				5 = Millis
	 */
	public int get(int index) {
		try {
			return Integer.parseInt(this.fields.get(index).getText());
		}catch(NumberFormatException e) {
			return 0;
		}
	}
	
	public int getDay() {
		return this.get(0);
	}

	public int getHour() {
		return this.get(1);
	}

	public int getMinute() {
		return this.get(2);
	}

	public int getSecond() {
		return this.get(3);
	}

	public int getTick() {
		return this.get(4);
	}

	public int getMillis() {
		return this.get(5);
	}

	public long getTimeOfMillis() {
		return EmailUtils.parseMillis(this.getDay(), this.getHour(), this.getMinute(), this.getSecond(), this.getTick(), this.getMillis());
	}
	
	public long getTimeOfTicks() {
		return this.getTimeOfMillis() / 50;
	}

	@Override
	public void closeScreen() {
		this.parent.closeScreen();
	}

	@Nonnull
	@Override
	public Minecraft getMinecraft() {
		return this.parent.getMinecraft();
	}

	public static class GuiFilterTextField extends TextFieldWidget {
		private Predicate<Character> typedCharFilter;
		private Predicate<Integer> keyCodeFilter;
		private final String defaultText;
		public GuiFilterTextField(String defaultText, FontRenderer fontrenderer, int x, int y, int par5Width, int par6Height) {
			super(fontrenderer, x, y, par5Width, par6Height, ITextComponent.getTextComponentOrEmpty(null));
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
		public boolean charTyped(char typedChar, int keyCode) {
			boolean typedCharTest = this.typedCharFilter != null && this.typedCharFilter.test(typedChar);
			
			if(typedCharTest) {
				if(this.defaultText.equals(this.getText())) {
					this.setText("");
				}
				boolean flag = super.charTyped(typedChar, keyCode);
				
				if(this.getText().isEmpty()) {
					this.setText(this.defaultText);
				}
				return flag;
			}else {
				return false;
			}
		}

		@Override
		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			boolean keyCodeTest = this.keyCodeFilter != null && this.keyCodeFilter.test(keyCode);
			return keyCodeTest || super.keyPressed(keyCode, scanCode, modifiers);
		}
	}
}
