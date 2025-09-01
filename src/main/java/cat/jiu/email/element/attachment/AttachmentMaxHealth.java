package cat.jiu.email.element.attachment;

import cat.jiu.core.api.IData;
import cat.jiu.core.util.Utils;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.*;
import java.util.Collections;
import java.util.List;

public class AttachmentMaxHealth implements IAttachment {
    public static final ResourceLocation ID = Utils.location(EmailMain.MODID, "attachment/health");

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
        this.read(tag);
    }

    public AttachmentMaxHealth(JsonObject json) {
        this.read(json);
    }
    public AttachmentMaxHealth(IData.IMapData<?> data) {
        this.read(data);
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
    public IData.IMapData<?> write(IData.IMapData<?> data) {
        data.putData("health", this.getHealth());
        data.putData("temp", this.isTempHealth());
        return data;
    }

    @Override
    public void read(IData.IMapData<?> data) {
        this.setHealth(data.getInt("health"));
        this.setTempHealth(data.getBoolean("temp", true));
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
//            AttributeInstance instance = Objects.requireNonNull(player.getAttribute(Attributes.MAX_HEALTH), "not found max health attribute");
//            AttachmentAttribute.AttributeState.Id id = AttachmentAttribute.AttributeState.id(Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION, this.isTempHealth());
//            double value = this.isTempHealth() ? this.getHealth() : AttachmentAttribute.AttributeState.get(player.getServer()).add(player.getStringUUID(), Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION, this.getHealth());
//            instance.removeModifier(id.uid);
//            instance.addPermanentModifier(new AttributeModifier(id.uid, id.id, value, AttributeModifier.Operation.ADDITION));

            AttachmentAttribute.accept(player, Attributes.MAX_HEALTH, AttributeModifier.Operation.ADDITION, new AttachmentAttribute.AttributeValue(this.getHealth(), this.isTempHealth()));
        }
    }

    @Override
    public List<Component> getHoverMessage() {
        return Collections.singletonList(
                Component.translatable("info.inbox.healths").append(": " + this.getHealth()).append(this.isTempHealth() ? Component.translatable("info.inbox.healths.temp") : CommonComponents.EMPTY)
        );
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawIcon(AttachmentEvent.Render event, int x) {
        event.graphics.setColor(1.0F, 1.0F, 1.0F, 1);
        event.graphics.blit(x-4, event.getY()-4, 0, 24, 24, Icon.getHealthIcon());
        event.graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("info.inbox.healths");
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
