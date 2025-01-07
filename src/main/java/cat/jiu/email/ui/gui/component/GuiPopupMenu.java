package cat.jiu.email.ui.gui.component;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiPopupMenu extends Widget {
	protected final FontRenderer font = Minecraft.getInstance().fontRenderer;
	protected final List<Button> buttons = Lists.newArrayList();
	protected boolean visible, resetWidth, resetHeight;
	protected int showCount = 0;

	public GuiPopupMenu() {
		super(0, 0, 0, 0, ITextComponent.getTextComponentOrEmpty(null));
	}

	@Override
	public void renderWidget(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
		if(this.visible) {
			for(Button btn : this.buttons) {
				btn.render(stack, mouseX, mouseY, partialTicks);
			}
		}
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
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

		int btnX = this.x;
		int btnY = this.y;
		int height = this.font.FONT_HEIGHT + 3;
		int width = 0;
		for(Button btn : this.buttons) {
			width = Math.max(width, this.font.getStringWidth(btn.getMessage().getString()) + 6);
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
				btn.x = btnX;
				btn.y = btnY;
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
		this.height = maxHeight + this.font.FONT_HEIGHT;
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

}
