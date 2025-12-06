package cat.jiu.email.element.attachment;

import cat.jiu.core.api.IData;
import cat.jiu.core.util.Utils;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.AttachmentSendScreenWidget;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.ui.gui.component.GuiFilterTextField;
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
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

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
    public boolean isEmpty() {
        return this.getLevels() == 0 && this.getPoints() == 0;
    }

    @Override
    public String getName() {
        return "info.inbox.xps";
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentXP attachment && !attachment.isEmpty()) {
            this.addLevels(attachment.getLevels());
            this.addPoints(attachment.getPoints());
        }
    }
    @Override
    public void accept(Player player) {
        player.giveExperienceLevels(this.getLevels());
        player.giveExperiencePoints(this.getPoints());
    }

    @Override
    public boolean onPlayerSendCheck(Player player, Consumer<Component> msgHandler) {
        if (!player.isCreative()){
            if (player.experienceLevel < this.getLevels()) {
                msgHandler.accept(Component.translatable("info.inbox.generate.attachment.exp.level.send.fail", this.getLevels(), player.experienceLevel));
                return false;
            }
            if (player.totalExperience < this.getPoints()) {
                msgHandler.accept(Component.translatable("info.inbox.generate.attachment.exp.point.send.fail", this.getPoints(), player.totalExperience));
                return false;
            }
        }
        return true;
    }

    @Override
    public void onPlayerSendChecked(Player player) {
        player.giveExperiencePoints(-this.getPoints());
        player.giveExperienceLevels(-this.getLevels());
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

    @OnlyIn(Dist.CLIENT)
    public static class Widget extends AttachmentSendScreenWidget {
        public static final Component
                LEVEL_TEXT = Component.translatable("info.inbox.xp.level").append(","),
                POINT_TEXT = Component.translatable("info.inbox.xp.point");
        public static final WidgetEntry INSTANCE = new WidgetEntry(ID, true, Widget::new);

        public final GuiFilterTextField levelBox, pointBox;
        private Widget() {
            super(Component.translatable("info.inbox.xps"));
            this.initSubWiget(true);

            int width = RenderUtils.width("000000000000");
            this.levelBox = this.addSubWidget(
                    new GuiFilterTextField("0", false, 0, 0, width, RenderUtils.fontHeight() + 1),
                    0, 0, 0, 0
            ).setWigetRender(null, widget->{
                RenderUtils.drawComponent(widget.graphics, LEVEL_TEXT,
                        widget.widget.getX() + widget.widget.getWidth() + 2, widget.widget.getY(), Color.WHITE.getRGB(), true
                );
                return RenderUtils.width(LEVEL_TEXT) + 8;
            }).cast();
            this.levelBox.setMaxLength(100);

            this.pointBox = this.addSubWidget(
                    new GuiFilterTextField("0", false, 0, 0, width, RenderUtils.fontHeight() + 1),
                    0, 0, 0, 0
            ).setWigetRender(null, widget->{
                RenderUtils.drawComponent(widget.graphics, POINT_TEXT,
                        widget.widget.getX() + widget.widget.getWidth() + 2, widget.widget.getY(), Color.WHITE.getRGB(), true
                );
                return RenderUtils.width(POINT_TEXT) + 1;
            }).cast();
            this.pointBox.setMaxLength(100);
        }

        @Override
        public int getWidth() {
            return super.getWidth() + RenderUtils.width(LEVEL_TEXT) + 8 + RenderUtils.width(POINT_TEXT) + 1;
        }

        @Override
        public IAttachment newAttachmentInstance() {
            return new AttachmentXP()
                    .setLevels(this.levelBox.getAsNumber().intValue())
                    .setPoints(this.pointBox.getAsNumber().intValue());
        }
    }
}
