package cat.jiu.email.ui.gui.config.entry;

import cat.jiu.email.ui.gui.config.ConfigEntry;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.ForgeConfigSpec;

import java.awt.*;
import java.util.Objects;
import java.util.StringJoiner;

public class BooleanEntry extends ConfigEntry {
    private final ForgeConfigSpec.BooleanValue value;
    private final ForgeConfigSpec.ValueSpec spec;
    private final Button button;
    private boolean cache;
    public BooleanEntry(ForgeConfigSpec.BooleanValue value, ForgeConfigSpec.ValueSpec spec) {
        this.value = value;
        this.spec = spec;
        this.cache = value.get();
        ITextComponent trueText = ITextComponent.getTextComponentOrEmpty(TextFormatting.GREEN + "true");
        ITextComponent falseText = ITextComponent.getTextComponentOrEmpty(TextFormatting.RED + "false");
        this.button = new Button(0,0,154, 20, cache ? trueText : falseText, btn->
            this.cache = !this.cache
        ){
            @Override
            public ITextComponent getMessage() {
                return cache ? trueText : falseText;
            }
        };
    }

    @Override
    public void render(MatrixStack matrix, int x, int y, int mouseX, int mouseY) {
        this.button.x = Minecraft.getInstance().getMainWindow().getScaledWidth()/2 - this.button.getWidth()/2 + this.button.getWidth() - this.button.getWidth()/2 - 2;
        this.button.y = y;
        this.button.render(matrix, mouseX, mouseY, 0);
        String s = I18n.format(this.spec.getTranslationKey());
        if(Objects.equals(s, this.spec.getTranslationKey())){
            s = this.value.getPath().get(this.value.getPath().size()-1);
        }
        this.drawAlignRightString(matrix, s, this.button.x-5, this.button.y+5, Color.WHITE.getRGB(), true);
    }

    @Override
    public boolean mouseClick(double mouseX, double mouseY, int button) {
        return this.button.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public ForgeConfigSpec.BooleanValue getConfigValue() {
        return this.value;
    }

    @Override
    public void undo() {
        this.cache = this.value.get();
    }

    @Override
    public void reset() {
        this.cache = this.value.get();
    }

    @Override
    public void save() {
        this.value.set(this.cache);
    }

    @Override
    public boolean isChanged() {
        return this.cache != this.value.get();
    }

    @Override
    public boolean isDefault() {
        return this.cache != (Boolean)this.spec.getDefault();
    }
}
