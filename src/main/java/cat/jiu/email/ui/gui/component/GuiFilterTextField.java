package cat.jiu.email.ui.gui.component;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public class GuiFilterTextField extends TextFieldWidget {
    private Predicate<Character> typedCharFilter;
    private final String defaultText;
    public GuiFilterTextField(String defaultText, FontRenderer fontrenderer, int x, int y, int par5Width, int par6Height) {
        super(fontrenderer, x, y, par5Width, par6Height, ITextComponent.getTextComponentOrEmpty(null));
        this.setText(defaultText);
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
            if(this.defaultText.equals(this.getText())) {
                this.setText("");
            }
            boolean flag = super.charTyped(typedChar, keyCode);

            if(this.getText().isEmpty()) {
                this.setText(this.defaultText);
            }
            return flag;
        }
        return false;
    }
}
