package cat.jiu.email.ui.gui.component;

import java.awt.Color;

import cat.jiu.email.ui.gui.GuiEmailMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiPopupMenu extends GuiScreen {
	private boolean
			visible = false,
			resetBtnWeight = true;
	public void setVisible(boolean visible) {
		this.visible = visible;
		
		int btnX = this.createX + 10;
		int btnY = this.createY;
		int width = 0;
		for(GuiButton btn : buttonList) {
			width = Math.max(width, Minecraft.getMinecraft().fontRenderer.getStringWidth(btn.displayString) + 6);
		}

		for (GuiButton btn : buttonList) {
			btn.visible = visible;
			if (visible) {
				if (this.resetBtnWeight) {
					btn.width = width;
				}
				btn.height = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 3;
				btn.x = btnX - btn.width / 2;
				btn.y = btnY + Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
				btnY += btn.height;
			}
		}
	}

	public GuiPopupMenu setResetBtnWeight(boolean resetBtnWeight) {
		this.resetBtnWeight = resetBtnWeight;
		return this;
	}

	public boolean isVisible() {
		return visible;
	}
	
	int createX = 0;
	int createY = 0;
	public void setCreatePoint(int createX, int createY) {
		this.createX = createX;
		this.createY = createY;
	}
	
	public void drawPopupMenu(long popupMenuCurrentEmail, Minecraft mc, int x, int y, float partialTicks) {
		if(this.visible) {
			if(popupMenuCurrentEmail >= 0) {
				GlStateManager.pushMatrix();
				GlStateManager.pushAttrib();
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				mc.getTextureManager().bindTexture(GuiEmailMain.BackGround);
				this.drawTexturedModalRect(this.createX - 2 + 6, this.createY - 1, 4, 15, 12, 10);
				this.drawCenteredString(mc.fontRenderer, String.valueOf(popupMenuCurrentEmail), this.createX - 2 + 12, this.createY + 1, Color.RED.getRGB());
				GlStateManager.popAttrib();
				GlStateManager.popMatrix();
			}
			
			int maxHeight = 0;
			for(GuiButton btn : this.buttonList) {
				maxHeight += btn.height;
			}
			
			if(this.createY + maxHeight + 8 >= this.height) {
				this.createY = this.height - maxHeight - mc.fontRenderer.FONT_HEIGHT - 5;
				this.setVisible(visible);
            }
			
			for(GuiButton btn : this.buttonList) {
				btn.drawButton(mc, x, y, partialTicks);
			}
		}
	}
	
	public boolean mouseClicked(Minecraft mc, int mouseX, int mouseY, int mouseButton) {
		boolean flag = false;
		if(mouseButton == 0 && this.isVisible()) {
			for(GuiButton btn : this.buttonList) {
				if(btn.mousePressed(mc, mouseX, mouseY)) {
					GuiScreenEvent.ActionPerformedEvent.Pre event = new GuiScreenEvent.ActionPerformedEvent.Pre(this, btn, this.buttonList);
                    if (MinecraftForge.EVENT_BUS.post(event)) break;
                    btn = event.getButton();
                    btn.playPressSound(mc.getSoundHandler());
                    btn.mouseReleased(mouseX, mouseY);
                    flag = true;
                    MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.ActionPerformedEvent.Post(this, btn, this.buttonList));
				}
			}
		}
		if(flag) this.setVisible(false);
		return flag;
	}
	public void clearPopupButton(){
		this.buttonList.clear();
	}
	public <T extends GuiButton> T addPopupButton(T buttonIn) {
		return this.addButton(buttonIn);
	}
	public GuiButton getPopupButton(int id) {
		return this.buttonList.get(id);
	}
	public GuiButton removePopupButton(int index){
		return this.buttonList.remove(index);
	}
	public int getButtonSize() {
		return this.buttonList.size();
	}
}
