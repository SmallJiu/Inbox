package cat.jiu.email.ui.gui.component;

import java.awt.Color;

import cat.jiu.email.ui.gui.GuiEmailMain;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

@OnlyIn(Dist.CLIENT)
public class GuiPopupMenu extends Screen {
	private boolean visible = false;
	private final FontRenderer font = Minecraft.getInstance().fontRenderer;

	public GuiPopupMenu() {
		super(ITextComponent.getTextComponentOrEmpty(null));
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		
		int btnX = this.createX + 10;
		int btnY = this.createY;
		int width = 0;
		int height = Minecraft.getInstance().fontRenderer.FONT_HEIGHT + 3;
		for(Widget btn : this.buttons) {
			width = Math.max(width, this.font.getStringWidth(btn.getMessage().getString()) + 6);
		}

		for(Widget btn : this.buttons) {
			btn.visible = visible;
			if(visible) {
				btn.setWidth(width);
				btn.setHeight(height);
				btn.x = btnX - btn.getWidth()/2;
				btn.y = btnY + Minecraft.getInstance().fontRenderer.FONT_HEIGHT;
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

	public void drawPopupMenu(MatrixStack matrix, long popupMenuCurrentEmail, Minecraft mc, int x, int y, float partialTicks) {
		this.font.drawString(matrix, String.valueOf(this.visible), this.createX, this.createY, Color.RED.getRGB());
		if(this.visible) {
			if(popupMenuCurrentEmail >= 0) {
				mc.getTextureManager().bindTexture(GuiEmailMain.BackGround);
				this.blit(matrix, this.createX - 2 + 6, this.createY - 1, 4, 15, 12, 10);
				this.font.drawStringWithShadow(matrix, String.valueOf(popupMenuCurrentEmail), this.createX - 2 + 12, this.createY + 1, Color.RED.getRGB());
			}
			
			int maxHeight = 0;
			for(Widget btn : this.buttons) {
				maxHeight += btn.getHeight();
			}
			
			if(this.createY + maxHeight + 8 >= this.height) {
				this.createY = this.height - maxHeight - Minecraft.getInstance().fontRenderer.FONT_HEIGHT - 5;
				this.setVisible(visible);
            }

			for(Widget btn : this.buttons) {
				btn.render(matrix, x, y, partialTicks);
			}

//			Widget btn = this.buttons.get(this.buttons.size()-1);
//			this.font.draw(matrix, btn.x, btn.x + btn.getWidth() - 2, btn.y + btn.getHeight(), Color.BLACK.getRGB());
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		boolean flag = false;
		if(mouseButton == 0 && this.isVisible()) {
			for(Widget btn : this.buttons) {
				if(btn.mouseClicked(mouseX, mouseY, mouseButton)) {
					GuiScreenEvent.MouseClickedEvent.Pre event = new GuiScreenEvent.MouseClickedEvent.Pre(this, mouseX, mouseY, mouseButton);
                    if (MinecraftForge.EVENT_BUS.post(event)) break;
                    flag = true;
                    MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.MouseClickedEvent.Post(this, mouseX, mouseY, mouseButton));
					break;
				}
			}
		}
		if(flag) this.setVisible(false);
		return flag;
	}
	public <T extends Button> T addPopupButton(T buttonIn) {
		return this.addButton(buttonIn);
	}
	public Widget getPopupButton(int id) {
		return this.buttons.get(id);
	}
	public int getButtonSize() {
		return this.buttons.size();
	}
}
