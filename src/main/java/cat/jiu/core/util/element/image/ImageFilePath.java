package cat.jiu.core.util.element.image;

import cat.jiu.core.util.element.image.gif.BaseGifImage;
import cat.jiu.core.util.client.GifDecoder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ImageFilePath extends BaseGifImage {
    public static final ResourceLocation ID = new ResourceLocation("jiucore", "element/image/path");
    protected static final List<Function<File, GifDecoder.IGifTexture>> DECODERS = new ArrayList<>();
    public static void register(Function<File, GifDecoder.IGifTexture> decoder) {
        DECODERS.add(decoder);
    }

    protected String[] paths;
    public ImageFilePath() {
    }

    public ImageFilePath(String... paths) {
        this.paths = paths;
    }

    public ImageFilePath(File... paths) {
        this.paths = new String[paths.length];
        for (int i = 0; i < paths.length; i++) {
            this.paths[i] = paths[i].toString();
        }
    }

    @Override
    public void init() {
        this.setGif(read(this.paths));
    }

    public String[] getPaths() {
        return paths;
    }

    public static GifDecoder.IGifTexture read(String... paths) {
        if (paths.length > 1) {
            GifDecoder.GifTextures image = new GifDecoder.GifTextures();
            for (String file : paths) {
                if (file.endsWith(".gif")) {
                    image.addTexture(GifDecoder.getTexture(file, -1));
                }else if (file.endsWith(".png")) {
                    try {
                        image.addTexture(GifDecoder.singleton(file));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else {
                    File f = new File(file);
                    for (Function<File, GifDecoder.IGifTexture> decoder : DECODERS) {
                        GifDecoder.IGifTexture texture = decoder.apply(f);
                        if (texture!=null) {
                            image.addTexture(texture);
                            break;
                        }
                    }
                }
            }
            return image;
        }else {
            String path = paths[0];
            if (path.endsWith(".gif")) {
                return GifDecoder.getTexture(paths[0], -1);
            }else if (path.endsWith(".png")) {
                try {
                    return GifDecoder.singleton(paths[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                File f = new File(path);
                for (Function<File, GifDecoder.IGifTexture> decoder : DECODERS) {
                    GifDecoder.IGifTexture texture = decoder.apply(f);
                    if (texture!=null) {
                        return texture;
                    }
                }
            }
        }
        return new GifDecoder.GifTextures();
    }

    @Override
    public ResourceLocation getImageType() {
        return ID;
    }

    @Override
    public JsonObject write(JsonObject data) {
        this.writeBaseInfo(data);
        JsonArray array = new JsonArray();
        for (String image : this.paths) {
            array.add(image);
        }
        data.add("paths", array);
        return data;
    }

    @Override
    public void read(JsonObject data) {
        this.readBaseInfo(data);
        JsonArray array = data.getAsJsonArray("paths");
        this.paths = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            this.paths[i] = array.get(i).getAsString();
        }
    }

    @Override
    public CompoundTag write(CompoundTag data) {
        this.writeBaseInfo(data);
        ListTag array = new ListTag();
        for (String image : this.paths) {
            array.add(StringTag.valueOf(image));
        }
        data.put("paths", array);
        return data;
    }

    @Override
    public void read(CompoundTag data) {
        this.readBaseInfo(data);
        ListTag array = data.getList("paths", 8);
        this.paths = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            this.paths[i] = array.getString(i);
        }
    }
}
