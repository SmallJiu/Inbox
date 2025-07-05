package cat.jiu.email.element.attachment;

import cat.jiu.core.util.JsonUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.util.EmailUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.awt.*;
import java.util.*;
import java.util.List;

@Mod.EventBusSubscriber
public class AttachmentAttribute implements IAttachment {
    public static final ResourceLocation ID = new ResourceLocation(EmailMain.MODID, "attachment/attribute");

    protected HashMap<Attribute, EnumMap<AttributeModifier.Operation, List<AttributeValue>>> attributeMap = new HashMap<>();

    public AttachmentAttribute() {
    }

    public AttachmentAttribute(CompoundTag tag) {
        this.readFrom(tag);
    }

    public AttachmentAttribute(JsonObject json) {
        this.readFrom(json);
    }

    public List<AttributeValue> getValue(Attribute attribute, AttributeModifier.Operation operation) {
        this.check(attribute, operation);
        return this.attributeMap.get(attribute).get(operation);
    }

    public AttachmentAttribute addValue(Attribute attribute, AttributeModifier.Operation operation, AttributeValue value) {
        this.check(attribute, operation);
        this.attributeMap.get(attribute).get(operation).add(value);
        return this;
    }
    protected void check(Attribute attribute, AttributeModifier.Operation operation) {
        if (!this.attributeMap.containsKey(attribute)) {
            this.attributeMap.put(attribute, new EnumMap<>(AttributeModifier.Operation.class));
        }
        if (!this.attributeMap.get(attribute).containsKey(operation)) {
            this.attributeMap.get(attribute).put(operation, new ArrayList<>());
        }
    }

    @Override
    public JsonObject write(JsonObject json) {
        JsonArray attributeObject = new JsonArray();
        for (Map.Entry<Attribute, EnumMap<AttributeModifier.Operation, List<AttributeValue>>> entry : this.attributeMap.entrySet()) {
            JsonArray array = new JsonArray();
            for (Map.Entry<AttributeModifier.Operation, List<AttributeValue>> operationEntry : entry.getValue().entrySet()) {
                for (AttributeValue value : operationEntry.getValue()) {
                    JsonObject object = new JsonObject();
                    object.addProperty("operation", operationEntry.getKey().toValue());
                    object.addProperty("value", value.value);
                    object.addProperty("temp", value.temp);
                    array.add(object);
                }
            }
            JsonObject object = new JsonObject();
            object.addProperty("id", String.valueOf(ForgeRegistries.ATTRIBUTES.getKey(entry.getKey())));
            object.add("values", array);
            attributeObject.add(object);
        }
        json.add("attribute", attributeObject);
        return json;
    }

