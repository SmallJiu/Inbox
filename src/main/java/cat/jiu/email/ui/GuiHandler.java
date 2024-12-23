package cat.jiu.email.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.ui.gui.*;
import com.google.common.collect.Lists;

import cat.jiu.email.element.Cooling;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.net.msg.MsgOpenGui;
import cat.jiu.email.ui.container.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
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

public class GuiHandler {
	public static final DeferredRegister<MenuType<?>> MENU_TYPE_REGISTER = DeferredRegister.create(ForgeRegistries.MENU_TYPES, EmailMain.MODID);
	private static final Map<Integer, GuiConsumer<?>> GUIS = new HashMap<>();

//	public static final RegistryObject<MenuType<ContainerEmailMain>> main_TYPE;
	public static final RegistryObject<MenuType<ContainerEmailSend>> send_TYPE;
	public static final RegistryObject<MenuType<ContainerInboxBlacklist>> blacklist_TYPE;
	public static final RegistryObject<MenuType<ContainerEmailGenerate>> generate_TYPE;

	public static final int EMAIL_MAIN = 0;
	public static final int EMAIL_SEND = 1;
	public static final int EMAIL_BLACKLIST = 2;
	public static final int EMAIL_Generate = 3;
	public static final int EMAIL_Scheduled = 4;

	@OnlyIn(Dist.CLIENT)
	public static void registerScreen() {
		MenuScreens.<ContainerEmailSend, GuiEmailSend>register(GuiHandler.send_TYPE.get(), (container, inventory, title) -> new GuiEmailSend(container, inventory));
		MenuScreens.<ContainerInboxBlacklist, GuiBlacklist>register(GuiHandler.blacklist_TYPE.get(), (container, inventory, title) -> new GuiBlacklist(container, inventory));
//		MenuScreens.<ContainerEmailMain, GuiEmailMain>register(GuiHandler.main_TYPE.get(), (container, inventory, title) -> new GuiEmailMain(container, inventory));
		MenuScreens.<ContainerEmailGenerate, GuiEmailGenerate>register(GuiHandler.generate_TYPE.get(), (container, inventory, title) -> new GuiEmailGenerate(container, inventory));

		ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, ()->new ConfigScreenHandler.ConfigScreenFactory((mc, parent)->
				new cat.jiu.core.util.client.config.GuiConfig("/config/jiu/email.toml", parent, EmailConfigs.CONFIG_MAIN)
		));
	}

	static {
//		main_TYPE = register(EMAIL_MAIN, "email_main_container", new GuiConsumer<>(
//				(guiID, inventory, data) -> new ContainerEmailMain(guiID, inventory),
//				null
//		));

		send_TYPE = register(EMAIL_SEND, "email_send_container", new GuiConsumer<>(
				(guiID, inventory, data) -> {
					ContainerEmailSend container = new ContainerEmailSend(guiID, inventory);
					try {
						CompoundTag nbt = data.readNbt();
						if(nbt!=null && nbt.contains("cooling")){
							container.setCooling(nbt.getLong("cooling"));
						}
					}catch (Exception ignored){}
					return container;
				},
				(player, buffer) -> {
					if(Cooling.isCooling(player.getName().getString())){
						CompoundTag nbt = new CompoundTag();
						nbt.putLong("cooling", Cooling.getCoolingTimeMillis(player.getName().getString()));
						buffer.writeNbt(nbt);
					}
				}
		));

		blacklist_TYPE = register(EMAIL_BLACKLIST, "email_blacklist_container", new GuiConsumer<>(
				(guiID, inventory, data) -> {
					List<String> list = Lists.newArrayList();
					try {
						data.readNbt().getList("list", 8).forEach(e->list.add(e.getAsString()));
					}catch (Exception ignored){}
					return new ContainerInboxBlacklist(guiID, inventory, list);
				},
				(player, buffer) -> {
					ListTag list = new ListTag();
					Inbox.get(player).getSenderBlacklist().forEach(e -> list.add(StringTag.valueOf(e)));
					CompoundTag nbt = new CompoundTag();
					nbt.put("list", list);
					buffer.writeNbt(nbt);
				}
		));

		generate_TYPE = register(EMAIL_Generate, "email_generate_container", new GuiConsumer<>(
				(guiID, inventory, data) -> new ContainerEmailGenerate(guiID, inventory),
				null
		));
	}

	@OnlyIn(Dist.CLIENT)
	public static void openGui(int ID){
		if (ID == EMAIL_MAIN
//				&& EmailConfigs.Screen_Inbox_Gui.get()
		) {
			EmailAPI.openInbox();
		}else if (ID == EMAIL_Scheduled) {
			Minecraft.getInstance().setScreen(new GuiScheduledEmail());
		}else {
			EmailMain.net.sendMessageToServer(new MsgOpenGui(ID));
		}
	}

	public static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> register(int id, String name, GuiConsumer<T> supplier){
		if (!GUIS.containsKey(id)) {
			GUIS.put(id, supplier);
			return MENU_TYPE_REGISTER.register(name, ()->new MenuType<>(supplier.container, FeatureFlagSet.of()));
		}
		return null;
	}

	public static void openGui(int ID, ServerPlayer player) {
		if (GUIS.containsKey(ID) && GUIS.get(ID) != null) {
			GuiConsumer<?> consumer = GUIS.get(ID);
			NetworkHooks.openScreen(player,
					new SimpleMenuProvider(
							(guiID, inventory, p) -> consumer.container.create(guiID, inventory),
							Component.empty()
					),
					buffer -> {
						if (consumer.extraDataWriter != null){
							consumer.extraDataWriter.accept(player, buffer);
						}
					});
		}
	}

	public static class GuiConsumer<T extends AbstractContainerMenu> {
		private final IContainerFactory<T> container;
		private final BiConsumer<ServerPlayer, FriendlyByteBuf> extraDataWriter;

		public GuiConsumer(IContainerFactory<T> container, BiConsumer<ServerPlayer, FriendlyByteBuf> extraDataWriter) {
			this.container = container;
			this.extraDataWriter = extraDataWriter;
		}
	}
}
