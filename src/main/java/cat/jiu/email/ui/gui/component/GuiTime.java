package cat.jiu.email.ui.gui.component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiTime extends AbstractWidget {
	protected final Font font;
	public final boolean isHorizontal;
	public boolean renderBackground = true;

	public final List<Time> times = new ArrayList<>();
	public final Time day, hour, minute, second, tick;
	
	public GuiTime(boolean isHorizontal) {
		this(isHorizontal, false, false, true, true, false);
	}
	public GuiTime(boolean isHorizontal, boolean addDay, boolean addHour, boolean addMinute, boolean addSecond, boolean addTick) {
		super(0, 0, 0, 0, CommonComponents.EMPTY);
		this.font = Minecraft.getInstance().font;
		this.isHorizontal = isHorizontal;

		int width = this.font.width("8") * 5 + 2;
		this.day = addDay ? this.createEditbox(Component.translatable("inbox.config.time.day"), width) : null;
		width = this.font.width("8") * 4 + 5;
		this.hour = addHour ? this.createEditbox(Component.translatable("inbox.config.time.hour"), width) : null;
		this.minute = addMinute ? this.createEditbox(Component.translatable("inbox.config.time.minute"), width) : null;
		this.second = addSecond ? this.createEditbox(Component.translatable("inbox.config.time.second"), width) : null;
		this.tick = addTick ? this.createEditbox(Component.translatable("inbox.config.time.tick"), width) : null;
	}

	public Time createEditbox(Component name, int width) {
		GuiFilterTextField field = new GuiFilterTextField("0", false, 0, 0, width, this.font.lineHeight + 1);
		field.setBordered(false);
		field.setMaxLength(4);
		Time time = new Time(name, field);
		this.times.add(time);
		return time;
	}
	public static class Time {
		public final Component name;
		public final GuiFilterTextField field;
		public Time(Component name, GuiFilterTextField field) {
			this.name = name;
			this.field = field;
		}
	}

	public GuiTime setTextFieldWidth(int width) {
		for (Time time : this.times) {
			time.field.setWidth(width);
		}
		return this;
	}

	public GuiTime setRenderPos(int x, int y) {
		this.setX(x);
		this.setY(y);
		return this;
	}
	public GuiTime setRenderBackgroubd(boolean render) {
		this.renderBackground = render;
		return this;
	}

	@Override
	public int getWidth() {
		int width = 0;
		if (this.isHorizontal) {
			width = this.times.size() * (this.times.get(0).field.getWidth() + 4);
			for (Time time : this.times) {
				width += this.font.width(time.name);
			}
		}else {
			for (int i = 0; i < this.times.size(); i++) {
				width = Math.max(width, this.font.width(this.times.get(i).name) + this.times.get(i).field.getWidth());
			}
		}
		return width;
	}

	@Override
	public int getHeight() {
		int height;
		if (this.isHorizontal) {
			height = this.times.get(0).field.getHeight();
		}else {
			height = this.times.get(0).field.getHeight() * this.times.size();
		}
		return height;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int x = this.getX();
		int y = this.getY();

		int width = 0;
		for (Time time : times) {
			width = Math.max(width, this.font.width(time.name));
		}
		int weightWidth = this.times.get(0).field.getWidth(),
			weightHeight = this.times.get(0).field.getHeight();

		if(this.isHorizontal) {
			x+=2;
			for (int i = 0; i < this.times.size(); i++) {
				Time time = this.times.get(i);
				time.field.setX(x);
				time.field.setY(y);
				time.field.renderWidget(graphics, x, y, partialTick);
				RenderUtils.hLine(graphics, time.field.getX(), time.field.getY() + time.field.getHeight() - 1, time.field.getWidth() - 4, RenderUtils.rgb(128, 128, 128, 150));
				x += time.field.getWidth();

				RenderUtils.drawComponent(graphics, time.name, x+1, y, Color.GREEN.getRGB(), true);
				x += this.font.width(time.name) + 5;
			}
		}else {
			if (this.renderBackground){
				graphics.fill(x - 3, y - 3, x + weightWidth - 3 + width + 2 + 5, y + (weightHeight + 3) * this.times.size() + 2, Color.BLACK.getRGB());
				graphics.fill(x - 4, y - 3, x + weightWidth - 3 + width + 3 + 5, y + (weightHeight + 3) * this.times.size() + 1, Color.BLACK.getRGB());
				graphics.fill(x - 3, y - 4, x + weightWidth - 3 + width + 2 + 5, y + (weightHeight + 3) * this.times.size(), Color.BLACK.getRGB());

				RenderUtils.hLine(graphics, x - 2, y - 3, weightWidth + width + 4, 1347420415);
				RenderUtils.hLine(graphics, x - 2, y + (weightHeight + 3) * this.times.size(), weightWidth + width + 4, 1347420415);

				RenderUtils.vLine(graphics, x - 3, y - 3, (weightHeight + 3) * this.times.size() + 3, 1347420415);
				RenderUtils.vLine(graphics, x + weightWidth + 3 + width, y - 3, (weightHeight + 3) * this.times.size() + 3, 1347420415);
			}
			x++;

			for (int i = 0; i < this.times.size(); i++) {
				Time time = this.times.get(i);
				time.field.setX(x);
				time.field.setY(y);
				time.field.renderWidget(graphics, x, y, partialTick);
				RenderUtils.hLine(graphics, time.field.getX(), time.field.getY() + time.field.getHeight() - 1, time.field.getWidth() - 4, RenderUtils.rgb(128, 128, 128, 150));
				RenderUtils.drawComponent(graphics, time.name, x + weightWidth-2, y, Color.GREEN.getRGB(), true);
				y += time.field.getHeight() + 3;
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		for (Time time : this.times) {
			time.field.setFocused(false);
		}
		for (Time time : this.times) {
			if(time.field.mouseClicked(mouseX, mouseY, mouseButton)){
				time.field.setFocused(true);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		for (Time time : this.times) {
			if(time.field.charTyped(typedChar, keyCode)){
				return true;
			}
		}
		return super.charTyped(typedChar, keyCode);
	}
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		for (Time time : this.times) {
			if(time.field.isFocused() && time.field.keyPressed(keyCode, scanCode, modifiers)){
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
		for (Time time : this.times) {
			if(time.field.isFocused() && time.field.keyReleased(pKeyCode, pScanCode, pModifiers)){
				return true;
			}
		}
		return super.keyReleased(pKeyCode, pScanCode, pModifiers);
	}

	@Override
	public void setFocused(boolean pFocused) {}

	@Override
	public boolean isFocused() {
		for (Time time : this.times) {
			if(time.field.isFocused()){
				return true;
			}
		}
		return false;
	}

	public boolean isEnable() {
		return this.visible;
	}
	public GuiTime setEnable(boolean isEnable) {
		this.visible = isEnable;
		this.times.forEach(time -> time.field.setVisible(isEnable));
		return this;
	}

	public int get(int index) {
		try {
			return Integer.parseInt(this.times.get(index).field.getValue());
		}catch(NumberFormatException e) {
			return 0;
		}
	}
	
	public int getDay() {
		return this.day != null ? this.day.field.getAsNumber().intValue() : 0;
	}

	public int getHour() {
		return this.hour != null ? this.hour.field.getAsNumber().intValue() : 0;
	}

	public int getMinute() {
		return this.minute != null ? this.minute.field.getAsNumber().intValue() : 0;
	}

	public int getSecond() {
		return this.second != null ? this.second.field.getAsNumber().intValue() : 0;
	}

	public int getTick() {
		return this.tick != null ? this.tick.field.getAsNumber().intValue() : 0;
	}

	public long getTimeOfMillis() {
		return EmailUtils.parseMillis(this.getDay(), this.getHour(), this.getMinute(), this.getSecond(), this.getTick(), 0);
	}

	public long getTimeOfTicks() {
		return this.getTimeOfMillis() / 50;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput output) {

	}
}
