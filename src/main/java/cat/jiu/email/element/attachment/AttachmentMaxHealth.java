package cat.jiu.email.element.attachment;

import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.util.EmailUtils;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class AttachmentMaxHealth implements IAttachment {
    public static final ResourceLocation ID = new ResourceLocation(EmailMain.MODID, "attachment/health");

    protected float health;

    public AttachmentMaxHealth() {
    }

    public AttachmentMaxHealth(float health) {
        this.health = health;
    }

    public AttachmentMaxHealth(CompoundTag tag) {
        this.readFrom(tag);
    }

    public AttachmentMaxHealth(JsonObject json) {
        this.readFrom(json);
    }

    public float getHealth() {
        return health;
    }

    public AttachmentMaxHealth setHealth(float health) {
        this.health = health;
        return this;
    }

    public AttachmentMaxHealth addLevels(float levels) {
        this.health += levels;
        return this;
    }

    @Override
    public JsonObject write(JsonObject json) {
        json.addProperty("health", this.getHealth());
        return json;
    }

    @Override
    public void read(JsonObject json) {
        this.setHealth(json.has("health") ? json.get("health").getAsInt() : 0);
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {
        nbt.putFloat("health", this.getHealth());
        return nbt;
    }

    @Override
    public void read(CompoundTag nbt) {
        this.setHealth(nbt.getFloat("health"));
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentMaxHealth attachment && !attachment.isEmpty()) {
            this.addLevels(attachment.getHealth());
        }
    }

    @Override
    public boolean isEmpty() {
        return this.getHealth() <= 0.0001 && this.getHealth() >= -0.0001;
    }

    @Override
    public String getName() {
        return "health";
    }

    @Override
    public void accept(Player player) {
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(player.getMaxHealth() + this.getHealth());
    }

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

                event.graphics.renderComponentTooltip(event.font, List.of(
                        Component.translatable("info.inbox.healths").append(": " + this.getHealth())
                ), event.mouseX, event.mouseY);

                event.enableScissor();
            }
            event.addY(event.font.lineHeight + 2);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void getHeight(AttachmentEvent.GetHeight event) {
        if (Icon.HEALTH_ICON == null) Icon.HEALTH_ICON = Minecraft.getInstance().getMobEffectTextures().get(MobEffects.HEAL);

        if (!this.isEmpty()) {
            event.addHeight(16);
        }
    }
    @OnlyIn(Dist.CLIENT)
    public static class Icon {
        static TextureAtlasSprite HEALTH_ICON = null;
        public static TextureAtlasSprite getHealthIcon() {
            return HEALTH_ICON;
        }
    }
}
