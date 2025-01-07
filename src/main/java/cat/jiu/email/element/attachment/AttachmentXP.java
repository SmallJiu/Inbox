package cat.jiu.email.element.attachment;

import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.util.EmailUtils;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class AttachmentXP implements IAttachment {
    public static final ResourceLocation ID = new ResourceLocation(EmailMain.MODID, "attachment/xp");
    public static final ItemStack EXPERIENCE_BOTTLE = new ItemStack(Items.EXPERIENCE_BOTTLE);

    protected int levels;
    protected int points;

    public AttachmentXP() {
    }

    public AttachmentXP(int levels, int points) {
        this.levels = levels;
        this.points = points;
    }

    public AttachmentXP(CompoundNBT tag) {
        this.readFrom(tag);
    }

    public AttachmentXP(JsonObject json) {
        this.readFrom(json);
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
    public JsonObject write(JsonObject json) {
        json.addProperty("levels", this.getLevels());
        json.addProperty("points", this.getPoints());
        return json;
    }

    @Override
    public void read(JsonObject json) {
        this.setLevels(json.has("levels") ? json.get("levels").getAsInt() : 0);
        this.setPoints(json.has("points") ? json.get("points").getAsInt() : 0);
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        nbt.putInt("levels", this.getLevels());
        nbt.putInt("points", this.getPoints());
        return nbt;
    }

    @Override
    public void read(CompoundNBT nbt) {
        this.setLevels(nbt.getInt("levels"));
        this.setPoints(nbt.getInt("points"));
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentXP && !other.isEmpty()) {
            this.addLevels(((AttachmentXP) other).getLevels());
            this.addPoints(((AttachmentXP) other).getPoints());
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
    public void accept(PlayerEntity player) {
        player.addExperienceLevel(this.getLevels());
        player.giveExperiencePoints(this.getPoints());
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(AttachmentEvent.Render event) {
        if (!this.isEmpty()) {
            RenderUtils.drawString(event.stack, "", event.x, event.getY(), Color.WHITE.getRGB(), true);
//            event.addY(event.font.lineHeight + 2);

            int cmd_x = event.x + RenderUtils.width(event.renderSaveTo("info.inbox.xps")) + 2;
            event.renderItem(EXPERIENCE_BOTTLE, cmd_x, event.getY());
            if (event.canSee() && EmailUtils.isInRange(event.mouseX, event.mouseY, cmd_x, event.getY(), 16, 16)) {
                event.disableScissor();

                RenderUtils.drawComponentTooltip(event.stack, Arrays.asList(
                        new TranslationTextComponent("info.inbox.xp_save_to_email.0", this.getLevels()),
                        new TranslationTextComponent("info.inbox.xp_save_to_email.1", this.getPoints())
                ), event.mouseX, event.mouseY);

                event.enableScissor();
            }
            event.addY(16);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void getHeight(AttachmentEvent.GetHeight event) {
        if (!this.isEmpty()) {
            event.addHeight(16);
        }
    }
}
