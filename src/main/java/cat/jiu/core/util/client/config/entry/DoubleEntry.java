package cat.jiu.core.util.client.config.entry;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * @author small_jiu
 */
public class DoubleEntry extends NumberEntry<Double>{
    public DoubleEntry(ForgeConfigSpec.ConfigValue<Double> value, ForgeConfigSpec.ValueSpec spec) {
        super(value, spec, true);
    }

    @Override
    protected Double parse(String value) {
        return Double.parseDouble(value);
    }
}
