package cat.jiu.core.util.registry;

import cat.jiu.core.api.handler.IJsonSerializable;
import cat.jiu.core.api.handler.INBTSerializable;
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

public class DynamicRegistry<K, V extends IJsonSerializable & INBTSerializable & Supplier<K>> {
    public static <K, V> FailBack<K, JsonObject, V> jsonFailBack(ResourceLocation typeID) {
        return (id, data)->{
            LogManager.getLogger("Registry").fatal("{} is not register to {}.", id, typeID);
            return null;
        };
    }
    public static <K, V> FailBack<K, CompoundTag, V> nbtFailBack(ResourceLocation typeID) {
        return (id, data)->{
            LogManager.getLogger("Registry").fatal("{} is not register to {}.", id, typeID);
            return null;
        };
    }

    protected final ConcurrentHashMap<K, Getter<V>> registry = new ConcurrentHashMap<>();
    protected final FailBack<K, JsonObject, V> jsonFailBack;
    protected final FailBack<K, CompoundTag, V> nbtFailBack;
    protected Function<CompoundTag, K> nbtKeyGetter;
    protected Function<JsonObject, K> jsonKeyGetter;

    public DynamicRegistry(String modid, String typeName) {
        this(new ResourceLocation(modid, typeName));
    }
    public DynamicRegistry(ResourceLocation id) {
        this(jsonFailBack(id), nbtFailBack(id));
    }

    public DynamicRegistry(
            FailBack<K, JsonObject, V> jsonFailBack,
            FailBack<K, CompoundTag, V> nbtFailBack
    ) {
        this.jsonFailBack = jsonFailBack;
        this.nbtFailBack = nbtFailBack;
    }

    public void init(){}

    public DynamicRegistry<K, V> register(Consumer<DynamicRegistry<K, V>> register) {
        register.accept(this);
        return this;
    }

    public DynamicRegistry<K, V> setKeyGetter(Function<CompoundTag, K> nbtKeyGetter, Function<JsonObject, K> jsonKeyGetter) {
        this.nbtKeyGetter = nbtKeyGetter;
        this.jsonKeyGetter = jsonKeyGetter;
        return this;
    }

    /**
     *
     * @param clazz need class has an empty public constructor.
     */
    public K register(K id, Class<? extends V> clazz) {
        return this.register(id, nbt->{
            try {
                V instance = clazz.getDeclaredConstructor().newInstance();
                instance.read(nbt);
                return instance;
            }catch (Exception e){
                e.printStackTrace();
                return this.nbtFailBack.apply(id, nbt);
            }
        }, json->{
            try {
                V instance = clazz.getDeclaredConstructor().newInstance();
                instance.read(json);
                return instance;
            }catch (Exception e){
                e.printStackTrace();
                return this.jsonFailBack.apply(id, json);
            }
        });
    }

    public K register(K id, Function<CompoundTag, V> nbtGetter, Function<JsonObject, V> jsonGetter) {
        if (!this.registry.containsKey(id)) {
            this.registry.put(id, new Getter<>(nbtGetter, jsonGetter));
        }
        return id;
    }

    public void unregister(K id) {
        this.registry.remove(id);
    }

    public boolean registered(K id) {
        return this.registry.containsKey(id);
    }

    public V get(K id, CompoundTag tag) {
        if (this.registry.containsKey(id)) {
            return this.registry.get(id).nbtGetter.apply(tag);
        }
        return this.nbtFailBack.apply(id, tag);
    }

    public V get(K id, JsonObject json) {
        if (this.registry.containsKey(id)) {
            return this.registry.get(id).jsonGetter.apply(json);
        }
        return this.jsonFailBack.apply(id, json);
    }

    public V get(CompoundTag data) {
        if (this.nbtKeyGetter != null) {
            return this.get(this.nbtKeyGetter.apply(data), data);
        }
        return null;
    }
    public V get(JsonObject data) {
        if (this.jsonKeyGetter != null) {
            return this.get(this.jsonKeyGetter.apply(data), data);
        }
        return null;
    }

    public V get(K id) {
        return this.get(id, new JsonObject());
    }

    public Set<K> getIDs() {
        return Collections.unmodifiableSet(this.registry.keySet());
    }

    protected static class Getter<T extends IJsonSerializable & INBTSerializable> {
        protected final Function<CompoundTag, T> nbtGetter;
        protected final Function<JsonObject, T> jsonGetter;

        public Getter(Function<CompoundTag, T> nbtGetter, Function<JsonObject, T> jsonGetter) {
            this.nbtGetter = nbtGetter;
            this.jsonGetter = jsonGetter;
        }
    }

    public static interface FailBack<T1, T2, R> {
        R apply(T1 a, T2 b );
    }
}
