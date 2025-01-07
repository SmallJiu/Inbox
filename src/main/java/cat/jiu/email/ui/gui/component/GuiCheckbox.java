package cat.jiu.email.ui.gui.component;

import cat.jiu.core.util.client.RenderUtils;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.util.text.ITextComponent;

import java.awt.Color;

public class GuiCheckbox extends CheckboxButton {
    private final Runnable selected;
    public GuiCheckbox(int pX, int pY, int pWidth, int pHeight, ITextComponent pMessage, boolean pSelected, Runnable selected) {
        super(pX, pY, pWidth, pHeight, pMessage, pSelected);
        this.selected = selected;
    }

    public GuiCheckbox(int pX, int pY, int pWidth, int pHeight, ITextComponent pMessage, boolean pSelected, boolean pShowLabel, Runnable selected) {
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
    public void renderWidget(MatrixStack stack, int pMouseX, int pMouseY, float pPartialTick) {
        if (false) {
            RenderUtils.vLine(stack, this.x, this.y, this.y+this.height, Color.BLACK.getRGB());
            RenderUtils.vLine(stack, this.x+this.width, this.y, this.y+this.height, Color.BLACK.getRGB());

            RenderUtils.hLine(stack, this.x, this.x+this.width, this.y, Color.BLACK.getRGB());
            RenderUtils.hLine(stack, this.x, this.x+this.width, this.y+this.height, Color.BLACK.getRGB());

            RenderUtils.fill(stack, this.x+1, this.y+1, this.x+this.width, this.y+this.height, Color.WHITE.getRGB());

            if (this.isChecked()) {
                RenderUtils.fill(stack, this.x+4, this.y+4, this.x+this.width-3, this.y+this.height-3, Color.RED.getRGB());
            }
        }

        RenderUtils.square(stack, this.x, this.y , this.width - 1, this.height - 1, this.isChecked() ? Color.RED.getRGB() : Color.BLACK.getRGB(), Color.WHITE.getRGB());
    }
}
