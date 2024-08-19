package cat.jiu.email.api;

import cat.jiu.core.api.handler.ISerializable;

import cat.jiu.email.EmailMain;
import cat.jiu.email.util.JsonParser;
import cat.jiu.sql.SQLValues;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public interface IAttachment extends Consumer<Player>, ISerializable {
    Logger LOGGER = LogManager.getLogger("Inbox:Attachment");
    ResourceLocation EMPTY_ID = new ResourceLocation(EmailMain.MODID, "attachment/empty");
    IAttachment EMPTY = new IAttachment() {
        @Override
        public CompoundTag write(CompoundTag nbt) {return nbt;}
        @Override
        public void read(CompoundTag nbt) {}
        @Override
        public JsonObject write(JsonObject json) {return json;}
        @Override
        public void read(JsonObject json) {}
        @Override
        public void accept(Player player) {}
        @Override
        public void merge(IAttachment other) {}
        @Override
        public ResourceLocation getID() {return EMPTY_ID;}
    };

    ResourceLocation getID();
    void merge(IAttachment other);
    void accept(Player player);

    default boolean isEmpty() {
        return EMPTY_ID.equals(this.getID());
    }

    default String getName(){
        return String.valueOf(this.getID());
    }

    default <T extends IAttachment> T cast() {
        return (T) this;
    }

    @Override
    default SQLValues write(SQLValues value) {
        value.put("data", "'" + this.writeTo(JsonObject.class).toString() + "'");
        return value;
    }
    @Override
    default void read(ResultSet result) throws SQLException {
        this.readFrom(JsonParser.parser.parse(result.getString("data")));
    }

    static <T extends SerializableAttachment> void register(ResourceLocation id, Class<T> clazz) {
        register(id, nbt->{
            try {
                return clazz.getDeclaredConstructor(CompoundTag.class).newInstance(nbt);
            }catch (Exception e){
                e.printStackTrace();
                return EMPTY;
            }
        }, json->{
            try {
                return clazz.getDeclaredConstructor(JsonObject.class).newInstance(json);
            }catch (Exception e){
                e.printStackTrace();
                return EMPTY;
            }
        });
    }

    static void register(ResourceLocation id, Function<CompoundTag, IAttachment> nbtGetter, Function<JsonObject, IAttachment> jsonGetter) {
        if (!Registry.REGISTRY.containsKey(id)) {
            Registry.REGISTRY.put(id, new Registry.Getter(nbtGetter, jsonGetter));
        }
    }

    static IAttachment newInstance(ResourceLocation id, CompoundTag tag) {
        if (Registry.REGISTRY.containsKey(id)) {
            return Registry.REGISTRY.get(id).nbtGetter.apply(tag);
        }
        LOGGER.error("Attachment '{}' is unregistered.", id);
        return EMPTY;
    }
    static IAttachment newInstance(ResourceLocation id, JsonObject json) {
        if (Registry.REGISTRY.containsKey(id)) {
            return Registry.REGISTRY.get(id).jsonGetter.apply(json);
        }
        LOGGER.error("Attachment '{}' is unregistered.", id);
        return EMPTY;
    }

    static Set<ResourceLocation> getAllAttachmentID() {
        return Collections.unmodifiableSet(Registry.REGISTRY.keySet());
    }

    default IAttachment copy() {
        return newInstance(this.getID(), this.writeTo(JsonObject.class));
    }

    class Registry {
        static final ConcurrentHashMap<ResourceLocation, Getter> REGISTRY = new ConcurrentHashMap<>();

        static class Getter {
            final Function<CompoundTag, IAttachment> nbtGetter;
            final Function<JsonObject, IAttachment> jsonGetter;

            public Getter(Function<CompoundTag, IAttachment> nbtGetter, Function<JsonObject, IAttachment> jsonGetter) {
                this.nbtGetter = nbtGetter;
                this.jsonGetter = jsonGetter;
            }
        }
    }

    abstract class SerializableAttachment implements IAttachment {
        public SerializableAttachment() {}

        public SerializableAttachment(JsonObject json) {
            this.readFrom(json);
        }

        public SerializableAttachment(CompoundTag nbt) {
            this.readFrom(nbt);
        }
    }
}
