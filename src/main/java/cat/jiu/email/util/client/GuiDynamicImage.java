package cat.jiu.email.util.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiDynamicImage {
	public final ResourceLocation texture;
	public final boolean canReverse;
	public final int maxStep,
			uWidth, vHeight,
			u, v,
			width, height,
			imgWidth, imgHeight;

	/**
	 * 
	 * @param texture 图片
	 * @param maxStep 最大帧数
	 * @param canReverse 是否可以反向绘制
	 * @param uWidth 每一帧的宽
	 * @param vHeight 每一帧的高
	 * @param u 每帧在图片内的x轴
	 * @param v 每帧在图片内的y轴
	 * @param width 每帧的在图片内的宽
	 * @param height 每帧在图片内的高
	 * @param imgWidth 图片的总宽
	 * @param imgHeight 图片的总高
	 */
	public GuiDynamicImage(ResourceLocation texture, int maxStep, boolean canReverse, int uWidth, int vHeight, int u, int v, int width, int height, int imgWidth, int imgHeight) {
		this.texture = texture;
		this.maxStep = maxStep;
		this.canReverse = canReverse;
		this.width = width;
		this.height = height;
		this.imgWidth = imgWidth;
		this.imgHeight = imgHeight;
		this.u = u;
		this.v = v;
		this.uWidth = uWidth;
		this.vHeight = vHeight;
	}
	
	protected int current = 0;
	protected boolean reverse = false;
	public void draw(MatrixStack matrix, int x, int y) {
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

		Minecraft.getInstance().getTextureManager().bindTexture(this.texture);
		Screen.blit(matrix, x, y, this.width, this.height, this.u, this.v + this.current * this.vHeight, this.uWidth, this.vHeight, this.imgWidth, this.imgHeight);
	}
}
