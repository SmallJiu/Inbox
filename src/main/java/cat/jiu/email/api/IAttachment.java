package cat.jiu.email.api;

import cat.jiu.core.api.handler.ISerializable;

import cat.jiu.core.util.registry.DynamicRegistry;
import cat.jiu.email.EmailMain;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.util.JsonParser;
import cat.jiu.sql.SQLValues;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IAttachment extends Consumer<PlayerEntity>, ISerializable, Supplier<ResourceLocation> {
    Logger LOGGER = LogManager.getLogger("Inbox:Attachment");
    ResourceLocation EMPTY_ID = new ResourceLocation(EmailMain.MODID, "attachment/empty");
    IAttachment EMPTY = new IAttachment() {
        @Override
        public CompoundNBT write(CompoundNBT nbt) {return nbt;}
        @Override
        public void read(CompoundNBT nbt) {}
        @Override
        public JsonObject write(JsonObject json) {return json;}
        @Override
        public void read(JsonObject json) {}
        @Override
        public void accept(PlayerEntity player) {}
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
    void accept(PlayerEntity player);

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
        this.readFrom(JsonParser.parser.parse(result.getString("data")));
    }

    static <T extends SerializableAttachment> void register(ResourceLocation id, Class<T> clazz) {
        IAttachment.REGISTRY.register(id, clazz);
    }


    default IAttachment copy() {
        return IAttachment.REGISTRY.get(this.getID(), this.writeTo(JsonObject.class));
    }

    abstract class SerializableAttachment implements IAttachment {
        public SerializableAttachment() {}

        public SerializableAttachment(JsonObject json) {
            this.readFrom(json);
        }

        public SerializableAttachment(CompoundNBT nbt) {
            this.readFrom(nbt);
        }
    }
}
