package cat.jiu.email.ui.gui.component;

import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.element.Email;
import cat.jiu.email.ui.gui.GuiEmailMain;
import cat.jiu.email.util.EmailUtils;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class EmailListWidget extends ObjectSelectionList<EmailListWidget.EmailEntry> {
    final GuiEmailMain parent;

    public EmailListWidget(GuiEmailMain parent, int width, int height, int x, int y) {
        super(Minecraft.getInstance(), width, height, y, y+height, parent.getFont().lineHeight * 2 + 8);
        this.parent = parent;
        this.setLeftPos(x);
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
        this.setRenderHeader(false, 0);
    }

    @Override
    protected void renderList(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        try {
            super.renderList(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }catch (Throwable ignored){}
    }

    @Override
    protected void renderSelection(GuiGraphics graphics, int y, int width, int height, int pOuterColor, int pInnerColor) {
        int x = this.x0 + (this.width - width) / 2;
        width -= 9;

        RenderUtils.draw(graphics, GuiEmailMain.BackGround, x, y, 7, 5, 168, 200); // 左上
        RenderUtils.draw(graphics, GuiEmailMain.BackGround, x, y + height - 3, 5, 7, 168, 225); //左下

        RenderUtils.draw(graphics, GuiEmailMain.BackGround, x + width - 4, y, 7, 7, 240, 200); // 右上
        RenderUtils.draw(graphics, GuiEmailMain.BackGround, x + width - 2, y + height - 3, 5, 7, 242, 225); // 右下

        RenderUtils.draw(graphics, GuiEmailMain.BackGround, x + 7, y, width - 11, 3, 175, 200, 1, 3, null); // 上
        RenderUtils.draw(graphics, GuiEmailMain.BackGround, x + 5, y + height - 3, width - 7, 7, 173, 225, 1, 7, null); // 下

        RenderUtils.draw(graphics, GuiEmailMain.BackGround, x, y + 5, 7, height - 8, 168, 205, 7, 1, null); //左
        RenderUtils.draw(graphics, GuiEmailMain.BackGround, x + width - 4, y + 7, 7, height - 10, 240, 207, 7, 1, null); //右
    }

    @Override
    public int getRowWidth() {
        return this.width;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.width;
    }

    @Override
    protected void renderBackground(GuiGraphics pGuiGraphics) {
        this.parent.renderBackground(pGuiGraphics);
        pGuiGraphics.fill(this.getLeft(), this.getTop(), this.getRight(), this.getBottom(), 0xC0101010);
    }

    @Override
    public void enableScissor(GuiGraphics pGuiGraphics) {
        super.enableScissor(pGuiGraphics);
    }

    @Override
    public void clearEntries() {
        super.clearEntries();
    }

    public void refreshList() {
        this.refreshList(null);
    }
    public void refreshList(Predicate<Email> predicate) {
        List<EmailEntry> entries = Lists.newArrayList();
        this.parent.getMenu().getInbox().getEmailIDs().forEach(id-> {
            Email email = this.parent.getMenu().getInbox().getEmail(id);
            if (email!=null && predicate == null || predicate.test(email)) {
                entries.add(new EmailEntry(this.parent, id, email, this.getWidth()));
            }
        });
        this.sort(entries);
    }

    public void sort(List<EmailEntry> entries) {
        entries.sort((e1,e2)->Long.compare(e2.getEmailId(), e1.getEmailId()));
        this.replaceEntries(entries);
    }

    public void sort() {
        this.sort(Lists.newArrayList(this.children()));
    }
    public class EmailEntry extends ObjectSelectionList.Entry<EmailEntry> {
        final GuiEmailMain parent;
        final long id;
        final int width;
        final Email email;
        Component sender, time, state;
        boolean updataExpiration;

        public EmailEntry(GuiEmailMain parent, long id, Email email, int width) {
            this.parent = parent;
            this.id = id;
            this.email = email;
            this.width = width;
            this.updata();
            this.updataExpiration = this.email.hasExpirationTime();
        }

        @Override
        public @NotNull Component getNarration() {
            return this.sender;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int index, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
            graphics.drawString(this.parent.getFont(), this.sender, pLeft + 2, pTop + 4, Color.WHITE.getRGB());
            graphics.drawString(this.parent.getFont(), this.time, pLeft + 2, pTop + this.parent.getFont().lineHeight + 2 + 3, Color.WHITE.getRGB());
            EmailUtils.drawAlignRightString(graphics, this.parent.getFont(), this.state, pLeft + pWidth - 9 - 3, pTop + 4, 0, true);
            if (this.updataExpiration && this.email.isExpiration()) {
                this.updata();
                this.updataExpiration = false;
            }
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (pButton == 0) {
                EmailListWidget.this.setSelected(this);
                this.parent.setCurrentEmail(this.id);
            }
            return false;
        }

        @Override
        public void renderBack(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            x -= 2;
            width -= 9;
            RenderUtils.draw(graphics, GuiEmailMain.BackGround, x, y, 3, 3, 168, 168); // 左上
            RenderUtils.draw(graphics, GuiEmailMain.BackGround, x, y + height + 1, 3, 3, 168, 197); //左下

            RenderUtils.draw(graphics, GuiEmailMain.BackGround, x + width, y, 3, 3, 244, 168); // 右上
            RenderUtils.draw(graphics, GuiEmailMain.BackGround, x + width, y + height + 1, 3, 3, 244, 197); // 右下

            RenderUtils.draw(graphics, GuiEmailMain.BackGround, x + 3, y, width - 3, 3, 171, 168, 1, 3, null); // 上
            RenderUtils.draw(graphics, GuiEmailMain.BackGround, x + 3, y + height + 1, width - 3, 3, 171, 197, 1, 3, null); // 下

            RenderUtils.draw(graphics, GuiEmailMain.BackGround, x, y + 3, 3, height - 2, 168, 171, 3, 1, null); //左
            RenderUtils.draw(graphics, GuiEmailMain.BackGround, x + width, y + 3, 3, height - 2, 244, 171, 3, 1, null); //右

            RenderUtils.draw(graphics, GuiEmailMain.BackGround, x + 3, y + 3, width - 3, height - 2, 171, 171, 1, 1, null); //右
        }

        public long getEmailId() {
            return id;
        }

        public Email getEmail() {
            return email;
        }

        public void updata() {
            MutableComponent state = Component.empty();
            if (!email.isRead()) {
                state = state.append(EmailUtils.createTextComponent(ChatFormatting.RED, "*"));
            }
            if (this.email.hasAttachment()) {
                state = state.append(EmailUtils.createTextComponent(this.email.isReceived() ? ChatFormatting.GREEN : ChatFormatting.RED, "$"));
            }
            if (this.email.hasExpirationTime()) {
                state = state.append(EmailUtils.createTextComponent(this.email.isExpiration() ? ChatFormatting.RED : ChatFormatting.GREEN , "#"));
            }
            this.state = state;

            MutableComponent sender = Component.literal(email.getSender().format());
            if (email.getSender().getStringWidth(this.parent.getFont()) > this.width - this.parent.getFont().width(state) - this.parent.getFont().width("...")) {
                sender = Component.literal(parent.getFont().plainSubstrByWidth(email.getSender().format(), this.width - 3 - 9 - this.parent.getFont().width(state) - this.parent.getFont().width("..."))).append("...");
            }
            this.sender = sender;

            this.time = Component.nullToEmpty(parent.getFont().plainSubstrByWidth(email.getCreateTimeAsString(EmailUtils.dateFormat_1).substring(2), this.width));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EmailEntry entry = (EmailEntry) o;
            return id == entry.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}
