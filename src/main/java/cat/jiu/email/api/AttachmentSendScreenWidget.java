package cat.jiu.email.api;

import cat.jiu.core.api.Lambdas;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.core.util.registry.StaticRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

@OnlyIn(Dist.CLIENT)
public abstract class AttachmentSendScreenWidget extends AbstractWidget {
    public static final StaticRegistry<ResourceLocation, WidgetEntry> REGISTRY = new StaticRegistry<ResourceLocation, WidgetEntry>();
    public static class WidgetEntry implements Supplier<ResourceLocation> {
        public final boolean canSendByNormalPlayer;
        public final ResourceLocation location;
        public final Supplier<AttachmentSendScreenWidget> widgetEntryGetter;
        public WidgetEntry(ResourceLocation location, boolean canSendByNormalPlayer, Supplier<AttachmentSendScreenWidget> widgetEntryGetter) {
            this.location = location;
            this.canSendByNormalPlayer = canSendByNormalPlayer;
            this.widgetEntryGetter = widgetEntryGetter;
        }
        @Override
        public ResourceLocation get() {
            return this.location;
        }
    }

    public SubWidget subWidget;
    public AttachmentSendScreenWidget(Component name) {
        super(0, 0, 0, 0, name);
    }
    protected void initSubWiget(boolean isHorizontal) {
        this.subWidget = new SubWidget(isHorizontal, this.getMessage());
    }

    public <T extends AbstractWidget> SubWidget.PositionWiget addSubWidget(T widget) {
        return this.addSubWidget(new SubWidget.PositionWiget(widget));
    }
    public <T extends AbstractWidget> SubWidget.PositionWiget addSubWidget(T widget, int top, int bottom, int left, int right) {
        return this.addSubWidget(new SubWidget.PositionWiget(widget, top, bottom, left, right));
    }
    public SubWidget.PositionWiget addSubWidget(SubWidget.PositionWiget widget) {
        if (this.subWidget != null) {
            return this.subWidget.addWiget(widget);
        }
        return widget;
    }

    @Override
    public int getWidth() {
        return this.subWidget != null ? this.subWidget.getWidth() : super.getWidth();
    }

    @Override
    public int getHeight() {
        return this.subWidget != null ? this.subWidget.getHeight() : super.getHeight();
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (this.subWidget != null && this.subWidget.mouseClicked(pMouseX, pMouseY, pButton)) {
            return true;
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        if (this.subWidget != null && this.subWidget.charTyped(pCodePoint, pModifiers)) {
            return true;
        }
        return super.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.subWidget != null && this.subWidget.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.subWidget != null && this.subWidget.keyReleased(pKeyCode, pScanCode, pModifiers)) {
            return true;
        }
        return super.keyReleased(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.subWidget != null) {
            this.subWidget.setPosition(this.getX(), this.getY());
            this.subWidget.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }
    }

