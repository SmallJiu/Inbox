package cat.jiu.email.ui.gui.config.entry;

import cat.jiu.email.ui.gui.config.ConfigEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.awt.*;

public class StringEntry extends ConfigEntry<String> {
    protected final EditBox field;
    protected String cache;
    public StringEntry(ForgeConfigSpec.ConfigValue<String> value, ForgeConfigSpec.ValueSpec spec) {
        super(value, spec);
        this.field = this.addWidget(new EditBox(Minecraft.getInstance().font, 0, 0, 150, 18, Component.nullToEmpty(null)));
        this.field.setMaxLength(Integer.MAX_VALUE);
        this.field.setX(Minecraft.getInstance().getWindow().getGuiScaledWidth()/2 - this.field.getWidth()/2 + this.field.getWidth() - this.field.getWidth()/2 - 1);
        this.addUndoAndReset();
        this.setCacheValue(value.get());
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

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        boolean flag = super.charTyped(codePoint, modifiers);
        this.setCacheValue(this.field.getValue());
        return flag;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean flag = super.keyPressed(keyCode, scanCode, modifiers);
        this.setCacheValue(this.field.getValue());
        return flag;
    }

    @Override
    protected String getCacheValue() {
        return this.cache;
    }

    @Override
    protected void setCacheValue(String newValue) {
        this.cache = newValue;
        if(this.field!=null) this.field.setValue(newValue);
    }

    @Override
    protected AbstractWidget getConfigWidget() {
        return this.field;
    }
}
