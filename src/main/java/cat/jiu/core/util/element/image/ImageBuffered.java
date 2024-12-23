package cat.jiu.core.util.element.image;

import cat.jiu.core.util.client.RenderUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.image.BufferedImage;

public class ImageBuffered extends ImageGL {
    public static final ResourceLocation ID = new ResourceLocation("jiucore", "element/image/buffered");
    protected BufferedImage image;

    public ImageBuffered() {
    }

    public ImageBuffered(BufferedImage image) {
        this.setImage(image);
    }

    public ImageBuffered(BufferedImage image, int glID) {
        this.image = image;
        super.setImage(glID);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(GuiGraphics graphics, int renderToX, int renderToY, int renderWidth, int renderHeight, float partialTick) {
        RenderUtils.draw(graphics, this.glID, renderToX, renderToY, renderWidth, renderHeight, this.u, this.v, this.uWidth, this.vHeight, this.image.getWidth(), this.image.getHeight());
    }

    @Override
    public void init() {
        super.setImage(this.image);
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        if (this.getGlID() != -1) {
            RenderUtils.uploadGLTexture(this.getGlID(), image);
        }
    }

    @Override
    public ResourceLocation getImageType() {
        return ID;
    }

    @Override
    public JsonObject write(JsonObject data) {
        return this.writeTo(data, this.getImage());
    }
    protected JsonObject writeTo(JsonObject data, BufferedImage image) {
        this.writeBaseInfo(data);
        data.addProperty("width", image.getWidth());
        data.addProperty("weight", image.getHeight());
        data.addProperty("colorType", image.getType());

        JsonArray pixels = new JsonArray();

        JsonObject pixel = new JsonObject();
        for (int u = 0; u < image.getWidth(); u++) {
            for (int v = 0; v < image.getHeight(); v++) {
                pixel.addProperty("u", u);
                pixel.addProperty("v", v);
                pixel.addProperty("color", image.getRGB(u, v));
                pixels.add(pixel);
            }
        }

        data.add("pixels", pixels);

        return data;
    }

    @Override
    public void read(JsonObject data) {
        this.readBaseInfo(data);
        this.setImage(this.readFrom(data));
    }
    protected BufferedImage readFrom(JsonObject data) {
        BufferedImage image = new BufferedImage(data.get("width").getAsInt(), data.get("height").getAsInt(), data.get("colorType").getAsInt());
        for (JsonElement pixelElement : data.getAsJsonArray("pixels")) {
            JsonObject pixel = pixelElement.getAsJsonObject();
            image.setRGB(pixel.get("u").getAsInt(), pixel.get("v").getAsInt(), pixel.get("color").getAsInt());
        }
        return image;
    }

    @Override
    public CompoundTag write(CompoundTag data) {
        return this.writeTo(data, this.getImage());
    }
    protected CompoundTag writeTo(CompoundTag data, BufferedImage image) {
        this.writeBaseInfo(data);
        data.putInt("width", image.getWidth());
        data.putInt("weight", image.getHeight());
        data.putInt("colorType", image.getType());

        ListTag pixels = new ListTag();

        CompoundTag pixel = new CompoundTag();
        for (int u = 0; u < image.getWidth(); u++) {
            for (int v = 0; v < image.getHeight(); v++) {
                pixel.putInt("u", u);
                pixel.putInt("v", v);
                pixel.putInt("color", image.getRGB(u, v));
                pixels.add(pixel);
            }
        }

        data.put("pixels", pixels);
        return data;
    }

    @Override
    public void read(CompoundTag data) {
        this.readBaseInfo(data);
        this.setImage(this.readFrom(data));
    }
    protected BufferedImage readFrom(CompoundTag data) {
        BufferedImage image = new BufferedImage(data.getInt("width"), data.getInt("height"), data.getInt("colorType"));
        ListTag pixels = data.getList("pixels", 10);
        for (int i = 0; i < pixels.size(); i++) {
            CompoundTag pixel = pixels.getCompound(i);
            image.setRGB(pixel.getInt("u"), pixel.getInt("v"), pixel.getInt("color"));
        }
        return image;
    }
}
