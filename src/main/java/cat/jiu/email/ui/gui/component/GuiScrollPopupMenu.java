package cat.jiu.email.ui.gui.component;

import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiScrollPopupMenu extends SubWidget {
	protected final Font font = Minecraft.getInstance().font;
	protected final List<Button> buttons = Lists.newArrayList();
	protected boolean visible, resetWidth, resetHeight;
	protected int showCount = 0;
	protected ButtonPanel panel;

	public GuiScrollPopupMenu() {
		super(0, 0, 0, 0, Component.empty());
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
	protected <T extends GuiEventListener & NarratableEntry> T addWidget(T pListener) {
		if (pListener instanceof Button buttonIn) {
			int maxHeight = 0;
			for(Button btn : this.buttons) {
				maxHeight += btn.getHeight();
			}
			maxHeight += buttonIn.getHeight();
			this.height = maxHeight + this.font.lineHeight;
			this.buttons.add(buttonIn);
		}
		return super.addWidget(pListener);
	}

	public class ButtonPanel extends ScrollPanel {
		final GuiScrollPopupMenu parent;
		protected int x, y;
		public ButtonPanel(GuiScrollPopupMenu parent) {
			super(Minecraft.getInstance(), 0, 0, 0, 0);
			this.parent = parent;
		}

		@Override
		protected int getContentHeight() {
			int height = 0;
			for (Button button : this.parent.buttons) {
				height += button.getHeight();
			}
			return height;
		}

		@Override
		protected void drawPanel(GuiGraphics graphics, int x, int y, Tesselator tess, int mouseX, int mouseY) {
			x = this.x;
			y = this.y + this.border - (int)this.scrollDistance;
			for (Button button : this.parent.buttons) {
				button.setX(x);
				button.setY(y);
				button.render(graphics, mouseX, mouseY, 0);
				y += button.getHeight();
			}
		}

		@Override
		public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
			for (GuiEventListener child : this.parent.buttons) {
				if (child.mouseClicked(pMouseX, pMouseY, pButton)) return true;
			}
			return false;
		}

		@Override
		public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
			for (GuiEventListener child : this.parent.buttons) {
				if (child.mouseReleased(pMouseX, pMouseY, pButton)) return true;
			}
			return false;
		}

		@Override
		public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
			for (GuiEventListener child : this.parent.buttons) {
				if (child.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)) return true;
			}
			return false;
		}

		@Override
		public boolean isMouseOver(double pMouseX, double pMouseY) {
			for (GuiEventListener child : this.parent.buttons) {
				if (child.isMouseOver(pMouseX, pMouseY)) return true;
			}
			return false;
		}

		@Override
		public boolean isFocused() {
			for (GuiEventListener child : this.parent.buttons) {
				if (child.isFocused()) return true;
			}
			return false;
		}

		@Override
		public void mouseMoved(double pMouseX, double pMouseY) {
			for (GuiEventListener child : this.parent.buttons) {
				child.mouseMoved(pMouseX, pMouseY);
			}
		}

		@Override
		public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
			for (GuiEventListener child : this.parent.buttons) {
				if (child.mouseScrolled(pMouseX, pMouseY, pDelta)) return true;
			}
			return false;
		}

		public void setPos(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public NarrationPriority narrationPriority() {return NarrationPriority.NONE;}
		@Override
		public void updateNarration(NarrationElementOutput pNarrationElementOutput) {}
	}
}
