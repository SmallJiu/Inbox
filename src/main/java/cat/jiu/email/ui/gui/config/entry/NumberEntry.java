package cat.jiu.email.ui.gui.config.entry;

import cat.jiu.email.ui.gui.component.GuiFilterTextField;
import cat.jiu.email.ui.gui.config.ConfigEntry;
import cat.jiu.email.util.EmailUtils;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraftforge.common.ForgeConfigSpec;

import java.awt.*;

public abstract class NumberEntry<T extends Number> extends ConfigEntry<T> {
    protected final GuiFilterTextField field;
    protected final boolean isDecimal;
    protected T cache;
    protected NumberEntry(ForgeConfigSpec.ConfigValue<T> value, ForgeConfigSpec.ValueSpec spec, boolean isDecimal) {
        super(value, spec);
        this.isDecimal = isDecimal;
        this.field = this.addWidget(new GuiFilterTextField(String.valueOf(cache), Minecraft.getInstance().fontRenderer, 0,0,150, 18).setTypedCharFilter(typedChar ->
                (isDecimal ? "0123456789." : "0123456789").contains(String.valueOf(typedChar))));
        this.field.x = Minecraft.getInstance().getMainWindow().getScaledWidth()/2 - this.field.getWidth()/2 + this.field.getWidth() - this.field.getWidth()/2 - 1;
        this.addUndoAndReset();
    }

    @Override
    protected Widget getConfigWidget() {
        return this.field;
    }

    @Override
    public void render(Screen gui, MatrixStack matrix, int x, int y, int mouseX, int mouseY) {
        this.renderWidget(gui, matrix, x, y, mouseX, mouseY);
        EmailUtils.drawAlignRightString(matrix, this.configName, this.field.x - 5, this.field.y + 5, Color.WHITE.getRGB(), true);
    }

    @Override
    public void drawHoverText(Screen gui, MatrixStack matrix, int mouseX, int mouseY) {
        try {
            this.drawCommentWithRange(gui, matrix, mouseX, mouseY,
                    this.field.x-5-gui.getMinecraft().fontRenderer.getStringWidth(this.configName), this.field.y+5,
                    gui.getMinecraft().fontRenderer.getStringWidth(this.configName), gui.getMinecraft().fontRenderer.FONT_HEIGHT);
        } catch (Exception e) {e.printStackTrace();}
    }

    protected abstract T parse(String value);

    @Override
    public boolean mouseClick(double mouseX, double mouseY, int button) {
        return super.mouseClick(mouseX, mouseY, button) || this.field.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean flag = this.field.charTyped(codePoint, modifiers);
        this.setCacheValue(this.parse(this.field.getText()));
        return flag;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean flag = this.field.keyPressed(keyCode, scanCode, modifiers);
        this.setCacheValue(this.parse(this.field.getText()));
        return flag;
    }

    @Override
    protected T getCacheValue() {
        return this.cache;
    }

    @Override
    protected void setCacheValue(T newValue) {
        this.cache = newValue;
        if(this.field!=null) this.field.setText(String.valueOf(newValue));
    }
}
