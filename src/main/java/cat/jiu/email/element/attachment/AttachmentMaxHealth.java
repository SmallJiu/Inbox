package cat.jiu.email.element.attachment;

import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.util.EmailUtils;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.awt.*;
import java.util.*;

public class AttachmentMaxHealth implements IAttachment {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(EmailMain.MODID, "attachment/health");

    protected int health;
    protected boolean temp;

    public AttachmentMaxHealth() {
    }

    public AttachmentMaxHealth(float health) {
        this((int)health);
    }
    public AttachmentMaxHealth(int health) {
        this.health = health;
    }

    public AttachmentMaxHealth(CompoundTag tag) {
        this.readFrom(tag);
    }

    public AttachmentMaxHealth(JsonObject json) {
        this.readFrom(json);
    }

    public boolean isTempHealth() {
        return temp;
    }

    /**
     * @param temp 如果此附件是一次性的, 则为true (true if the max health is disposable)
     */
    public AttachmentMaxHealth setTempHealth(boolean temp) {
        this.temp = temp;
        return this;
    }

    public int getHealth() {
        return health;
    }

    @Deprecated
    public AttachmentMaxHealth setHealth(float health) {
        return this.setHealth((int)health);
    }
    public AttachmentMaxHealth setHealth(int health) {
        this.health = health;
        return this;
    }

    @Deprecated
    public AttachmentMaxHealth addLevels(float health) {
        return this.addHealth((int)health);
    }
    public AttachmentMaxHealth addHealth(int health) {
        this.health += health;
        return this;
    }

    @Override
    public JsonObject write(JsonObject json) {
        json.addProperty("health", this.getHealth());
        json.addProperty("temp", this.isTempHealth());
        return json;
    }

    @Override
    public void read(JsonObject json) {
        this.setHealth(json.has("health") ? json.get("health").getAsInt() : 0);
        this.setTempHealth(json.has("temp") ? json.get("temp").getAsBoolean() : true);
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {
        nbt.putInt("health", this.getHealth());
        nbt.putBoolean("temp", this.isTempHealth());
        return nbt;
    }

    @Override
    public void read(CompoundTag nbt) {
        this.setHealth(nbt.getInt("health"));
        this.setTempHealth(nbt.contains("temp") ? nbt.getBoolean("temp") : true);
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentMaxHealth attachment && !attachment.isEmpty()) {
            this.addHealth(attachment.getHealth());
        }
    }

    @Override
    public boolean isEmpty() {
        return this.getHealth() <= 0;
    }

    @Override
    public String getName() {
        return "health";
    }

    @Override
    public void accept(Player player) {
        if (!player.level().isClientSide()) {
            AttributeInstance instance = Objects.requireNonNull(player.getAttribute(Attributes.MAX_HEALTH), "not found max health attribute");
            ResourceLocation id = AttachmentAttribute.AttributeState.id(Attributes.MAX_HEALTH.value(), AttributeModifier.Operation.ADD_VALUE, this.isTempHealth());

            double value = this.isTempHealth() ? this.getHealth() : AttachmentAttribute.AttributeState.get(player.getServer()).add(player.getStringUUID(), Attributes.MAX_HEALTH.value(), AttributeModifier.Operation.ADD_VALUE, this.getHealth());
            instance.removeModifier(id);
            instance.addPermanentModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
//            if (this.isTempHealth()) {
//                instance.removeModifier(Health_Modifier_UUID_TEMP);
//                instance.addPermanentModifier(new AttributeModifier(Health_Modifier_UUID_TEMP, ID.toString(), this.getHealth(), AttributeModifier.Operation.ADDITION));
//            }else {
//                double v = AttachmentAttribute.AttributeState.get(player.getServer()).add(player.getStringUUID(), Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION, this.getHealth());
//                instance.removeModifier(Health_Modifier_UUID);
//                instance.addPermanentModifier(new AttributeModifier(Health_Modifier_UUID, ID.toString(), v, AttributeModifier.Operation.ADDITION));
//            }
        }
    }

//    public static void loadHealth(Player player) {
//        double health = AttachmentAttribute.AttributeState.get(player.getServer()).get(player.getStringUUID(), Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION);
//        if (health > 0) {
//            AttributeInstance instance = Objects.requireNonNull(player.getAttribute(Attributes.MAX_HEALTH), "not found max health attribute");
//            instance.removeModifier(Health_Modifier_UUID);
//            instance.addPermanentModifier(new AttributeModifier(Health_Modifier_UUID, ID.toString(), health, AttributeModifier.Operation.ADDITION));
//        }
//    }
//
//    @SubscribeEvent
//    public static void onPlayerClone(PlayerEvent.Clone event){
//        if (!event.getEntity().level().isClientSide() && !event.isWasDeath()) {
//            loadHealth(event.getEntity());
//        }
//    }
//    @SubscribeEvent
//    public static void onPlayerJoinWorld(EntityJoinLevelEvent event){
//        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof Player) {
//            loadHealth((Player) event.getEntity());
//        }
//    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(AttachmentEvent.Render event) {
        if (!this.isEmpty()) {
            event.graphics.drawString(event.font, Component.nullToEmpty(null), event.x, event.getY(), Color.WHITE.getRGB());

            int x = event.x + event.font.width(event.renderSaveTo("info.inbox.healths")) + 2;

            event.graphics.setColor(1.0F, 1.0F, 1.0F, 1);
            event.graphics.blit(x-4, event.getY()-4, 0, 24, 24, Icon.getHealthIcon());
            event.graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

            if (event.canSee() && EmailUtils.isInRange(event.mouseX, event.mouseY, x, event.getY(), 16, 16)) {
                event.disableScissor();

                event.graphics.renderTooltip(event.font,
                        Component.translatable("info.inbox.healths").append(": " + this.getHealth()).append(this.isTempHealth() ? Component.translatable("info.inbox.healths.temp") : CommonComponents.EMPTY)
                , event.mouseX, event.mouseY);

                event.enableScissor();
            }
            event.addY(event.font.lineHeight + 2);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void getHeight(AttachmentEvent.GetHeight event) {
        if (!this.isEmpty()) {
            event.addHeight(16);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Icon {
        public static TextureAtlasSprite
                HEALTH_ICON = null,
                LUCK_ICON = null
        ;
        public static TextureAtlasSprite getHealthIcon() {
            if (Icon.HEALTH_ICON == null) {
                Icon.HEALTH_ICON = Minecraft.getInstance().getMobEffectTextures().get(MobEffects.HEAL);
            }
            return HEALTH_ICON;
        }

        public static TextureAtlasSprite getLuckIcon() {
            if (Icon.LUCK_ICON == null) {
                Icon.LUCK_ICON = Minecraft.getInstance().getMobEffectTextures().get(MobEffects.LUCK);
            }
            return LUCK_ICON;
        }
    }
}
