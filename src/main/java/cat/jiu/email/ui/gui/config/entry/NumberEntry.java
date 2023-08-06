package cat.jiu.email.ui.gui.config.entry;

import cat.jiu.email.ui.gui.component.GuiFilterTextField;
import cat.jiu.email.ui.gui.config.ConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.common.ForgeConfigSpec;

import java.awt.*;

public abstract class NumberEntry<T extends Number> extends ConfigEntry<T> {
    protected final GuiFilterTextField field;
    protected final boolean isDecimal;
    protected T cache;
    protected NumberEntry(ForgeConfigSpec.ConfigValue<T> value, ForgeConfigSpec.ValueSpec spec, boolean isDecimal) {
        super(value, spec);
        this.isDecimal = isDecimal;
        this.field = this.addWidget(new GuiFilterTextField(String.valueOf(this.cache), Minecraft.getInstance().font, 0,0,150, 18).setTypedCharFilter(typedChar ->
                (isDecimal ? "0123456789." : "0123456789").contains(String.valueOf(typedChar))));
        this.field.setX(Minecraft.getInstance().getWindow().getGuiScaledWidth()/2 - this.field.getWidth()/2 + this.field.getWidth() - this.field.getWidth()/2 - 1);
        this.addUndoAndReset();
    }

    @Override
    protected AbstractWidget getConfigWidget() {
        return this.field;
    }

    @Override
    public void render(Screen gui, GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        this.renderWidget(gui, graphics, x, y, mouseX, mouseY);
        this.drawAlignRightString(graphics, this.configName, this.field.getX() - 5, this.field.getY() + 5, Color.WHITE.getRGB(), true, gui.getMinecraft().font);
    }

    @Override
    public void drawHoverText(Screen gui, GuiGraphics graphics, int mouseX, int mouseY) {
        try {
            this.drawCommentWithRange(gui, graphics, mouseX, mouseY,
                    this.field.getX() -5-gui.getMinecraft().font.width(this.configName), this.field.getY() +5,
                    gui.getMinecraft().font.width(this.configName), gui.getMinecraft().font.lineHeight);
        } catch (Exception e) {e.printStackTrace();}
    }

    protected abstract T parse(String value);

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean flag = super.charTyped(codePoint, modifiers);
        this.setCacheValue(this.parse(this.field.getValue()));
        return flag;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean flag = super.keyPressed(keyCode, scanCode, modifiers);
        this.setCacheValue(this.parse(this.field.getValue()));
        return flag;
    }

    @Override
    protected T getCacheValue() {
        return this.cache;
    }

    @Override
    protected void setCacheValue(T newValue) {
        this.cache = newValue;
        if(this.field!=null) this.field.setValue(String.valueOf(newValue));
    }
}
