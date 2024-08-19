package cat.jiu.core.util.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

public class RenderUtils {
    static final HashMap<ResourceLocation, Integer> TEXTURE_ID = new HashMap<>();
    static final HashMap<Integer, ResourceLocation> TEXTURE_RL = new HashMap<>();
    public static void draw(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        graphics.blit(texture, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
    }
    public static void draw(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, float u, float v, int textureWidth, int textureHeight) {
        draw(graphics, texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }
    public static void draw(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, Object nothing) {
        draw(graphics, texture, x, y, width, height, u, v, uWidth, vHeight, 256, 256);
    }
    public static void draw(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, int u, int v) {
        draw(graphics, texture, x, y, width, height, u, v, 256, 256);
    }

    public static void draw(GuiGraphics graphics, int texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        int
                x2 = x + width,
                y2 = y + height;
        float
                minU = (u + 0.0F) / (float)textureWidth,
                maxU = (u + (float)uWidth) / (float)textureWidth,
                minV = (v + 0.0F) / (float)textureHeight,
                maxV = (v + (float)vHeight) / (float)textureHeight;
        int blitOffset = 0;

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = graphics.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix4f, (float) x, (float) y, (float)blitOffset).uv(minU, minV).endVertex();
        bufferbuilder.vertex(matrix4f, (float) x, (float)y2, (float)blitOffset).uv(minU, maxV).endVertex();
        bufferbuilder.vertex(matrix4f, (float)x2, (float)y2, (float)blitOffset).uv(maxU, maxV).endVertex();
        bufferbuilder.vertex(matrix4f, (float)x2, (float) y, (float)blitOffset).uv(maxU, minV).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
    }
    public static void draw(GuiGraphics graphics, int texture, int x, int y, int width, int height, float u, float v, int textureWidth, int textureHeight) {
        draw(graphics, texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }
    public static void draw(GuiGraphics graphics, int texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, Object nothing) {
        draw(graphics, texture, x, y, width, height, u, v, uWidth, vHeight, 256, 256);
    }
    public static void draw(GuiGraphics graphics, int texture, int x, int y, int width, int height, int u, int v) {
        draw(graphics, texture, x, y, width, height, u, v, 256, 256);
    }

    public static void bindTexture(int texture) {
        if (TEXTURE_RL.containsKey(texture)) {
            RenderSystem.setShaderTexture(0, texture);
        }else {
            RenderSystem.setShaderTexture(0, TEXTURE_RL.get(texture));
        }
    }
    public static void bindTexture(ResourceLocation texture) {
        if (TEXTURE_ID.containsKey(texture)) {
            RenderSystem.setShaderTexture(0, TEXTURE_ID.get(texture));
        }else {
            RenderSystem.setShaderTexture(0, texture);
        }
    }

    public static int genGLTextureID(ResourceLocation rl, BufferedImage image, boolean enableAlpha) {
        int glID = genGLTextureID(image, enableAlpha);
        TEXTURE_ID.put(rl, glID);
        TEXTURE_RL.put(glID, rl);
        return glID;
    }
    public static int genGLTextureID(BufferedImage image, boolean enableAlpha) {
        int glID = TextureUtil.generateTextureId();
        uploadGLTexture(glID, enableAlpha, image);
        return glID;
    }

    public static void uploadGLTexture(int glID, boolean enableAlpha, BufferedImage image) {
        int[] pixels = new int[image.getWidth() * image.getHeight()];//创建像素列表
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());//获取图片像素

        ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * (enableAlpha ? 4 : 3));//创建字节缓冲区，*4是包含alpha *3不包含

        //遍历图片像素转换为RGBA
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = pixels[y * image.getWidth() + x];
                Color color = new Color(pixel);
                buffer.put((byte) color.getRed());//像素点的Red
                buffer.put((byte) color.getGreen());//像素点的Green
                buffer.put((byte) color.getBlue());//像素点的Blue
                if (enableAlpha) {
                    buffer.put((byte) color.getAlpha());//像素点的Alpha
                }
            }
        }
        buffer.flip(); //一定要翻转

        try(NativeImage nativeImage = NativeImage.read(buffer)) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(() -> {
                    TextureUtil.prepareImage(glID, image.getWidth(), image.getHeight());
                    nativeImage.upload(0, 0, 0, false);
                });
            } else {
                TextureUtil.prepareImage(glID, image.getWidth(), image.getHeight());
                nativeImage.upload(0, 0, 0, false);
            }
        } catch (IOException ignored) {
        }
    }

    public static void drawVerticalString(GuiGraphics graphics, List<String> text, int x, int y, int color, boolean drawShadow, boolean alignRight, int marinSide, int marinDown) {
        for (String s : text) {
            drawVerticalString(graphics, s, x, y, color, drawShadow, marinDown);
            int firstWidth = getFontRenderer().width(s) + marinSide;
            if (alignRight) {
                x += firstWidth;
            }else {
                x -= firstWidth;
            }
        }
    }

    public static void drawString(GuiGraphics graphics, List<String> text, int x, int y, int color, boolean drawShadow, boolean alignRight, int marinSide, int marinDown) {
        for (String s : text) {
            drawString(graphics, s, x, y, color, drawShadow, alignRight, marinSide);
            y += getFontRenderer().lineHeight+ marinDown;
        }
    }

    public static void drawString(GuiGraphics graphics, String text, int x, int y, int color, boolean drawShadow, boolean alignRight, int marinSide) {
        if (marinSide!=0) {
            for (char c : text.toCharArray()) {
                String s = String.valueOf(c);
                drawString(graphics, s, x, y, color, drawShadow);
                if (alignRight) {
                    x -= getFontRenderer().width(s) + marinSide;
                }else {
                    x += getFontRenderer().width(s) + marinSide;
                }
            }
        }else {
            if (alignRight) {
                drawRightString(graphics, text, x, y, color, drawShadow);
            }else {
                drawString(graphics, text, x, y, color, drawShadow);
            }
        }
    }
    public static void drawVerticalString(GuiGraphics graphics, String text, int x, int y, int color, boolean drawShadow, int marinDown) {
        for (char c : text.toCharArray()) {
            String s = String.valueOf(c);
            drawString(graphics, s, x, y, color, drawShadow);
            y += getFontRenderer().lineHeight + marinDown;
        }
    }
    public static void drawRightString(GuiGraphics graphics, String text, int x, int y, int color, boolean drawShadow) {
        drawString(graphics, text, x - getFontRenderer().width(text), y, color, drawShadow);
    }

    public static void drawCenteredString(GuiGraphics graphics, String pText, int pX, int pY, int pColor, boolean drawShadow) {
        drawString(graphics, pText, pX - getFontRenderer().width(pText) / 2, pY, pColor, drawShadow);
    }
    public static void drawString(GuiGraphics graphics, String s, int x, int y, int color, boolean drawShadow) {
        graphics.drawString(getFontRenderer(), s, x, y, color, drawShadow);
    }

    public static Font getFontRenderer(){
        return Minecraft.getInstance().font;
    }
    public static int getTextWidth(String s) {
        return getFontRenderer().width(s);
    }
    public static int getTextWidth(Component s) {
        return getFontRenderer().width(s);
    }
}
