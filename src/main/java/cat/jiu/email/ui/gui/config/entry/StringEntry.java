package cat.jiu.email.ui.gui.config.entry;

import cat.jiu.email.ui.gui.config.ConfigEntry;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeConfigSpec;

import java.awt.*;

public class StringEntry extends ConfigEntry<String> {
    protected final TextFieldWidget field;
    protected String cache;
    public StringEntry(ForgeConfigSpec.ConfigValue<String> value, ForgeConfigSpec.ValueSpec spec) {
        super(value, spec);
        this.field = this.addWidget(new TextFieldWidget(Minecraft.getInstance().fontRenderer, 0, 0, 150, 18, ITextComponent.getTextComponentOrEmpty(null)));
        this.field.setMaxStringLength(Integer.MAX_VALUE);
        this.field.x = Minecraft.getInstance().getMainWindow().getScaledWidth()/2 - this.field.getWidth()/2 + this.field.getWidth() - this.field.getWidth()/2 - 1;
        this.addUndoAndReset();
        this.setCacheValue(value.get());
    }

    @Override
    public void render(Screen gui, MatrixStack matrix, int x, int y, int mouseX, int mouseY) {
        this.renderWidget(gui, matrix, x, y, mouseX, mouseY);
        this.drawAlignRightString(matrix, this.configName, this.field.x - 5, this.field.y + 5, Color.WHITE.getRGB(), true, gui.getMinecraft().fontRenderer);
    }

    @Override
    public void drawHoverText(Screen gui, MatrixStack matrix, int mouseX, int mouseY) {
        try {
            this.drawCommentWithRange(gui, matrix, mouseX, mouseY,
                    this.field.x-5-gui.getMinecraft().fontRenderer.getStringWidth(this.configName), this.field.y+5,
                    gui.getMinecraft().fontRenderer.getStringWidth(this.configName), gui.getMinecraft().fontRenderer.FONT_HEIGHT);
        } catch (Exception e) {e.printStackTrace();}
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean flag = super.charTyped(codePoint, modifiers);
        this.setCacheValue(this.field.getText());
        return flag;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean flag = super.keyPressed(keyCode, scanCode, modifiers);
        this.setCacheValue(this.field.getText());
        return flag;
    }

    @Override
    protected String getCacheValue() {
        return this.cache;
    }

    @Override
    protected void setCacheValue(String newValue) {
        this.cache = newValue;
        if(this.field!=null) this.field.setText(newValue);
    }

    @Override
    protected Widget getConfigWidget() {
        return this.field;
    }
}
