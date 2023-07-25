package cat.jiu.email.ui.gui.config.entry;

import cat.jiu.email.ui.gui.config.ConfigEntry;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeConfigSpec;

import java.awt.*;
import java.util.Objects;

public class NumberEntry extends ConfigEntry {
    private final ForgeConfigSpec.ConfigValue<Number> value;
    private final ForgeConfigSpec.ValueSpec spec;
    private final TextFieldWidget field;
    private Number cache;
    public NumberEntry(ForgeConfigSpec.ConfigValue<Number> value, ForgeConfigSpec.ValueSpec spec) {
        this.value = value;
        this.spec = spec;
        this.cache = value.get();
        this.field = new TextFieldWidget(Minecraft.getInstance().fontRenderer, 0,0,150, 18, ITextComponent.getTextComponentOrEmpty(String.valueOf(this.cache)));
    }

    @Override
    public void render(MatrixStack matrix, int x, int y, int mouseX, int mouseY) {
        this.field.x = Minecraft.getInstance().getMainWindow().getScaledWidth()/2 - this.field.getWidth()/2 + this.field.getWidth() - this.field.getWidth()/2 - 1;
        this.field.y = y;
        this.field.render(matrix, mouseX, mouseY, 0);
        String s;
        if(this.spec.getTranslationKey()!=null){
            s = I18n.format(this.spec.getTranslationKey());
            if(Objects.equals(s, this.spec.getTranslationKey())){
                s = this.value.getPath().get(this.value.getPath().size()-1);
            }
        }else {
            s = this.value.getPath().get(this.value.getPath().size()-1);
        }
        this.drawAlignRightString(matrix, s, this.field.x-5, this.field.y+5, Color.WHITE.getRGB(), true);
    }

    @Override
    public ForgeConfigSpec.ConfigValue<? extends Number> getConfigValue() {
        return this.value;
    }

    @Override
    public boolean mouseClick(double mouseX, double mouseY, int button) {
        return this.field.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.field.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.field.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void undo() {
        this.field.setText(String.valueOf(value.get()));
    }

    @Override
    public void reset() {
        this.field.setText(String.valueOf(value.get()));
    }

    @Override
    public void save() {
        this.value.set(this.cache);
    }

    @Override
    public boolean isChanged() {
        return !Objects.equals(this.cache, this.value.get());
    }


    @Override
    public boolean isDefault() {
        return this.cache != this.spec.getDefault();
    }
}
