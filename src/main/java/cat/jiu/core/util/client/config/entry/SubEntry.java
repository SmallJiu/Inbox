package cat.jiu.core.util.client.config.entry;

import cat.jiu.core.util.client.config.ConfigEntry;
import cat.jiu.core.util.client.config.GuiConfig;
import cat.jiu.core.util.client.config.GuiButton;

import com.electronwill.nightconfig.core.Config;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author small_jiu
 */
public class SubEntry extends ConfigEntry<Object> {
    private final Button button;
    private final List<ConfigEntry<?>> entries;
    private final String comment;
    public SubEntry(String name, ForgeConfigSpec spec, Config config, String path, List<String> paths, GuiConfig parent) {
        super(null, null);
        GuiConfig gui = new GuiConfig(parent.configFile, parent, spec, path, paths);
        this.entries = gui.createEntries(path, spec, config.valueMap());
        gui.setConfigEntries(this.entries);

        String lC = spec.getLevelComment(paths);
        String comment = I18n.get(StringUtil.isNullOrEmpty(lC) ? "" : lC);
        this.comment = StringUtil.isNullOrEmpty(comment) ? null : comment;

        String key = spec.getLevelTranslationKey(paths);
        this.button = this.addWidget(new GuiButton(0, 0, 300, 20, Component.nullToEmpty(key != null ? I18n.get(key) : name), btn-> parent.getMinecraft().setScreen(gui), Supplier::get));
        this.button.setX(Minecraft.getInstance().getWindow().getGuiScaledWidth()/2 - this.button.getWidth()/2);
        this.addUndoAndReset();
    }

    @Override
    public void render(Screen gui, GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        this.renderWidget(gui, graphics, x, y, mouseX, mouseY);
    }

    @Override
    public void drawHoverText(Screen gui, GuiGraphics graphics, int mouseX, int mouseY) {
        try {
            this.drawCommentWithRange(gui, graphics, mouseX, mouseY, this.button.getX(), this.button.getY(), this.button.getWidth(), this.button.getHeight());
        } catch (Exception ignored) {}
    }

    @Override
    protected void drawComment(Screen gui, GuiGraphics graphics, int mouseX, int mouseY) throws Exception {
        if (this.comment !=null) {
            List<Component> comments = Lists.newArrayList();
            comments.add(Component.nullToEmpty(this.comment));
            graphics.renderComponentTooltip(gui.getMinecraft().font, comments, mouseX+5, mouseY);
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
