package cat.jiu.email.ui.gui.component;

import java.awt.Color;
import java.util.function.Supplier;

import cat.jiu.email.util.ClientUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiImageButton extends Button {
	protected static final Color HOVERED_COLOR = new Color(0, 255, 255, 77);
	protected final Screen gui;
	protected Supplier<ResourceLocation> background;
	protected ResourceLocation staticBackground;
	protected int u, v = 0;
	protected final int uWidth, uHeight, tileWidth, tileHeight;
	protected int hoveredColor = HOVERED_COLOR.getRGB();
	protected Supplier<Component> hoveringText = super::getMessage;
	protected boolean refreshBackground;

	/**
	 *
	 * @param widthIn 绘制出来的宽
	 * @param heightIn 绘制出来的高
	 * @param tileWidth 图片整体宽
	 * @param tileHeight 图片整体高
	 * @param uWidth 图片内需要绘制的宽
	 * @param uHeight 图片内需要绘制的高
	 */
	public GuiImageButton(Screen gui, int x, int y, int widthIn, int heightIn, String hoveringText, int tileWidth, int tileHeight, int uWidth, int uHeight, Button.OnPress onClicked) {
		super(x, y, widthIn, heightIn, Component.nullToEmpty(hoveringText), onClicked);
		this.gui = gui;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.uWidth = uWidth;
		this.uHeight = uHeight;
	}

	/**
	 * @param widthIn    绘制出来的宽
	 * @param heightIn   绘制出来的高
	 * @param tileWidth  图片整体宽
	 * @param tileHeight 图片整体高
	 * @param uWidth     图片内需要绘制的宽
	 * @param uHeight    图片内需要绘制的高
	 */
	public GuiImageButton(Screen gui, int x, int y, int widthIn, int heightIn, String hoveringText, int tileWidth, int tileHeight, int u, int v, int uWidth, int uHeight, Button.OnPress onClicked) {
		super(x, y, widthIn, heightIn, Component.nullToEmpty(hoveringText), onClicked);
		this.gui = gui;
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
	 * @param tileWidth 图片整体宽
	 * @param tileHeight 图片整体高
	 * @param uWidth 图片内需要绘制的宽
	 * @param uHeight 图片内需要绘制的高
	 */
	public GuiImageButton(Screen gui, int x, int y, int widthIn, int heightIn, Supplier<Component> hoveringText, int tileWidth, int tileHeight, int uWidth, int uHeight, Button.OnPress onClicked) {
		super(x, y, widthIn, heightIn, hoveringText.get(), onClicked);
		this.gui = gui;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.uWidth = uWidth;
		this.uHeight = uHeight;
		this.hoveringText = hoveringText;
	}

	/**
	 * @param widthIn    绘制出来的宽
	 * @param heightIn   绘制出来的高
	 * @param tileWidth  图片整体宽
	 * @param tileHeight 图片整体高
	 * @param uWidth     图片内需要绘制的宽
	 * @param uHeight    图片内需要绘制的高
	 */
	public GuiImageButton(Screen gui, int x, int y, int widthIn, int heightIn, Supplier<Component> hoveringText, int tileWidth, int tileHeight, int u, int v, int uWidth, int uHeight, Button.OnPress onClicked) {
		super(x, y, widthIn, heightIn, hoveringText.get(), onClicked);
		this.gui = gui;
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

	public GuiImageButton setBackground(Supplier<ResourceLocation> background) {
		this.background = background;
		return this;
	}

	public GuiImageButton setUOffset(int u) {
		this.u = u;
		return this;
	}

	public GuiImageButton setVOffset(int v) {
		this.v = v;
		return this;
	}

	public GuiImageButton setRefreshBackground(boolean refreshBackground) {
		this.refreshBackground = refreshBackground;
		return this;
	}

	@Override
	public void renderButton(PoseStack stack, int mouseX, int mouseY, float pPartialTick) {
		if(this.visible) {
			if (this.refreshBackground) {
				this.staticBackground = this.background.get();
			}else {
				if (this.staticBackground == null) {
					this.staticBackground = this.background.get();
				}
			}
			ClientUtil.bindTexture(this.staticBackground);
			blit(stack, this.x, this.y, this.width, this.height, this.u, this.v, this.uWidth, this.uHeight, this.tileWidth, this.tileHeight);

			if(this.isHoveredOrFocused() && this.hoveringText.get()!=null) {
				fill(stack, this.x, this.y, this.x + this.width, this.y + this.height, this.hoveredColor);
				renderToolTip(stack, mouseX, mouseY);
			}
		}
	}

	@Override
	public Component getMessage() {
		return this.hoveringText.get();
	}
}
