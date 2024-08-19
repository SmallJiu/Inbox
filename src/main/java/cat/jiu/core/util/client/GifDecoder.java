package cat.jiu.core.util.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Class GifDecoder - Decodes a GIF file into one or more frames.
 * <br><pre>
 * Example:
 *    GifDecoder d = new GifDecoder();
 *    d.read("sample.gif");
 *    int n = d.getFrameCount();
 *    for (int i = 0; i < n; i++) {
 *       BufferedImage frame = d.getFrame(i);  // frame i
 *       int t = d.getDelay(i);  // display duration of frame in milliseconds
 *       // do something with frame
 *    }
 * </pre>
 * No copyright asserted on the source code of this class.  May be used for
 * any purpose, however, refer to the Unisys LZW patent for any additional
 * restrictions.  Please forward any corrections to questions at fmsware.com.
 *
 * @author Kevin Weiner, FM Software; LZW decoder adapted from John Cristy's ImageMagick.
 * @version 1.03 November 2003
 *
 */

public class GifDecoder {

	/**
	 * File read status: No errors.
	 */
	public static final int STATUS_OK = 0;

	/**
	 * File read status: Error decoding file (may be partially decoded)
	 */
	public static final int STATUS_FORMAT_ERROR = 1;

	/**
	 * File read status: Unable to open source.
	 */
	public static final int STATUS_OPEN_ERROR = 2;

	/**
	 * @author small_jiu
	 */
	public static GifDecoder decode(String file) {
		GifDecoder decoder = new GifDecoder();
		decoder.read(file);
		return decoder;
	}
	/**
	 * @author small_jiu
	 */
	public static GifDecoder decode(InputStream stream) {
		GifDecoder decoder = new GifDecoder();
		decoder.read(stream);
		return decoder;
	}
	/**
	 * @author small_jiu
	 */
	public static GifTexture getTexture(String file, boolean useSingleTextureID) {
		return new GifTexture(GifDecoder.decode(file), useSingleTextureID);
	}
	/**
	 * @author small_jiu
	 */
	public static GifTexture getTexture(InputStream stream, boolean useSingleTextureID) {
		return new GifTexture(GifDecoder.decode(stream), useSingleTextureID);
	}

	public static GifTextures getTextures(boolean useSingleTextureID, String... files) {
		GifTextures textures = new GifTextures();
		for (String file : files) {
			textures.addTexture(getTexture(file, useSingleTextureID));
		}
		return textures;
	}
	public static GifTextures getTextures(boolean useSingleTextureID, InputStream... files) {
		GifTextures textures = new GifTextures();
		for (InputStream file : files) {
			textures.addTexture(getTexture(file, useSingleTextureID));
		}
		return textures;
	}
	public static GifTextures getTextures(IGifTexture... gifs) {
		GifTextures textures = new GifTextures();
		for (IGifTexture gif : gifs) {
			textures.addTexture(gif);
		}
		return textures;
	}

	public static interface IGifTexture {
		void render(GuiGraphics graphics, long delay, boolean addCurrentIndex);
		IGifTexture setImageInfo(int u, int v, int uWidth, int vHeight);
		IGifTexture setRenderInfo(int x, int y, int width, int height);
		IGifTexture setCurrentIndex(int currentIndex);
		int getCurrentIndex();
		int getAllTextureCount();
		void destroy();
		IGifTexture copy();
	}

	/**
	 * @author small_jiu
	 */
	public static class GifTexture implements IGifTexture {
		public final GifDecoder gif;
		public final Dimension imageSize;
		protected final ArrayList<Integer> textureIDs = new ArrayList<>();
		protected int currentIndex = 0, glID, lastImageIndex;
		protected boolean enableAutoNextFrame, useCustomFrameOrder, isSingleTextureID;
		protected int
				x, y,  width,  height,
				u, v, uWidth, vHeight;
		protected int[] frameOrder;
		protected Function<Integer, Integer> nextFrameGetter;

		public GifTexture(GifDecoder gif) {
			this(gif, true, true);
		}
		public GifTexture(GifDecoder gif, boolean isSingleTextureID) {
			this(gif, isSingleTextureID, !isSingleTextureID);
		}
		public GifTexture(GifDecoder gif, boolean isSingleTextureID, boolean genTextureID) {
			this.gif = gif;
			this.isSingleTextureID = isSingleTextureID;
			this.imageSize = this.gif.getFrameSize();
			this.setImageInfo(0, 0).setRenderSize(1.0f, true);
			if (isSingleTextureID) {
				this.glID = TextureUtil.generateTextureId();
			}else if (genTextureID){
				this.genGLTextureID();
			}
		}

		protected void genGLTextureID() {
			this.destroy();
			for (int i = 0; i < this.gif.getFrameCount(); i++) {
				this.textureIDs.add(genGLTextureID(this.gif.getFrame(i)));
			}
		}

