package cat.jiu.email.ui.gui.component;

import java.awt.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiImageButton extends GuiButton {
	protected static final Color HOVERED_COLOR = new Color(0, 255, 255, 85);
	protected final GuiScreen gui;
	protected final ResourceLocation background;
	protected int u, v = 0;
	protected final int uWidth, vHeight, tileWidth, tileHeight;
	protected int hoveredColor = HOVERED_COLOR.getRGB();
	
	public GuiImageButton(GuiScreen gui, int buttonId, int x, int y, int widthIn, int heightIn, String hoveringText, ResourceLocation background, int tileWidth, int tileHeight, int uWidth, int vHeight) {
		super(buttonId, x, y, widthIn, heightIn, hoveringText);
		this.gui = gui;
		this.background = background;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.uWidth = uWidth;
		this.vHeight = vHeight;
		this.width = this.uWidth;
		this.height = this.vHeight;
	}
	
	public GuiImageButton(GuiScreen gui, int buttonId, int x, int y, String hoveringText, ResourceLocation background, int tileWidth, int tileHeight, int uWidth, int vHeight) {
		super(buttonId, x, y, hoveringText);
		this.gui = gui;
		this.background = background;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.uWidth = uWidth;
		this.vHeight = vHeight;
		this.width = this.uWidth;
		this.height = this.vHeight;
	}
	
	public GuiImageButton(GuiScreen gui, int buttonId, int x, int y, int widthIn, int heightIn, String hoveringText, ResourceLocation background, int tileWidth, int tileHeight, int u, int v, int uWidth, int vHeight) {
		super(buttonId, x, y, widthIn, heightIn, hoveringText);
		this.gui = gui;
		this.background = background;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.u = u;
		this.v = v;
		this.uWidth = uWidth;
		this.vHeight = vHeight;
		this.width = this.uWidth;
		this.height = this.vHeight;
	}
	
	public GuiImageButton(GuiScreen gui, int buttonId, int x, int y, String hoveringText, ResourceLocation background, int tileWidth, int tileHeight, int u, int v, int uWidth, int vHeight) {
		super(buttonId, x, y, hoveringText);
		this.gui = gui;
		this.background = background;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.u = u;
		this.v = v;
		this.uWidth = uWidth;
		this.vHeight = vHeight;
		this.width = this.uWidth;
		this.height = this.vHeight;
	}
	
	public GuiImageButton setImageX(int u) {
		this.u = u;
		return this;
	}
	public GuiImageButton setImageY(int v) {
		this.v = v;
		return this;
	}
	public GuiImageButton setImageWidth(int drawWidth) {
		this.width = drawWidth;
		return this;
	}
	public GuiImageButton setImageHeight(int drawHeight) {
		this.height = drawHeight;
		return this;
	}
	
	public GuiImageButton setHoveredColor(int hoveredColor) {
		this.hoveredColor = hoveredColor;
		return this;
	}
	public GuiImageButton setHoveredColor(Color hoveredColor) {
		this.hoveredColor = hoveredColor.getRGB();
		return this;
	}
	
	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if(this.visible) {
			this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
            
			GlStateManager.pushMatrix();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			mc.getTextureManager().bindTexture(this.background);
            Gui.drawScaledCustomSizeModalRect(this.x, this.y, this.u, this.v, this.uWidth, this.vHeight, this.width, this.height, this.tileWidth, this.tileHeight);
			
			if(this.isMouseOver()) {
				this.drawGradientRect(this.x, this.y, this.x + this.width, this.y + this.height, this.hoveredColor, this.hoveredColor);
				if(this.displayString!=null) this.gui.drawHoveringText(this.displayString, mouseX, mouseY);
			}

			GlStateManager.popMatrix();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		}
	}
}
