package cat.jiu.email.element.attachment;

import cat.jiu.core.api.IData;
import cat.jiu.core.api.element.IOverlay;
import cat.jiu.core.util.Utils;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.configs.EmailConfigClient;
import cat.jiu.email.net.msg.MsgUndying;
import cat.jiu.email.util.RenderCorner;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

@Mod.EventBusSubscriber
public class AttachmentUndying implements IAttachment {
    public static final ResourceLocation ID = Utils.location(EmailMain.MODID, "attachment/undying");
    public static final ItemStack TOTEM_UNDYING = new ItemStack(Items.TOTEM_OF_UNDYING);
    protected int count;

    public AttachmentUndying(int count) {
        this.count = count;
    }
    public AttachmentUndying(IData.IMapData<?> data) {
        this.read(data);
    }

    public AttachmentUndying addUndyingCount(int count) {
        this.count += count;
        return this;
    }
    public AttachmentUndying subUndyingCount(int count) {
        this.count -= count;
        return this;
    }
    public int getUndyingCount() {
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
    public void merge(IAttachment other) {
        if (other instanceof AttachmentUndying && !other.isEmpty()) {
            this.addUndyingCount(((AttachmentUndying) other).getUndyingCount());
        }
    }

    @Override
    public void accept(Player player) {
        if (!this.isEmpty() && player.getServer() != null) {
            UndyingState state = UndyingState.get(player.getServer());
            state.addCount(player.getUUID(), this.getUndyingCount());
            if (player instanceof ServerPlayer) {
                EmailMain.NETWORK.sendMessageToPlayer(
                        new MsgUndying(state.getCount(player.getUUID())),
                        (ServerPlayer) player);
            }
            EmailAPI.setUndyingCount(state.getCount(player.getUUID()));
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
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            UndyingState state = UndyingState.get(player.getServer());
            if (state.undying.containsKey(player.getStringUUID()) && state.getCount(player.getUUID()) > 0) {

                player.setHealth(1.0F);
                player.removeAllEffects();
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
                player.level().broadcastEntityEvent(player, (byte)35);

                state.subCount(player.getUUID(), 1);
                if (player instanceof ServerPlayer) {
                    EmailMain.NETWORK.sendMessageToPlayer(
                            new MsgUndying(state.getCount(player.getUUID())),
                            (ServerPlayer) player);
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

    @OnlyIn(Dist.CLIENT)
    public static class UndyingCountOverlay implements IOverlay {
        public static final UndyingCountOverlay INSTANCE = new UndyingCountOverlay();
        @Override
        public boolean render(GuiGraphics graphics, RenderGuiEvent event, int windowCenterX, int windowCenterY) {
            RenderCorner corner = EmailConfigClient.Undying_Count_Render_Side.get();
            Component component = Component.translatable("info.inbox.undying").append(String.format(": %s", EmailAPI.getUndyingCount()));
            int x = corner.getX();
            if (x == event.getWindow().getGuiScaledWidth()) {
                x -= RenderUtils.width(component);
            }
            int y = corner.getY();
            if (y == event.getWindow().getGuiScaledHeight()) {
                y -= RenderUtils.fontHeight();
            }
            RenderUtils.drawComponent(graphics,
                    component,
                    x, y, Color.WHITE.getRGB(), true
            );
            return false;
        }

        @Override
        public boolean isEnable(EventAction event) {
            return event.isPostRenderEvent() && EmailAPI.getUndyingCount() > 0;
        }
    }

    public static class UndyingState extends SavedData {
        public static final String PATH = EmailMain.MODID + "/undying";
        public final HashMap<String, Long> undying = new HashMap<>();

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
        public UndyingState subCount(UUID uid, int count) {
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
            DimensionDataStorage manager = Objects.requireNonNull(server, "not found server").overworld().getDataStorage();
            return manager.computeIfAbsent(UndyingState::fromNbt, UndyingState::new, PATH);
        }
    }
}
