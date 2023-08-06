package cat.jiu.email.ui.gui.config.entry;

import cat.jiu.email.ui.gui.config.ConfigEntry;
import cat.jiu.email.ui.gui.config.GuiConfig;

import com.electronwill.nightconfig.core.Config;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class SubEntry extends ConfigEntry<Object> {
    private final Button button;
    private final List<ConfigEntry<?>> entries;
    public SubEntry(String name, ForgeConfigSpec spec, Config config, String path, GuiConfig parent) {
        super(null, null);
        GuiConfig gui = new GuiConfig(parent.configFile, parent, spec, path);
        this.entries = gui.createEntries(path, spec, config.valueMap());
        gui.setConfigEntries(this.entries);

        this.button = this.addWidget(new Button(0, 0, 300, 20, ITextComponent.getTextComponentOrEmpty(name), btn-> parent.getMinecraft().displayGuiScreen(gui)));
        this.button.x = Minecraft.getInstance().getMainWindow().getScaledWidth()/2 - this.button.getWidth()/2;
        this.addUndoAndReset();
    }

    @Override
    public void render(Screen gui, MatrixStack matrix, int x, int y, int mouseX, int mouseY) {
        this.renderWidget(gui, matrix, x, y, mouseX, mouseY);
    }

    @Override
    public void drawHoverText(Screen gui, MatrixStack matrix, int mouseX, int mouseY) {
        if(this.button.isHovered()){
            try {
                this.drawComment(gui, matrix, mouseX, mouseY);
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
    protected Widget getConfigWidget() {
        return this.button;
    }
}
