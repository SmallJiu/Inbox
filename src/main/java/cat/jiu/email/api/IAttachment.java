package cat.jiu.email.api;

import cat.jiu.core.api.serializable.ISerializable;

import cat.jiu.core.util.registry.DynamicRegistry;
import cat.jiu.email.EmailMain;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.sql.SQLValues;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IAttachment extends Consumer<Player>, ISerializable, Supplier<ResourceLocation> {
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

    DynamicRegistry<ResourceLocation, IAttachment> REGISTRY = new DynamicRegistry<>((id, json) -> {
        LOGGER.error("Attachment '{}' is not registered.", id);
        return EMPTY;
    }, (id, nbt) -> {
        LOGGER.error("Attachment '{}' is not registered.", id);
        return EMPTY;
    });

    ResourceLocation getID();

    @Override
    default ResourceLocation get(){return this.getID();}

    void merge(IAttachment other);
    void accept(Player player);

    @OnlyIn(Dist.CLIENT)
    default void render(AttachmentEvent.Render event) {}
    @OnlyIn(Dist.CLIENT)
    default void getHeight(AttachmentEvent.GetHeight event) {}

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
        this.readFrom(JsonParser.parseString(result.getString("data")));
    }

    static <T extends SerializableAttachment> void register(ResourceLocation id, Class<T> clazz) {
        IAttachment.REGISTRY.register(id, clazz);
    }

    @Deprecated
    static IAttachment newInstance(ResourceLocation id, CompoundTag tag) {
        return IAttachment.REGISTRY.get(id, tag);
    }
    @Deprecated
    static IAttachment newInstance(ResourceLocation id, JsonObject json) {
        return IAttachment.REGISTRY.get(id, json);
    }
    @Deprecated
    static Set<ResourceLocation> getAllAttachmentID() {
        return IAttachment.REGISTRY.getIDs();
    }

    default IAttachment copy() {
        return IAttachment.REGISTRY.get(this.getID(), this.writeTo(JsonObject.class));
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
