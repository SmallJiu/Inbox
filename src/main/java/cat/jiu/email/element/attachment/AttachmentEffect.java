package cat.jiu.email.element.attachment;

import cat.jiu.core.util.JsonToStackUtil;
import cat.jiu.core.util.NBTUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.util.EmailUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AttachmentEffect implements IAttachment {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(EmailMain.MODID, "attachment/effect");
    public static final ItemStack GLASS_BOTTLE = new ItemStack(Items.GLASS_BOTTLE);

    protected ArrayList<MobEffectInstance> effects;

    public AttachmentEffect() {
    }

    public AttachmentEffect(MobEffectInstance... effects) {
        this.effects = new ArrayList<>();
        this.effects.addAll(Arrays.asList(effects));
    }

    public AttachmentEffect(CompoundTag tag) {
        this.readFrom(tag);
    }

    public AttachmentEffect(JsonObject json) {
        this.readFrom(json);
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
    public JsonObject write(JsonObject json) {
        JsonArray effects = new JsonArray();
        for (MobEffectInstance effect : this.getEffects()) {
            effects.add(NBTUtils.toJson(effect.save()));
        }
        json.add("effects", effects);
        return json;
    }

    @Override
    public void read(JsonObject json) {
        JsonArray effects = json.getAsJsonArray("effects");
        for (JsonElement effect : effects) {
            this.addEffect(MobEffectInstance.load(JsonToStackUtil.toNBT(effect.getAsJsonObject())));
        }
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {
        ListTag effects = new ListTag();
        for (MobEffectInstance effect : this.getEffects()) {
            effects.add(effect.save());
        }
        nbt.put("effects", effects);
        return nbt;
    }

    @Override
    public void read(CompoundTag nbt) {
        ListTag effects = nbt.getList("effects", 10);
        for (int i = 0; i < effects.size(); i++) {
            this.addEffect(MobEffectInstance.load(effects.getCompound(i)));
        }
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentEffect attachment && !attachment.isEmpty()) {
            this.addEffects(attachment.getEffects());
        }
    }

    @Override
    public boolean isEmpty() {
        return this.getEffects() == null || this.getEffects().isEmpty();
    }

    @Override
    public String getName() {
        return "effect";
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

                        MutableComponent effect_name = Component.translatable(effect.getDescriptionId());
                        if (effect.getAmplifier() > 0) {
                            effect_name.append(CommonComponents.SPACE).append(Component.literal(String.valueOf(effect.getAmplifier() + 1)));
                        }
                        event.graphics.renderComponentTooltip(Minecraft.getInstance().font, Arrays.asList(effect_name, MobEffectUtil.formatDuration(effect, 1.0F, effect.getDuration())), event.mouseX, event.mouseY);

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
}
