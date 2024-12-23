package cat.jiu.core.util.element.image;

import cat.jiu.core.api.element.IImage;
import cat.jiu.core.util.client.RenderUtils;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ImageMC extends IImage.BaseImage {
    public static final ResourceLocation ID = new ResourceLocation("jiucore", "element/image/mc");
    protected ResourceLocation texture;

    public ImageMC() {
    }

    public ImageMC(ResourceLocation texture) {
        this.texture = texture;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.setTexture(new ResourceLocation(texture));
    }
    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(GuiGraphics graphics, int renderToX, int renderToY, int renderWidth, int renderHeight, float partialTick) {
        RenderUtils.draw(graphics, this.texture, renderToX, renderToY, renderWidth, renderHeight, this.getU(), this.getV(), this.getUWidth(), this.getVHeight(), null);
    }

    @Override
    public ResourceLocation getImageType() {
        return ID;
    }

    @Override
    public void init() {

    }

    @Override
    public JsonObject write(JsonObject data) {
        this.writeBaseInfo(data);
        data.addProperty("texture", String.valueOf(this.texture));
        return data;
    }

    @Override
    public void read(JsonObject data) {
        this.readBaseInfo(data);
        this.setTexture(data.get("texture").getAsString());
    }

    @Override
    public CompoundTag write(CompoundTag data) {
        this.writeBaseInfo(data);
        data.putString("texture", String.valueOf(this.texture));
        return data;
    }

    @Override
    public void read(CompoundTag data) {
        this.readBaseInfo(data);
        this.setTexture(data.getString("texture"));
    }
}
