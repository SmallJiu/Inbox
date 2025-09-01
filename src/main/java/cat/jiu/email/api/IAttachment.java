package cat.jiu.email.api;

import cat.jiu.core.api.IData;
import cat.jiu.core.api.serializable.IDataSerializable;
import cat.jiu.core.api.serializable.ISerializable;

import cat.jiu.core.util.Utils;
import cat.jiu.core.util.element.data.JsonData;
import cat.jiu.core.util.element.data.NBTData;
import cat.jiu.core.util.registry.DynamicRegistry2;
import cat.jiu.email.EmailMain;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.sql.SQLValues;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface IAttachment extends Consumer<Player>, IDataSerializable<IData.IMapData<?>>, Supplier<ResourceLocation> {
    Logger LOGGER = LogManager.getLogger("Inbox:Attachment");
    ResourceLocation EMPTY_ID = Utils.location(EmailMain.MODID, "attachment/empty");
    String ID_NAME = "id";
    IAttachment EMPTY = new IAttachment() {
        @Override
        public void read(IData.IMapData<?> data) {}
        @Override
        public IData.IMapData<?> write(IData.IMapData<?> data) {return data;}
        @Override
        public void accept(Player player) {}
        @Override
        public void merge(IAttachment other) {}
        @Override
        public ResourceLocation getID() {return EMPTY_ID;}
    };

    DynamicRegistry2<ResourceLocation, IAttachment> REGISTRY = new DynamicRegistry2<ResourceLocation, IAttachment>((id, data) -> {
        LOGGER.error("Attachment '{}' is not registered.", id);
        return EMPTY;
    }).setKeyGetter(
            data->data.getLocation(ID_NAME)
    );

    ResourceLocation getID();

    @Override
    default ResourceLocation get(){return this.getID();}

    void merge(IAttachment other);
    void accept(Player player);

    @OnlyIn(Dist.CLIENT)
    default void render(AttachmentEvent.Render event) {
        int icon_x = event.x + event.font.width(event.renderSaveTo(this.getDisplayName())) + 2;
        this.drawIcon(event, icon_x);

        if (event.canSee() && EmailUtils.isInRange(event.mouseX, event.mouseY, icon_x, event.getY(), 16, 16)) {
            List<Component> message = this.getHoverMessage();
            if (message != null && !message.isEmpty()) {
                event.disableScissor();
                event.graphics.renderComponentTooltip(event.font, message, event.mouseX, event.mouseY);
                event.enableScissor();
            }
        }
        event.addY(16);
    }
    @OnlyIn(Dist.CLIENT)
    default void getHeight(AttachmentEvent.GetHeight event) {
        if (!this.isEmpty()) {
            event.addHeight(16);
        }
    }
    @OnlyIn(Dist.CLIENT)
    default void drawIcon(AttachmentEvent.Render event, int x){
        event.graphics.renderFakeItem(this.getDisplayStack(), x, event.getY());
    }
    default Component getDisplayName() {
        return CommonComponents.EMPTY;
    }
    ItemStack STACK = new ItemStack(Items.ITEM_FRAME);
    default ItemStack getDisplayStack() {
        return STACK;
    }
    default List<Component> getHoverMessage(){
        return Collections.emptyList();
    }

    default boolean isEmpty() {
        return EMPTY_ID.equals(this.getID());
    }

    default String getName(){
        return String.valueOf(this.getID());
    }

    default <T extends IAttachment> T cast() {
        return (T) this;
    }

    default SQLValues write(SQLValues value) {
        value.put("data", "'" + this.write(JsonData.map()).getData() + "'");
        return value;
    }
    default void read(ResultSet result) throws SQLException {
        this.read(JsonData.map(JsonParser.parseString(result.getString("data")).getAsJsonObject()));
    }
    default JsonObject write(JsonObject data) {
        this.write(JsonData.map(data));
        return data;
    }
    default void read(JsonObject data) {
        this.read(JsonData.map(data));
    }
    default CompoundTag write(CompoundTag data) {
        this.write(NBTData.map(data));
        return data;
    }
    default void read(CompoundTag data) {
        this.read(NBTData.map(data));
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
        return IAttachment.REGISTRY.get(this.getID(), this.write(JsonData.map()));
    }

    abstract class SerializableAttachment implements IAttachment {
        protected ItemStack displayStack = IAttachment.STACK;
        protected Component displayName = CommonComponents.EMPTY;

        public SerializableAttachment() {}

        public SerializableAttachment(JsonObject json) {
            this.read(JsonData.map(json));
        }
        public SerializableAttachment(CompoundTag nbt) {
            this.read(NBTData.map(nbt));
        }
        public SerializableAttachment(IData.IMapData<?> data) {
            this.read(data);
        }
        public SerializableAttachment(Component displayName, ItemStack displayStack) {
            this.displayName = displayName;
            this.displayStack = displayStack;
        }

        @Override
        public ItemStack getDisplayStack() {
            return displayStack;
        }

        @Override
        public Component getDisplayName() {
            return displayName;
        }
    }
}
