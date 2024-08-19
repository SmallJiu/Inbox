package cat.jiu.email.event;

import cat.jiu.email.api.IAttachment;
import cat.jiu.email.element.Email;
import cat.jiu.email.util.EmailUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.NotNull;

public class AttachmentEvent extends Event {
    public final Email email;
    public final IAttachment attachment;

    protected AttachmentEvent(Email email, IAttachment attachment) {
        this.email = email;
        this.attachment = attachment;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Render extends AttachmentEvent {
        public final GuiGraphics graphics;
        public final Font font;
        public final int x, originalY, mouseX, mouseY, guiWidth, guiHeight, top, bottom, left, right;
        protected int y;
        protected Render(Email email, IAttachment attachment, GuiGraphics graphics, Font font, int guiWidth, int guiHeight, int x, int y, int mouseX, int mouseY, int top, int bottom, int left, int right) {
            super(email, attachment);
            this.graphics = graphics;
            this.font = font;
            this.guiWidth = guiWidth;
            this.guiHeight = guiHeight;
            this.x = x;
            this.originalY = y;
            this.y = y;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.top = top;
            this.bottom = bottom;
            this.left = left;
            this.right = right;
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
            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            RenderSystem.enableScissor((int)(this.left * scale), (int)(Minecraft.getInstance().getWindow().getHeight() - (this.bottom * scale)),
                    (int)(this.guiWidth * scale), (int)(this.guiWidth * scale));
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
            return EmailUtils.isInRange(this.mouseX, this.mouseY, this.left, this.top, this.guiWidth, this.guiHeight);
        }

        @Cancelable
        public static class Pre extends Render {
            public Pre(Email email, IAttachment attachment, GuiGraphics graphics, Font font, int guiWidth, int guiHeight, int x, int y, int mouseX, int mouseY, int top, int bottom, int left, int right) {
                super(email, attachment, graphics, font, guiWidth, guiHeight, x, y, mouseX, mouseY, top, bottom, left, right);
            }
        }
        public static class Post extends Render {
            public Post(Email email, IAttachment attachment, GuiGraphics graphics, Font font, int guiWidth, int guiHeight, int x, int y, int mouseX, int mouseY, int top, int bottom, int left, int right) {
                super(email, attachment, graphics, font, guiWidth, guiHeight, x, y, mouseX, mouseY, top, bottom, left, right);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Cancelable
    public static class GetHeight extends AttachmentEvent {
        public final Font font;
        public final int guiWidth, guiHeight;
        protected int height = 0;
        public GetHeight(Email email, IAttachment attachment, Font font, int guiWidth, int guiHeight) {
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
