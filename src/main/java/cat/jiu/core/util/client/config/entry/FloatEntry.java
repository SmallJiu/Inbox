package cat.jiu.core.util.client.config.entry;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * @author small_jiu
 */
public class FloatEntry extends NumberEntry<Float> {
    public FloatEntry(ForgeConfigSpec.ConfigValue<Float> value, ForgeConfigSpec.ValueSpec spec) {
        super(value, spec, true);
    }

    @Override
    protected Float parse(String value) {
        return Float.parseFloat(value);
    }
}
