package cat.jiu.email.element.attachment;

import cat.jiu.core.api.IData;
import cat.jiu.core.api.element.IOverlay;
import cat.jiu.core.util.Utils;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.AttachmentSendScreenWidget;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.email.net.msg.MsgUndying;
import cat.jiu.email.ui.gui.GuiInbox;
import cat.jiu.email.ui.gui.component.GuiFilterTextField;
import cat.jiu.email.util.TotemLike;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

@Mod.EventBusSubscriber
public class AttachmentUndying implements IAttachment {
    public static final ResourceLocation ID = Utils.location(EmailMain.MODID, "attachment/undying");
    public static final ItemStack TOTEM_UNDYING = new ItemStack(Items.TOTEM_OF_UNDYING);

    protected long count;

    public AttachmentUndying() {

    }
    public AttachmentUndying(long count) {
        this.count = count;
    }
    public AttachmentUndying(IData.IMapData<?> data) {
        this.read(data);
    }

    public AttachmentUndying addUndyingCount(long count) {
        this.count += count;
        return this;
    }
    public AttachmentUndying subUndyingCount(long count) {
        this.count -= count;
        return this;
    }

    public AttachmentUndying setUndyingCount(long count) {
        this.count = count;
        return this;
    }

    public long getUndyingCount() {
        return this.count;
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean isEmpty() {
        return this.getUndyingCount() <= 0;
    }

    @Override
    public String getName() {
        return "info.inbox.undying";
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentUndying && !other.isEmpty()) {
            this.addUndyingCount(((AttachmentUndying) other).getUndyingCount());
        }
    }

    @Override
    public void accept(Player player) {
        if (!this.isEmpty()) {
            if (player.getServer() != null) {
                UndyingState state = UndyingState.get(player.getServer());
                state.addCount((ServerPlayer) player, this.getUndyingCount());
            }
        }
    }

    @Override
    public boolean onPlayerSendCheck(Player player, Consumer<Component> msgHandler) {
        if (!this.isEmpty() && !player.isCreative() && player.getServer() != null) {
            long count = UndyingState.get(player.getServer()).getCount(player.getUUID());
            if (count < this.getUndyingCount()) {
                msgHandler.accept(Component.translatable("info.inbox.generate.attachment.undying.send.fail", this.getUndyingCount(), count));
                return false;
            }
        }
        return true;
    }

    @Override
    public void onPlayerSendChecked(Player player) {
        if (!this.isEmpty() && !player.isCreative() && player.getServer() != null) {
            UndyingState.get(player.getServer()).subCount((ServerPlayer) player, this.getUndyingCount());
        }
    }

    @Override
    public void read(IData.IMapData<?> data) {
        this.count = data.getInt("undyingCount");
    }

    @Override
    public IData.IMapData<?> write(IData.IMapData<?> data) {
        data.putData("undyingCount", this.count);
        return data;
    }