    public abstract IAttachment newAttachmentInstance();

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        if (this.subWidget != null) {
            this.subWidget.updateWidgetNarration(output);
        }
    }

    public static class ModifierWidget extends SubWidget {
        public final Button up_left, down_right;

        public ModifierWidget(boolean isHorizontal, AtomicInteger index, int maxIndex, Lambdas.Consumer2<ModifierWidget, Integer> indexConsumer) {
            super(isHorizontal);
            this.up_left = this.addWiget(Button.builder(Component.literal(isHorizontal ? "<" : "∧"), b -> {
                                index.set(index.get() + 1);
                                if (index.get() >= maxIndex) {
                                    index.set(0);
                                }
                                indexConsumer.accept(this, index.get());
                            })
                            .size(RenderUtils.fontHeight(), RenderUtils.fontHeight())
                            .build(), -3, 0, 0, 0)
                    .setConsumerEvent(false, true, false, false)
                    .cast();

            this.down_right = this.addWiget(Button.builder(Component.literal(isHorizontal ? ">" : "∨"), b -> {
                                index.set(index.get() - 1);
                                if (index.get() < 0) {
                                    index.set(maxIndex-1);
                                }
                                indexConsumer.accept(this, index.get());
                            })
                            .size(RenderUtils.fontHeight(), RenderUtils.fontHeight())
                            .build(), -3, 0, 0, 0)
                    .setConsumerEvent(false, true, false, false)
                    .cast();
        }

        public ModifierWidget(boolean isHorizontal, Runnable up_left, Runnable down_right) {
            super(isHorizontal);
            this.up_left = this.addWiget(Button.builder(Component.literal(isHorizontal ? "<" : "∧"), b -> up_left.run())
                    .size(RenderUtils.fontHeight(), RenderUtils.fontHeight())
                    .build(), -3, 0, 0, 0)
                    .setConsumerEvent(false, true, false, false)
                    .cast();

            this.down_right = this.addWiget(Button.builder(Component.literal(isHorizontal ? ">" : "∨"), b -> down_right.run())
                    .size(RenderUtils.fontHeight(), RenderUtils.fontHeight())
                    .build(), -3, 0, 0, 0)
                    .setConsumerEvent(false, true, false, false)
                    .cast();
        }

        @Override
        public void setTooltip(@Nullable Tooltip pTooltip) {
            super.setTooltip(pTooltip);
            this.up_left.setTooltip(pTooltip);
            this.down_right.setTooltip(pTooltip);
        }
    }

    public static class SubWidget extends AbstractWidget {
        public static class PositionWiget {
            public final AbstractWidget widget;
            public final int top, bottom, left, right;

            public Function<PositionWiget, Integer> preRender, postRender;
            public GuiGraphics graphics;
            public int mouseX, mouseY;
            public float partialTick;
            public boolean
                    consumerCharTyped = true,
                    consumerMouseClicked = true,
                    consumerKeyPressed = true,
                    consumerKeyReleased = true;

            public PositionWiget(AbstractWidget widget) {
                this(widget, 0, 0, 0, 0);
            }
            public PositionWiget(AbstractWidget widget, int top, int bottom, int left, int right) {
                this.widget = widget;
                this.top = top;
                this.bottom = bottom;
                this.left = left;
                this.right = right;
            }
            @SuppressWarnings("unchecked")
            public <T extends AbstractWidget> T cast(){
                return (T) this.widget;
            }
            public PositionWiget setWigetRender(Function<PositionWiget, Integer> pre, Function<PositionWiget, Integer> post) {
                this.preRender = pre;
                this.postRender = post;
                return this;
            }

            public PositionWiget setConsumerEvent(
                    boolean consumerCharTyped,
                    boolean consumerMouseClicked,
                    boolean consumerKeyPressed,
                    boolean consumerKeyReleased) {
                this.consumerCharTyped = consumerCharTyped;
                this.consumerMouseClicked = consumerMouseClicked;
                this.consumerKeyPressed = consumerKeyPressed;
                this.consumerKeyReleased = consumerKeyReleased;
                return this;
            }
        }

        public final boolean isHorizontal;
        public final List<PositionWiget> widgets = new ArrayList<>();

        public SubWidget(boolean isHorizontal) {
            this(isHorizontal, CommonComponents.EMPTY);
        }
        public SubWidget(boolean isHorizontal, Component pMessage) {
            super(0, 0, 0, 0, pMessage);
            this.isHorizontal = isHorizontal;
        }

        public <T extends AbstractWidget> PositionWiget addWiget(T widget) {
            PositionWiget wiget = new PositionWiget(widget);
            this.widgets.add(wiget);
            return wiget;
        }
        public <T extends AbstractWidget> PositionWiget addWiget(T widget, int top, int bottom, int left, int right) {
            PositionWiget wiget = new PositionWiget(widget, top, bottom, left, right);
            this.widgets.add(wiget);
            return wiget;
        }
        public PositionWiget addWiget(PositionWiget widget) {
            this.widgets.add(widget);
            return widget;
        }

        @Override
        public int getHeight() {
            int height = 0;
            PositionWiget last = null;
            for (PositionWiget widget : this.widgets) {
                if (widget.widget.visible){
                    if (this.isHorizontal) {
                        height = Math.max(height, widget.widget.getHeight());
                    } else {
                        if (last != null) height += last.bottom;
                        height += widget.top + widget.widget.getHeight();
                    }
                    last = widget;
                }
            }
            return height;
        }

        @Override
        public int getWidth() {
            int width = 0;
            PositionWiget last = null;
            for (PositionWiget widget : this.widgets) {
                if (widget.widget.visible){
                    if (this.isHorizontal) {
                        if (last != null) width += last.right;
                        width += widget.left + widget.widget.getWidth();
                    } else {
                        width = Math.max(width, widget.widget.getWidth());
                    }
                    last = widget;
                }
            }
            return width;
        }

        @Override
        public boolean charTyped(char pCodePoint, int pModifiers) {
            if (!this.visible || !this.active) {
                return false;
            }
            boolean result = false;
            for (int i = 0; i < this.widgets.size(); i++) {
                PositionWiget wiget = this.widgets.get(i);
                if (wiget.widget.visible && wiget.widget.active && wiget.consumerCharTyped && wiget.widget.charTyped(pCodePoint, pModifiers)) {
                    result = true;
                }
            }
            return result || super.charTyped(pCodePoint, pModifiers);
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (!this.visible || !this.active) {
                return false;
            }
            boolean result = false;
            for (int i = 0; i < this.widgets.size(); i++) {
                PositionWiget wiget = this.widgets.get(i);
                wiget.widget.setFocused(false);
                if (wiget.widget.visible && wiget.widget.active && wiget.consumerMouseClicked && wiget.widget.mouseClicked(pMouseX, pMouseY, pButton)) {
                    wiget.widget.setFocused(true);
                    result = true;
                }
            }
            return result || super.mouseClicked(pMouseX, pMouseY, pButton);
        }

        @Override
        public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
            if (!this.visible || !this.active) {
                return false;
            }
            boolean result = false;
            for (int i = 0; i < this.widgets.size(); i++) {
                PositionWiget wiget = this.widgets.get(i);
                if (wiget.widget.visible && wiget.widget.active && wiget.consumerKeyPressed && wiget.widget.keyPressed(pKeyCode, pScanCode, pModifiers)) {
                    result = true;
                    break;
                }
            }
            return result || super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }

        @Override
        public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
            if (!this.visible || !this.active) {
                return false;
            }
            boolean result = false;
            for (int i = 0; i < this.widgets.size(); i++) {
                PositionWiget wiget = this.widgets.get(i);
                if (wiget.widget.visible && wiget.widget.active && wiget.consumerKeyReleased && wiget.widget.keyReleased(pKeyCode, pScanCode, pModifiers)) {
                    result = true;
                }
            }
            return result || super.keyReleased(pKeyCode, pScanCode, pModifiers);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            for (int i = 0; i < this.widgets.size(); i++) {
                PositionWiget widget = this.widgets.get(i);
                if (widget.widget == null || !widget.widget.visible) continue;
                widget.graphics = graphics;
                widget.mouseX = mouseX;
                widget.mouseY = mouseY;
                widget.partialTick = partialTick;

                if (widget.preRender!=null) {
                    int width_or_height = widget.preRender.apply(widget);
                    if (this.isHorizontal){
                        x += width_or_height;
                    }else {
                        y += width_or_height;
                    }
                }
                widget.widget.setPosition(widget.left + x, widget.top + y);
                widget.widget.render(graphics, mouseX, mouseY, partialTick);
                if (widget.postRender!=null) {
                    int width_or_height = widget.postRender.apply(widget);
                    if (this.isHorizontal){
                        x += width_or_height;
                    }else {
                        y += width_or_height;
                    }
                }

                if (this.isHorizontal){
                    x += widget.widget.getWidth() + widget.right;
                    y += widget.bottom;
                }else {
                    x += widget.right;
                    y += widget.widget.getHeight() + widget.bottom;
                }
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            for (PositionWiget widget : this.widgets) {
                widget.widget.updateNarration(output);
            }
        }
    }
}
