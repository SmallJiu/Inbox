package cat.jiu.email.ui.gui.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.Event;

public class ConfigWriteEvent extends Event {
    public final String file;
    public final ForgeConfigSpec spec;
    public ConfigWriteEvent(String file, ForgeConfigSpec spec) {
        this.file = file;
        this.spec = spec;
    }
}
