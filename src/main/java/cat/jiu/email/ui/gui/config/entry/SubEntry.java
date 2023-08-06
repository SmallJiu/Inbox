package cat.jiu.email.ui.gui.config.entry;

import cat.jiu.email.ui.gui.component.GuiButton;
import cat.jiu.email.ui.gui.config.ConfigEntry;
import cat.jiu.email.ui.gui.config.GuiConfig;

import com.electronwill.nightconfig.core.Config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.function.Supplier;

public class SubEntry extends ConfigEntry<Object> {
    private final Button button;
    private final List<ConfigEntry<?>> entries;
    public SubEntry(String name, ForgeConfigSpec spec, Config config, String path, GuiConfig parent) {
        super(null, null);
        GuiConfig gui = new GuiConfig(parent.configFile, parent, spec, path);
        this.entries = gui.createEntries(path, spec, config.valueMap());
        gui.setConfigEntries(this.entries);

        this.button = this.addWidget(new GuiButton(0, 0, 300, 20, Component.nullToEmpty(name), btn-> parent.getMinecraft().setScreen(gui), Supplier::get));
        this.button.setX(Minecraft.getInstance().getWindow().getGuiScaledWidth()/2 - this.button.getWidth()/2);
        this.addUndoAndReset();
    }

    @Override
    public void render(Screen gui, GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        this.renderWidget(gui, graphics, x, y, mouseX, mouseY);
    }

    @Override
    public void drawHoverText(Screen gui, GuiGraphics graphics, int mouseX, int mouseY) {
        if(this.button.isHovered()){
            try {
                this.drawComment(gui, graphics, mouseX, mouseY);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void save() {
        this.entries.forEach(ConfigEntry::save);
    }

    @Override
    public void undo() {
        this.entries.forEach(ConfigEntry::undo);
    }

    @Override
    public void reset() {
        this.entries.forEach(ConfigEntry::reset);
    }

    @Override
    public int getWeight() {
        return 0;
    }

    @Override
    public boolean isChanged() {
        for (ConfigEntry<?> entry : this.entries) {
            if (entry.isChanged()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDefault() {
        for (ConfigEntry<?> entry : this.entries) {
            if (entry.isDefault()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Object getCacheValue() {
        return null;
    }

    @Override
    protected void setCacheValue(Object newValue) {

    }

    @Override
    protected AbstractWidget getConfigWidget() {
        return this.button;
    }
}
