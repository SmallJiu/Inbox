package cat.jiu.email.ui.gui.component;

import java.awt.Color;
import java.util.List;

import cat.jiu.email.ui.gui.GuiEmailMain;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.compress.utils.Lists;

@OnlyIn(Dist.CLIENT)
public class GuiPopupMenu extends Screen {
	private boolean visible = false;
	private final Font font = Minecraft.getInstance().font;
	private final List<Button> buttons = Lists.newArrayList();

	public GuiPopupMenu() {
		super(Component.nullToEmpty(null));
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		
		int btnX = this.createX + 10;
		int btnY = this.createY;
		int width = 0;
		int height = Minecraft.getInstance().font.lineHeight + 3;
		for(Button btn : this.buttons) {
			width = Math.max(width, this.font.width(btn.getMessage().getString()) + 6);
		}

		for(Button btn : this.buttons) {
			btn.visible = visible;
			if(visible) {
				btn.setWidth(width);
				btn.setHeight(height);
				btn.setX(btnX - btn.getWidth()/2);
				btn.setY(btnY + Minecraft.getInstance().font.lineHeight);
				btnY += btn.getHeight();
			}
		}
	}
	
	public boolean isVisible() {
		return visible;
	}
	
	int createX = 0, createY = 0;
	public void setCreatePoint(double createX, double createY) {
		this.createX = (int) createX;
		this.createY = (int) createY;
	}
	public void setCreatePoint(int createX, int createY) {
		this.createX = createX;
		this.createY = createY;
	}

	public void drawPopupMenu(GuiGraphics graphics, long popupMenuCurrentEmail, Minecraft mc, int x, int y, float partialTicks) {
//		this.font.drawString(matrix, String.valueOf(this.visible), this.createX, this.createY, Color.RED.getRGB());
		if(this.visible) {
			if(popupMenuCurrentEmail >= 0) {
				graphics.blit(GuiEmailMain.BackGround, this.createX - 2 + 6, this.createY - 2, 4, 15, 12, 10);

				graphics.drawString(mc.font, String.valueOf(popupMenuCurrentEmail), this.createX - 2 + 12 - this.font.width(String.valueOf(popupMenuCurrentEmail))/2f, this.createY - 1, Color.RED.getRGB(), false);
			}

			for(Button btn : this.buttons) {
				btn.render(graphics, x, y, partialTicks);
				if(btn.isMouseOver(x,y)){
					graphics.hLine(btn.getX(), btn.getX() + btn.getWidth() - 2, btn.getY() + btn.getHeight() - 1, Color.WHITE.getRGB());
				}
			}

			Button btn = this.buttons.get(this.buttons.size()-1);
			graphics.hLine(btn.getX(), btn.getX() + btn.getWidth() - 2, btn.getY() + btn.getHeight(), (btn.isMouseOver(x,y) ? Color.WHITE : Color.BLACK).getRGB());
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		boolean flag = false;
		if(mouseButton == 0 && this.isVisible()) {
			for(Button btn : this.buttons) {
				if(btn.isMouseOver(mouseX, mouseY)) {
					ScreenEvent.MouseButtonReleased.Pre event = new ScreenEvent.MouseButtonReleased.Pre(this, mouseX, mouseY, mouseButton);
                    if (MinecraftForge.EVENT_BUS.post(event)) break;
                    flag = btn.mouseClicked(mouseX, mouseY, mouseButton);
                    MinecraftForge.EVENT_BUS.post(new ScreenEvent.MouseButtonReleased.Post(this, mouseX, mouseY, mouseButton, false));
					break;
				}
			}
		}
		if(flag) this.setVisible(false);
		return flag;
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
}
