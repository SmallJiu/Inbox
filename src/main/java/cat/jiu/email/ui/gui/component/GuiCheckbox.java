package cat.jiu.email.ui.gui.component;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
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

    @Override
    public void onPress() {
        super.onPress();
        if (this.selected!=null) {
            this.selected.run();
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        graphics.vLine(this.getX(), this.getY(), this.getY()+this.height, Color.BLACK.getRGB());
        graphics.vLine(this.getX()+this.width, this.getY(), this.getY()+this.height, Color.BLACK.getRGB());

        graphics.hLine(this.getX(), this.getX()+this.width, this.getY(), Color.BLACK.getRGB());
        graphics.hLine(this.getX(), this.getX()+this.width, this.getY()+this.height, Color.BLACK.getRGB());

        graphics.fill(this.getX()+1, this.getY()+1, this.getX()+this.width, this.getY()+this.height, Color.WHITE.getRGB());

        if (this.selected()) {
            graphics.fill(this.getX()+4, this.getY()+4, this.getX()+this.width-3, this.getY()+this.height-3, Color.RED.getRGB());
        }
    }
}
