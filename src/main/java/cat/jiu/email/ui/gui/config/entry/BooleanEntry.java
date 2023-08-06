package cat.jiu.email.ui.gui.config.entry;

import cat.jiu.email.ui.gui.component.GuiButton;
import cat.jiu.email.ui.gui.config.ConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.function.Supplier;

public class BooleanEntry extends ConfigEntry<Boolean> {
    public static final Component trueText = Component.nullToEmpty(ChatFormatting.GREEN + "true"),
                                       falseText = Component.nullToEmpty(ChatFormatting.RED + "false");
    private final Button button;
    private boolean cache;
    public BooleanEntry(ForgeConfigSpec.BooleanValue value, ForgeConfigSpec.ValueSpec spec) {
        super(value, spec);
        this.button = this.addWidget(new GuiButton(0,0,154, 20, cache ? trueText : falseText, btn->
            this.setCacheValue(!this.getCacheValue())
        , Supplier::get){
            @Override
            public Component getMessage() {
                return cache ? trueText : falseText;
            }
        });

        this.button.setX(Minecraft.getInstance().getWindow().getGuiScaledWidth()/2 - this.button.getWidth()/2 + this.button.getWidth() - this.button.getWidth()/2 - 2);
        this.addUndoAndReset();
    }

    @Override
    protected AbstractWidget getConfigWidget() {
        return this.button;
    }

    @Override
    public void render(Screen gui, GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        this.renderWidget(gui, graphics, x, y, mouseX, mouseY);
        this.drawAlignRightString(graphics, this.configName, this.button.getX() - 5, this.button.getY() + 5, Color.WHITE.getRGB(), true, gui.getMinecraft().font);
    }

    @Override
    public void drawHoverText(Screen gui, GuiGraphics graphics, int mouseX, int mouseY) {
        try {
            this.drawCommentWithRange(gui, graphics, mouseX, mouseY, this.button.getX() -5-gui.getMinecraft().font.width(this.configName), this.button.getY() +5, gui.getMinecraft().font.width(this.configName), Minecraft.getInstance().font.lineHeight);
        } catch (Exception ignored) {}
    }

    @Override
    protected Boolean getCacheValue() {
        return this.cache;
    }

    @Override
    protected void setCacheValue(Boolean newValue) {
        this.cache = newValue;
    }
}
