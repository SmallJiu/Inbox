package cat.jiu.email.ui.gui.config.entry;

import net.minecraftforge.common.ForgeConfigSpec;

public class FloatEntry extends NumberEntry<Float> {
    public FloatEntry(ForgeConfigSpec.ConfigValue<Float> value, ForgeConfigSpec.ValueSpec spec) {
        super(value, spec, true);
    }

    @Override
    protected Float parse(String value) {
        return Float.parseFloat(value);
    }
}