    @Override
    public List<Component> getHoverMessage() {
        return Collections.singletonList(
                Component.translatable("info.inbox.undying").append(String.format(" +%s", this.count))
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("info.inbox.undying");
    }

    @Override
    public ItemStack getDisplayStack() {
        return TOTEM_UNDYING;
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            LivingEntity entity = event.getEntity();
            UndyingState state = UndyingState.get(entity.getServer());
            if (state.undying.containsKey(entity.getStringUUID()) && state.getCount(entity.getUUID()) > 0) {

                entity.setHealth(1.0F);
                for (MobEffect effect : new HashSet<>(entity.getActiveEffectsMap().keySet())) {
                    if (effect.getCategory() == MobEffectCategory.HARMFUL) {
                        entity.removeEffect(effect);
                    }
                }
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 31));
                entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                entity.level().broadcastEntityEvent(entity, (byte)35);

//                entity.invulnerableTime = 0;
                state.subCount(event.getEntity().getUUID(), 1);
                if (event.getEntity() instanceof ServerPlayer) {
                    EmailMain.NETWORK.sendMessageToPlayer(new MsgUndying(state.getCount(entity.getUUID())), (ServerPlayer) event.getEntity());
                }

                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            Player player = event.getEntity();
            EmailMain.NETWORK.sendMessageToPlayer(
                    new MsgUndying(UndyingState.get(player.getServer()).getCount(player.getUUID())),
                    (ServerPlayer) player);
        }
    }

    @SubscribeEvent
    public static void onTotemUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer && EmailConfigServer.Totem_To_Undiying.get() && TotemLike.isTotemLike(event.getItemStack()) && event.getItemStack().getUseDuration() == 0) {
            UndyingState state = UndyingState.get(event.getEntity().getServer());
            state.addCount((ServerPlayer) event.getEntity(), 1);
            if(TotemLike.canShrinkTotemLike(event.getItemStack())) {
                TotemLike.shrinkTotemLike(event.getItemStack());
            }
        }
    }
    @SubscribeEvent
    public static void onTotemUseFinsh(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer && EmailConfigServer.Totem_To_Undiying.get() && TotemLike.isTotemLike(event.getItem())) {
            UndyingState state = UndyingState.get(event.getEntity().getServer());
            state.addCount((ServerPlayer) event.getEntity(), 1);
            TotemLike.checkAndShrinkTotemLike(event.getResultStack());
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class UndyingCountOverlay implements IOverlay {
        public static final UndyingCountOverlay INSTANCE = new UndyingCountOverlay();

        @Override
        public boolean render(ForgeGui hud, Window window, GuiGraphics graphics, float partialTicks, ResourceLocation overlay) {
            int x = window.getGuiScaledWidth() / 2 - 91;
            int y = window.getGuiScaledHeight() - hud.leftHeight;
            if (!Minecraft.getInstance().player.isCreative()) {
                y += Minecraft.getInstance().player.getArmorValue() > 0 ? 0 : 10;
            }

            for (int i = 0; i < Math.min(10, EmailAPI.getUndyingCount()); i++) {
              RenderUtils.draw(graphics, GuiInbox.ICON, x, y, 9, 9, 0, 9, 16, 16, 0);
                // icon from https://www.bilibili.com/video/BV1W2aizzEWi
//                RenderUtils.draw(graphics, GuiInbox.ICON, x, y, 9, 9, 0, 0, 9, 9, 0);
                x += 8;
            }
            if (EmailAPI.getUndyingCount() > 10) {
                String s = String.valueOf(EmailAPI.getUndyingCount()-10);
                int x1 = window.getGuiScaledWidth() / 2 - 91 + 5 * 8;
                RenderUtils.drawCenteredString(graphics, s, x1 + 1, y, 0, true);
                RenderUtils.drawCenteredString(graphics, s, x1 - 1, y, 0, true);
                RenderUtils.drawCenteredString(graphics, s, x1, y + 1, 0, true);
                RenderUtils.drawCenteredString(graphics, s, x1, y - 1, 0, true);
                RenderUtils.drawCenteredString(graphics, s, x1, y, 8453920, true);
            }
            if (!Minecraft.getInstance().player.isCreative()){
                hud.leftHeight -= 10;
            }
            return false;
        }

        @Override
        public boolean isEnable(ResourceLocation overlay, EventAction event) {
            if (event.isPostRenderEvent() && EmailAPI.getUndyingCount() > 0) {
                return VanillaGuiOverlay.ARMOR_LEVEL.id().equals(overlay) || Minecraft.getInstance().player.isCreative();
            }
            return false;
        }
    }

    public static class UndyingState extends SavedData {
        public static final String PATH = EmailMain.MODID + "/undying";
        private static UndyingState instance;
        public final HashMap<String, Long> undying = new HashMap<>();

        public UndyingState addCount(ServerPlayer player, long count) {
            this.addCount(player.getUUID(), count);
            EmailMain.NETWORK.sendMessageToPlayer(new MsgUndying(this.getCount(player.getUUID())), player);
            return this;
        }
        public UndyingState addCount(UUID uid, long count) {
            if (this.undying.containsKey(uid.toString())) {
                long c = this.undying.get(uid.toString()) + count;
                if (this.undying.get(uid.toString()) > 0 && count > 0 && c < 0) {
                    c = Integer.MAX_VALUE;
                }
                this.undying.put(uid.toString(), c);
            }else {
                this.undying.put(uid.toString(), count);
            }
            this.setDirty();
            return this;
        }

        public UndyingState subCount(ServerPlayer player, long count) {
            this.subCount(player.getUUID(), count);
            EmailMain.NETWORK.sendMessageToPlayer(new MsgUndying(this.getCount(player.getUUID())), player);
            return this;
        }
        public UndyingState subCount(UUID uid, long count) {
            if (this.undying.containsKey(uid.toString())) {
                long c = this.undying.get(uid.toString()) - count;
                if (c < 0) {
                    c = 0;
                }
                this.undying.put(uid.toString(), c);
                this.setDirty();
            }
            return this;
        }
        public long getCount(UUID uuid) {
            return this.undying.getOrDefault(uuid.toString(), 0L);
        }
        @Override
        public CompoundTag save(CompoundTag data) {
            this.undying.forEach(data::putLong);
            return data;
        }

        @Override
        public void save(File pFile) {
            pFile.getParentFile().mkdirs();
            super.save(pFile);
        }

        private static UndyingState fromNbt(CompoundTag data) {
            UndyingState state = new UndyingState();
            data.getAllKeys().forEach(key ->
                state.undying.put(key, data.getLong(key))
            );
            return state;
        }
        public static UndyingState get(MinecraftServer server) {
            if (instance == null) {
                DimensionDataStorage manager = Objects.requireNonNull(server, "not found server").overworld().getDataStorage();
                instance = manager.computeIfAbsent(UndyingState::fromNbt, UndyingState::new, PATH);
            }
            return instance;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Widget extends AttachmentSendScreenWidget {
        public static final Component COUNT_TEXT = Component.translatable("info.inbox.undying");
        public static final WidgetEntry INSTANCE = new WidgetEntry(ID, true, Widget::new);

        public final GuiFilterTextField countBox;
        private Widget() {
            super(Component.translatable("info.inbox.undying"));
            this.initSubWiget(true);

            int width = RenderUtils.width("0000000000000000000000000000");
            this.countBox = this.addSubWidget(
                    new GuiFilterTextField("0", false, 0, 0, width, RenderUtils.fontHeight() + 1).setCanBeNegative(true),
                    0, 0, 0, 0
            ).setWigetRender(null, widget->{
                RenderUtils.drawComponent(widget.graphics, COUNT_TEXT,
                    widget.widget.getX() + widget.widget.getWidth() + 2, widget.widget.getY(), Color.WHITE.getRGB(), true
                );
                return RenderUtils.width(COUNT_TEXT) + 1;
            }).cast();
            this.countBox.setMaxLength(Integer.MAX_VALUE);
        }

        @Override
        public IAttachment newAttachmentInstance() {
            return new AttachmentUndying().setUndyingCount(this.countBox.getAsNumber().longValue());
        }

        @Override
        public int getWidth() {
            return super.getWidth() + RenderUtils.width(COUNT_TEXT) + 1;
        }
    }
}
