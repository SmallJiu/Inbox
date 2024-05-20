package cat.jiu.email.ui.gui.component;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class GuiButton extends Button {
    public GuiButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress, Button.OnTooltip pOnTooltip) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress, pOnTooltip);
    }
    public GuiButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress);
    }
}
