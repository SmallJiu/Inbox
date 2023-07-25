package cat.jiu.email.ui.gui.config.entry;

import cat.jiu.email.ui.gui.config.ConfigEntry;
import cat.jiu.email.ui.gui.config.GuiConfig;

import com.electronwill.nightconfig.core.Config;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class SubEntry extends ConfigEntry {
    private final Config config;
    private final Button button;
    private final List<ConfigEntry> entries;
    public SubEntry(String name, ForgeConfigSpec spec, Config config, String path, GuiConfig parent) {
        this.config = config;
        this.entries = parent.create(path, spec, config.valueMap());
        this.button = new Button(0, 0, 300, 20, ITextComponent.getTextComponentOrEmpty(name), btn->
            parent.getMinecraft().displayGuiScreen(new GuiConfig(parent.configFile, parent, spec, config.valueMap(), path))
        );
    }

    @Override
    public void render(MatrixStack matrix, int x, int y, int mouseX, int mouseY) {
        this.button.x = Minecraft.getInstance().getMainWindow().getScaledWidth()/2 - this.button.getWidth()/2;
        this.button.y = y;
        this.button.render(matrix, mouseX, mouseY, 0);
    }

    @Override
    public void save() {
        this.entries.forEach(ConfigEntry::save);
    }

    @Override
    public ForgeConfigSpec.ConfigValue<?> getConfigValue() {
        return null;
    }

    @Override
    public void undo() {
        this.entries.forEach(ConfigEntry::undo);
    }

    @Override
    public void reset() {
        this.entries.forEach(ConfigEntry::undo);
    }

    @Override
    public int getWeight() {
        return 0;
    }

    @Override
    public boolean mouseClick(double mouseX, double mouseY, int button) {
        return this.button.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isChanged() {
        for (ConfigEntry entry : this.entries) {
            if (entry.isChanged()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDefault() {
        for (ConfigEntry entry : this.entries) {
            if (entry.isDefault()) {
                return true;
            }
        }
        return false;
    }
}
