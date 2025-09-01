package cat.jiu.email.element.attachment;

import cat.jiu.core.api.IData;
import cat.jiu.core.util.Utils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.util.EmailUtils;
import com.google.gson.JsonObject;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class AttachmentXP implements IAttachment {
    public static final ResourceLocation ID = Utils.location(EmailMain.MODID, "attachment/xp");
    public static final ItemStack EXPERIENCE_BOTTLE = new ItemStack(Items.EXPERIENCE_BOTTLE);

    protected int levels;
    protected int points;

    public AttachmentXP() {
    }

    public AttachmentXP(int levels, int points) {
        this.levels = levels;
        this.points = points;
    }

    public AttachmentXP(CompoundTag tag) {
        this.read(tag);
    }

    public AttachmentXP(JsonObject json) {
        this.read(json);
    }
    public AttachmentXP(IData.IMapData<?> data) {
        this.read(data);
    }

    public int getLevels() {
        return levels;
    }

    public AttachmentXP setLevels(int levels) {
        this.levels = levels;
        return this;
    }

    public AttachmentXP addLevels(int levels) {
        this.levels += levels;
        return this;
    }

    public int getPoints() {
        return points;
    }

    public AttachmentXP setPoints(int points) {
        this.points = points;
        return this;
    }

    public AttachmentXP addPoints(int points) {
        this.points += points;
        return this;
    }

    @Override
    public IData.IMapData<?> write(IData.IMapData<?> data) {
        data.putData("levels", this.getLevels());
        data.putData("points", this.getPoints());
        return data;
    }

    @Override
    public void read(IData.IMapData<?> data) {
        this.setLevels(data.getInt("levels"));
        this.setPoints(data.getInt("points"));
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentXP attachment && !attachment.isEmpty()) {
            this.addLevels(attachment.getLevels());
            this.addPoints(attachment.getPoints());
        }
    }

    @Override
    public boolean isEmpty() {
        return this.getLevels() == 0 && this.getPoints() == 0;
    }

    @Override
    public String getName() {
        return "xp";
    }

    @Override
    public void accept(Player player) {
        player.giveExperienceLevels(this.getLevels());
        player.giveExperiencePoints(this.getPoints());
    }

    @Override
    public List<Component> getHoverMessage() {
        return List.of(
                Component.literal(I18n.get("info.inbox.xp_save_to_email.0", this.getLevels())),
                Component.literal(I18n.get("info.inbox.xp_save_to_email.1", this.getPoints()))
        );
    }

    @Override
    public ItemStack getDisplayStack() {
        return EXPERIENCE_BOTTLE;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("info.inbox.xps");
    }
}
