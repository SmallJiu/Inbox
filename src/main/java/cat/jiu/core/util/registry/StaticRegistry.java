package cat.jiu.core.util.registry;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class StaticRegistry<K, V extends Supplier<K>> {
    public static <K, T> Function<K, T>  failBack(ResourceLocation typeID) {
        return id -> {
            LogManager.getLogger("Registry").fatal("{} is not register to {}. ", id, typeID);
            return null;
        };
    }

    protected final ConcurrentHashMap<K, V> registry = new ConcurrentHashMap<>();
    protected final Function<K, V> failBack;

    public StaticRegistry(ResourceLocation id) {
        this(failBack(id));
    }
    public StaticRegistry(String modid, String typeName) {
        this(new ResourceLocation(modid, typeName));
    }

    public StaticRegistry(Function<K, V> failBack) {
        this.failBack = failBack;
    }

    public void init(){}

    public StaticRegistry<K, V> register(Consumer<StaticRegistry<K, V>> register) {
        register.accept(this);
        return this;
    }
    public K register(V instance) {
        return this.register(instance.get(), instance);
    }
    public K register(K id, V instance) {
        if (!this.registry.containsKey(id)) {
            this.registry.put(id, instance);
        }
        return id;
    }

    public boolean registered(K id) {
        return this.registry.containsKey(id);
    }

    public V unregister(K id) {
        return this.registry.remove(id);
    }

    public V get(K id) {
        if (this.registry.containsKey(id)) {
            return this.registry.get(id);
        }
        return this.failBack.apply(id);
    }

    public Set<K> getIDs() {
        return Collections.unmodifiableSet(this.registry.keySet());
    }
}
