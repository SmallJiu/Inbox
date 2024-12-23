package cat.jiu.core.util.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class BufferedTexture extends AbstractTexture implements Dumpable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private NativeImage pixels;
    protected BufferedImage image;

    public BufferedTexture(BufferedImage image, boolean useCalloc) {
        this(-1, image, useCalloc);
    }
    public BufferedTexture(int id, BufferedImage image, boolean useCalloc) {
        this.id = id;
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
                TextureUtil.prepareImage(this.getId(), this.pixels.getWidth(), this.pixels.getHeight());
                this.upload();
            });
        } else {
            TextureUtil.prepareImage(this.getId(), this.pixels.getWidth(), this.pixels.getHeight());
            this.upload();
        }
    }

    public void load(ResourceManager pManager) {
    }

    public void upload() {
        if (this.pixels != null) {
            this.bind();
            this.pixels.upload(0, 0, 0, false);
        }else {
            LOGGER.warn("Trying to upload disposed texture {}", this.getId());
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
    }
    public void setPixels(NativeImage pPixels) {
        if (this.pixels != null) {
            this.pixels.close();
        }
        this.pixels = pPixels;
    }

    public void close() {
        if (this.pixels != null) {
            this.pixels.close();
            this.releaseId();
            this.pixels = null;
        }
    }

    public void dumpContents(ResourceLocation pResourceLocation, Path pPath) throws IOException {
        if (this.pixels != null) {
            String s = pResourceLocation.toDebugFileName() + ".png";
            Path path = pPath.resolve(s);
            this.pixels.writeToFile(path);
        }
    }
}
