package cat.jiu.core.api.element;

import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.NBTUtils;
import cat.jiu.core.api.handler.IJsonSerializable;
import cat.jiu.core.api.handler.INBTSerializable;
import cat.jiu.core.util.element.image.*;
import cat.jiu.core.util.element.image.gif.*;
import cat.jiu.core.util.registry.DynamicRegistry;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

public interface IImage extends IJsonSerializable, INBTSerializable, Supplier<ResourceLocation> {
    String ID_NAME = "type";
    DynamicRegistry<ResourceLocation, IImage> REGISTRY = new DynamicRegistry<ResourceLocation, IImage>("jiucore", "element/image")
            .register(registry -> {
                registry.register(ImageGL.ID,                  ImageGL.class);
                registry.register(ImageBuffered.ID,            ImageBuffered.class);
                registry.register(ImageSingletonGif.ID,        ImageSingletonGif.class);
                registry.register(ImageGif.ID,                 ImageGif.class);
                registry.register(ImageGifs.ID,                ImageGifs.class);
                registry.register(ImageFilePath.ID,            ImageFilePath.class);
                registry.register(ImageJarPath.ID,             ImageJarPath.class);
                registry.register(ImageMC.ID,                  ImageMC.class);
            })
            .setKeyGetter(
                    data->new ResourceLocation(data.getString(ID_NAME)),
                    data->new ResourceLocation(data.get(ID_NAME).getAsString())
            );

    @OnlyIn(Dist.CLIENT)
    void render(MatrixStack stack, int renderToX, int renderToY, int renderWidth, int renderHeight, float partialTick);

    boolean isInitImage();
    void initImage();
    ResourceLocation getImageType();
    @Override
    default ResourceLocation get(){
        return this.getImageType();
    }

    default void setRenderInfo(int u, int v, int uWidth, int vHeight) {

    }
    default void setImageSize(int width, int height) {

    }

    default int getWidth() {
        return 50;
    }
    default int getHeight() {
        return 50;
    }

    static abstract class BaseImage implements IImage {
        protected int width = 50, height = 50, u, v, uWidth, vHeight;
        public void setImageSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
        public void setRenderInfo(int u, int v, int uWidth, int vHeight) {
            this.u = u;
            this.v = v;
            this.uWidth = uWidth;
            this.vHeight = vHeight;
        }

        public int getU() {
            return u;
        }

        public int getV() {
            return v;
        }

        public int getUWidth() {
            return uWidth;
        }

        public int getVHeight() {
            return vHeight;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        protected boolean init;
        @OnlyIn(Dist.CLIENT)
        @Override
        public void initImage() {
            if (!this.isInitImage()) {
                this.init();
                this.init = true;
            }
        }
        @OnlyIn(Dist.CLIENT)
        protected abstract void init();
        @OnlyIn(Dist.CLIENT)
        @Override
        public boolean isInitImage() {
            return this.init;
        }

        @Override
        public JsonObject write(JsonObject data) {
            this.writeBaseInfo(data);
            return data;
        }
        protected void writeBaseInfo(JsonObject data) {
            data.addProperty(ID_NAME, String.valueOf(this.getImageType()));
            data.addProperty("width", this.width);
            data.addProperty("height", this.height);
            data.addProperty("u", this.u);
            data.addProperty("v", this.v);
            data.addProperty("uWidth", this.uWidth);
            data.addProperty("vHeight", this.vHeight);
        }

        @Override
        public void read(JsonObject data) {
            this.readBaseInfo(data);
        }
        protected void readBaseInfo(JsonObject data) {
            this.width = JsonUtils.get(data, "width", 50);
            this.height = JsonUtils.get(data, "height", 50);
            this.u = JsonUtils.get(data, "u", 0);
            this.v = JsonUtils.get(data, "v", 0);
            this.uWidth = JsonUtils.get(data, "uWidth", 0);
            this.vHeight = JsonUtils.get(data, "vHeight", 0);
        }

        @Override
        public CompoundNBT write(CompoundNBT data) {
            this.writeBaseInfo(data);
            return data;
        }
        protected void writeBaseInfo(CompoundNBT data) {
            data.putString(ID_NAME, String.valueOf(this.getImageType()));
            data.putInt("width", this.width);
            data.putInt("height", this.height);
            data.putInt("u", this.u);
            data.putInt("v", this.v);
            data.putInt("uWidth", this.uWidth);
            data.putInt("vHeight", this.vHeight);
        }

        @Override
        public void read(CompoundNBT data) {
            this.readBaseInfo(data);
        }
        protected void readBaseInfo(CompoundNBT data) {
            this.width = NBTUtils.get(data, "width", 50);
            this.height = NBTUtils.get(data, "height", 50);
            this.u = NBTUtils.get(data, "u", 0);
            this.v = NBTUtils.get(data, "v", 0);
            this.uWidth = NBTUtils.get(data, "uWidth", 0);
            this.vHeight = NBTUtils.get(data, "vHeight", 0);
        }
    }
}
