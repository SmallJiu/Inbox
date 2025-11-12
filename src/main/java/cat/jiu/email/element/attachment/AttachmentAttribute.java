package cat.jiu.email.element.attachment;

import cat.jiu.core.api.IData;
import cat.jiu.core.util.Utils;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.AttachmentSendScreenWidget;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.element.Email;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.ui.gui.component.GuiCheckbox;
import cat.jiu.email.ui.gui.component.GuiFilterTextField;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
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

import java.io.File;
import java.util.*;
import java.util.List;

@Mod.EventBusSubscriber
public class AttachmentAttribute implements IAttachment {
    public static final ResourceLocation ID = Utils.location(EmailMain.MODID, "attachment/attribute");

    protected HashMap<Attribute, EnumMap<AttributeModifier.Operation, List<AttributeValue>>> attributeMap = new HashMap<>();

    public AttachmentAttribute() {
    }

    public AttachmentAttribute(CompoundTag tag) {
        this.read(tag);
    }

    public AttachmentAttribute(JsonObject json) {
        this.read(json);
    }
    public AttachmentAttribute(IData.IMapData<?> data) {
        this.read(data);
    }

    public List<AttributeValue> getValue(Attribute attribute, AttributeModifier.Operation operation) {
        this.check(attribute, operation);
        return this.attributeMap.get(attribute).get(operation);
    }

