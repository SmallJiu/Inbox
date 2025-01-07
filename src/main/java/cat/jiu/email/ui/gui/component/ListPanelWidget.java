package cat.jiu.email.ui.gui.component;

import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.ui.gui.GuiInbox;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.list.AbstractList;
import net.minecraft.client.gui.widget.list.ExtendedList;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ListPanelWidget<T extends Screen, E> extends ExtendedList<ListPanelWidget.PanelEntry<T, E>> {
    public static interface IEntryRender<E> {
        void render(MatrixStack stack, E element, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isScroll, boolean isMouseOver, float partialTick);
    }
    protected final T parent;
    protected final IEntryRender<E> render;
    protected Consumer<E> selectedListener;

    public ListPanelWidget(T parent, int x, int y, int width, int height, int slotHeight, IEntryRender<E> render) {
        super(Minecraft.getInstance(), width, height, y, y+height, slotHeight);
        this.parent = parent;
        this.render = render;
        this.setLeftPos(x);
        this.setRenderSelection(false);
        this.func_244605_b(false); // setRenderBackground
        this.func_244606_c(false); // setRenderTopAndBottom
        this.setRenderHeader(false, 0);
    }

    @Override
    protected void renderList(MatrixStack stack, int x, int y, int mouseX, int mouseY, float partialTicks) {
        try {
//            RenderUtils.enableScissor(this.x0, this.y0, this.width, this.height);
            super.renderList(stack, x, y, mouseX, mouseY, partialTicks);
//            RenderUtils.disableScissor();
        }catch (Throwable ignored){}
    }

    public void addEntry(E element) {
        this.addEntry(new PanelEntry<>(this, this.parent, this.getItemCount(), element));
    }
    public void clearEntries_() {
        super.clearEntries();
    }
    public void sort(Comparator<E> predicate) {
        this.getEventListeners().sort((e1, e2)->predicate.compare(e1.getElement(), e2.getElement()));
    }

    @Override
    public int getRowWidth() {
        return this.width;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.width;
    }

    public void setSelectedListener(Consumer<E> selectedListener) {
        this.selectedListener = selectedListener;
    }

    @Override
    public void setSelected(@Nullable PanelEntry<T, E> entry) {
        super.setSelected(entry);
        if (this.selectedListener != null) {
            this.selectedListener.accept(entry != null? entry.getElement() : null);
        }
    }

    @Override
    protected void renderBackground(MatrixStack stack) {
        RenderUtils.fill(stack, this.getLeft(), this.getTop(), this.width, this.height, 0xC0101010);
    }

    public static class PanelEntry<T extends Screen, E> extends AbstractList.AbstractListEntry<PanelEntry<T, E>> {
        protected final ListPanelWidget<T, E> list;
        protected final T parent;
        protected final int index;
        protected final E element;

        public PanelEntry(ListPanelWidget<T, E> list, T parent, int index, E element) {
            this.list = list;
            this.parent = parent;
            this.index = index;
            this.element = element;
        }

        @Override
        public void render(MatrixStack stack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
            PanelEntry<T, E> entry = this.list.getSelected();
            boolean isScroll = this.list.getMaxScroll() > 0;
            if (entry != null && entry.index == this.index) {
                this.renderSelection(stack, index, top, left, width, height, mouseX, mouseY, isScroll, isMouseOver, partialTicks);
            }else {
                this.renderBack(stack, index, top, left, width, height, mouseX, mouseY, isScroll, isMouseOver, partialTicks);
            }
            this.list.render.render(stack, this.getElement(), index, top, left, width, height, mouseX, mouseY, isScroll, isMouseOver, partialTicks);
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (pButton == 0) {
                this.list.setSelected(this);
            }
            return false;
        }

        protected void renderBack(MatrixStack stack, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isScroll, boolean isMouseOver, float partialTick) {
            x -= 2;
            width -= isScroll  ? 9 : 3;
            RenderUtils.draw(stack, GuiInbox.BackGround, x, y, 3, 3, 168, 168); // 左上
            RenderUtils.draw(stack, GuiInbox.BackGround, x, y + height + 1, 3, 3, 168, 197); //左下

            RenderUtils.draw(stack, GuiInbox.BackGround, x + width, y, 3, 3, 244, 168); // 右上
            RenderUtils.draw(stack, GuiInbox.BackGround, x + width, y + height + 1, 3, 3, 244, 197); // 右下

            RenderUtils.draw(stack, GuiInbox.BackGround, x + 3, y, width - 3, 3, 171, 168, 1, 3, null); // 上
            RenderUtils.draw(stack, GuiInbox.BackGround, x + 3, y + height + 1, width - 3, 3, 171, 197, 1, 3, null); // 下

            RenderUtils.draw(stack, GuiInbox.BackGround, x, y + 3, 3, height - 2, 168, 171, 3, 1, null); //左
            RenderUtils.draw(stack, GuiInbox.BackGround, x + width, y + 3, 3, height - 2, 244, 171, 3, 1, null); //右

            RenderUtils.draw(stack, GuiInbox.BackGround, x + 3, y + 3, width - 3, height - 2, 171, 171, 1, 1, null); //右
        }

        protected void renderSelection(MatrixStack stack, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isScroll, boolean isMouseOver, float partialTick) {
            x -= 2;
            width -= isScroll ? 9 : 3;
            RenderUtils.draw(stack, GuiInbox.BackGround, x + 3, y + 3, width - 3, height - 2, 171, 171, 1, 1, null); //右

            RenderUtils.draw(stack, GuiInbox.BackGround, x, y, 7, 5, 168, 200); // 左上
            RenderUtils.draw(stack, GuiInbox.BackGround, x, y + height - 3, 5, 7, 168, 225); //左下

            RenderUtils.draw(stack, GuiInbox.BackGround, x + width - 4, y, 7, 7, 240, 200); // 右上
            RenderUtils.draw(stack, GuiInbox.BackGround, x + width - 2, y + height - 3, 5, 7, 242, 225); // 右下

            RenderUtils.draw(stack, GuiInbox.BackGround, x + 7, y, width - 11, 3, 175, 200, 1, 3, null); // 上
            RenderUtils.draw(stack, GuiInbox.BackGround, x + 5, y + height - 3, width - 7, 7, 173, 225, 1, 7, null); // 下

            RenderUtils.draw(stack, GuiInbox.BackGround, x, y + 5, 7, height - 8, 168, 205, 7, 1, null); //左
            RenderUtils.draw(stack, GuiInbox.BackGround, x + width - 4, y + 7, 7, height - 10, 240, 207, 7, 1, null); //右
        }

        public E getElement() {
            return element;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PanelEntry<T, E> entry = (PanelEntry<T, E>) o;
            return index == entry.index;
        }

        @Override
        public int hashCode() {
            return this.index;
        }
    }
}