		public static int genGLTextureID(BufferedImage image) {
			int glID = TextureUtil.generateTextureId();
			uploadTexture(glID, image);
			return glID;
		}
		public static void uploadTexture(int glID, BufferedImage image) {
			int[] pixels = new int[image.getWidth() * image.getHeight()];//创建像素列表
			image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());//获取图片像素

			ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);//创建字节缓冲区，*4是包含alpha *3不包含

			//遍历图片像素转换为RGBA
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int pixel = pixels[y * image.getWidth() + x];
					Color color = new Color(pixel);
					buffer.put((byte) color.getRed());//像素点的Red
					buffer.put((byte) color.getGreen());//像素点的Green
					buffer.put((byte) color.getBlue());//像素点的Blue
					buffer.put((byte) color.getAlpha());//像素点的Alpha
				}
			}
			buffer.flip(); //一定要翻转

			try(NativeImage nativeImage = NativeImage.read(buffer)) {
				if (!RenderSystem.isOnRenderThread()) {
					RenderSystem.recordRenderCall(() -> {
						TextureUtil.prepareImage(glID, image.getWidth(), image.getHeight());
						nativeImage.upload(0, 0, 0, false);
					});
				} else {
					TextureUtil.prepareImage(glID, image.getWidth(), image.getHeight());
					nativeImage.upload(0, 0, 0, false);
				}
			} catch (IOException ignored) {
			}
		}

		public void genGLTextureID(Function2<Integer, BufferedImage, Integer> function) {
			this.destroy();
			for (int i = 0; i < this.gif.getFrameCount(); i++) {
				this.textureIDs.add(function.apply(i, this.gif.getFrame(i)));
			}
		}

		public void destroy() {
			this.textureIDs.forEach(TextureUtil::releaseTextureId);
			this.textureIDs.clear();
			this.currentIndex = 0;
		}

		public GifTexture readNewTexture(String file) {
			this.destroy();
			this.gif.read(file);
			this.genGLTextureID();
			return this;
		}
		public GifTexture readNewTexture(String file, Function2<Integer, BufferedImage, Integer> function) {
			this.destroy();
			this.gif.read(file);
			this.genGLTextureID(function);
			return this;
		}
		public GifTexture readNewTexture(InputStream file) {
			this.destroy();
			this.gif.read(file);
			this.genGLTextureID();
			return this;
		}
		public GifTexture readNewTexture(InputStream file, Function2<Integer, BufferedImage, Integer> function) {
			this.destroy();
			this.gif.read(file);
			this.genGLTextureID(function);
			return this;
		}

		public GifTexture setCurrentIndex(int currentIndex) {
			this.currentIndex = currentIndex;
			return this;
		}
		public GifTexture addCurrentIndex() {
			int currentIndex = this.getCurrentIndex() + 1;
			if (currentIndex >= (this.isUseCustomFrameOrder() ? this.getFrameOrder().length : this.gif.getFrameCount())) {
				currentIndex = 0;
			}
			return this.setCurrentIndex(currentIndex);
		}
		public GifTexture subtractCurrentIndex() {
			int currentIndex = this.getCurrentIndex() - 1;
			if (currentIndex < 0) {
				currentIndex = (this.isUseCustomFrameOrder() ? this.getFrameOrder().length : this.gif.getFrameCount()) - 1;
			}
			return this.setCurrentIndex(currentIndex);
		}

		public GifTexture setFrameOrder(int... order) {
			this.frameOrder = order;
			return this.setUseCustomFrameOrder(true);
		}

		public int[] getFrameOrder() {
			return frameOrder;
		}

		public GifTexture setUseCustomFrameOrder(boolean useCustomFrameOrder) {
			this.useCustomFrameOrder = useCustomFrameOrder;
			return this;
		}

		public boolean isUseCustomFrameOrder() {
			return useCustomFrameOrder;
		}

		@Override
		public GifTexture copy() {
			GifTexture gif = new GifTexture(this.gif, this.isSingleTextureID, false);
			if (!this.isSingleTextureID) {
				gif.textureIDs.addAll(this.textureIDs);
			}
			return gif
					.setAutoNextFrame(this.enableAutoNextFrame)
					.setNextFrameGetter(this.nextFrameGetter)
					.setFrameOrder(this.getFrameOrder())
					.setUseCustomFrameOrder(this.isUseCustomFrameOrder())
					.setRenderInfo(0, 0, this.width, this.height)
					.setImageInfo(this.u, this.v, this.uWidth, this.vHeight);
		}

		/**
		 * @param x draw x pos
		 * @param y draw y pos
		 * @param width draw width
		 * @param height draw height
		 */

		public GifTexture setRenderInfo(int x, int y, int width, int height) {
			return this.setRenderPos(x, y).setRenderSize(width, height);
		}
		public GifTexture setRenderInfo(int x, int y, float scale, boolean defaultImageSize) {
			return this.setRenderPos(x, y).setRenderSize(scale, defaultImageSize);
		}
		public GifTexture setRenderSize(float scale, boolean defaultImageSize) {
			if (defaultImageSize) {
				this.width = (int) (this.imageSize.getWidth() * scale);
				this.height = (int) (this.imageSize.getHeight() * scale);
			}else {
				this.width = (int) (this.uWidth * scale);
				this.height = (int) (this.vHeight * scale);
			}
			return this;
		}

		public GifTexture setRenderInfoWithCenter(int x, int y, float scale, boolean defaultImageSize) {
			return this
					.setRenderSize(scale, defaultImageSize)
					.setRenderPos(x - this.width / 2, y - this.height / 2);
		}
		public GifTexture setRenderInfoWithCenter(int x, int y) {
			return this
					.setRenderPos(x - this.width / 2, y - this.height / 2);
		}

		public GifTexture setRenderPos(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}
		public GifTexture setRenderSize(int width, int height) {
			this.width = width;
			this.height = height;
			return this;
		}

		/**
		 * @param u image u
		 * @param v image v
		 * @param uWidth image width
		 * @param vHeight image height
		 */
		public GifTexture setImageInfo(int u, int v, int uWidth, int vHeight) {
			this.u = u;
			this.v = v;
			this.uWidth = uWidth;
			this.vHeight = vHeight;
			return this;
		}
		public GifTexture setImageInfo(int u, int v) {
			return this.setImageInfo(u, v, (int) this.imageSize.getWidth(), (int) this.imageSize.getHeight());
		}

		public GifTexture setAutoNextFrame(boolean enableAutoNextFrame) {
			this.enableAutoNextFrame = enableAutoNextFrame;
			return this;
		}

		public GifTexture setNextFrameGetter(Function<Integer, Integer> nextFrameGetter) {
			this.nextFrameGetter = nextFrameGetter;
			return this;
		}

		public void render(GuiGraphics graphics) {
			this.render(graphics, -1, true);
		}
		/**
		 * draw current frame into gui
		 * @param delay next frame delay. set -1 to use default delay.
		 */
		@Override
		public void render(GuiGraphics graphics, long delay, boolean addCurrentIndex) {
			this.nextFrame(delay, addCurrentIndex);
			if (this.isSingleTextureID) {
				if (this.lastImageIndex != this.getCurrentImageIndex()) {
					uploadTexture(this.glID, this.gif.getFrame(this.getCurrentImageIndex()));
					this.lastImageIndex = this.getCurrentImageIndex();
				}

				draw(graphics,
						this.glID,
						this.x, this.y, this.width, this.height,
						this.u, this.v, this.uWidth, this.vHeight,
						(float) this.imageSize.getWidth(), (float) this.imageSize.getHeight()
				);
			}else {
				draw(graphics,
						this.getCurrentTextureID(),
						this.x, this.y, this.width, this.height,
						this.u, this.v, this.uWidth, this.vHeight,
						(float) this.imageSize.getWidth(), (float) this.imageSize.getHeight()
				);
			}
		}

		protected long delay;
		protected void nextFrame(long delay, boolean addCurrentIndex) {
			if (this.enableAutoNextFrame) {
				long m = System.currentTimeMillis();
				if (m > this.delay) {
					if (addCurrentIndex) {
						this.addCurrentIndex();
					} else if (this.nextFrameGetter != null) {
						this.setCurrentIndex(this.nextFrameGetter.apply(this.getCurrentIndex()));
					} else {
						this.subtractCurrentIndex();
					}
					this.delay = m + (delay > 0 ? delay : this.gif.getDelay(this.getCurrentImageIndex()));
				}
			}
		}

		public int getRenderX() {
			return x;
		}

		public int getRenderY() {
			return y;
		}

		public int getRenderWidth() {
			return width;
		}

		public int getRenderHeight() {
			return height;
		}

		@Override
		public int getCurrentIndex() {
			return currentIndex;
		}

		@Override
		public int getAllTextureCount() {
			return this.gif.getFrameCount();
		}

		public int getCurrentImageIndex() {
			return this.isUseCustomFrameOrder() ? this.getFrameOrder()[this.getCurrentIndex()] : this.getCurrentIndex();
		}
		public int getCurrentTextureID() {
			return this.isSingleTextureID ? this.glID : this.textureIDs.get(this.getCurrentImageIndex());
		}

		public static void draw(GuiGraphics graphics, int texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, float textureWidth, float textureHeight) {
			int
					x2 = x + width,
					y2 = y + height;
			float
					minU = (u + 0.0F) / textureWidth,
					maxU = (u + (float)uWidth) / textureWidth,
					minV = (v + 0.0F) / textureHeight,
					maxV = (v + (float)vHeight) / textureHeight;
			int blitOffset = 0;

			RenderSystem.setShaderTexture(0, texture);
			RenderSystem.setShader(GameRenderer::getPositionTexShader);
			Matrix4f matrix4f = graphics.pose().last().pose();
			BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
			bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
			bufferbuilder.vertex(matrix4f, (float) x, (float) y, (float)blitOffset).uv(minU, minV).endVertex();
			bufferbuilder.vertex(matrix4f, (float) x, (float)y2, (float)blitOffset).uv(minU, maxV).endVertex();
			bufferbuilder.vertex(matrix4f, (float)x2, (float)y2, (float)blitOffset).uv(maxU, maxV).endVertex();
			bufferbuilder.vertex(matrix4f, (float)x2, (float) y, (float)blitOffset).uv(maxU, minV).endVertex();
			BufferUploader.drawWithShader(bufferbuilder.end());
		}
		public interface Function2 <T1, T2, R> {
			R apply(T1 t1, T2 t2);
		}
	}

	public static class GifTextures implements IGifTexture {
		protected final ArrayList<IGifTexture> textures = new ArrayList<>();
		protected boolean enableAutoNextFrame, useCustomFrameOrder;
		protected Function<Integer, Integer> nextFrameGetter;
		protected int[] frameOrder;
		protected int
				x, y,  width,  height,
				u, v, uWidth, vHeight,
				currentTextureIdx, nextTextureDelay = 0;

		public GifTextures() {
		}

		public GifTextures addTexture(IGifTexture... texture) {
			this.textures.addAll(Arrays.asList(texture));
			return this;
		}
		public GifTextures addTexture(List<IGifTexture> texture) {
			this.textures.addAll(texture);
			return this;
		}

		public IGifTexture getTexture(int index) {
			return this.textures.get(index);
		}

		public IGifTexture removeTexture(int index) {
			return this.textures.remove(index);
		}

		public IGifTexture getCurrentTexture() {
			return this.getTexture(this.getCurrentImageIndex());
		}

		protected List<IGifTexture> unmodifiable;
		public List<IGifTexture> getTextures(){
			if (this.unmodifiable==null) {
				this.unmodifiable = Collections.unmodifiableList(this.textures);
			}
			return this.unmodifiable;
		}
		public GifTextures foreach(Consumer<IGifTexture> consumer) {
			this.textures.forEach(consumer);
			return this;
		}

		public void render(GuiGraphics graphics) {
			this.render(graphics, -1, true);
		}
		@Override
		public void render(GuiGraphics graphics, long delay, boolean addCurrentIndex) {
			this.nextFrame(delay, addCurrentIndex);
			this.textures.get(this.getCurrentImageIndex()).render(graphics, delay, addCurrentIndex);
		}

		protected long delay;
		protected void nextFrame(long delay, boolean addCurrentIndex) {
			if (this.enableAutoNextFrame) {
				long m = System.currentTimeMillis();
				if (m > this.delay) {
					if (addCurrentIndex) {
						this.addCurrentIndex();
					} else if (this.nextFrameGetter != null) {
						this.setCurrentIndex(this.nextFrameGetter.apply(this.getCurrentIndex()));
					} else {
						this.subtractCurrentIndex();
					}
					this.delay = m + (delay > 0 ? delay : this.nextTextureDelay);
				}
			}
		}

		@Override
		public GifTextures setImageInfo(int u, int v, int uWidth, int vHeight) {
			for (IGifTexture texture : this.textures) {
				texture.setImageInfo(u, v, uWidth, vHeight);
			}
			return this;
		}

		@Override
		public GifTextures setRenderInfo(int x, int y, int width, int height) {
			for (IGifTexture texture : this.textures) {
				texture.setRenderInfo(x, y, width, height);
			}
			return this;
		}

		@Override
		public GifTextures setCurrentIndex(int currentIndex) {
			this.currentTextureIdx = currentIndex;
			return this;
		}

		public GifTextures setAutoNextFrame(boolean enableAutoNextFrame) {
			this.enableAutoNextFrame = enableAutoNextFrame;
			return this;
		}

		public GifTextures setNextFrameGetter(Function<Integer, Integer> nextFrameGetter) {
			this.nextFrameGetter = nextFrameGetter;
			return this;
		}

		@Override
		public int getCurrentIndex() {
			return this.currentTextureIdx;
		}

		public int getCurrentImageIndex() {
			return this.isUseCustomFrameOrder() ? this.getFrameOrder()[this.getCurrentIndex()] : this.getCurrentIndex();
		}

		public GifTextures addCurrentIndex() {
			int currentIndex = this.getCurrentIndex() + 1;
			if (currentIndex >= (this.isUseCustomFrameOrder() ? this.getFrameOrder().length : this.getAllTextureCount())) {
				currentIndex = 0;
			}
			return this.setCurrentIndex(currentIndex);
		}
		public GifTextures subtractCurrentIndex() {
			int currentIndex = this.getCurrentIndex() - 1;
			if (currentIndex < 0) {
				currentIndex = (this.isUseCustomFrameOrder() ? this.getFrameOrder().length : this.getAllTextureCount()) - 1;
			}
			return this.setCurrentIndex(currentIndex);
		}

		public GifTextures setFrameOrder(int... order) {
			this.frameOrder = order;
			return this.setUseCustomFrameOrder(true);
		}

		public int[] getFrameOrder() {
			return frameOrder;
		}

		public GifTextures setNextTextureDelay(int nextTextureDelay) {
			this.nextTextureDelay = nextTextureDelay;
			return this;
		}

		public GifTextures setUseCustomFrameOrder(boolean useCustomFrameOrder) {
			this.useCustomFrameOrder = useCustomFrameOrder;
			return this;
		}

		public boolean isUseCustomFrameOrder() {
			return useCustomFrameOrder;
		}

		@Override
		public int getAllTextureCount() {
			return this.textures.size();
		}

		@Override
		public void destroy() {
			this.textures.forEach(IGifTexture::destroy);
		}

		@Override
		public GifTextures copy() {
			return new GifTextures()
					.addTexture(this.textures.stream().map(IGifTexture::copy).collect(Collectors.toList()))
					.setNextTextureDelay(this.nextTextureDelay)
					.setAutoNextFrame(this.enableAutoNextFrame)
					.setNextFrameGetter(this.nextFrameGetter)
					.setUseCustomFrameOrder(this.isUseCustomFrameOrder())
					.setFrameOrder(this.getFrameOrder());
		}
	}

	protected BufferedInputStream in;
	protected int status;

	protected int width; // full image width
	protected int height; // full image height
	protected boolean gctFlag; // global color table used
	protected int gctSize; // size of global color table
	protected int loopCount = 1; // iterations; 0 = repeat forever

	protected int[] gct; // global color table
	protected int[] lct; // local color table
	protected int[] act; // active color table

	protected int bgIndex; // background color index
	protected int bgColor; // background color
	protected int lastBgColor; // previous bg color
	protected int pixelAspect; // pixel aspect ratio

	protected boolean lctFlag; // local color table flag
	protected boolean interlace; // interlace flag
	protected int lctSize; // local color table size

	protected int ix, iy, iw, ih; // current image rectangle
	protected Rectangle lastRect; // last image rect
	protected BufferedImage image; // current frame
	protected BufferedImage lastImage; // previous frame

	protected byte[] block = new byte[256]; // current data block
	protected int blockSize = 0; // block size

	// last graphic control extension info
	protected int dispose = 0;
	// 0=no action; 1=leave in place; 2=restore to bg; 3=restore to prev
	protected int lastDispose = 0;
	protected boolean transparency = false; // use transparent color
	protected int delay = 0; // delay in milliseconds
	protected int transIndex; // transparent color index

	protected static final int MaxStackSize = 4096;
	// max decoder pixel stack size

	// LZW decoder working arrays
	protected short[] prefix;
	protected byte[] suffix;
	protected byte[] pixelStack;
	protected byte[] pixels;

	protected ArrayList<GifFrame> frames; // frames read from current file
	protected int frameCount;

	static class GifFrame {
		public GifFrame(BufferedImage im, int del) {
			image = im;
			delay = del;
		}
		public BufferedImage image;
		public int delay;
	}

	/**
	 * Gets display duration for specified frame.
	 *
	 * @param n int index of frame
	 * @return delay in milliseconds
	 */
	public int getDelay(int n) {
		//
		delay = -1;
		if ((n >= 0) && (n < frameCount)) {
			delay = frames.get(n).delay;
		}
		return delay;
	}

	/**
	 * Gets the number of frames read from file.
	 * @return frame count
	 */
	public int getFrameCount() {
		return frameCount;
	}

	/**
	 * Gets the first (or only) image read.
	 *
	 * @return BufferedImage containing first frame, or null if none.
	 */
	public BufferedImage getImage() {
		return getFrame(0);
	}

	/**
	 * Gets the "Netscape" iteration count, if any.
	 * A count of 0 means repeat indefinitiely.
	 *
	 * @return iteration count if one was specified, else 1.
	 */
	public int getLoopCount() {
		return loopCount;
	}

	/**
	 * Creates new frame image from current data (and previous
	 * frames as specified by their disposition codes).
	 */
	protected void setPixels() {
		// expose destination image's pixels as int array
		int[] dest =
			((DataBufferInt) image.getRaster().getDataBuffer()).getData();

		// fill in starting image contents based on last image's dispose code
		if (lastDispose > 0) {
			if (lastDispose == 3) {
				// use image before last
				int n = frameCount - 2;
				if (n > 0) {
					lastImage = getFrame(n - 1);
				} else {
					lastImage = null;
				}
			}

			if (lastImage != null) {
				int[] prev =
					((DataBufferInt) lastImage.getRaster().getDataBuffer()).getData();
				System.arraycopy(prev, 0, dest, 0, width * height);
				// copy pixels

				if (lastDispose == 2) {
					// fill last image rect area with background color
					Graphics2D g = image.createGraphics();
					Color c = null;
					if (transparency) {
						c = new Color(0, 0, 0, 0); 	// assume background is transparent
					} else {
						c = new Color(lastBgColor); // use given background color
					}
					g.setColor(c);
					g.setComposite(AlphaComposite.Src); // replace area
					g.fill(lastRect);
					g.dispose();
				}
			}
		}

		// copy each source line to the appropriate place in the destination
		int pass = 1;
		int inc = 8;
		int iline = 0;
		for (int i = 0; i < ih; i++) {
			int line = i;
			if (interlace) {
				if (iline >= ih) {
					pass++;
					switch (pass) {
						case 2 :
							iline = 4;
							break;
						case 3 :
							iline = 2;
							inc = 4;
							break;
						case 4 :
							iline = 1;
							inc = 2;
					}
				}
				line = iline;
				iline += inc;
			}
			line += iy;
			if (line < height) {
				int k = line * width;
				int dx = k + ix; // start of line in dest
				int dlim = dx + iw; // end of dest line
				if ((k + width) < dlim) {
					dlim = k + width; // past dest edge
				}
				int sx = i * iw; // start of line in source
				while (dx < dlim) {
					// map color and insert in destination
					int index = ((int) pixels[sx++]) & 0xff;
					int c = act[index];
					if (c != 0) {
						dest[dx] = c;
					}
					dx++;
				}
			}
		}
	}

	/**
	 * Gets the image contents of frame n.
	 *
	 * @return BufferedImage representation of frame, or null if n is invalid.
	 */
	public BufferedImage getFrame(int n) {
		BufferedImage im = null;
		if ((n >= 0) && (n < frameCount)) {
			im = frames.get(n).image;
		}
		return im;
	}

	/**
	 * Gets image size.
	 *
	 * @return GIF image dimensions
	 */
	public Dimension getFrameSize() {
		return new Dimension(width, height);
	}

	/**
	 * Reads GIF image from stream
	 *
	 * @param is containing GIF file.
	 * @return read status code (0 = no errors)
	 */
	public int read(BufferedInputStream is) {
		init();
		if (is != null) {
			in = is;
			readHeader();
			if (!err()) {
				readContents();
				if (frameCount < 0) {
					status = STATUS_FORMAT_ERROR;
				}
			}
		} else {
			status = STATUS_OPEN_ERROR;
		}
		try {
			is.close();
		} catch (IOException e) {
		}
		return status;
	}

	/**
	 * Reads GIF image from stream
	 *
	 * @param is containing GIF file.
	 * @return read status code (0 = no errors)
	 */
	public int read(InputStream is) {
		init();
		if (is != null) {
			if (!(is instanceof BufferedInputStream))
				is = new BufferedInputStream(is);
			in = (BufferedInputStream) is;
			readHeader();
			if (!err()) {
				readContents();
				if (frameCount < 0) {
					status = STATUS_FORMAT_ERROR;
				}
			}
		} else {
			status = STATUS_OPEN_ERROR;
		}
		try {
			is.close();
		} catch (IOException e) {
		}
		return status;
	}

	/**
	 * Reads GIF file from specified file/URL source  
	 * (URL assumed if name contains ":/" or "file:")
	 *
	 * @param name String containing source
	 * @return read status code (0 = no errors)
	 */
	public int read(String name) {
		status = STATUS_OK;
		try {
			name = name.trim().toLowerCase();
			if (name.startsWith("http://") || name.startsWith("https://")) {
				URL url = new URL(name);
				in = new BufferedInputStream(url.openStream());
			} else {
				in = new BufferedInputStream(Files.newInputStream(Paths.get(name)));
			}
			status = read(in);
		} catch (IOException e) {
			status = STATUS_OPEN_ERROR;
		}

		return status;
	}

	/**
	 * Decodes LZW image data into pixel array.
	 * Adapted from John Cristy's ImageMagick.
	 */
	protected void decodeImageData() {
		int NullCode = -1;
		int npix = iw * ih;
		int available, 
			clear,
			code_mask,
			code_size,
			end_of_information,
			in_code,
			old_code,
			bits,
			code,
			count,
			i,
			datum,
			data_size,
			first,
			top,
			bi,
			pi;

		if ((pixels == null) || (pixels.length < npix)) {
			pixels = new byte[npix]; // allocate new pixel array
		}
		if (prefix == null) prefix = new short[MaxStackSize];
		if (suffix == null) suffix = new byte[MaxStackSize];
		if (pixelStack == null) pixelStack = new byte[MaxStackSize + 1];

		//  Initialize GIF data stream decoder.

		data_size = read();
		clear = 1 << data_size;
		end_of_information = clear + 1;
		available = clear + 2;
		old_code = NullCode;
		code_size = data_size + 1;
		code_mask = (1 << code_size) - 1;
		for (code = 0; code < clear; code++) {
			prefix[code] = 0;
			suffix[code] = (byte) code;
		}

		//  Decode GIF pixel stream.

		datum = bits = count = first = top = pi = bi = 0;

		for (i = 0; i < npix;) {
			if (top == 0) {
				if (bits < code_size) {
					//  Load bytes until there are enough bits for a code.
					if (count == 0) {
						// Read a new data block.
						count = readBlock();
						if (count <= 0)
							break;
						bi = 0;
					}
					datum += (((int) block[bi]) & 0xff) << bits;
					bits += 8;
					bi++;
					count--;
					continue;
				}

				//  Get the next code.

				code = datum & code_mask;
				datum >>= code_size;
				bits -= code_size;

				//  Interpret the code

				if ((code > available) || (code == end_of_information))
					break;
				if (code == clear) {
					//  Reset decoder.
					code_size = data_size + 1;
					code_mask = (1 << code_size) - 1;
					available = clear + 2;
					old_code = NullCode;
					continue;
				}
				if (old_code == NullCode) {
					pixelStack[top++] = suffix[code];
					old_code = code;
					first = code;
					continue;
				}
				in_code = code;
				if (code == available) {
					pixelStack[top++] = (byte) first;
					code = old_code;
				}
				while (code > clear) {
					pixelStack[top++] = suffix[code];
					code = prefix[code];
				}
				first = ((int) suffix[code]) & 0xff;

				//  Add a new string to the string table,

				if (available >= MaxStackSize)
					break;
				pixelStack[top++] = (byte) first;
				prefix[available] = (short) old_code;
				suffix[available] = (byte) first;
				available++;
				if (((available & code_mask) == 0)
					&& (available < MaxStackSize)) {
					code_size++;
					code_mask += available;
				}
				old_code = in_code;
			}

			//  Pop a pixel off the pixel stack.

			top--;
			pixels[pi++] = pixelStack[top];
			i++;
		}

		for (i = pi; i < npix; i++) {
			pixels[i] = 0; // clear missing pixels
		}

	}

	/**
	 * Returns true if an error was encountered during reading/decoding
	 */
	protected boolean err() {
		return status != STATUS_OK;
	}

	/**
	 * Initializes or re-initializes reader
	 */
	protected void init() {
		status = STATUS_OK;
		frameCount = 0;
		frames = new ArrayList();
		gct = null;
		lct = null;
	}

	/**
	 * Reads a single byte from the input stream.
	 */
	protected int read() {
		int curByte = 0;
		try {
			curByte = in.read();
		} catch (IOException e) {
			status = STATUS_FORMAT_ERROR;
		}
		return curByte;
	}

	/**
	 * Reads next variable length block from input.
	 *
	 * @return number of bytes stored in "buffer"
	 */
	protected int readBlock() {
		blockSize = read();
		int n = 0;
		if (blockSize > 0) {
			try {
				int count = 0;
				while (n < blockSize) {
					count = in.read(block, n, blockSize - n);
					if (count == -1) 
						break;
					n += count;
				}
			} catch (IOException e) {
			}

			if (n < blockSize) {
				status = STATUS_FORMAT_ERROR;
			}
		}
		return n;
	}

	/**
	 * Reads color table as 256 RGB integer values
	 *
	 * @param ncolors int number of colors to read
	 * @return int array containing 256 colors (packed ARGB with full alpha)
	 */
	protected int[] readColorTable(int ncolors) {
		int nbytes = 3 * ncolors;
		int[] tab = null;
		byte[] c = new byte[nbytes];
		int n = 0;
		try {
			n = in.read(c);
		} catch (IOException e) {
		}
		if (n < nbytes) {
			status = STATUS_FORMAT_ERROR;
		} else {
			tab = new int[256]; // max size to avoid bounds checks
			int i = 0;
			int j = 0;
			while (i < ncolors) {
				int r = ((int) c[j++]) & 0xff;
				int g = ((int) c[j++]) & 0xff;
				int b = ((int) c[j++]) & 0xff;
				tab[i++] = 0xff000000 | (r << 16) | (g << 8) | b;
			}
		}
		return tab;
	}

	/**
	 * Main file parser.  Reads GIF content blocks.
	 */
	protected void readContents() {
		// read GIF file content blocks
		boolean done = false;
		while (!(done || err())) {
			int code = read();
			switch (code) {

				case 0x2C : // image separator
					readImage();
					break;

				case 0x21 : // extension
					code = read();
					switch (code) {
						case 0xf9 : // graphics control extension
							readGraphicControlExt();
							break;

						case 0xff : // application extension
							readBlock();
							String app = "";
							for (int i = 0; i < 11; i++) {
								app += (char) block[i];
							}
							if (app.equals("NETSCAPE2.0")) {
								readNetscapeExt();
							}
							else
								skip(); // don't care
							break;

						default : // uninteresting extension
							skip();
					}
					break;

				case 0x3b : // terminator
					done = true;
					break;

				case 0x00 : // bad byte, but keep going and see what happens
					break;

				default :
					status = STATUS_FORMAT_ERROR;
			}
		}
	}

	/**
	 * Reads Graphics Control Extension values
	 */
	protected void readGraphicControlExt() {
		read(); // block size
		int packed = read(); // packed fields
		dispose = (packed & 0x1c) >> 2; // disposal method
		if (dispose == 0) {
			dispose = 1; // elect to keep old image if discretionary
		}
		transparency = (packed & 1) != 0;
		delay = readShort() * 10; // delay in milliseconds
		transIndex = read(); // transparent color index
		read(); // block terminator
	}

	/**
	 * Reads GIF file header information.
	 */
	protected void readHeader() {
		String id = "";
		for (int i = 0; i < 6; i++) {
			id += (char) read();
		}
		if (!id.startsWith("GIF")) {
			status = STATUS_FORMAT_ERROR;
			return;
		}

		readLSD();
		if (gctFlag && !err()) {
			gct = readColorTable(gctSize);
			bgColor = gct[bgIndex];
		}
	}

	/**
	 * Reads next frame image
	 */
	protected void readImage() {
		ix = readShort(); // (sub)image position & size
		iy = readShort();
		iw = readShort();
		ih = readShort();

		int packed = read();
		lctFlag = (packed & 0x80) != 0; // 1 - local color table flag
		interlace = (packed & 0x40) != 0; // 2 - interlace flag
		// 3 - sort flag
		// 4-5 - reserved
		lctSize = 2 << (packed & 7); // 6-8 - local color table size

		if (lctFlag) {
			lct = readColorTable(lctSize); // read table
			act = lct; // make local table active
		} else {
			act = gct; // make global table active
			if (bgIndex == transIndex)
				bgColor = 0;
		}
		int save = 0;
		if (transparency) {
			save = act[transIndex];
			act[transIndex] = 0; // set transparent color if specified
		}

		if (act == null) {
			status = STATUS_FORMAT_ERROR; // no color table defined
		}

		if (err()) return;

		decodeImageData(); // decode pixel data
		skip();

		if (err()) return;

		frameCount++;

		// create new image to receive frame data
		image =
			new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);

		setPixels(); // transfer pixel data to image

		frames.add(new GifFrame(image, delay)); // add image to frame list

		if (transparency) {
			act[transIndex] = save;
		}
		resetFrame();

	}

	/**
	 * Reads Logical Screen Descriptor
	 */
	protected void readLSD() {

		// logical screen size
		width = readShort();
		height = readShort();

		// packed fields
		int packed = read();
		gctFlag = (packed & 0x80) != 0; // 1   : global color table flag
		// 2-4 : color resolution
		// 5   : gct sort flag
		gctSize = 2 << (packed & 7); // 6-8 : gct size

		bgIndex = read(); // background color index
		pixelAspect = read(); // pixel aspect ratio
	}

	/**
	 * Reads Netscape extenstion to obtain iteration count
	 */
	protected void readNetscapeExt() {
		do {
			readBlock();
			if (block[0] == 1) {
				// loop count sub-block
				int b1 = ((int) block[1]) & 0xff;
				int b2 = ((int) block[2]) & 0xff;
				loopCount = (b2 << 8) | b1;
			}
		} while ((blockSize > 0) && !err());
	}

	/**
	 * Reads next 16-bit value, LSB first
	 */
	protected int readShort() {
		// read 16-bit value, LSB first
		return read() | (read() << 8);
	}

	/**
	 * Resets frame state for reading next image.
	 */
	protected void resetFrame() {
		lastDispose = dispose;
		lastRect = new Rectangle(ix, iy, iw, ih);
		lastImage = image;
		lastBgColor = bgColor;
		int dispose = 0;
		boolean transparency = false;
		int delay = 0;
		lct = null;
	}

	/**
	 * Skips variable length blocks up to and including
	 * next zero length block.
	 */
	protected void skip() {
		do {
			readBlock();
		} while ((blockSize > 0) && !err());
	}
}