    public AttachmentAttribute addValue(Attribute attribute, AttributeModifier.Operation operation, AttributeValue value) {
        if (attribute != null){
            this.check(attribute, operation);
            this.attributeMap.get(attribute).get(operation).add(value);
        }
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
    public IData.IMapData<?> write(IData.IMapData<?> data) {
        IData.IListData<?> attributeObject = data.newList();
        for (Map.Entry<Attribute, EnumMap<AttributeModifier.Operation, List<AttributeValue>>> entry : this.attributeMap.entrySet()) {
            IData.IListData<?> array = data.newList();
            for (Map.Entry<AttributeModifier.Operation, List<AttributeValue>> operationEntry : entry.getValue().entrySet()) {
                for (AttributeValue value : operationEntry.getValue()) {
                    IData.IMapData<?> object = data.newMap();
                    object.putData("operation", operationEntry.getKey().toValue());
                    object.putData("value", value.value);
                    object.putData("temp", value.temp);
                    array.putData(object);
                }
            }
            IData.IMapData<?> object = data.newMap();
            object.putData("id", String.valueOf(ForgeRegistries.ATTRIBUTES.getKey(entry.getKey())));
            object.putData("values", array);
            attributeObject.putData(object);
        }
        data.putData("attribute", attributeObject);
        return data;
    }

    @Override
    public void read(IData.IMapData<?> data) {
        data.getList("attribute", IData.IMapData.class).foreach((index, value) -> {
            IData.IMapData<?> attributeTag = value.getAsMap();
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(attributeTag.getLocation("id"));
            int globOperationID = attributeTag.getInt("operation", -1);
            AttributeModifier.Operation globOperation = globOperationID >= 0 ? AttributeModifier.Operation.fromValue(globOperationID) : null;

            boolean useGlobTemp = attributeTag.containsKey("temp");
            attributeTag.getList("values", IData.IMapData.class).foreach((i, v)->{
                IData.IMapData<?> valueTag = v.getAsMap();
                this.addValue(attribute,
                        globOperation != null ? globOperation : AttributeModifier.Operation.fromValue(valueTag.getInt("operation", AttributeModifier.Operation.ADDITION.toValue())),
                        new AttributeValue(valueTag.getDouble("value"), useGlobTemp ? attributeTag.getBoolean("temp") : valueTag.getBoolean("temp", true))
                );
            });
        });
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
    public void accept(Player player) {
        if (!player.level().isClientSide()) {
            for (Map.Entry<Attribute, EnumMap<AttributeModifier.Operation, List<AttributeValue>>> attributeEntry : this.attributeMap.entrySet()) {
                for (Map.Entry<AttributeModifier.Operation, List<AttributeValue>> operationEntry : attributeEntry.getValue().entrySet()) {
                    for (AttributeValue value : operationEntry.getValue()) {
                        try {
                            accept(player, attributeEntry.getKey(), operationEntry.getKey(), value);
                        } catch (Exception e) {
                            Email.sendReceiveErrorMessage(player, this, e);
                        }
                    }
                }
            }
        }
    }

    public static void accept(LivingEntity entity, Attribute attribute, AttributeModifier.Operation operation, AttributeValue value) {
        AttributeInstance instance = Objects.requireNonNull(entity.getAttribute(attribute), String.format("not found player attribute: %s", ForgeRegistries.ATTRIBUTES.getKey(attribute)));
        AttributeState.Id id = AttributeState.id(attribute, operation, value.temp);
        instance.removeModifier(id.uid);
        if (value.temp) {
            instance.addPermanentModifier(new AttributeModifier(id.uid, id.id, value.value, operation));
        }else {
            double v = AttributeState.get(entity.getServer()).add(entity.getStringUUID(), attribute, operation, value.value);
            instance.addPermanentModifier(new AttributeModifier(id.uid, id.id, v, operation));
        }
    }

    public static void loadAttribute(LivingEntity entity) {
        AttributeState state = AttributeState.get(entity.getServer());
        for (Attribute attribute : state.getEffectAttribute(entity.getStringUUID())) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            for (AttributeModifier.Operation operation : state.getEffectOperation(entity.getStringUUID(), attribute)) {
                double value = state.get(entity.getStringUUID(), attribute, operation);
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
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof LivingEntity) {
            loadAttribute((LivingEntity) event.getEntity());
        }
    }

    @Override
    public List<Component> getHoverMessage() {
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
        return tooltips;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawIcon(AttachmentEvent.Render event, int x) {
        event.graphics.setColor(1.0F, 1.0F, 1.0F, 1);
        event.graphics.blit(x, event.getY(), 0, 16, 16, AttachmentMaxHealth.Icon.getLuckIcon());
        event.graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("info.inbox.attribute");
    }

    @Override
    public String getName() {
        return "info.inbox.attribute";
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
         * 如果此附件是一次性的, 则为true (true if the attribute on next respawn remove)
         */
        public boolean temp;

        /**
         * @param temp 如果此附件是一次性的, 则为true (true if the attribute on next respawn remove)
         */
        public AttributeValue(double value, boolean temp) {
            this.value = value;
            this.temp = temp;
        }
    }

    public static class AttributeState extends SavedData {
        public static final String PATH = EmailMain.MODID + "/attribute";

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

        @Override
        public void save(File pFile) {
            pFile.getParentFile().mkdirs();
            super.save(pFile);
        }

        public static AttributeState fromNbt(CompoundTag nbt) {
            AttributeState state = new AttributeState();
            for (String s : nbt.getAllKeys()) {
                ListTag list = nbt.getList(s, 10);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag tag = list.getCompound(i);
                    state.add(s,
                            ForgeRegistries.ATTRIBUTES.getValue(Utils.location(tag.getString("attribute"))),
                            AttributeModifier.Operation.fromValue(tag.getInt("operation")),
                            tag.getDouble("value")
                    );
                }
            }
            return state;
        }

        public static AttributeState get(MinecraftServer server) {
            DimensionDataStorage manager = Objects.requireNonNull(server, "not found server").overworld().getDataStorage();

            String oldPath = EmailMain.MODID + "_attribute";
            File oldFile = new File(manager.dataFolder, oldPath + ".dat");
            if (oldFile.exists()) {
                AttributeState state = manager.computeIfAbsent(AttributeState::fromNbt, AttributeState::new, oldPath);
                manager.cache.remove(oldPath);
                manager.cache.put(PATH, state);
                oldFile.delete();
                return state;
            }
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

    @OnlyIn(Dist.CLIENT)
    public static class Widget extends AttachmentSendScreenWidget {
        public static final WidgetEntry INSTANCE = new WidgetEntry(ID, false, Widget::new);
        public static final List<ResourceLocation> ATTRIBUTES = Collections.unmodifiableList(new ArrayList<>(ForgeRegistries.ATTRIBUTES.getKeys()));

        public final SubWidget attributes;
        public final Button add;
        private Widget() {
            super(Component.translatable("info.inbox.attribute"));
            this.initSubWiget(false);

            this.attributes = this.addSubWidget(
                    new SubWidget(false, this.getMessage()),
                    0, 4, 0, 0
            ).cast();

            this.add = this.addSubWidget(Button.builder(Component.literal("++++++  ").append(Component.translatable("info.inbox.attribute")).append("  ++++++"), b->{
                                UUID uuid = UUID.randomUUID();
                                this.attributes.addWiget(new AttributeWidget(uuid, ()->
                                    this.attributes.widgets.removeIf(wiget -> {
                                        if (wiget.widget instanceof AttributeWidget) {
                                            return ((AttributeWidget) wiget.widget).uuid.equals(uuid);
                                        }
                                        return false;
                                    })
                                ),0, 8, 0, 0);
                            })
                            .size(350, 15)
                            .build(),0, 0, 0, 0)
                    .setConsumerEvent(false, true, false, false).cast();
        }

        @Override
        public IAttachment newAttachmentInstance() {
            AttachmentAttribute attachment = new AttachmentAttribute();
            for (SubWidget.PositionWiget widget : this.attributes.widgets) {
                if (widget.widget instanceof AttributeWidget) {
                    AttributeWidget attribute =  (AttributeWidget) widget.widget;
                    attachment.addValue(
                            ForgeRegistries.ATTRIBUTES.getValue(Utils.location(attribute.attribute.getValue())),
                            attribute.operation,
                            new AttributeValue(attribute.value.getAsNumber().doubleValue(), attribute.isTemp.selected())
                    );
                }
            }
            return attachment;
        }

        public static class AttributeWidget extends SubWidget {
            public final UUID uuid;
            public final EditBox attribute;
            public int attributeIndex = 0;
            public AttributeModifier.Operation operation = AttributeModifier.Operation.ADDITION;
            public final GuiFilterTextField value;
            public final GuiCheckbox isTemp;

            protected ModifierWidget operationModifier, attributeModifier;

            public AttributeWidget(UUID uuid, Runnable onRemove) {
                super(true);
                this.uuid = uuid;

                this.attribute = this.addWiget(new EditBox(
                        RenderUtils.getFontRenderer(), 0, 0, 200, RenderUtils.fontHeight()+4, CommonComponents.EMPTY
                )).cast();
                this.attribute.setValue(ATTRIBUTES.get(0).toString());
                this.attribute.setResponder(s->{
                    ResourceLocation id = Utils.location(this.attribute.getValue());
                    Attribute value = ForgeRegistries.ATTRIBUTES.getValue(id);
                    this.attributeModifier.setTooltip(Tooltip.create(Component.translatable(
                            value == null ? "Not found attribute." : value.getDescriptionId()
                    )));
                    this.attribute.setTooltip(this.attributeModifier.getTooltip());
                    if (value != null) {
                        this.attributeIndex = ATTRIBUTES.indexOf(id);
                    }
                });

                this.attributeModifier = this.addWiget(new ModifierWidget(false, ()->{
                    this.attributeIndex--;
                    if (this.attributeIndex < 0) {
                        this.attributeIndex = ATTRIBUTES.size() - 1;
                    }
                    this.attribute.setValue(ATTRIBUTES.get(this.attributeIndex).toString());

                    Attribute value = ForgeRegistries.ATTRIBUTES.getValue(ATTRIBUTES.get(this.attributeIndex));
                    this.attributeModifier.setTooltip(Tooltip.create(Component.translatable(
                            value == null ? "Not found attribute." : value.getDescriptionId()
                    )));
                    this.attribute.setTooltip(this.attributeModifier.getTooltip());
                }, ()->{
                    this.attributeIndex++;
                    if (this.attributeIndex >= ATTRIBUTES.size()) {
                        this.attributeIndex = 0;
                    }
                    this.attribute.setValue(ATTRIBUTES.get(this.attributeIndex).toString());

                    Attribute value = ForgeRegistries.ATTRIBUTES.getValue(ATTRIBUTES.get(this.attributeIndex));
                    this.attributeModifier.setTooltip(Tooltip.create(Component.translatable(
                            value == null ? "Not found attribute." : value.getDescriptionId()
                    )));
                    this.attribute.setTooltip(this.attributeModifier.getTooltip());
                }), 0, 0, 2, 3).cast();

                this.attributeModifier.setTooltip(Tooltip.create(Component.translatable(
                        ForgeRegistries.ATTRIBUTES.getValue(ATTRIBUTES.get(0)).getDescriptionId()
                )));
                this.attribute.setTooltip(this.attributeModifier.getTooltip());

                this.value = this.addWiget(new GuiFilterTextField(
                        "0", true, 0, 0, 30, RenderUtils.fontHeight()+4
                ).setCanBeNegative(true)).cast();
                this.value.setResponder(s->{
                    this.operationModifier.setTooltip(Tooltip.create(Component.literal(AttachmentAttribute.getMethod(this.operation, this.value.getAsNumber().doubleValue()))));
                    this.value.setTooltip(this.operationModifier.getTooltip());
                });
                this.operationModifier = this.addWiget(new ModifierWidget(false, ()->{
                    int index = this.operation.toValue() - 1;
                    if (index < 0) {
                        index = 2;
                    }
                    this.operation = AttributeModifier.Operation.fromValue(index);
                    this.operationModifier.setTooltip(Tooltip.create(Component.literal(AttachmentAttribute.getMethod(this.operation, this.value.getAsNumber().doubleValue()))));
                    this.value.setTooltip(this.operationModifier.getTooltip());
                }, ()->{
                    int index = this.operation.toValue() + 1;
                    if (index > 2) {
                        index = 0;
                    }
                    this.operation = AttributeModifier.Operation.fromValue(index);
                    this.operationModifier.setTooltip(Tooltip.create(Component.literal(AttachmentAttribute.getMethod(this.operation, this.value.getAsNumber().doubleValue()))));
                    this.value.setTooltip(this.operationModifier.getTooltip());
                }), 0, 0, 2, 3).cast();

                this.operationModifier.setTooltip(Tooltip.create(Component.literal(AttachmentAttribute.getMethod(this.operation, this.value.getAsNumber().doubleValue()))));
                this.value.setTooltip(this.operationModifier.getTooltip());

                this.isTemp = this.addWiget(new GuiCheckbox(
                        0, 0, RenderUtils.fontHeight(), RenderUtils.fontHeight(), CommonComponents.EMPTY, true, null
                )).cast();
                this.isTemp.setTooltip(Tooltip.create(Component.translatable("info.inbox.healths.temp")));

                this.addWiget(Button.builder(Component.literal("X"), b->
                    onRemove.run()
                ).size(RenderUtils.fontHeight(), RenderUtils.fontHeight()).build(), 0, 0, 10, 0)
                        .setConsumerEvent(false, true, false, false);
            }

            @Override
            public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
                if (this.attribute.isFocused()) {
                    super.keyPressed(pKeyCode, pScanCode, pModifiers);
                    return true;
                }
                return super.keyPressed(pKeyCode, pScanCode, pModifiers);
            }
        }
    }
}