    @Override
    public void read(JsonObject json) {
        JsonArray attributes = json.getAsJsonArray("attribute");
        for (int attributeIndex = 0; attributeIndex < attributes.size(); attributeIndex++) {
            JsonObject attributeTag = attributes.get(attributeIndex).getAsJsonObject();
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(attributeTag.get("id").getAsString()));

            int globOperationID = JsonUtils.get(attributeTag, "operation", -1);
            AttributeModifier.Operation globOperation = globOperationID >= 0 ? AttributeModifier.Operation.fromValue(globOperationID) : null;

            boolean useGlobTemp = attributeTag.has("temp");

            JsonArray values = attributeTag.getAsJsonArray("values");
            for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
                JsonObject valueTag = values.get(valueIndex).getAsJsonObject();
                this.addValue(attribute,
                        globOperation != null ? globOperation : AttributeModifier.Operation.fromValue(JsonUtils.get(valueTag, "operation", AttributeModifier.Operation.ADDITION.toValue())),
                        new AttributeValue(valueTag.get("value").getAsDouble(), useGlobTemp ? attributeTag.get("temp").getAsBoolean() : JsonUtils.get(valueTag, "temp", true))
                );
            }
        }
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {
        ListTag attributeObject = new ListTag();
        for (Map.Entry<Attribute, EnumMap<AttributeModifier.Operation, List<AttributeValue>>> entry : this.attributeMap.entrySet()) {
            ListTag array = new ListTag();
            for (Map.Entry<AttributeModifier.Operation, List<AttributeValue>> operationEntry : entry.getValue().entrySet()) {
                for (AttributeValue value : operationEntry.getValue()) {
                    CompoundTag object = new CompoundTag();
                    object.putInt("operation", operationEntry.getKey().toValue());
                    object.putDouble("value", value.value);
                    object.putBoolean("temp", value.temp);
                    array.add(object);
                }
            }
            CompoundTag object = new CompoundTag();
            object.putString("id", String.valueOf(ForgeRegistries.ATTRIBUTES.getKey(entry.getKey())));
            object.put("values", array);

            attributeObject.add(object);
        }
        nbt.put("attribute", attributeObject);
        return nbt;
    }

    @Override
    public void read(CompoundTag nbt) {
        ListTag attributes = nbt.getList("attribute", 10);
        for (int attributeIndex = 0; attributeIndex < attributes.size(); attributeIndex++) {
            CompoundTag attributeTag = attributes.getCompound(attributeIndex);

            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(attributeTag.getString("id")));
            ListTag values = attributeTag.getList("values", 10);
            for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
                CompoundTag valueTag = values.getCompound(valueIndex);
                this.addValue(
                        attribute, AttributeModifier.Operation.fromValue(valueTag.getInt("operation")),
                        new AttributeValue(valueTag.getDouble("value"), valueTag.getBoolean("temp"))
                );
            }
        }
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentAttribute attachment && !attachment.isEmpty()) {
            AttachmentAttribute attribute = other.cast();
            for (Map.Entry<Attribute, EnumMap<AttributeModifier.Operation, List<AttributeValue>>> attributeEntry : attribute.attributeMap.entrySet()) {
                for (Map.Entry<AttributeModifier.Operation, List<AttributeValue>> operationEntry : attributeEntry.getValue().entrySet()) {
                    operationEntry.getValue().forEach(e->this.addValue(
                            attributeEntry.getKey(), operationEntry.getKey(), e
                    ));
                }
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return this.attributeMap.isEmpty();
    }

    @Override
    public String getName() {
        return "attribute";
    }

    @Override
    public void accept(Player player) {
        if (!player.level().isClientSide()) {
            for (Map.Entry<Attribute, EnumMap<AttributeModifier.Operation, List<AttributeValue>>> attributeEntry : this.attributeMap.entrySet()) {
                for (Map.Entry<AttributeModifier.Operation, List<AttributeValue>> operationEntry : attributeEntry.getValue().entrySet()) {
                    for (AttributeValue value : operationEntry.getValue()) {
                        accept(player, attributeEntry.getKey(), operationEntry.getKey(), value);
                    }
                }
            }
        }
    }

    public static void accept(Player player, Attribute attribute, AttributeModifier.Operation operation, AttributeValue value) {
        AttributeInstance instance = Objects.requireNonNull(player.getAttribute(attribute), String.format("not found attribute: %s", attribute));
        AttributeState.Id id = AttributeState.id(attribute, operation, value.temp);
        if (value.temp) {
            instance.removeModifier(id.uid);
            instance.addPermanentModifier(new AttributeModifier(id.uid, id.id, value.value, operation));
        }else {
            double v = AttributeState.get(player.getServer()).add(player.getStringUUID(), attribute, operation, value.value);
            instance.removeModifier(id.uid);
            instance.addPermanentModifier(new AttributeModifier(id.uid, id.id, v, operation));
        }
    }

    public static void loadAttribute(Player player) {
        AttributeState state = AttributeState.get(player.getServer());
        for (Attribute attribute : state.getEffectAttribute(player.getStringUUID())) {
            AttributeInstance instance = Objects.requireNonNull(player.getAttribute(attribute), String.format("not found attribute: %s", attribute));
            for (AttributeModifier.Operation operation : state.getEffectOperation(player.getStringUUID(), attribute)) {
                double value = state.get(player.getStringUUID(), attribute, operation);
                if (value > 0) {
                    AttributeState.Id id = AttributeState.id(attribute, operation, false);
                    instance.removeModifier(id.uid);
                    instance.addPermanentModifier(new AttributeModifier(id.uid, id.id, value, operation));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event){
        if (!event.getEntity().level().isClientSide() && !event.isWasDeath()) {
            loadAttribute(event.getEntity());
        }
    }
    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinLevelEvent event){
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof Player) {
            loadAttribute((Player) event.getEntity());
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(AttachmentEvent.Render event) {
        if (!this.isEmpty()) {
            event.graphics.drawString(event.font, Component.nullToEmpty(null), event.x, event.getY(), Color.WHITE.getRGB());

            int x = event.x + event.font.width(event.renderSaveTo("info.inbox.attribute")) + 2;
            event.graphics.setColor(1.0F, 1.0F, 1.0F, 1);
            event.graphics.blit(x, event.getY(), 0, 16, 16, AttachmentMaxHealth.Icon.getLuckIcon());
            event.graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (event.canSee() && EmailUtils.isInRange(event.mouseX, event.mouseY, x, event.getY(), 16, 16)) {
                event.disableScissor();

                List<Component> tooltips = new ArrayList<>();
                for (Map.Entry<Attribute, EnumMap<AttributeModifier.Operation, List<AttributeValue>>> attributeEntry : this.attributeMap.entrySet()) {
                    for (Map.Entry<AttributeModifier.Operation, List<AttributeValue>> operationEntry : attributeEntry.getValue().entrySet()) {
                        for (AttributeValue value : operationEntry.getValue()) {
                            tooltips.add(
                                    Component.translatable(attributeEntry.getKey().getDescriptionId())
                                            .append(CommonComponents.SPACE)
                                            .append(getMethod(operationEntry.getKey(), value.value))
                                            .append(value.temp ? Component.translatable("info.inbox.healths.temp") : CommonComponents.EMPTY)
                            );
                        }
                    }
                }
                event.graphics.renderComponentTooltip(event.font, tooltips, event.mouseX, event.mouseY);

                event.enableScissor();
            }
            event.addY(event.font.lineHeight + 2);
        }
    }

    public static String getMethod(AttributeModifier.Operation operation, double value) {
        switch (operation) {
            case ADDITION -> {
                return String.format("+%s", value);
            }
            case MULTIPLY_BASE -> {
                return String.format("+%sx", value);
            }
            case MULTIPLY_TOTAL -> {
                return String.format("x%s", value);
            }
        }
        return String.valueOf(value);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void getHeight(AttachmentEvent.GetHeight event) {
        if (!this.isEmpty()) {
            event.addHeight(16);
        }
    }

    public static class AttributeValue {
        public double value;
        /**
         * 如果此附件是一次性的, 则为true (true if the max health is disposable)
         */
        public boolean temp;

        public AttributeValue(double value, boolean temp) {
            this.value = value;
            this.temp = temp;
        }
    }

    public static class AttributeState extends SavedData {
        public static final String PATH = EmailMain.MODID + "_attribute";

        public final HashMap<String, HashMap<Attribute, EnumMap<AttributeModifier.Operation, Double>>> map;
        public AttributeState() {
            map = new HashMap<>();
        }

        public Set<Attribute> getEffectAttribute(String uid){
            if (map.containsKey(uid)) {
                return map.get(uid).keySet();
            }
            return Collections.emptySet();
        }

        public Set<AttributeModifier.Operation> getEffectOperation(String uid, Attribute attribute){
            if (map.containsKey(uid) && map.get(uid).containsKey(attribute)) {
                return map.get(uid).get(attribute).keySet();
            }
            return Collections.emptySet();
        }

        public double get(String uid, Attribute attribute, AttributeModifier.Operation operation) {
            if (map.containsKey(uid) && map.get(uid).containsKey(attribute) && map.get(uid).get(attribute).containsKey(operation)) {
                return map.get(uid).get(attribute).get(operation);
            }
            return 0;
        }

        public double add(String uid, Attribute attribute, AttributeModifier.Operation operation, double value) {
            if (!map.containsKey(uid)) {
                map.put(uid, new HashMap<>());
            }
            if (!map.get(uid).containsKey(attribute)) {
                map.get(uid).put(attribute, new EnumMap<>(AttributeModifier.Operation.class));
            }
            if (map.get(uid).get(attribute).containsKey(operation)) {
                map.get(uid).get(attribute).put(operation, get(uid, attribute, operation)+value);
            }else {
                map.get(uid).get(attribute).put(operation, value);
            }
            setDirty();
            return get(uid, attribute, operation);
        }

        public double sub(String uid, Attribute attribute, AttributeModifier.Operation operation, double value) {
            if (!map.containsKey(uid)) {
                map.put(uid, new HashMap<>());
            }
            if (!map.get(uid).containsKey(attribute)) {
                map.get(uid).put(attribute, new EnumMap<>(AttributeModifier.Operation.class));
            }
            if (map.get(uid).containsKey(attribute)) {
                map.get(uid).get(attribute).put(operation, get(uid, attribute, operation)+value);
            }
            if (get(uid, attribute, operation)<0) {
                map.get(uid).get(attribute).put(operation, 0d);
            }
            setDirty();
            return get(uid, attribute, operation);
        }

        @Override
        public CompoundTag save(CompoundTag nbt) {
            for (String uid : map.keySet()) {
                ListTag list = new ListTag();
                for (Attribute attribute : getEffectAttribute(uid)) {
                    for (AttributeModifier.Operation operation : getEffectOperation(uid, attribute)) {
                        CompoundTag tag = new CompoundTag();
                        tag.putString("attribute", String.valueOf(ForgeRegistries.ATTRIBUTES.getKey(attribute)));
                        tag.putInt("operation", operation.toValue());
                        tag.putDouble("value", get(uid, attribute, operation));
                        list.add(tag);
                    }
                }
                nbt.put(uid, list);
            }
            return nbt;
        }

        public static AttributeState fromNbt(CompoundTag nbt) {
            AttributeState state = new AttributeState();
            for (String s : nbt.getAllKeys()) {
                ListTag list = nbt.getList(s, 10);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag tag = list.getCompound(i);
                    state.add(s,
                            ForgeRegistries.ATTRIBUTES.getValue(ResourceLocation.tryParse(tag.getString("attribute"))),
                            AttributeModifier.Operation.fromValue(tag.getInt("operation")),
                            tag.getDouble("value")
                    );
                }
            }
            return state;
        }

        public static AttributeState get(MinecraftServer server) {
            DimensionDataStorage manager = Objects.requireNonNull(server, "not found server").overworld().getDataStorage();
            return manager.computeIfAbsent(AttributeState::fromNbt, AttributeState::new, PATH);
        }
        public static Id id(Attribute attribute, AttributeModifier.Operation operation, boolean temp) {
            if (!Id.map.containsKey(attribute)) {
                Id.map.put(attribute, new EnumMap<>(AttributeModifier.Operation.class));
            }
            if (!Id.map.get(attribute).containsKey(operation)) {
                Id.map.get(attribute).put(operation, new HashMap<>());
            }
            if (!Id.map.get(attribute).get(operation).containsKey(temp)) {
                Id.map.get(attribute).get(operation).put(temp, new Id(
                        UUID.nameUUIDFromBytes(String.format("%s.%s%s", ForgeRegistries.ATTRIBUTES.getKey(attribute), operation.toValue(), temp ? ".temp" : "").getBytes()),
                        String.format("%s/%s/%s%s", ID, ForgeRegistries.ATTRIBUTES.getKey(attribute), operation.toValue(), temp ? ".temp" : "")
                ));
            }
            return Id.map.get(attribute).get(operation).get(temp);
        }
        public static class Id {
            public static final HashMap<Attribute, EnumMap<AttributeModifier.Operation, HashMap<Boolean, Id>>> map = new HashMap<>();
            public final UUID uid;
            public final String id;
            public Id(UUID uid, String id) {
                this.uid = uid;
                this.id = id;
            }
        }
    }
}
