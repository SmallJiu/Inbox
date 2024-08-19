package cat.jiu.core.util.client.config.entry;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * @author small_jiu
 */
public class LongEntry extends NumberEntry<Long> {
    public LongEntry(ForgeConfigSpec.ConfigValue<Long> value, ForgeConfigSpec.ValueSpec spec) {
        super(value, spec, false);
    }

    @Override
    protected Long parse(String value) {
        return Long.parseLong(value);
    }
}
