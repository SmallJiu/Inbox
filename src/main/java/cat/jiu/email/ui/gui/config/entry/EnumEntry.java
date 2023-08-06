package cat.jiu.email.ui.gui.config.entry;

import cat.jiu.email.ui.gui.config.ConfigEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.common.ForgeConfigSpec;

public class EnumEntry<T extends Enum<T>> extends ConfigEntry<T> {
    private T cache;
    public EnumEntry(ForgeConfigSpec.EnumValue<T> value, ForgeConfigSpec.ValueSpec spec) {
        super(value, spec);
    }

    @Override
    public void render(Screen gui, GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {

    }

    @Override
    protected T getCacheValue() {
        return this.cache;
    }

    @Override
    protected void setCacheValue(T newValue) {
        this.cache = newValue;
    }

    @Override
    protected AbstractWidget getConfigWidget() {
        return null;
    }
}
