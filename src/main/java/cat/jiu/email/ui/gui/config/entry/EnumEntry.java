package cat.jiu.email.ui.gui.config.entry;

import cat.jiu.email.ui.gui.config.ConfigEntry;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraftforge.common.ForgeConfigSpec;

public class EnumEntry<T extends Enum<T>> extends ConfigEntry {
    private final ForgeConfigSpec.EnumValue<T> value;
    private final ForgeConfigSpec.ValueSpec spec;
    private final T cache;
    public EnumEntry(ForgeConfigSpec.EnumValue<T> value, ForgeConfigSpec.ValueSpec spec) {
        this.value = value;
        this.spec = spec;
        this.cache = value.get();
    }

    @Override
    public void render(MatrixStack matrix, int x, int y, int mouseX, int mouseY) {

    }

    @Override
    public ForgeConfigSpec.EnumValue<? extends Enum<?>> getConfigValue() {
        return this.value;
    }

    @Override
    public void undo() {

    }

    @Override
    public void reset() {

    }

    @Override
    public boolean isChanged() {
        return this.cache != this.value.get();
    }

    @Override
    public boolean isDefault() {
        return this.cache != this.spec.getDefault();
    }
}
