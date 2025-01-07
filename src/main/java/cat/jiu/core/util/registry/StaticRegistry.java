package cat.jiu.core.util.registry;

import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
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
    protected Function<CompoundTag, K> nbtKeyGetter;
    protected Function<JsonObject, K> jsonKeyGetter;

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
    public StaticRegistry<K, V> setKeyGetter(Function<CompoundTag, K> nbtKeyGetter, Function<JsonObject, K> jsonKeyGetter) {
        this.nbtKeyGetter = nbtKeyGetter;
        this.jsonKeyGetter = jsonKeyGetter;
        return this;
    }

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
    public void unregisterAll() {
        this.registry.clear();
    }

    public V get(K id) {
        if (this.registry.containsKey(id)) {
            return this.registry.get(id);
        }
        return this.failBack.apply(id);
    }
    public V get(CompoundTag data) {
        if (this.nbtKeyGetter != null) {
            return this.get(this.nbtKeyGetter.apply(data));
        }
        return null;
    }
    public V get(JsonObject data) {
        if (this.jsonKeyGetter != null) {
            return this.get(this.jsonKeyGetter.apply(data));
        }
        return null;
    }

    public Set<K> getIDs() {
        return Collections.unmodifiableSet(this.registry.keySet());
    }
}
