package cat.jiu.email.ui.gui.config.entry;

import net.minecraftforge.common.ForgeConfigSpec;

public class DoubleEntry extends NumberEntry<Double>{
    public DoubleEntry(ForgeConfigSpec.ConfigValue<Double> value, ForgeConfigSpec.ValueSpec spec) {
        super(value, spec, true);
    }

    @Override
    protected Double parse(String value) {
        return Double.parseDouble(value);
    }
}
