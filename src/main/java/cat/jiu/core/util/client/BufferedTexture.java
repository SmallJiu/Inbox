package cat.jiu.core.util.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.resources.IResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class BufferedTexture extends Texture {
    private static final Logger LOGGER = LogManager.getLogger();
    private NativeImage pixels;
    protected BufferedImage image;

    public BufferedTexture(BufferedImage image, boolean useCalloc) {
        this(-1, image, useCalloc);
    }
    public BufferedTexture(int id, BufferedImage image, boolean useCalloc) {
        this.glTextureId = id;
        this.image = image;
        this.pixels = new NativeImage(image.getWidth(), image.getHeight(), useCalloc);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getRGB(x, y);
                this.getPixels().setPixelRGBA(x, y, (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF));
            }
        }

        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                TextureUtil.prepareImage(this.getGlTextureId(), this.pixels.getWidth(), this.pixels.getHeight());
                this.upload();
            });
        } else {
            TextureUtil.prepareImage(this.getGlTextureId(), this.pixels.getWidth(), this.pixels.getHeight());
            this.upload();
        }
    }

    public void upload() {
        if (this.pixels != null) {
            this.bindTexture();
            this.pixels.uploadTextureSub(0, 0, 0, false);
        }
    }

    public NativeImage getPixels() {
        return this.pixels;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image, boolean useCalloc) {
        this.image = image;
        this.setPixels(new NativeImage(image.getWidth(), image.getHeight(), useCalloc));

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getRGB(x, y);
                this.getPixels().setPixelRGBA(x, y, (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF));
            }
        }

        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                TextureUtil.prepareImage(this.getGlTextureId(), this.pixels.getWidth(), this.pixels.getHeight());
                this.upload();
            });
        } else {
            TextureUtil.prepareImage(this.getGlTextureId(), this.pixels.getWidth(), this.pixels.getHeight());
            this.upload();
        }
    }
    public void setPixels(NativeImage pPixels) {
        if (this.pixels != null) {
            this.pixels.close();
        }
        this.pixels = pPixels;
    }

    @Override
    public void loadTexture(IResourceManager manager) throws IOException {

    }

    public void close() {
        if (this.pixels != null) {
            this.pixels.close();
//				this.deleteGlTexture();
            this.pixels = null;
        }
    }
}
