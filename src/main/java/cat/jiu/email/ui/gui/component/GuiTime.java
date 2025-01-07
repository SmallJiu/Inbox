package cat.jiu.email.ui.gui.component;

import java.awt.Color;
import java.util.List;
import java.util.function.Predicate;

import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.util.EmailUtils;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public class GuiTime implements IGuiEventListener, IRenderable {
	protected final Screen parent;
	private boolean isEnable = false;
	protected final FontRenderer font;
	private final boolean isHorizontal;
	private final List<TextFieldWidget> fields = Lists.newArrayList();
	
	public GuiTime(Screen parent, boolean isHorizontal) {
		this.font = Minecraft.getInstance().fontRenderer;
		this.parent = parent;
		this.isHorizontal = isHorizontal;
		
		Predicate<Character> charFilter = typedChar ->
			"0123456789".contains(String.valueOf(typedChar));
		
		int width = this.font.getStringWidth("8") * 3 + 7;
		for (int i = 0; i < 6; i++) {
			GuiFilterTextField field = new GuiFilterTextField("0", this.font, 0, 0, width, this.font.FONT_HEIGHT + 1).setTypedCharFilter(charFilter);
			field.setEnableBackgroundDrawing(false);
			field.setMaxStringLength(4);
			this.fields.add(field);
		}
	}
	public GuiTime setTextFieldWidth(int width) {
		for (TextFieldWidget field : this.fields) {
			field.setWidth(width);
		}
		return this;
	}

	@Override
	public void render(MatrixStack stack, int x, int y, float p_230430_4_) {
		if(!this.isEnable) return;
		final String[] times = {
				I18n.format("inbox.config.time.day"),
				I18n.format("inbox.config.time.hour"),
				I18n.format("inbox.config.time.minute"),
				I18n.format("inbox.config.time.second"),
				I18n.format("inbox.config.time.tick"),
				I18n.format("inbox.config.time.millis")
		};

		int width = 0;
		for (String time : times) {
			width = Math.max(width, this.font.getStringWidth(time));
		}
		int weightWidth = this.fields.get(0).getWidth(),
			weightHeight = this.fields.get(0).getHeight();

		if(this.isHorizontal) {
			RenderUtils.fill(stack, x - 2, y - 3, x + weightWidth - 3 + width + 2 + 5, y + weightHeight + 3 - 1, Color.BLACK.getRGB());
			RenderUtils.fill(stack, x - 4, y - 3, x + weightWidth - 3 + width + 3 + 5, y + weightHeight + 3 - 1, Color.BLACK.getRGB());
			RenderUtils.fill(stack, x - 2, y - 4, x + weightWidth - 3 + width + 2 + 5, y + weightHeight + 3, Color.BLACK.getRGB());

			RenderUtils.hLine(stack, x - 2, x + weightWidth - 3 + width + 5, y - 2, 1347420415);
			RenderUtils.hLine(stack, x - 2, x + weightWidth - 3 + width + 5, y + weightHeight + 3 - 3, 1347420415);

			RenderUtils.vLine(stack, x-2, y - 2, y + weightHeight + 3 - 3, 1347420415);
			RenderUtils.vLine(stack, x + weightWidth - 3 + width + 5, y - 2, y + weightHeight + 3 - 3, 1347420415);
			x+=2;

			for (int i = 0; i < this.fields.size(); i++) {
				TextFieldWidget field = this.fields.get(i);
				field.setX(x);
				field.y = y;
				field.renderWidget(stack, x, y, p_230430_4_);
				x += field.getWidth();

				RenderUtils.drawString(stack, times[i]+",", x, y, Color.GREEN.getRGB(), true);
				x += this.font.getStringWidth(times[i]) + 5;
			}
		}else {
			RenderUtils.square(stack, x-2, y-3, width + weightWidth + 5, (weightHeight+3) *times.length, Color.BLACK.getRGB(), 1347420415);
//			RenderUtils.fill(stack, x - 2, y - 1, weightWidth - 3 + width + 2 + 5, (weightHeight + 3) * 6 - 1);
//			RenderUtils.fill(stack, x - 4, y - 3, weightWidth - 3 + width + 3 + 5, (weightHeight + 3) * 6 - 1, Color.BLACK.getRGB());
//			RenderUtils.fill(stack, x - 2, y - 4, weightWidth - 3 + width + 2 + 5, (weightHeight + 3) * 6, Color.BLACK.getRGB());

//			RenderUtils.hLine(stack, x - 2, x + weightWidth - 3 + width + 5, y - 2);
//			RenderUtils.hLine(stack, x - 2, x + weightWidth - 3 + width + 5, (weightHeight + 3) * 6 - 3, 1347420415);

//			RenderUtils.vLine(stack, x - 3, y - 2, y + (weightHeight + 3) * 6 - 3, 1347420415);
//			RenderUtils.vLine(stack, x + weightWidth + 3 + width, y - 2, y + (weightHeight + 3) * 6 - 3, 1347420415);
			x++;

			for (int i = 0; i < this.fields.size(); i++) {
				TextFieldWidget field = this.fields.get(i);
				field.setX(x);
				field.y = y;
				field.renderWidget(stack, x, y, p_230430_4_);
				RenderUtils.drawString(stack, times[i], x + weightWidth, y, Color.GREEN.getRGB(), true);
				y += field.getHeight() + 3;
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if(this.isEnable){
			for (TextFieldWidget field : this.fields) {
				if(field.mouseClicked(mouseX, mouseY, mouseButton)){
					field.setFocused2(true);
					return true;
				}
				field.setFocused2(false);
			}
		}
		return false;
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
		return false;
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
		return false;
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
}
