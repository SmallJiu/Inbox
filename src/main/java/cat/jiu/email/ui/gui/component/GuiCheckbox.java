package cat.jiu.email.ui.gui.component;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

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
    public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {

        vLine(pPoseStack, this.x, this.y, this.y+this.height, Color.BLACK.getRGB());
        vLine(pPoseStack, this.x+this.width-1, this.y, this.y+this.height, Color.BLACK.getRGB());

        hLine(pPoseStack, this.x, this.x+this.width-1, this.y, Color.BLACK.getRGB());
        hLine(pPoseStack, this.x, this.x+this.width-1, this.y+this.height, Color.BLACK.getRGB());

        fill(pPoseStack, this.x+1, this.y+1, this.x+this.width-1, this.y+this.height-1, Color.WHITE.getRGB());

        if (this.selected()) {
            fill(pPoseStack, this.x+4, this.y+4, this.x+this.width-1-4, this.y+this.height-4, Color.RED.getRGB());
        }
    }
}
