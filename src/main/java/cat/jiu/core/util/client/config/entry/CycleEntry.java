package cat.jiu.core.util.client.config.entry;

import cat.jiu.core.util.client.config.ConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CycleEntry<T> extends ConfigEntry<T> {
    protected final Button button;
    protected final List<T> list = new ArrayList<>();
    protected int cacheIndex;
    public CycleEntry(ForgeConfigSpec.ConfigValue<T> value, ForgeConfigSpec.ValueSpec spec) {
        super(value, spec);

        this.button = this.addWidget(Button.builder(Component.nullToEmpty(String.valueOf(this.defaultValue)), b->{
            this.cacheIndex++;
            if (this.cacheIndex >= this.list.size()) {
                this.cacheIndex = 0;
            }
            this.setCacheValue(this.list.get(this.cacheIndex));
        })
                        .pos(Minecraft.getInstance().getWindow().getGuiScaledWidth()/2 - 150/2 + 150 - 150/2 - 1, 0)
                        .size(150, 18)
                .build());
        this.addUndoAndReset();
        this.setCacheValue(value.get());
    }

    @Override
    public void render(Screen gui, GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        this.renderWidget(gui, graphics, x, y, mouseX, mouseY);
        this.drawAlignRightString(graphics, this.configName, this.button.getX() - 5, this.button.getY() + 5, Color.WHITE.getRGB(), true, gui.getMinecraft().font);
    }

    @Override
    public void drawHoverText(Screen gui, GuiGraphics graphics, int mouseX, int mouseY) {
        try {
            this.drawCommentWithRange(gui, graphics, mouseX, mouseY,
                    this.button.getX() -5-gui.getMinecraft().font.width(this.configName), this.button.getY() +5,
                    gui.getMinecraft().font.width(this.configName), gui.getMinecraft().font.lineHeight);
        } catch (Exception e) {e.printStackTrace();}
    }

    @Override
    protected T getCacheValue() {
        return this.list.get(this.cacheIndex);
    }

    @Override
    protected void setCacheValue(T newValue) {
        this.cacheIndex = this.list.indexOf(newValue);
        this.button.setMessage(Component.translatable(String.valueOf(newValue)));
    }

    @Override
    protected AbstractWidget getConfigWidget() {
        return this.button;
    }
}
