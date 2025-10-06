package cat.jiu.email.ui.gui.component;

import cat.jiu.core.util.client.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.awt.*;

public class GuiCheckbox extends Checkbox {
    private final Runnable selected;
    public GuiCheckbox(int pX, int pY, int pWidth, int pHeight, Component pMessage, boolean pSelected, Runnable selected) {
        super(pX, pY, pWidth, pHeight, pMessage, pSelected);
        this.selected = selected;
    }

    public GuiCheckbox(int pX, int pY, int pWidth, int pHeight, Component pMessage, boolean pSelected, boolean pShowLabel, Runnable selected) {
        super(pX, pY, pWidth, pHeight, pMessage, pSelected, pShowLabel);
        this.selected = selected;
    }

    public GuiCheckbox messageToTooltip(){
        this.setTooltip(Tooltip.create(this.getMessage()));
        return this;
    }

    @Override
    public void onPress() {
        super.onPress();
        if (this.selected!=null) {
            this.selected.run();
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        return this.isFocused() && super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        RenderUtils.fill(graphics, this.getX()+1, this.getY()+1, this.width, this.height, Color.WHITE.getRGB());

        RenderUtils.vLine(graphics, this.getX(), this.getY(), this.height, Color.BLACK.getRGB());
        RenderUtils.vLine(graphics, this.getX() + this.width, this.getY(), this.height, Color.BLACK.getRGB());

        RenderUtils.hLine(graphics, this.getX(), this.getY(), this.width, Color.BLACK.getRGB());
        RenderUtils.hLine(graphics, this.getX(), this.getY()+this.height, this.width, Color.BLACK.getRGB());

        if (this.selected()) {
            RenderUtils.fill(graphics, this.getX()+2, this.getY()+2, this.width-3, this.height-3, Color.BLACK.getRGB());
        }
    }
}
