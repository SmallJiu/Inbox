package cat.jiu.email.ui;

import javax.annotation.Nullable;
import java.util.List;

import cat.jiu.email.element.Cooling;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.SizeReport;
import com.google.common.collect.Lists;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.net.msg.MsgOpenGui;
import cat.jiu.email.ui.container.*;
import cat.jiu.email.ui.gui.GuiBlacklist;
import cat.jiu.email.ui.gui.GuiEmailMain;
import cat.jiu.email.ui.gui.GuiEmailSend;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("all")
public class GuiHandler {
	public static final DeferredRegister<MenuType<?>> MENU_TYPE_REGISTER = DeferredRegister.create(ForgeRegistries.MENU_TYPES, EmailMain.MODID);
	private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerType(String id, IContainerFactory<T> factory) {
		return MENU_TYPE_REGISTER.register(id, ()->new MenuType<>(factory, FeatureFlagSet.of()));
	}

	public static final RegistryObject<MenuType<ContainerInboxBlacklist>> blacklist_TYPE = registerType("email_blacklist_container", (windowId, inv, data) -> {
		List<String> list = Lists.newArrayList();
		data.readNbt().getList("list", 8).forEach(e->list.add(e.getAsString()));
		return new ContainerInboxBlacklist(windowId, inv, list);
	});
	public static final RegistryObject<MenuType<ContainerEmailSend>> send_TYPE = registerType("email_send_container", (windowId, inv, data) -> {
		ContainerEmailSend container = new ContainerEmailSend(windowId, inv);
		try {
			CompoundTag nbt = data.readNbt();
			if(nbt!=null && nbt.contains("cooling")){
				container.setCooling(nbt.getLong("cooling"));
			}
		}catch (Exception ignored){}
		return container;
	});
	public static final RegistryObject<MenuType<ContainerEmailMain>> main_TYPE = registerType("email_main_container", (windowId, inv, data) ->
			new ContainerEmailMain(windowId, inv, Inbox.get(inv.player.getUUID(), data.readNbt()))
	);

	@OnlyIn(Dist.CLIENT)
	public static void registerScreen() {
		MenuScreens.<ContainerEmailSend, GuiEmailSend>register(GuiHandler.send_TYPE.get(), (container, inventory, title) -> new GuiEmailSend(container, inventory));
		MenuScreens.<ContainerInboxBlacklist, GuiBlacklist>register(GuiHandler.blacklist_TYPE.get(), (container, inventory, title) -> new GuiBlacklist(container, inventory));
		MenuScreens.<ContainerEmailMain, GuiEmailMain>register(GuiHandler.main_TYPE.get(), (container, inventory, title) -> new GuiEmailMain(container, inventory));

		ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, ()->new ConfigScreenHandler.ConfigScreenFactory((mc, parent)->
				new cat.jiu.email.ui.gui.config.GuiConfig("/config/jiu/email.toml", parent, EmailConfigs.CONFIG_MAIN)
		));
	}



	public static final int EMAIL_MAIN = 0;
	public static final int EMAIL_SEND = 1;
	public static final int EMAIL_BLACKLIST = 2;

	@OnlyIn(Dist.CLIENT)
	public static void openGui(int ID){
		EmailMain.net.sendMessageToServer(new MsgOpenGui(ID));
	}

	public static void openGui(int ID, ServerPlayer player) {
		try {
			NetworkHooks.openScreen(player, new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.empty();
				}
				@Nullable
				@Override
				public AbstractContainerMenu createMenu(int windowID, Inventory inventory, Player player) {
					switch (ID) {
						case EMAIL_MAIN: return new ContainerEmailMain(windowID, inventory, Inbox.get(player));
						case EMAIL_SEND: return new ContainerEmailSend(windowID, inventory);
						case EMAIL_BLACKLIST: return new ContainerInboxBlacklist(windowID, inventory, Inbox.get(player).getSenderBlacklist());
						default: return null;
					}
				}
			}, buffer -> {
				if (ID == EMAIL_MAIN) {
					Inbox inbox = Inbox.get(player);
					if(!EmailConfigs.isInfiniteSize()){
						SizeReport report = EmailUtils.checkInboxSize(inbox);
						if(!SizeReport.SUCCESS.equals(report)) {
							player.sendSystemMessage(Component.translatable("info.email.error.to_big.0"));
							player.sendSystemMessage(Component.translatable("info.email.error.to_big.1", report.id(), report.slot(), report.size()));
							throw new SizeReport.ToBigException();
						}else {
							buffer.writeNbt(inbox.writeTo(CompoundTag.class));
						}
					}else {
						buffer.writeNbt(inbox.writeTo(CompoundTag.class));
					}
				} else if (ID == EMAIL_BLACKLIST) {
					ListTag list = new ListTag();
					Inbox.get(player).getSenderBlacklist().forEach(e -> list.add(StringTag.valueOf(e)));
					CompoundTag nbt = new CompoundTag();
					nbt.put("list", list);
					buffer.writeNbt(nbt);
				}else if(ID == EMAIL_SEND){
					if(Cooling.isCooling(player.getName().getString())){
						CompoundTag nbt = new CompoundTag();
						nbt.putLong("cooling", Cooling.getCoolingTimeMillis(player.getName().getString()));
						buffer.writeNbt(nbt);
					}
				}
			});
		}catch (SizeReport.ToBigException e){}
	}
}
