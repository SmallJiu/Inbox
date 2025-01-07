package cat.jiu.core.util.client;

import cat.jiu.core.api.element.IText;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.*;
import org.lwjgl.BufferUtils;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RenderUtils {

    // image
    public static void draw(MatrixStack stack, ResourceLocation texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        bindTexture(texture);
        Screen.blit(stack, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
    }
    public static void draw(MatrixStack stack, ResourceLocation texture, int x, int y, int width, int height, float u, float v, int textureWidth, int textureHeight) {
        draw(stack, texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }
    public static void draw(MatrixStack stack, ResourceLocation texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, Object nothing) {
        draw(stack, texture, x, y, width, height, u, v, uWidth, vHeight, 256, 256);
    }
    public static void draw(MatrixStack stack, ResourceLocation texture, int x, int y, int width, int height, int u, int v) {
        draw(stack, texture, x, y, width, height, u, v, 256, 256);
    }

    public static void draw(MatrixStack stack, int texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, int textureWidth, int textureHeight) {
//        int
//                x2 = x + width,
//                y2 = y + height;
//        float
//                minU = (u + 0.0F) / (float)textureWidth,
//                maxU = (u + (float)uWidth) / (float)textureWidth,
//                minV = (v + 0.0F) / (float)textureHeight,
//                maxV = (v + (float)vHeight) / (float)textureHeight;
//        int blitOffset = 0;

        bindTexture(texture);
        Screen.blit(stack, x, y, width, height, u, v, uWidth, vHeight, textureWidth, textureHeight);
//        RenderSystem.setShader(GameRenderer::getPositionTexShader);
//        Matrix4f matrix4f = stack.pose().last().pose();
//        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
//        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
//        bufferbuilder.vertex(matrix4f, (float) x, (float) y, (float)blitOffset).uv(minU, minV).endVertex();
//        bufferbuilder.vertex(matrix4f, (float) x, (float)y2, (float)blitOffset).uv(minU, maxV).endVertex();
//        bufferbuilder.vertex(matrix4f, (float)x2, (float)y2, (float)blitOffset).uv(maxU, maxV).endVertex();
//        bufferbuilder.vertex(matrix4f, (float)x2, (float) y, (float)blitOffset).uv(maxU, minV).endVertex();
//        BufferUploader.drawWithShader(bufferbuilder.end());
//        Matrix4f stack4f = stack.getLast().getMatrix();
    }
    public static void draw(MatrixStack stack, int texture, int x, int y, int width, int height, float u, float v, int textureWidth, int textureHeight) {
        draw(stack, texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }
    public static void draw(MatrixStack stack, int texture, int x, int y, int width, int height, float u, float v, int uWidth, int vHeight, Object nothing) {
        draw(stack, texture, x, y, width, height, u, v, uWidth, vHeight, 256, 256);
    }
    public static void draw(MatrixStack stack, int texture, int x, int y, int width, int height, int u, int v) {
        draw(stack, texture, x, y, width, height, u, v, 256, 256);
    }

    public static void bindTexture(int texture) {
        RenderSystem.bindTexture(texture);
    }
    public static void bindTexture(ResourceLocation texture) {
        Minecraft.getInstance().getTextureManager().bindTexture(texture);
    }

    public static int uploadGLTexture(BufferedImage image) {
        int glID;
        try(BufferedTexture texture = new BufferedTexture(image, true)) {
            glID = texture.getGlTextureId();
        }
        return glID;
    }

    public static void uploadGLTexture(int glID, BufferedImage image) {
        new BufferedTexture(glID, image, true).close();
    }

    public static void registerToMinecraft(ResourceLocation id, BufferedImage image, boolean enableAlpha) throws IOException {
        Minecraft.getInstance().getTextureManager().loadTexture(id, new DynamicTexture(NativeImage.read(toBuffer(image, enableAlpha))));
    }

    public static ByteBuffer toBuffer(BufferedImage image, boolean enableAlpha) {
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
        return buffer;
    }

    // text

    public static FontRenderer getFontRenderer(){
        return Minecraft.getInstance().fontRenderer;
    }
    public static int fontHeight(){
        return getFontRenderer().FONT_HEIGHT;
    }
    public static int getFontHeight() {
        return getFontRenderer().FONT_HEIGHT;
    }
    public static int width(IText s) {
        return getFontRenderer().getStringWidth(s.format());
    }
    public static int width(String s) {
        return getFontRenderer().getStringWidth(s);
    }
    public static int width(char s) {
        return getFontRenderer().getStringWidth(String.valueOf(s));
    }
    public static int width(ITextComponent s) {
        return getFontRenderer().getStringPropertyWidth(s);
    }
    public static int width(IReorderingProcessor s) {
        return getFontRenderer().func_243245_a(s);
    }

    public static List<IReorderingProcessor> split(String text, int maxLength, boolean useMcWarp) {
        return split(new TranslationTextComponent(text), maxLength, useMcWarp);
    }
    public static List<IReorderingProcessor> split(ITextComponent text, int maxLength, boolean useMcWarp) {
        if (useMcWarp) {
            return getFontRenderer().trimStringToWidth(text, maxLength);
        }else {
            List<IReorderingProcessor> list = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            for (char c : text.getString().toCharArray()) {
                sb.append(c);
                if (width(sb.toString()) >= maxLength) {
                    list.add(IReorderingProcessor.fromString(sb.toString(), Style.EMPTY));
                }
            }
            return list;
        }
    }

        // string

    public static void drawStringTooltip(MatrixStack stack, List<String> tooltips, int x, int y) {
        net.minecraftforge.fml.client.gui.GuiUtils.drawHoveringText(stack, tooltips.stream().map(StringTextComponent::new).collect(Collectors.toList()), x, y, Minecraft.getInstance().getMainWindow().getScaledWidth(), Minecraft.getInstance().getMainWindow().getScaledHeight(), -1, getFontRenderer());
    }

    public static void drawString(MatrixStack stack, String text, int x, int y, int color, boolean drawShadow) {
        if (drawShadow) {
            getFontRenderer().drawStringWithShadow(stack, text, x, y, color);
        }else {
            getFontRenderer().drawString(stack, text, x, y, color);
        }
    }
    public static void drawString(MatrixStack stack, List<String> text, int x, int y, int color, boolean drawShadow) {
        for (String s : text) {
            drawString(stack, s, x, y, color, drawShadow);
            y += getFontHeight() + 1;
        }
    }

    public static void drawCenteredString(MatrixStack stack, String text, int x, int y, int color, boolean drawShadow) {
        drawString(stack, text, x - width(text) / 2, y, color, drawShadow);
    }
    public static void drawCenteredString(MatrixStack stack, List<String> text, int x, int y, int color, boolean drawShadow) {
        for (String s : text) {
            drawCenteredString(stack, s, x, y, color, drawShadow);
            y += getFontHeight() + 1;
        }
    }

    /**
     * like<p>
     *     't'<p>
     *     'h'<p>
     *     'i'<p>
     *     's'<p>
     *     ' '<p>
     *     'i'<p>
     *     's'<p>
     *     ' '<p>
     *     't'<p>
     *     'e'<p>
     *     'x'<p>
     *     't'
     */
    public static void drawVerticalString(MatrixStack stack, String text, int x, int y, int color, boolean drawShadow, int marinDown) {
        for (char c : text.toCharArray()) {
            String s = String.valueOf(c);
            drawString(stack, s, x, y, color, drawShadow);
            y += getFontHeight() + marinDown;
        }
    }
    public static void drawVerticalString(MatrixStack stack, List<String> text, int x, int y, int color, boolean drawShadow, boolean alignRight, int marinSide, int marinDown) {
        for (String s : text) {
            drawVerticalString(stack, s, x, y, color, drawShadow, marinDown);
            int firstWidth = width(s) + marinSide;
            if (alignRight) {
                x += firstWidth;
            }else {
                x -= firstWidth;
            }
        }
    }

    public static void drawRightString(MatrixStack stack, String text, int x, int y, int color, boolean drawShadow) {
        drawString(stack, text, x - width(text), y, color, drawShadow);
    }

    /**
     * like<p>
     * '           this is text a'<p>
     * '        this is test text'<p>
     * 'hey! this is a test text!'<p>
     * '      this is a test text'<p>
     */
    public static void drawRightString(MatrixStack stack, List<String> text, int x, int y, int color, boolean drawShadow) {
        int maxWidth = 0;
        for (String s : text) {
            maxWidth = Math.max(maxWidth, width(s));
        }
        x -= maxWidth;
        for (String s : text) {
            drawString(stack, s, x, y, color, drawShadow);
            y += getFontHeight() + 1;
        }
    }

    public static void drawString(MatrixStack stack, String text, int x, int y, int color, boolean drawShadow, boolean alignRight, int marinSide) {
        if (marinSide!=0) {
            for (char c : text.toCharArray()) {
                String s = String.valueOf(c);
                drawString(stack, s, x, y, color, drawShadow);
                if (alignRight) {
                    x -= width(s) + marinSide;
                }else {
                    x += width(s) + marinSide;
                }
            }
        }else {
            if (alignRight) {
                drawRightString(stack, text, x, y, color, drawShadow);
            }else {
                drawString(stack, text, x, y, color, drawShadow);
            }
        }
    }
    public static void drawString(MatrixStack stack, List<String> text, int x, int y, int color, boolean drawShadow, boolean alignRight, int marinSide, int marinDown) {
        for (String s : text) {
            drawString(stack, s, x, y, color, drawShadow, alignRight, marinSide);
            y += getFontHeight()+ marinDown;
        }
    }

        // Component

    public static void drawComponent(MatrixStack stack, ITextComponent text, int x, int y, int color, boolean drawShadow) {
        if (drawShadow) {
            getFontRenderer().drawTextWithShadow(stack, text, x, y, color);
        }else {
            getFontRenderer().drawText(stack, text, x, y, color);
        }
    }
    public static void drawComponent(MatrixStack stack, List<ITextComponent> text, int x, int y, int color, boolean drawShadow) {
        for (ITextComponent s : text) {
            drawComponent(stack, s, x, y, color, drawShadow);
            y += getFontHeight() + 1;
        }
    }

    public static void drawCenteredComponent(MatrixStack stack, ITextComponent text, int pX, int pY, int pColor, boolean drawShadow) {
        drawComponent(stack, text, pX - width(text) / 2, pY, pColor, drawShadow);
    }
    public static void drawCenteredComponent(MatrixStack stack, List<ITextComponent> text, int x, int y, int color, boolean drawShadow) {
        for (ITextComponent s : text) {
            drawCenteredComponent(stack, s, x, y, color, drawShadow);
            y += getFontHeight() + 1;
        }
    }

    public static void drawRightComponent(MatrixStack stack, ITextComponent text, int x, int y, int color, boolean drawShadow) {
        drawComponent(stack, text, x - width(text), y, color, drawShadow);
    }
    public static void drawRightComponent(MatrixStack stack, List<ITextComponent> text, int x, int y, int color, boolean drawShadow) {
        int maxWidth = 0;
        for (ITextComponent component : text) {
            maxWidth = Math.max(maxWidth, width(component));
        }
        x -= maxWidth;
        for (ITextComponent s : text) {
            drawComponent(stack, s, x, y, color, drawShadow);
            y += getFontHeight() + 1;
        }
    }

    public static void drawComponentTooltip(MatrixStack stack, List<? extends ITextProperties> tooltips, int x, int y) {
        net.minecraftforge.fml.client.gui.GuiUtils.drawHoveringText(stack, tooltips, x, y, Minecraft.getInstance().getMainWindow().getScaledWidth(), Minecraft.getInstance().getMainWindow().getScaledHeight(), -1, getFontRenderer());
    }

    /*
    public static void drawVerticalComponent(MatrixStack stack, Component text, int x, int y, int color, boolean drawShadow, int marinDown) {
        for (char c : text.getString().toCharArray()) {
            String s = String.valueOf(c);
            drawString(stack, s, x, y, color, drawShadow);
            y += getFontHeight() + marinDown;
        }
    }
    public static void drawVerticalComponent(MatrixStack stack, List<Component> text, int x, int y, int color, boolean drawShadow, boolean alignRight, int marinSide, int marinDown) {
        for (Component s : text) {
            drawVerticalComponent(stack, s, x, y, color, drawShadow, marinDown);
            int firstWidth = width(s) + marinSide;
            if (alignRight) {
                x += firstWidth;
            }else {
                x -= firstWidth;
            }
        }
    }

    public static void drawComponent(MatrixStack stack, Component text, int x, int y, int color, boolean drawShadow, boolean alignRight, int marinSide) {
        if (marinSide!=0) {
            for (char c : text.getString().toCharArray()) {
                String s = String.valueOf(c);
                drawString(stack, s, x, y, color, drawShadow);
                if (alignRight) {
                    x -= width(s) + marinSide;
                }else {
                    x += width(s) + marinSide;
                }
            }
        }else {
            if (alignRight) {
                drawRightComponent(stack, text, x, y, color, drawShadow);
            }else {
                drawComponent(stack, text, x, y, color, drawShadow);
            }
        }
    }
    public static void drawComponent(MatrixStack stack, List<Component> text, int x, int y, int color, boolean drawShadow, boolean alignRight, int marinSide, int marinDown) {
        for (Component s : text) {
            drawComponent(stack, s, x, y, color, drawShadow, alignRight, marinSide);
            y += getFontHeight()+ marinDown;
        }
    }
     */

        // Sequence

    public static void drawSequence(MatrixStack stack, IReorderingProcessor text, int x, int y, int color, boolean drawShadow) {
        if (drawShadow) {
            getFontRenderer().drawTextWithShadow(stack, text, x, y, color);
        }else {
            getFontRenderer().func_238422_b_(stack, text, x, y, color);
        }
    }

    public static void drawCenteredSequence(MatrixStack stack, IReorderingProcessor text, int x, int y, int color, boolean drawShadow) {
        drawSequence(stack, text, x - width(text) / 2, y, color, drawShadow);
    }
    public static void drawCenteredSequence(MatrixStack stack, List<IReorderingProcessor> text, int x, int y, int color, boolean drawShadow) {
        for (IReorderingProcessor s : text) {
            drawCenteredSequence(stack, s, x, y, color, drawShadow);
            y += getFontHeight() + 1;
        }
    }

    public static void drawRightSequence(MatrixStack stack, IReorderingProcessor text, int x, int y, int color, boolean drawShadow) {
        drawSequence(stack, text, x - width(text), y, color, drawShadow);
    }
    public static void drawRightSequence(MatrixStack stack, List<IReorderingProcessor> text, int x, int y, int color, boolean drawShadow) {
        int maxWidth = 0;
        for (IReorderingProcessor sequence : text) {
            maxWidth = Math.max(maxWidth, width(sequence));
        }
        x -= maxWidth;
        for (IReorderingProcessor s : text) {
            drawSequence(stack, s, x, y, color, drawShadow);
            y += getFontHeight() + 1;
        }
    }

    /*
    public static void drawVerticalSequence(MatrixStack stack, IReorderingProcessor text, int x, int y, int color, boolean drawShadow, int marinDown) {
        for (char c : text.toCharArray()) {
            String s = String.valueOf(c);
            drawString(stack, s, x, y, color, drawShadow);
            y += getFontHeight() + marinDown;
        }
    }
    public static void drawVerticalSequence(MatrixStack stack, List<IReorderingProcessor> text, int x, int y, int color, boolean drawShadow, boolean alignRight, int marinSide, int marinDown) {
        for (IReorderingProcessor s : text) {
            drawVerticalSequence(stack, s, x, y, color, drawShadow, marinDown);
            int firstWidth = width(s) + marinSide;
            if (alignRight) {
                x += firstWidth;
            }else {
                x -= firstWidth;
            }
        }
    }

    public static void drawSequence(MatrixStack stack, IReorderingProcessor text, int x, int y, int color, boolean drawShadow, boolean alignRight, int marinSide) {
        if (marinSide!=0) {
            for (char c : text.toCharArray()) {
                String s = String.valueOf(c);
                drawSequence(stack, s, x, y, color, drawShadow);
                if (alignRight) {
                    x -= width(s) + marinSide;
                }else {
                    x += width(s) + marinSide;
                }
            }
        }else {
            if (alignRight) {
                drawRightSequence(stack, text, x, y, color, drawShadow);
            }else {
                drawSequence(stack, text, x, y, color, drawShadow);
            }
        }
    }
    public static void drawSequence(MatrixStack stack, List<IReorderingProcessor> text, int x, int y, int color, boolean drawShadow, boolean alignRight, int marinSide, int marinDown) {
        for (IReorderingProcessor s : text) {
            drawSequence(stack, s, x, y, color, drawShadow, alignRight, marinSide);
            y += getFontHeight()+ marinDown;
        }
    }
     */

// graphical

   public static void fill(MatrixStack stack, int x, int y, int width, int height, int color1, int color2) {
       fillGradient(stack, x, y, width, height, color1, color2);
   }
   public static void fill(MatrixStack stack, int x, int y, int width, int height, int color) {
        Screen.fill(stack, x, y, x + width, y + height, color);
   }
   public static void hLine(MatrixStack stack, int x, int y, int width, int color) {
       fill(stack, x, y, width, 1, color);
   }
   public static void vLine(MatrixStack stack, int x, int y, int height, int color) {
       fill(stack, x, y, 1, height, color);
   }

   public static void fillCentered(MatrixStack stack, int x, int y, int width, int height, int color1, int color2) {
        fill(stack, x - width/2, y, width, height, color1, color2);
   }
   public static void fillCentered(MatrixStack stack, int x, int y, int width, int height, int color) {
        fill(stack, x - width/2, y, width, height, color);
   }
    public static void hLineCentered(MatrixStack stack, int x, int y, int width, int color) {
        hLine(stack, x - width/2, y, width, color);
    }
    public static void vLineCentered(MatrixStack stack, int x, int y, int height, int color) {
        vLine(stack, x, y - height/2, height, color);
    }

    public static void squareCentered(MatrixStack stack, int x, int y, int width, int height, int bgColor, int borderColor, boolean centerWidth, boolean centerHeight) {
        square(stack, x - (centerWidth ? width / 2 : 0), y - (centerHeight ? height / 2 : 0), width, height, bgColor, borderColor);
    }

    public static void square(MatrixStack stack, int x, int y, int width, int height, int bgColor, int borderColor) {
        fill(stack, x - 1, y - 1, width + 3, height + 3, bgColor); // 背景

        hLine(stack, x+1, y, width-1, borderColor); // 上
        hLine(stack, x, y + height, width, borderColor); // 下
        vLine(stack, x, y, height, borderColor); // 左
        vLine(stack, x + width, y, height+1, borderColor); // 右
    }

    public static void hLineGradient(MatrixStack stack, boolean anti, int x, int y, int width, int height, int color1, int color2) {
        int x2 = x + width;
        int y2 = y + height;

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);

        Matrix4f matrix4f = stack.getLast().getMatrix();
        float fromAlpha = (float)(color1 >> 24 & 255) / 255.0F;
        float fromRed = (float)(color1 >> 16 & 255) / 255.0F;
        float fromGreen = (float)(color1 >> 8 & 255) / 255.0F;
        float fromBlue = (float)(color1 & 255) / 255.0F;
        float toAlpha = (float)(color2 >> 24 & 255) / 255.0F;
        float toRed = (float)(color2 >> 16 & 255) / 255.0F;
        float toGreen = (float)(color2 >> 8 & 255) / 255.0F;
        float toBlue = (float)(color2 & 255) / 255.0F;
        // toRed, toGreen, toBlue, toAlpha
        // fromRed, fromGreen, fromBlue, fromAlpha
        if (anti) {
            bufferbuilder.pos(matrix4f, (float) x, (float) y, (float)0).color(toRed, toGreen, toBlue, toAlpha).endVertex();
            bufferbuilder.pos(matrix4f, (float) x, (float)y2, (float)0).color(toRed, toGreen, toBlue, toAlpha).endVertex();
            bufferbuilder.pos(matrix4f, (float)x2, (float)y2, (float)0).color(fromRed, fromGreen, fromBlue, fromAlpha).endVertex();
            bufferbuilder.pos(matrix4f, (float)x2, (float) y, (float)0).color(fromRed, fromGreen, fromBlue, fromAlpha).endVertex();
        }else {
            bufferbuilder.pos(matrix4f, (float) x, (float) y, (float)0).color(fromRed, fromGreen, fromBlue, fromAlpha).endVertex();
            bufferbuilder.pos(matrix4f, (float) x, (float)y2, (float)0).color(fromRed, fromGreen, fromBlue, fromAlpha).endVertex();
            bufferbuilder.pos(matrix4f, (float)x2, (float)y2, (float)0).color(toRed, toGreen, toBlue, toAlpha).endVertex();
            bufferbuilder.pos(matrix4f, (float)x2, (float) y, (float)0).color(toRed, toGreen, toBlue, toAlpha).endVertex();
        }
        tessellator.draw();
        RenderSystem.shadeModel(7424);
        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
    }

    public static void fillGradient(MatrixStack stack, int x, int y, int width, int height, int colorFrom, int colorTo) {
        int x2 = x + width;
        int y2 = y + height;

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);

        Matrix4f matrix = stack.getLast().getMatrix();;
        float fromRed = (float)(colorFrom >> 24 & 255) / 255.0F;
        float fromGreen = (float)(colorFrom >> 16 & 255) / 255.0F;
        float fromBlue = (float)(colorFrom >> 8 & 255) / 255.0F;
        float fromAlpha = (float)(colorFrom & 255) / 255.0F;
        float toRed = (float)(colorTo >> 24 & 255) / 255.0F;
        float toGreen = (float)(colorTo >> 16 & 255) / 255.0F;
        float toBlue = (float)(colorTo >> 8 & 255) / 255.0F;
        float toAlpha = (float)(colorTo & 255) / 255.0F;
        bufferbuilder.pos(matrix, (float)x2, (float)y, 0).color(fromGreen, fromBlue, fromAlpha, fromRed).endVertex();
        bufferbuilder.pos(matrix, (float)x, (float)y, 0).color(fromGreen, fromBlue, fromAlpha, fromRed).endVertex();
        bufferbuilder.pos(matrix, (float)x, (float)y2, 0).color(toGreen, toBlue, toAlpha, toRed).endVertex();
        bufferbuilder.pos(matrix, (float)x2, (float)y2, 0).color(toGreen, toBlue, toAlpha, toRed).endVertex();

        tessellator.draw();
        RenderSystem.shadeModel(7424);
        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
    }

    public static void tooltipBackground(final MatrixStack stack, final int x, final int y, final int width, final int height, final boolean centerWidth, final boolean centerHeight) {
//        TooltipRenderUtil.renderTooltipBackground(stack, x - (centerWidth ? (width / 2) : 0), y - (centerHeight ? (height / 2) : 0), width, height, 0);
    }

    public static void tooltipBackground(final MatrixStack stack, final int x, final int y, final int width, final int height, final int bgColor, final int borderColor, final boolean centerWidth, final boolean centerHeight) {
//        TooltipRenderUtil.renderTooltipBackground(stack, x - (centerWidth ? (width / 2) : 0), y - (centerHeight ? (height / 2) : 0), width, height, 0, bgColor, bgColor, borderColor, borderColor);
    }

    public static int getRGB(final float r, final float g, final float b, final float a) {
        return getRGB((int)(r * 255.0f + 0.5), (int)(g * 255.0f + 0.5), (int)(b * 255.0f + 0.5), (int)(a * 255.0f + 0.5));
    }

    public static int getRGB(final int r, final int g, final int b, final int a) {
        return (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
    }

// other
    public static int rgb(float r, float g, float b, float a, boolean anti) {
        return rgb((int)(r*255+0.5), (int)(g*255+0.5), (int)(b*255+0.5), (int)(a*255+0.5), anti);
    }
    public static int rgb(int r, int g, int b, int a, boolean anti) {
       r = anti ? r - 255 : r;
       g = anti ? g - 255 : g;
       b = anti ? b - 255 : b;
        return ((a & 0xFF) << 24) |
                ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8)  |
                 (b & 0xFF);
    }

    public static int red(int color) {
       return (color >> 16) & 0xFF;
    }
    public static int green(int color) {
       return (color >> 8) & 0xFF;
    }
    public static int blue(int color) {
       return color & 0xFF;
    }
    public static int alpha(int color) {
       return (color >> 24) & 0xFF;
    }

    public static void drawItemTooltip(MatrixStack stack, ItemStack item, int mouseX, int mouseY) {
        net.minecraftforge.fml.client.gui.GuiUtils.preItemToolTip(item);
        drawComponentTooltip(stack,
                item.getTooltip(Minecraft.getInstance().player, Minecraft.getInstance().gameSettings.advancedItemTooltips ? ITooltipFlag.TooltipFlags.ADVANCED : ITooltipFlag.TooltipFlags.NORMAL)
                , mouseX, mouseY);
        net.minecraftforge.fml.client.gui.GuiUtils.postItemToolTip();
    }

    public static void enableScissor(int x, int y, int width, int height) {
        double scale = Minecraft.getInstance().getMainWindow().getGuiScaleFactor();
        RenderSystem.enableScissor(
                (int)(x  * scale), (int)(Minecraft.getInstance().getMainWindow().getFramebufferHeight() - ((y + height) * scale)),
                (int)(width * scale), (int)(height * scale)
        );
    }
    public static void disableScissor(){
       RenderSystem.disableScissor();
    }
}
