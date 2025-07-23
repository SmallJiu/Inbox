package cat.jiu.email.ui.gui.component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiPopupMenu extends AbstractWidget {
	protected final Font font = Minecraft.getInstance().font;
	protected final List<Button> buttons = Lists.newArrayList();
	protected boolean visible, resetWidth, resetHeight;
	protected int showCount = 0;


	public GuiPopupMenu() {
		super(0, 0, 0, 0, Component.empty());
	}

	@Override
	protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if(this.visible) {
			for(Button btn : this.buttons) {
				btn.render(graphics, mouseX, mouseY, partialTick);
			}
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		boolean flag = false;
		if(mouseButton == 0 && this.isVisible()) {
			for(Button btn : this.buttons) {
				if(btn.isMouseOver(mouseX, mouseY)) {
					flag = btn.mouseClicked(mouseX, mouseY, mouseButton);
					break;
				}
			}
		}
		if(flag) this.setVisible(false);
		return flag;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;

		int btnX = this.getX();
		int btnY = this.getY();
		int height = Minecraft.getInstance().font.lineHeight + 3;
		int width = 0;
		for(Button btn : this.buttons) {
			width = Math.max(width, this.font.width(btn.getMessage().getString()) + 6);
		}

		for(Button btn : this.buttons) {
			btn.visible = visible;
			if(visible) {
				if (this.resetWidth) {
					btn.setWidth(width);
				}
				if (this.resetHeight) {
					btn.setHeight(height);
				}
				btn.setX(btnX);
				btn.setY(btnY);
				btnY += btn.getHeight()+2;
			}
		}
	}
	public boolean isVisible() {
		return visible;
	}

	public void setResetButtonSize(boolean resetWidth, boolean resetHeight) {
		this.resetWidth = resetWidth;
		this.resetHeight = resetHeight;
	}

	@Override
	public int getHeight() {
		int height = 0;
		for(Button btn : this.buttons) {
			height += btn.getHeight();
		}
		return height + 5;
	}

	@Override
	public int getWidth() {
		int width = 0;
		for(Button btn : this.buttons) {
			width = Math.max(width, btn.getWidth());
		}
		return width;
	}

	public <T extends Button> T addPopupButton(T buttonIn) {
		int maxHeight = 0;
		for(Button btn : this.buttons) {
			maxHeight += btn.getHeight();
		}
		maxHeight += buttonIn.getHeight();
		this.height = maxHeight + this.font.lineHeight;
		this.buttons.add(buttonIn);
		return buttonIn;
	}
	public Button getPopupButton(int id) {
		return this.buttons.get(id);
	}
	public int getButtonSize() {
		return this.buttons.size();
	}
	public void clearPopupButtons(){
		this.buttons.clear();
	}

	@Override
	protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
}
