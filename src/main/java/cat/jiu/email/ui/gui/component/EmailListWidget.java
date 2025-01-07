package cat.jiu.email.ui.gui.component;

import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.element.Email;
import cat.jiu.email.ui.gui.GuiInbox;
import cat.jiu.email.util.EmailUtils;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class EmailListWidget extends ObjectSelectionList<EmailListWidget.EmailEntry> {
    final GuiInbox parent;

    public EmailListWidget(GuiInbox parent, int width, int height, int x, int y) {
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
        width -= EmailListWidget.this.getMaxScroll() > 0 ? 9 : 3;

        RenderUtils.draw(graphics, GuiInbox.BackGround, x, y, 7, 5, 168, 200); // 左上
        RenderUtils.draw(graphics, GuiInbox.BackGround, x, y + height - 3, 5, 7, 168, 225); //左下

        RenderUtils.draw(graphics, GuiInbox.BackGround, x + width - 4, y, 7, 7, 240, 200); // 右上
        RenderUtils.draw(graphics, GuiInbox.BackGround, x + width - 2, y + height - 3, 5, 7, 242, 225); // 右下

        RenderUtils.draw(graphics, GuiInbox.BackGround, x + 7, y, width - 11, 3, 175, 200, 1, 3, null); // 上
        RenderUtils.draw(graphics, GuiInbox.BackGround, x + 5, y + height - 3, width - 7, 7, 173, 225, 1, 7, null); // 下

        RenderUtils.draw(graphics, GuiInbox.BackGround, x, y + 5, 7, height - 8, 168, 205, 7, 1, null); //左
        RenderUtils.draw(graphics, GuiInbox.BackGround, x + width - 4, y + 7, 7, height - 10, 240, 207, 7, 1, null); //右
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
//        this.parent.renderBackground(pGuiGraphics);
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
        this.parent.getInbox().getEmailIDs().forEach(id-> {
            Email email = this.parent.getInbox().getEmail(id);
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
    public class EmailEntry extends Entry<EmailEntry> {
        final GuiInbox parent;
        final long id;
        final int width;
        final Email email;
        Component title, sender;

        public EmailEntry(GuiInbox parent, long id, Email email, int width) {
            this.parent = parent;
            this.id = id;
            this.email = email;
            this.width = width;
            Component title = email.getTitle().toTextComponent();
            if (email.getTitle().getStringWidth(this.parent.getFont()) >= this.width - this.parent.getFont().width("...")) {
                title = Component.literal(parent.getFont().plainSubstrByWidth(email.getTitle().format(), this.width - 3 - 9 - this.parent.getFont().width("..."))).append("...");
            }
            this.title = title;

            String sender = ChatFormatting.GRAY + I18n.get("info.inbox.main.from", this.email.getSender().format());
            if (RenderUtils.width(sender) >= this.width - this.parent.getFont().width("...")) {
                sender = parent.getFont().plainSubstrByWidth(sender, this.width - 3 - 9 - this.parent.getFont().width("...")) + "...";
            }
            this.sender = Component.literal(sender);
        }

        @Override
        public @NotNull Component getNarration() {
            return this.title;
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int index, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
            graphics.drawString(this.parent.getFont(), this.title, pLeft + 2, pTop + 4, Color.WHITE.getRGB());
//            graphics.drawString(this.parent.getFont(), this.time, pLeft + 2, pTop + this.parent.getFont().lineHeight + 2 + 3, Color.WHITE.getRGB());
//            EmailUtils.drawAlignRightString(graphics, this.parent.getFont(), this.state, pLeft + pWidth - 9 - 3, pTop + 4, 0, true);

            boolean showSender = true;
            if (this.email.hasExpirationTime()) {
                RenderUtils.draw(graphics, GuiInbox.ICON, pLeft + 2, pTop + RenderUtils.getFontHeight() + 2 + 2, RenderUtils.getFontHeight(), RenderUtils.getFontHeight(), 176 + (this.email.isExpiration() ? 40 : 0), 0, 40, 40, 256, 256);
                RenderUtils.drawString(graphics, EmailUtils.getExpirationTime(this.email), pLeft + 3 + RenderUtils.getFontHeight(), pTop + RenderUtils.getFontHeight() + 2 + 3, (this.email.isExpiration() ? Color.RED : Color.WHITE).getRGB(), true);
                showSender = false;
            }
            int x = pLeft + pWidth - (EmailListWidget.this.getMaxScroll() > 0 ? 9 : 3) - 2;
            int y = pTop + RenderUtils.getFontHeight() + 2 + 2;
            if (!this.email.isRead()) {
                x -= 4;
                RenderUtils.draw(graphics, GuiInbox.ICON, x, y, 3, RenderUtils.getFontHeight(), 189, 122, 14, 38, 256, 256);
                x -= 1;
            }
            if (this.email.hasAttachment()) {
                x -= RenderUtils.getFontHeight();
                RenderUtils.draw(graphics, GuiInbox.ICON, x, y, RenderUtils.getFontHeight(), RenderUtils.getFontHeight(), 176 + (!this.email.isReceived() ? 40 : 0), 41, 40, 40, 256, 256);
                x -= 1;
            }

            if (showSender) {
                graphics.drawString(this.parent.getFont(), this.sender, pLeft + 2, pTop + this.parent.getFont().lineHeight + 2 + 3, Color.WHITE.getRGB());
            }
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (pButton == 0) {
                EmailListWidget.this.setSelected(this);
                this.parent.setCurrentEmail(this.id);
                this.email.setRead(true);
            }
            return false;
        }

        @Override
        public void renderBack(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            x -= 2;
            width -= EmailListWidget.this.getMaxScroll() > 0 ? 9 : 3;
            RenderUtils.draw(graphics, GuiInbox.BackGround, x, y, 3, 3, 168, 168); // 左上
            RenderUtils.draw(graphics, GuiInbox.BackGround, x, y + height + 1, 3, 3, 168, 197); //左下

            RenderUtils.draw(graphics, GuiInbox.BackGround, x + width, y, 3, 3, 244, 168); // 右上
            RenderUtils.draw(graphics, GuiInbox.BackGround, x + width, y + height + 1, 3, 3, 244, 197); // 右下

            RenderUtils.draw(graphics, GuiInbox.BackGround, x + 3, y, width - 3, 3, 171, 168, 1, 3, null); // 上
            RenderUtils.draw(graphics, GuiInbox.BackGround, x + 3, y + height + 1, width - 3, 3, 171, 197, 1, 3, null); // 下

            RenderUtils.draw(graphics, GuiInbox.BackGround, x, y + 3, 3, height - 2, 168, 171, 3, 1, null); //左
            RenderUtils.draw(graphics, GuiInbox.BackGround, x + width, y + 3, 3, height - 2, 244, 171, 3, 1, null); //右

            RenderUtils.draw(graphics, GuiInbox.BackGround, x + 3, y + 3, width - 3, height - 2, 171, 171, 1, 1, null); //右
        }

        public long getEmailId() {
            return id;
        }

        public Email getEmail() {
            return email;
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
