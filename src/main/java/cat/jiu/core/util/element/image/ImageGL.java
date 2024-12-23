package cat.jiu.core.util.element.image;

import cat.jiu.core.api.element.IImage;
import cat.jiu.core.util.client.RenderUtils;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.image.BufferedImage;

public class ImageGL extends IImage.BaseImage {
    public static final ResourceLocation ID = new ResourceLocation("jiucore", "element/image/gl");
    protected int glID = -1;

    public ImageGL() {
    }

    public ImageGL(int glID) {
        this.glID = glID;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(GuiGraphics graphics, int renderToX, int renderToY, int renderWidth, int renderHeight, float partialTick) {
        RenderUtils.draw(graphics, this.glID, renderToX, renderToY, renderWidth, renderHeight, this.getU(), this.getV(), this.getUWidth(), this.getVHeight(), null);
    }

    public int getGlID() {
        return glID;
    }

    public void setImage(BufferedImage image) {
        if (image != null) {
            this.setImage(RenderUtils.uploadGLTexture(image));
        }
    }
    public void setImage(int glID) {
        if (this.glID!=-1) {
            RenderSystem.deleteTexture(this.glID);
        }
        this.glID = glID;
    }

    @Override
    public void init() {

    }

    @Override
    public ResourceLocation getImageType() {
        return ID;
    }

    @Override
    public JsonObject write(JsonObject json) {
        super.write(json);
        json.addProperty("glID", this.glID);
        return json;
    }

    @Override
    public void read(JsonObject json) {
        super.read(json);
        this.setImage(json.has("glID") ? json.get("glID").getAsInt() : -1);
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {
        super.write(nbt);
        nbt.putInt("glID", this.glID);
        return nbt;
    }

    @Override
    public void read(CompoundTag nbt) {
        super.read(nbt);
        this.setImage(nbt.contains("glID") ? nbt.getInt("glID") : -1);
    }
}
