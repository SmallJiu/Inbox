package cat.jiu.email.ui.gui.component;

import java.awt.Color;
import java.util.function.Supplier;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiImageButton extends Button {
	protected static final Color HOVERED_COLOR = new Color(0, 255, 255, 77);
	protected final Screen gui;
	protected final ResourceLocation background;
	protected int u, v = 0;
	protected final int uWidth, uHeight, tileWidth, tileHeight;
	protected int hoveredColor = HOVERED_COLOR.getRGB();
	protected Supplier<ITextComponent> hoveringText = super::getMessage;

	/**
	 *
	 * @param widthIn 绘制出来的宽
	 * @param heightIn 绘制出来的高
	 * @param background 图片
	 * @param tileWidth 图片整体宽
	 * @param tileHeight 图片整体高
	 * @param uWidth 图片内需要绘制的宽
	 * @param uHeight 图片内需要绘制的高
	 */
	public GuiImageButton(Screen gui, int x, int y, int widthIn, int heightIn, String hoveringText, ResourceLocation background, int tileWidth, int tileHeight, int uWidth, int uHeight, Button.IPressable onClicked) {
		super(x, y, widthIn, heightIn, ITextComponent.getTextComponentOrEmpty(hoveringText), onClicked);
		this.gui = gui;
		this.background = background;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.uWidth = uWidth;
		this.uHeight = uHeight;
	}

	/**
	 * @param widthIn    绘制出来的宽
	 * @param heightIn   绘制出来的高
	 * @param background 图片
	 * @param tileWidth  图片整体宽
	 * @param tileHeight 图片整体高
	 * @param uWidth     图片内需要绘制的宽
	 * @param uHeight    图片内需要绘制的高
	 */
	public GuiImageButton(Screen gui, int x, int y, int widthIn, int heightIn, String hoveringText, ResourceLocation background, int tileWidth, int tileHeight, int u, int v, int uWidth, int uHeight, Button.IPressable onClicked) {
		super(x, y, widthIn, heightIn, ITextComponent.getTextComponentOrEmpty(hoveringText), onClicked);
		this.gui = gui;
		this.background = background;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.u = u;
		this.v = v;
		this.uWidth = uWidth;
		this.uHeight = uHeight;
	}

	/**
	 *
	 * @param widthIn 绘制出来的宽
	 * @param heightIn 绘制出来的高
	 * @param background 图片
	 * @param tileWidth 图片整体宽
	 * @param tileHeight 图片整体高
	 * @param uWidth 图片内需要绘制的宽
	 * @param uHeight 图片内需要绘制的高
	 */
	public GuiImageButton(Screen gui, int x, int y, int widthIn, int heightIn, Supplier<ITextComponent> hoveringText, ResourceLocation background, int tileWidth, int tileHeight, int uWidth, int uHeight, Button.IPressable onClicked) {
		super(x, y, widthIn, heightIn, hoveringText.get(), onClicked);
		this.gui = gui;
		this.background = background;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.uWidth = uWidth;
		this.uHeight = uHeight;
		this.hoveringText = hoveringText;
	}

	/**
	 * @param widthIn    绘制出来的宽
	 * @param heightIn   绘制出来的高
	 * @param background 图片
	 * @param tileWidth  图片整体宽
	 * @param tileHeight 图片整体高
	 * @param uWidth     图片内需要绘制的宽
	 * @param uHeight    图片内需要绘制的高
	 */
	public GuiImageButton(Screen gui, int x, int y, int widthIn, int heightIn, Supplier<ITextComponent> hoveringText, ResourceLocation background, int tileWidth, int tileHeight, int u, int v, int uWidth, int uHeight, Button.IPressable onClicked) {
		super(x, y, widthIn, heightIn, hoveringText.get(), onClicked);
		this.gui = gui;
		this.background = background;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.u = u;
		this.v = v;
		this.uWidth = uWidth;
		this.uHeight = uHeight;
		this.hoveringText = hoveringText;
	}
	
	public GuiImageButton setImageX(int u) {
		this.u = u;
		return this;
	}
	public GuiImageButton setImageY(int v) {
		this.v = v;
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
	public void renderWidget(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		if(this.visible) {
			this.gui.getMinecraft().getTextureManager().bindTexture(this.background);
			blit(matrix, this.x, this.y, this.width, this.height, this.u, this.v, this.uWidth, this.uHeight, this.tileWidth, this.tileHeight);

			if(this.isHovered()) {
				fillGradient(matrix, this.x, this.y, this.x + this.width, this.y + this.height, this.hoveredColor, this.hoveredColor);
				this.gui.renderTooltip(matrix, this.getMessage(), mouseX, mouseY);
			}
		}
	}

	@Override
	public ITextComponent getMessage() {
		return this.hoveringText.get();
	}
}
