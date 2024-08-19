package cat.jiu.email.ui.gui.component;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SubWidget extends AbstractWidget {
    public final List<Renderable> renderables = Lists.newArrayList();
    private final List<GuiEventListener> children = Lists.newArrayList();
    private final List<NarratableEntry> narratables = Lists.newArrayList();

    public SubWidget(int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        super(pX, pY, pWidth, pHeight, pMessage);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, pMouseX, pMouseY, pPartialTick);
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        for (GuiEventListener child : this.children) {
            if (child.mouseClicked(pMouseX, pMouseY, pButton)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        for (GuiEventListener child : this.children) {
            if (child.mouseReleased(pMouseX, pMouseY, pButton)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        for (GuiEventListener child : this.children) {
            if (child.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)) return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        for (GuiEventListener child : this.children) {
            if (child.isMouseOver(pMouseX, pMouseY)) return true;
        }
        return false;
    }

    @Override
    public boolean isFocused() {
        for (GuiEventListener child : this.children) {
            if (child.isFocused()) return true;
        }
        return false;
    }

    @Override
    public void mouseMoved(double pMouseX, double pMouseY) {
        for (GuiEventListener child : this.children) {
            child.mouseMoved(pMouseX, pMouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        for (GuiEventListener child : this.children) {
            if (child.mouseScrolled(pMouseX, pMouseY, pDelta)) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        for (GuiEventListener child : this.children) {
            if (child.keyPressed(pKeyCode, pScanCode, pModifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        for (GuiEventListener child : this.children) {
            if (child.keyReleased(pKeyCode, pScanCode, pModifiers)) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        for (GuiEventListener child : this.children) {
            if (child.charTyped(pCodePoint, pModifiers)) return true;
        }
        return false;
    }

    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T pWidget) {
        this.renderables.add(pWidget);
        return this.addWidget(pWidget);
    }

    protected <T extends Renderable> T addRenderableOnly(T pRenderable) {
        this.renderables.add(pRenderable);
        return pRenderable;
    }

    protected <T extends GuiEventListener & NarratableEntry> T addWidget(T pListener) {
        this.children.add(pListener);
        this.narratables.add(pListener);
        return pListener;
    }

    protected void removeWidget(GuiEventListener pListener) {
        if (pListener instanceof Renderable) {
            this.renderables.remove((Renderable)pListener);
        }

        if (pListener instanceof NarratableEntry) {
            this.narratables.remove((NarratableEntry)pListener);
        }

        this.children.remove(pListener);
    }

    protected void clearWidgets() {
        this.renderables.clear();
        this.children.clear();
        this.narratables.clear();
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        for (NarratableEntry narratable : this.narratables) {
            narratable.updateNarration(output);
        }
    }
}
