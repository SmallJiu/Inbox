package cat.jiu.core.util.element.image.gif;

import cat.jiu.core.util.element.image.ImageBuffered;
import cat.jiu.core.util.client.GifDecoder;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public abstract class BaseGifImage extends ImageBuffered {
    protected GifDecoder.IGifTexture gif;

    public BaseGifImage() {
    }

    public BaseGifImage(GifDecoder.IGifTexture gif) {
        this.gif = gif;
    }

    public GifDecoder.IGifTexture getGif() {
        return gif;
    }

    public void setGif(GifDecoder.IGifTexture gif) {
        this.gif = gif;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(GuiGraphics graphics, int renderToX, int renderToY, int renderWidth, int renderHeight, float partialTick) {
        if (this.getGif()!=null) {
            this.getGif()
                    .setRenderInfo(renderToX, renderToY, renderWidth, renderHeight)
                    .setImageInfo(this.getU(), this.getV(), this.getUWidth(), this.getVHeight())
                    .render(graphics);
        }
    }

    @Override
    public abstract ResourceLocation getImageType();

    @Override
    public abstract JsonObject write(JsonObject data);

    @Override
    public abstract void read(JsonObject data);

    @Override
    public abstract CompoundTag write(CompoundTag data);

    @Override
    public abstract void read(CompoundTag data);
}
