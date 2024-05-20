package cat.jiu.email.ui.gui.component;

import java.awt.Color;
import java.util.List;

import cat.jiu.email.ui.gui.GuiEmailMain;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
				btn.x = btnX - btn.getWidth()/2;
				btn.y = btnY + Minecraft.getInstance().font.lineHeight;
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

	public void drawPopupMenu(PoseStack stack, long popupMenuCurrentEmail, Minecraft mc, int x, int y, float partialTicks) {
//		this.font.drawString(matrix, String.valueOf(this.visible), this.createX, this.createY, Color.RED.getRGB());
		if(this.visible) {
			if(popupMenuCurrentEmail >= 0) {
				Minecraft.getInstance().getTextureManager().bindForSetup(GuiEmailMain.BackGround);
				blit(stack, this.createX - 2 + 6, this.createY - 2, 4, 15, 12, 10);

				drawString(stack, mc.font, String.valueOf(popupMenuCurrentEmail), (int) (this.createX - 2 + 12 - this.font.width(String.valueOf(popupMenuCurrentEmail))/2f), this.createY - 1, Color.RED.getRGB());
			}

			for(Button btn : this.buttons) {
				btn.render(stack, x, y, partialTicks);
				if(btn.isMouseOver(x,y)){
					hLine(stack, btn.x, btn.x + btn.getWidth() - 2, btn.y + btn.getHeight() - 1, Color.WHITE.getRGB());
				}
			}

			Button btn = this.buttons.get(this.buttons.size()-1);
			hLine(stack, btn.x, btn.x + btn.getWidth() - 2, btn.y + btn.getHeight(), (btn.isMouseOver(x,y) ? Color.WHITE : Color.BLACK).getRGB());
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
