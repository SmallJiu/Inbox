package cat.jiu.core.util.element.image;

import cat.jiu.core.util.element.image.gif.BaseGifImage;
import cat.jiu.core.util.client.GifDecoder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.util.*;

public class ImageJarPath extends BaseGifImage {
    public static final ResourceLocation ID = new ResourceLocation("jiucore", "element/image/path/jar");
    protected static final List<Function2<Class<?>, String, GifDecoder.IGifTexture>> DECODERS = new ArrayList<>();
    public static void register(Function2<Class<?>, String, GifDecoder.IGifTexture> decoder) {
        DECODERS.add(decoder);
    }
    public static interface Function2<T1, T2, R> {
        R apply(T1 t1, T2 t2);
    }

    protected final Map<Class<?>, Set<String>> paths = new HashMap<>();
    public ImageJarPath() {
    }

    public Map<Class<?>, Set<String>> getPaths() {
        return paths;
    }
    public ImageJarPath addPaths(Class<?> clazz, String... paths) {
        if (!this.paths.containsKey(clazz)) {
            this.paths.put(clazz, new HashSet<>());
        }
        for (String path : paths) {
            this.paths.get(clazz).add(path);
        }
        return this;
    }

    @Override
    public void init() {
        this.loadImages();
    }

    public ImageJarPath loadImages() {
        this.setGif(read(this.paths));
        return this;
    }

    public static GifDecoder.IGifTexture read(Map<Class<?>, Set<String>> paths) {
        if (paths.size() > 1) {
            GifDecoder.GifTextures image = new GifDecoder.GifTextures();
            paths.forEach((k, v)->
                image.addTexture(read(k, v))
            );
            return image;
        }
        Class<?> clazz = paths.keySet().toArray(new Class<?>[1])[0];
        return read(clazz, paths.get(clazz));
    }

    public static GifDecoder.IGifTexture read(Class<?> clazz, Set<String> paths) {
        if (paths.size() > 1) {
            GifDecoder.GifTextures image = new GifDecoder.GifTextures();
            for (String file : paths) {
                try {
                    if (file.endsWith(".gif")) {
                        image.addTexture(GifDecoder.getTexture(clazz.getResourceAsStream(file), -1));
                    }else if (file.endsWith(".png")) {
                        image.addTexture(GifDecoder.singleton(ImageIO.read(clazz.getResourceAsStream(file))));

                    }else {
                        for (Function2<Class<?>, String, GifDecoder.IGifTexture> decoder : DECODERS) {
                            GifDecoder.IGifTexture texture = decoder.apply(clazz, file);
                            if (texture!=null) {
                                image.addTexture(texture);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return image;
        }else {
            String path = paths.toArray(new String[1])[0];
            if (path.endsWith(".gif")) {
                return GifDecoder.getTexture(clazz.getResourceAsStream(path), -1);
            }else if (path.endsWith(".png")) {
                try {
                    return GifDecoder.singleton(ImageIO.read(clazz.getResourceAsStream(path)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else {
                for (Function2<Class<?>, String, GifDecoder.IGifTexture> decoder : DECODERS) {
                    GifDecoder.IGifTexture texture = decoder.apply(clazz, path);
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
        this.paths.forEach((clazz, list)->{
            JsonObject object = new JsonObject();
            object.addProperty("clazz", String.valueOf(clazz));

            JsonArray array1 = new JsonArray();
            list.forEach(array1::add);
            object.add("path", array1);

            array.add(object);
        });
        data.add("paths", array);
        return data;
    }

    @Override
    public void read(JsonObject data) {
        this.readBaseInfo(data);
        data.getAsJsonArray("paths").forEach(element -> {
            JsonObject object = element.getAsJsonObject();
            try {
                Class<?> clazz = Class.forName(object.get("clazz").getAsString());
                object.getAsJsonArray("path").forEach(path->
                    this.addPaths(clazz, path.getAsString())
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public CompoundTag write(CompoundTag data) {
        this.writeBaseInfo(data);
        ListTag array = new ListTag();
        this.paths.forEach((clazz, list)->{
            CompoundTag object = new CompoundTag();
            object.putString("clazz", String.valueOf(clazz));

            ListTag array1 = new ListTag();
            list.forEach(path->array1.add(StringTag.valueOf(path)));
            object.put("path", array1);

            array.add(object);
        });
        data.put("paths", array);
        return data;
    }

    @Override
    public void read(CompoundTag data) {
        this.readBaseInfo(data);
        ListTag array = data.getList("paths", 10);
        for (int i = 0; i < array.size(); i++) {
            CompoundTag object = array.getCompound(i);
            try {
                Class<?> clazz = Class.forName(object.getString("clazz"));
                ListTag array1 = data.getList("path", 8);
                for (int i1 = 0; i1 < array1.size(); i1++) {
                    this.addPaths(clazz, array1.getString(i1));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
