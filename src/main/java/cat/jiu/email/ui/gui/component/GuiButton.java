package cat.jiu.email.ui.gui.component;

import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;

public class GuiButton extends Button {

    public GuiButton(int x, int y, int width, int height, ITextComponent title, IPressable pressedAction) {
        super(x, y, width, height, title, pressedAction);
    }

    public GuiButton(int x, int y, int width, int height, ITextComponent title, IPressable pressedAction, ITooltip onTooltip) {
        super(x, y, width, height, title, pressedAction, onTooltip);
    }
}
