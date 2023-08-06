package cat.jiu.email.ui.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public class GuiFilterTextField extends EditBox {
    private Predicate<Character> typedCharFilter;
    private final String defaultText;
    public GuiFilterTextField(String defaultText, Font font, int x, int y, int par5Width, int par6Height) {
        super(font, x, y, par5Width, par6Height, Component.nullToEmpty(null));
        this.setValue(defaultText);
        this.defaultText = defaultText;
    }

    public GuiFilterTextField setTypedCharFilter(Predicate<Character> filter) {
        this.typedCharFilter = filter;
        return this;
    }

    @Override
    public boolean charTyped(char typedChar, int keyCode) {
        boolean typedCharTest = this.typedCharFilter != null && this.typedCharFilter.test(typedChar);
        if(typedCharTest && this.isFocused()) {
            if(this.defaultText.equals(this.getValue())) {
                this.setValue("");
            }
            boolean flag = super.charTyped(typedChar, keyCode);

            if(this.getValue().isEmpty()) {
                this.setValue(this.defaultText);
            }
            return flag;
        }
        return false;
    }
}
