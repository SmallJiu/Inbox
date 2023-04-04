package cat.jiu.email.ui.gui;

import java.awt.Color;

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
	private boolean visible = false;
	public void setVisible(boolean visible) {
		this.visible = visible;
		
		int btnX = this.createX + 10;
		int btnY = this.createY;
		for(int i = 0; i < buttonList.size(); i++) {
			GuiButton btn = this.buttonList.get(i);
			btn.visible = visible;
			if(visible) {
				btn.width = Minecraft.getMinecraft().fontRenderer.getStringWidth(btn.displayString) + 6;
				btn.x = btnX - btn.width/2;
				btn.y = btnY + Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
				btnY += btn.height;
			}
		}
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
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				mc.getTextureManager().bindTexture(GuiEmailMain.BackGround);
				this.drawTexturedModalRect(this.createX - 2 + 6, this.createY - 1, 4, 15, 12, 10);
				this.drawCenteredString(mc.fontRenderer, String.valueOf(popupMenuCurrentEmail), this.createX - 2 + 12, this.createY + 1, Color.RED.getRGB());
				GlStateManager.popMatrix();
			}
			
			for(int i = 0; i < this.buttonList.size(); i++) {
				GuiButton btn = this.buttonList.get(i);
				btn.drawButton(mc, x, y, partialTicks);
			}
			
			GuiButton btn = this.buttonList.get(this.buttonList.size()-1);
			this.drawHorizontalLine(btn.x, btn.x + btn.width - 2, btn.y + btn.height, Color.BLACK.getRGB());
		}
	}
	
	public boolean mouseClicked(Minecraft mc, int mouseX, int mouseY, int mouseButton) {
		boolean flag = false;
		if(mouseButton == 0) {
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
	public <T extends GuiButton> T addPopupButton(T buttonIn) {
		return this.addButton(buttonIn);
	}
}
