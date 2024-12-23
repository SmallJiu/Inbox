package cat.jiu.core.util.element.image.gif;

import cat.jiu.core.util.client.GifDecoder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;

public class ImageGif extends BaseGifImage {
    public static final ResourceLocation ID = new ResourceLocation("jiucore", "element/image/gif/normal");

    public ImageGif() {
    }

    public ImageGif(GifDecoder.GifTexture gif) {
        super(gif);
    }

    @Override
    public ResourceLocation getImageType() {
        return ID;
    }

    @Override
    public JsonObject write(JsonObject data) {
        this.writeBaseInfo(data);
        JsonArray array = new JsonArray();
        for (int i = 0; i < this.getGif().getAllTextureCount(); i++) {
            array.add(this.writeTo(new JsonObject(), this.getGif().getImage(i)));
        }
        data.add("frames", array);
        return data;
    }

    @Override
    public void read(JsonObject data) {
        this.readBaseInfo(data);
        this.setGif(new GifDecoder.GifTextures());
        for (JsonElement element : data.getAsJsonArray("frames")) {
            ((GifDecoder.GifTextures)this.getGif()).addTexture(new GifDecoder.SingletonGifTexture(this.readFrom(element.getAsJsonObject())));
        }
    }

    @Override
    public CompoundTag write(CompoundTag data) {
        this.writeBaseInfo(data);
        ListTag array = new ListTag();
        for (int i = 0; i < this.getGif().getAllTextureCount(); i++) {
            array.add(this.writeTo(new CompoundTag(), this.getGif().getImage(i)));
        }
        data.put("frames", array);
        return data;
    }

    @Override
    public void read(CompoundTag data) {
        this.readBaseInfo(data);
        this.setGif(new GifDecoder.GifTextures());
        ListTag array = data.getList("frames", 10);
        for (int i = 0; i < array.size(); i++) {
            ((GifDecoder.GifTextures)this.getGif()).addTexture(new GifDecoder.SingletonGifTexture(this.readFrom(array.getCompound(i))));
        }
    }
}
