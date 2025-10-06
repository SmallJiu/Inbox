package cat.jiu.email.ui.gui.component;

import com.google.gson.internal.LazilyParsedNumber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public class GuiFilterTextField extends EditBox {
    public static final Predicate<Character>
            NUMBER_FILTER = c->"0123456789".contains(c.toString()),
            DECIAML_FILTER = c->"0123456789.".contains(c.toString());

    private Predicate<Character> typedCharFilter;
    private final String defaultText;

    public GuiFilterTextField(String defaultText, boolean deciamlNumber, int x, int y, int width, int height) {
        this(defaultText, x, y, width, height);
        this.setTypedCharFilter(deciamlNumber ? DECIAML_FILTER : NUMBER_FILTER);
    }
    public GuiFilterTextField(String defaultText, int x, int y, int width, int height) {
        this(defaultText, Minecraft.getInstance().font, x, y, width, height);
    }
    public GuiFilterTextField(String defaultText, Font font, int x, int y, int width, int height) {
        super(font, x, y, width, height, CommonComponents.EMPTY);
        this.setValue(defaultText);
        this.defaultText = defaultText;
    }

    public GuiFilterTextField setTypedCharFilter(Predicate<Character> filter) {
        this.typedCharFilter = filter;
        return this;
    }

    public Number getAsNumber(){
        if (this.getValue().isEmpty())  {
            return 0;
        }
        try {
            return new LazilyParsedNumber(this.getValue());
        }catch (Exception e){
            return 0;
        }
    }

    @Override
    public boolean charTyped(char typedChar, int keyCode) {
        boolean typedCharTest = this.typedCharFilter == null || this.typedCharFilter.test(typedChar);
        if(typedCharTest) {
            boolean flag = super.charTyped(typedChar, keyCode);

            if(this.getValue().isEmpty()) {
                this.setValue(this.defaultText);
            }
            return flag;
        }
        return false;
    }
}
