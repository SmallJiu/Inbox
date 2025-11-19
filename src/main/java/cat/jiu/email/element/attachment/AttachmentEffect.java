package cat.jiu.email.element.attachment;

import cat.jiu.core.api.IData;
import cat.jiu.core.util.*;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.core.util.element.data.NBTData;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.AttachmentSendScreenWidget;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.ui.gui.component.GuiCheckbox;
import cat.jiu.email.ui.gui.component.GuiFilterTextField;
import cat.jiu.email.ui.gui.component.GuiTime;
import cat.jiu.email.util.EmailUtils;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class AttachmentEffect implements IAttachment {
    public static final ResourceLocation ID = Utils.location(EmailMain.MODID, "attachment/effect");
    public static final ItemStack GLASS_BOTTLE = new ItemStack(Items.GLASS_BOTTLE);

    protected ArrayList<MobEffectInstance> effects;

    public AttachmentEffect() {
    }

    public AttachmentEffect(MobEffectInstance... effects) {
        this.effects = new ArrayList<>();
        this.effects.addAll(Arrays.asList(effects));
    }

    public AttachmentEffect(CompoundTag tag) {
        this.read(tag);
    }

    public AttachmentEffect(JsonObject json) {
        this.read(json);
    }
    public AttachmentEffect(IData.IMapData<?> data) {
        this.read(data);
    }

    public List<MobEffectInstance> getEffects() {
        return effects;
    }

    public AttachmentEffect setEffects(MobEffectInstance... effects) {
        this.effects = new ArrayList<>();
        this.effects.addAll(Arrays.asList(effects));
        return this;
    }

    public AttachmentEffect addEffect(MobEffectInstance effect) {
        if (this.effects==null) this.effects = new ArrayList<>();
        this.effects.add(effect);
        return this;
    }
    public AttachmentEffect addEffects(List<MobEffectInstance> effects) {
        if (this.effects==null) this.effects = new ArrayList<>();
        this.effects.addAll(effects);
        return this;
    }
    public AttachmentEffect addEffects(MobEffectInstance... effects) {
        if (this.effects==null) this.effects = new ArrayList<>();
        this.effects.addAll(Arrays.asList(effects));
        return this;
    }

    @Override
    public IData.IMapData<?> write(IData.IMapData<?> data) {
        IData.IListData<?> effects = data.newList();
        for (MobEffectInstance effect : this.getEffects()) {
            IData.IMapData<?> dataEffect = data.newMap();
            NBTData.map(effect.save(new CompoundTag())).foreach((key, data1) -> {
                if (!"Amplifier".equalsIgnoreCase(key) && data1.isPrimitive() && data1.getAsPrimitive().isBoolean()) {
                    dataEffect.putData(key, data1.getAsPrimitive().getAsBoolean());
                }else {
                    dataEffect.putData(key, data1);
                }
            });
            effects.putData(dataEffect);
        }
        data.putData("effects", effects);
        return data;
    }

    @Override
    public void read(IData.IMapData<?> data) {
        data.getList("effects", IData.IMapData.class).foreach((index, value)-> {
            IData.IMapData<?> effect = value.getAsMap();
            if (effect.containsKey("string") && effect.containsKey("byte")) {
                if (effect.getData() instanceof JsonObject) {
                    this.addEffect(MobEffectInstance.load(JsonToStackUtil.toNBT((JsonObject) effect.getData())));
                    return;
                }
            }
            this.addEffect(DataUtils.loadMobEffect(effect));
        });
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean isEmpty() {
        return this.getEffects() == null || this.getEffects().isEmpty();
    }

    @Override
    public String getName() {
        return "info.inbox.effects";
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentEffect attachment && !attachment.isEmpty()) {
            this.addEffects(attachment.getEffects());
        }
    }
    @Override
    public void accept(Player player) {
        for (MobEffectInstance effect : this.getEffects()) {
            player.addEffect(effect, player);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(AttachmentEvent.Render event) {
        if (!this.isEmpty()) {
            event.renderSaveTo("info.inbox.effects");
            event.addY(event.font.lineHeight + 4);

            int x = event.x;
            Runnable seeEffect = null;

            for (MobEffectInstance effect : this.getEffects()) {
                if (x >= event.x + event.viewWidth - 35) {
                    event.addY(24+1);
                    x = event.x;
                }
                int y = event.getY() + 2;

                if (effect.isAmbient()) {
                    event.graphics.blit(AbstractContainerScreen.INVENTORY_LOCATION, x, y, 165, 166, 24, 24);
                } else {
                    event.graphics.blit(AbstractContainerScreen.INVENTORY_LOCATION, x, y, 141, 166, 24, 24);
                }
                event.graphics.setColor(1.0F, 1.0F, 1.0F, 1);
                event.graphics.blit(x + 3, y + 3, 0, 18, 18, Minecraft.getInstance().getMobEffectTextures().get(effect.getEffect()));
                event.graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

                if (event.canSee() && EmailUtils.isInRange(event.mouseX, event.mouseY, x, y, 23, 23)) {
                    seeEffect = ()->{
                        event.disableScissor();

                        MutableComponent effect_name = effect.getEffect().getDisplayName().copy();
                        if (effect.getAmplifier() > 0) {
                            effect_name.append(CommonComponents.SPACE).append(Component.literal(String.valueOf(effect.getAmplifier() + 1)));
                        }
                        event.graphics.renderComponentTooltip(Minecraft.getInstance().font, Arrays.asList(effect_name, MobEffectUtil.formatDuration(effect, 1.0F)), event.mouseX, event.mouseY);

                        event.enableScissor();
                    };
                }
                x += 24 + 1;
            }
            if (seeEffect != null) {
                seeEffect.run();
            }
            event.addY(24 + 2);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void getHeight(AttachmentEvent.GetHeight event) {
        if (!this.isEmpty()) {
            event.addHeight(event.font.lineHeight + 2);

            event.addHeight(24 + 4);
            int itemX = 0;
            for (MobEffectInstance effect : this.getEffects()) {
                if (itemX >= event.guiWidth - 48) {
                    event.addHeight(24);
                    itemX = 0;
                }
                itemX += 24;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Widget extends AttachmentSendScreenWidget {
        public static final WidgetEntry INSTANCE = new WidgetEntry(ID, false, Widget::new);
        public static final List<ResourceLocation> EFFECTS = Collections.unmodifiableList(new ArrayList<>(ForgeRegistries.MOB_EFFECTS.getKeys()));

        public final SubWidget effects;
        public final Button add;
        private Widget() {
            super(Component.translatable("info.inbox.effects"));
            this.initSubWiget(false);
            this.effects = this.addSubWidget(
                    new SubWidget(false),
                    3, 4, 0, 0
            ).cast();

            this.add = this.addSubWidget(Button.builder(Component.literal("++++++  ").append(Component.translatable("info.inbox.effects")).append("  ++++++"), b->{
                UUID uuid = UUID.randomUUID();
                this.effects.addWiget(new EffectWidget(uuid, ()->
                        this.effects.widgets.removeIf(wiget -> {
                            if (wiget.widget instanceof EffectWidget) {
                                return ((EffectWidget) wiget.widget).uuid.equals(uuid);
                            }
                            return false;
                        })
                ),0, 2, 0, 0);
            }).size(350, 15)
                    .build(), 0, 0, 0, 0).cast();
        }

        @Override
        public IAttachment newAttachmentInstance() {
            AttachmentEffect attachment = new AttachmentEffect();
            for (SubWidget.PositionWiget widget : this.effects.widgets) {
                if (widget.widget instanceof EffectWidget) {
                    EffectWidget effect =  (EffectWidget) widget.widget;
                    MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(Utils.location(effect.effect.getValue()));
                    if (mobEffect != null) {
                        MobEffectInstance instance = new MobEffectInstance(
                                mobEffect, effect.infinite.selected() ? -1 : (int)effect.time.getTimeOfTicks(), effect.level.getAsNumber().intValue(),
                                effect.beaconAdd.selected(), effect.showParticles.selected(), effect.showIcon.selected()
                        );
                        if (effect.setMainhandItemToCurativeItem.selected() && !Minecraft.getInstance().player.getMainHandItem().isEmpty()) {
                            instance.getCurativeItems().add(Minecraft.getInstance().player.getMainHandItem());
                        }
                        attachment.addEffect(instance);
                    }
                }
            }
            return attachment;
        }

        public static class EffectWidget extends SubWidget {
            public final UUID uuid;
            public final EditBox effect;
            public ModifierWidget effectModifier;
            public final GuiTime time;
            public final GuiFilterTextField level;
            public final GuiCheckbox infinite, beaconAdd, showIcon, showParticles, setMainhandItemToCurativeItem;

            public EffectWidget(UUID uuid, Runnable onRemove) {
                super(true);
                this.uuid = uuid;
                this.effect = this.addWiget(new EditBox(
                        RenderUtils.getFontRenderer(), 0, 0, 200, RenderUtils.fontHeight()+4, CommonComponents.EMPTY
                )).cast();
                EffectIcon icon = this.addWiget(new EffectIcon(), -4, 0, 2, 0).cast();
                icon.effect = MobEffects.MOVEMENT_SPEED;
                this.effect.setValue("minecraft:speed");
                this.effect.setResponder(s-> {
                    MobEffect effectInstance = ForgeRegistries.MOB_EFFECTS.getValue(Utils.location(s));

                    icon.effect = effectInstance;
                    Tooltip effectName = Tooltip.create(Component.translatable(effectInstance != null ? effectInstance.getDescriptionId() : "Not found effect"));
                    this.effect.setTooltip(effectName);
                    icon.setTooltip(effectName);
                    this.effectModifier.setTooltip(effectName);
                });

                AtomicInteger effectIndex = new AtomicInteger(0);
                this.effectModifier = this.addWiget(new ModifierWidget(false, ()-> {
                    effectIndex.set(effectIndex.get() + 1);
                    if (effectIndex.get() >= EFFECTS.size()) {
                        effectIndex.set(0);
                    }
                    MobEffect effectInstance = ForgeRegistries.MOB_EFFECTS.getValue(EFFECTS.get(effectIndex.get()));
                    this.effect.setValue(String.valueOf(EFFECTS.get(effectIndex.get())));

                    Tooltip effectName = Tooltip.create(Component.translatable(effectInstance != null ? effectInstance.getDescriptionId() : "Not found effect"));
                    this.effect.setTooltip(effectName);
                    icon.setTooltip(effectName);
                    this.effectModifier.setTooltip(effectName);
                }, ()->{
                    effectIndex.set(effectIndex.get() - 1);
                    if (effectIndex.get() < 0) {
                        effectIndex.set(EFFECTS.size()-1);
                    }
                    MobEffect effectInstance = ForgeRegistries.MOB_EFFECTS.getValue(EFFECTS.get(effectIndex.get()));
                    this.effect.setValue(String.valueOf(EFFECTS.get(effectIndex.get())));

                    Tooltip effectName = Tooltip.create(Component.translatable(effectInstance != null ? effectInstance.getDescriptionId() : "Not found effect"));
                    this.effect.setTooltip(effectName);
                    icon.setTooltip(effectName);
                    this.effectModifier.setTooltip(effectName);
                }), 0, 0, 2, 2).cast();

                Tooltip effectName = Tooltip.create(Component.translatable(MobEffects.MOVEMENT_SPEED.getDescriptionId()));
                this.effect.setTooltip(effectName);
                icon.setTooltip(effectName);
                this.effectModifier.setTooltip(effectName);

                this.time = this.addWiget(
                        new GuiTime(false).setRenderBackgroubd(false),
                        -4, 0, 0, 0
                ).cast();

                this.level = this.addWiget(new GuiFilterTextField(
                        "0", false, 0, 0, 20, RenderUtils.fontHeight()+4
                ), 0, 0, 3, 0).cast();
                this.level.setMaxLength(2);
                this.addWiget(new ModifierWidget(false, ()->
                    this.level.setValue(String.valueOf(this.level.getAsNumber().intValue()+1))
                , ()->{
                    this.level.setValue(String.valueOf(this.level.getAsNumber().intValue()-1));
                    if (this.level.getAsNumber().intValue() < 0) {
                        this.level.setValue("0");
                    }
                }), 0, 0, 6, 2).cast().setTooltip(Tooltip.create(Component.translatable("info.inbox.generate.attachment.effect.amplifier")));
                this.level.setTooltip(Tooltip.create(Component.translatable("info.inbox.generate.attachment.effect.amplifier")));

                this.infinite = this.addWiget(createCheckbox(Component.translatable("info.inbox.generate.attachment.effect.infinite"), false, null),
                        0, 0, 4, 2).setConsumerEvent(false, true, false, false).cast();
                this.beaconAdd = this.addWiget(createCheckbox(Component.translatable("info.inbox.generate.attachment.effect.beacon"), false, null),
                        0, 0, 4, 2).setConsumerEvent(false, true, false, false).cast();
                this.showIcon = this.addWiget(createCheckbox(Component.translatable("info.inbox.generate.attachment.effect.show_icon"), true, null),
                        0, 0, 4, 2).setConsumerEvent(false, true, false, false).cast();
                this.showParticles = this.addWiget(createCheckbox(Component.translatable("info.inbox.generate.attachment.effect.show_particles"), true, null),
                        0, 0, 4, 6).setConsumerEvent(false, true, false, false).cast();
                this.setMainhandItemToCurativeItem = this.addWiget(createCheckbox(Component.translatable("info.inbox.generate.attachment.effect.mainhand_to_curative"), false, null))
                        .setConsumerEvent(false, true, false, false).cast();

                this.addWiget(Button.builder(Component.literal("X"), b->
                                onRemove.run()
                        ).size(RenderUtils.fontHeight(), RenderUtils.fontHeight()).build(), 0, 0, 10, 0)
                        .setConsumerEvent(false, true, false, false);
            }

            @Override
            public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
                if (this.effect.isFocused()) {
                    super.keyPressed(pKeyCode, pScanCode, pModifiers);
                    return true;
                }
                return super.keyPressed(pKeyCode, pScanCode, pModifiers);
            }

            public static class EffectIcon extends AbstractWidget {
                public MobEffect effect;
                public EffectIcon() {
                    super(0, 0, 24, 24, CommonComponents.EMPTY);
                }

                @Override
                protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                    int x = this.getX(), y = this.getY();
                    RenderUtils.draw(graphics, AbstractContainerScreen.INVENTORY_LOCATION, x, y, 24, 24, 141, 166);

                    if (this.effect != null) {
                        graphics.setColor(1.0F, 1.0F, 1.0F, 1);
                        graphics.blit(x + 3, y + 3, 0, 18, 18, Minecraft.getInstance().getMobEffectTextures().get(this.effect));
                        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }

                @Override
                public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
                    return false;
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {}
            }

            public static GuiCheckbox createCheckbox(Component tooltip, boolean defaultSelected, Runnable selected){
                GuiCheckbox checkbox = new GuiCheckbox(0, 0, RenderUtils.fontHeight(), RenderUtils.fontHeight(), CommonComponents.EMPTY, defaultSelected, selected);
                checkbox.setTooltip(Tooltip.create(tooltip));
                return checkbox;
            }
        }
    }
}
