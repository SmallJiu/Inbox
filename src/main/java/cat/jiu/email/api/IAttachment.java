package cat.jiu.email.api;

import cat.jiu.core.api.IData;
import cat.jiu.core.api.serializable.IDataSerializable;

import cat.jiu.core.util.Utils;
import cat.jiu.core.util.client.RenderUtils;
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
import net.minecraft.network.FriendlyByteBuf;
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

public interface IAttachment extends IDataSerializable<IData.IMapData<?>>, Consumer<Player>, Supplier<ResourceLocation> {
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

    DynamicRegistry2<ResourceLocation, IAttachment> REGISTRY = new DynamicRegistry2<ResourceLocation, IAttachment>(EmailMain.MODID, "attachment")
            .setKeyGetter(data -> data.getLocation(ID_NAME))
    ;

    ResourceLocation getID();

    /**
     * @return attachment id
     */
    @Override
    default ResourceLocation get(){return this.getID();}

    /**
     * 合并附件<p>
     * merge attachment
     */
    void merge(IAttachment other);

    /**
     * 玩家领取附件<p>
     * Player receive attachment
     */
    void accept(Player player);

    /**
     * 用于检查此附件是否可以在玩家发送时添加到邮件<p>
     * Used to check if this attachment can be added to the message when the player sends it
     */
    default boolean onPlayerSendCheck(Player player, Consumer<Component> msgHandler) {
        return true;
    }
    /**
     * 检查完成的后处理<p>
     * Check the finished post-processing
     */
    default void onPlayerSendChecked(Player player) {

    }

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

    @OnlyIn(Dist.CLIENT)
    default boolean onClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default Component getDisplayName() {
        return Component.translatable(this.getName());
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

    @SuppressWarnings("unchecked")
    default <T extends IAttachment> T cast() {
        return (T) this;
    }

    /**
     * 如果有数据为发送时的检查使用，则重写<p>
     * If there have data for the check to use at the time of sending, it can rewritten
     * @see cat.jiu.email.net.msg.MsgSend#toBytes(FriendlyByteBuf)
     * @see cat.jiu.email.element.Email#setNetworkSend(boolean)
     * @see #onPlayerSendCheck(Player, Consumer)
     * @see #onPlayerSendChecked(Player)
     * @see cat.jiu.email.element.attachment.AttachmentItem#write(IData.IMapData)
     * @see cat.jiu.email.element.attachment.AttachmentItem#onPlayerSendCheck(Player, Consumer)
     * @see cat.jiu.email.element.attachment.AttachmentItem#onPlayerSendChecked(Player)
     */
    default IAttachment setNetworkSend(boolean isNetworkSend) {
        return this;
    }
    default boolean isNetworkSend(){
        return false;
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
        public SerializableAttachment(IData.IMapData<?> data) {
            this.read(data);
        }

        protected SerializableAttachment setDisplay(Component name, ItemStack stack) {
            this.displayName = name;
            this.displayStack = stack;
            return this;
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

    abstract class ClickIconAttachment extends SerializableAttachment {
        private int iconX, iconY;
        protected int iconWidth = 16, iconHeight = 16;

        public ClickIconAttachment() {
        }
        public ClickIconAttachment(IData.IMapData<?> data) {
            super(data);
        }

        public ClickIconAttachment setIconSize(int iconWidth, int iconHeight) {
            this.iconWidth = iconWidth;
            this.iconHeight = iconHeight;
            return this;
        }

        @OnlyIn(Dist.CLIENT)
        @Override
        public void drawIcon(AttachmentEvent.Render event, int x) {
            this.iconX = x;
            this.iconY = event.getY();
            super.drawIcon(event, x);
        }

        @OnlyIn(Dist.CLIENT)
        @Override
        public boolean onClicked(double mouseX, double mouseY, int button) {
            if (button == 0
                    && RenderUtils.inRange(mouseX, mouseY, this.iconX, this.iconY, this.iconWidth, this.iconHeight)) {
                this.onIconClicked(mouseX, mouseY, button);
                return true;
            }
            return false;
        }
        @OnlyIn(Dist.CLIENT)
        protected void onIconClicked(double mouseX, double mouseY, int button){

        }
    }
}
