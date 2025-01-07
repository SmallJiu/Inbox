package cat.jiu.core.util.element.image.gif;

import cat.jiu.core.util.client.GifDecoder;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;

public class ImageGifs extends BaseGifImage {
    public static final ResourceLocation ID = new ResourceLocation("jiucore", "element/image/gif/more");

    protected GifDecoder.GifTextures gif;
    public ImageGifs() {
    }

    public ImageGifs(GifDecoder.GifTextures gif) {
        super(gif);
        this.gif = gif;
    }

    public void setGif(GifDecoder.GifTextures gif) {
        this.gif = gif;
    }

    @Override
    public GifDecoder.GifTextures getGif() {
        return gif;
    }

    @Override
    public ResourceLocation getImageType() {
        return ID;
    }

    @Override
    public JsonObject write(JsonObject data) {
        this.writeBaseInfo(data);
        return data;
    }

    @Override
    public void read(JsonObject data) {
        this.readBaseInfo(data);

    }

    @Override
    public CompoundNBT write(CompoundNBT data) {
        this.writeBaseInfo(data);
        return data;
    }

    @Override
    public void read(CompoundNBT data) {
        this.readBaseInfo(data);

    }
}
