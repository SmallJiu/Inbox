package cat.jiu.core.util.client.config;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * @author small_jiu
 */
public class GuiButton extends Button {
    public GuiButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress, CreateNarration pCreateNarration) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress, pCreateNarration);
    }
    public GuiButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress) {
        super(pX, pY, pWidth, pHeight, pMessage, pOnPress, Button.DEFAULT_NARRATION);
    }

    public GuiButton(Builder builder) {
        super(builder);
    }
}
