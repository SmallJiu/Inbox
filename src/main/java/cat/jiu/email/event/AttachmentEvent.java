package cat.jiu.email.event;

import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.element.Email;
import cat.jiu.email.util.EmailUtils;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import java.awt.*;

public class AttachmentEvent extends Event {
    public final Email email;
    public final IAttachment attachment;

    protected AttachmentEvent(Email email, IAttachment attachment) {
        this.email = email;
        this.attachment = attachment;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Render extends AttachmentEvent {
        public final MatrixStack stack;
        public final FontRenderer font;
        /**              X轴， 初始Y轴，  鼠标X，  鼠标Y，   可视宽，     可视高，   顶，   底，    左，   右  */
        public final int x, originalY, mouseX, mouseY, viewWidth, viewHeight, top, bottom, left, right;
        @Deprecated
        public final int guiWidth, guiHeight;
        protected int y;

        protected Render(Email email, IAttachment attachment, MatrixStack stack, FontRenderer font, int viewWidth, int viewHeight, int x, int y, int mouseX, int mouseY, int top, int bottom, int left, int right) {
            super(email, attachment);
            this.stack = stack;
            this.font = font;
            this.viewWidth = viewWidth;
            this.viewHeight = viewHeight;
            this.x = x;
            this.originalY = y;
            this.y = y;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.top = top;
            this.bottom = bottom;
            this.left = left;
            this.right = right;

            this.guiWidth = this.viewWidth;
            this.guiHeight = this.viewHeight;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
        public void addY(int add) {
            this.y += add;
        }

        public void enableScissor() {
            double scale = Minecraft.getInstance().getMainWindow().getGuiScaleFactor();
            RenderSystem.enableScissor((int)(this.left * scale), (int)(Minecraft.getInstance().getMainWindow().getFramebufferHeight() - (this.bottom * scale)),
                    (int)(this.viewWidth * scale), (int)(this.viewWidth * scale));
        }
        public void disableScissor() {
            RenderSystem.disableScissor();
        }

        public void disableScissorRender(Runnable runnable) {
            this.disableScissor();
            runnable.run();
            this.enableScissor();
        }

        public boolean canSee() {
            return EmailUtils.isInRange(this.mouseX, this.mouseY, this.left, this.top, this.viewWidth, this.viewHeight);
        }

        public ITextComponent renderSaveTo(String name, Object... args) {
            return this.renderSaveTo(new TranslationTextComponent(name, args));
        }
        public ITextComponent renderSaveTo(ITextComponent name) {
            ITextComponent info = new TranslationTextComponent("info.inbox.save_to_email", name).appendString(" (").appendSibling(new TranslationTextComponent(this.email.isReceived() ? "info.inbox.filter.is_accept" : "info.inbox.filter.not_accept")).appendString("): ");
            RenderUtils.drawComponent(this.stack, info, this.x, this.getY()+4, Color.WHITE.getRGB(), true);
            return info;
        }

        public void renderItem(ItemStack stack, int x, int y) {
            this.renderItem(stack, null, x, y);
        }
        public void renderItem(ItemStack stack, String text, int x, int y) {
            Minecraft.getInstance().getItemRenderer().renderItemAndEffectIntoGUI(stack, x, y);
            Minecraft.getInstance().getItemRenderer().renderItemOverlayIntoGUI(font, stack, x, y, text);
        }

        @Cancelable
        public static class Pre extends Render {
            public Pre(Email email, IAttachment attachment, MatrixStack stack, FontRenderer font, int guiWidth, int guiHeight, int x, int y, int mouseX, int mouseY, int top, int bottom, int left, int right) {
                super(email, attachment, stack, font, guiWidth, guiHeight, x, y, mouseX, mouseY, top, bottom, left, right);
            }
        }
        public static class Post extends Render {
            public Post(Email email, IAttachment attachment, MatrixStack stack, FontRenderer font, int guiWidth, int guiHeight, int x, int y, int mouseX, int mouseY, int top, int bottom, int left, int right) {
                super(email, attachment, stack, font, guiWidth, guiHeight, x, y, mouseX, mouseY, top, bottom, left, right);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Cancelable
    public static class GetHeight extends AttachmentEvent {
        public final FontRenderer font;
        public final int guiWidth, guiHeight;
        protected int height = 0;
        public GetHeight(Email email, IAttachment attachment, FontRenderer font, int guiWidth, int guiHeight) {
            super(email, attachment);
            this.font = font;
            this.guiWidth = guiWidth;
            this.guiHeight = guiHeight;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }
        public void addHeight(int height) {
            this.setHeight(this.getHeight() + height);
        }
    }
}
