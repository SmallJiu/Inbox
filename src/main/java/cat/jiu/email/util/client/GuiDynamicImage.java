package cat.jiu.email.util.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiDynamicImage {
	public final ResourceLocation texture;
	public final int maxStep,
					 stepWidth,
					 stepHeight,
					 u, v,
					 showWidth,
					 showHeight,
					 imgWidth,
					 imgHeight;
	
	public final boolean canReverse;
	
	/**
	 * 
	 * @param texture 图片
	 * @param maxStep 最大帧数
	 * @param canReverse 是否可以反向绘制
	 * @param stepWidth 每一帧的宽
	 * @param stepHeight 每一帧的高
	 * @param u 每帧在图片内的x轴
	 * @param v 每帧在图片内的y轴
	 * @param width 每帧的在图片内的宽
	 * @param height 每帧在图片内的高
	 * @param imgWidth 图片的总宽
	 * @param imgHeight 图片的总高
	 */
	public GuiDynamicImage(ResourceLocation texture, int maxStep, boolean canReverse, int stepWidth, int stepHeight, int u, int v, int width, int height, int imgWidth, int imgHeight) {
		this.texture = texture;
		this.maxStep = maxStep;
		this.canReverse = canReverse;
		this.stepWidth = stepWidth;
		this.stepHeight = stepHeight;
		this.u = u;
		this.v = v;
		this.showWidth = width;
		this.showHeight = height;
		this.imgWidth = imgWidth;
		this.imgHeight = imgHeight;
	}
	
	protected int current = 0;
	protected boolean reverse = false;
	public void draw(int x, int y) {
		if(this.canReverse) {
			if(this.current >= this.maxStep) {
				this.reverse = true;
			}else if(this.current <= 0) {
				this.reverse = false;
			}
			if(this.reverse) {
				this.current--;
			}else {
				this.current++;
			}
		}else {
			this.current++;
			if(this.current >= this.maxStep) {
				this.current = 0;
			}
		}
		
		GlStateManager.pushMatrix();
		GlStateManager.resetColor();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getMinecraft().getTextureManager().bindTexture(this.texture);
		Gui.drawScaledCustomSizeModalRect(x, y, this.u, this.v + this.current * this.showHeight, this.showWidth, this.showHeight, this.stepWidth, this.stepHeight, this.imgWidth, this.imgHeight);
		GlStateManager.popMatrix();
	}
}
