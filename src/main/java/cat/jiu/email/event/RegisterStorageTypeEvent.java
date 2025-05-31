package cat.jiu.email.event;

import cat.jiu.core.util.registry.StaticRegistry;
import cat.jiu.email.element.StorageType;
import net.minecraftforge.eventbus.api.Event;

public class RegisterStorageTypeEvent extends Event {
    public final StaticRegistry<String, StorageType> registry;

    public RegisterStorageTypeEvent(StaticRegistry<String, StorageType> registry) {
        this.registry = registry;
    }
    public void register(StorageType type) {
        this.registry.register(type);
    }
}
