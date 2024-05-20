package cat.jiu.email.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.google.common.collect.Lists;

import cat.jiu.email.element.Cooling;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.SizeReport;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.net.msg.MsgOpenGui;
import cat.jiu.email.ui.container.*;
import cat.jiu.email.ui.gui.*;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.inventory.MenuType;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class GuiHandler {
	public static final DeferredRegister<MenuType<?>> MENU_TYPE_REGISTER = DeferredRegister.create(ForgeRegistries.CONTAINERS, EmailMain.MODID);
	private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerType(String id, IContainerFactory<T> factory) {
		return MENU_TYPE_REGISTER.register(id, ()->new MenuType<>(factory));
	}

	public static final RegistryObject<MenuType<ContainerInboxBlacklist>> blacklist_TYPE = registerType("email_blacklist_container", (guiID, inv, data) -> {
		List<String> list = Lists.newArrayList();
		data.readNbt().getList("list", 8).forEach(e->list.add(e.getAsString()));
		return new ContainerInboxBlacklist(guiID, inv, list);
	});
	public static final RegistryObject<MenuType<ContainerEmailSend>> send_TYPE = registerType("email_send_container", (guiID, inv, data) -> {
		ContainerEmailSend container = new ContainerEmailSend(guiID, inv);
		try {
			CompoundTag nbt = data.readNbt();
			if(nbt!=null && nbt.contains("cooling")){
				container.setCooling(nbt.getLong("cooling"));
			}
		}catch (Exception ignored){}
		return container;
	});
	public static final RegistryObject<MenuType<ContainerEmailMain>> main_TYPE = registerType("email_main_container", (guiID, inv, data) ->
			new ContainerEmailMain(guiID, inv, Inbox.get(inv.player.getUUID(), data.readNbt()))
	);
	public static final RegistryObject<MenuType<ContainerEmailGenerate>> generate_TYPE = registerType("email_generate_container", (guiID, inv, data) ->
			new ContainerEmailGenerate(guiID, inv)
	);

	public static final int EMAIL_MAIN = 0;
	public static final int EMAIL_SEND = 1;
	public static final int EMAIL_BLACKLIST = 2;
	public static final int EMAIL_Generate = 3;
	private static final Map<Integer, GuiConsumer> GUIS = new HashMap<>();

	@OnlyIn(Dist.CLIENT)
	public static void registerScreen() {
		MenuScreens.<ContainerEmailSend, GuiEmailSend>register(GuiHandler.send_TYPE.get(), (container, inventory, title) -> new GuiEmailSend(container, inventory));
		MenuScreens.<ContainerInboxBlacklist, GuiBlacklist>register(GuiHandler.blacklist_TYPE.get(), (container, inventory, title) -> new GuiBlacklist(container, inventory));
		MenuScreens.<ContainerEmailMain, GuiEmailMain>register(GuiHandler.main_TYPE.get(), (container, inventory, title) -> new GuiEmailMain(container, inventory));
		MenuScreens.<ContainerEmailGenerate, GuiEmailGenerate>register(GuiHandler.generate_TYPE.get(), (container, inventory, title) -> new GuiEmailGenerate(container, inventory));

		ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class, ()->new ConfigGuiHandler.ConfigGuiFactory((mc, parent)->
				new cat.jiu.email.ui.gui.config.GuiConfig("/config/jiu/email.toml", parent, EmailConfigs.CONFIG_MAIN)
		));
	}

	static {
		register(EMAIL_MAIN, new GuiConsumer(
				(guiID, inventory, player) -> new ContainerEmailMain(guiID, inventory, Inbox.get(player)),
				(player, buffer) ->{
					Inbox inbox = Inbox.get(player);
					if(!EmailConfigs.isInfiniteSize()){
						SizeReport report = EmailUtils.checkInboxSize(inbox);
						if(!SizeReport.SUCCESS.equals(report)) {
							player.displayClientMessage(new TranslatableComponent("info.email.error.to_big.0"), false);
							player.displayClientMessage(new TranslatableComponent("info.email.error.to_big.1", report.id(), report.slot(), report.size()), false);
							throw new SizeReport.ToBigException();
						}else {
							buffer.writeNbt(inbox.writeTo(CompoundTag.class));
						}
					}else {
						buffer.writeNbt(inbox.writeTo(CompoundTag.class));
					}
				}
		));

		register(EMAIL_SEND, new GuiConsumer(
				(guiID, inventory, player) -> new ContainerEmailSend(guiID, inventory),
				(player, buffer) -> {
					if(Cooling.isCooling(player.getName().getString())){
						CompoundTag nbt = new CompoundTag();
						nbt.putLong("cooling", Cooling.getCoolingTimeMillis(player.getName().getString()));
						buffer.writeNbt(nbt);
					}
				}
		));

		register(EMAIL_BLACKLIST, new GuiConsumer(
				(guiID, inventory, player) -> new ContainerInboxBlacklist(guiID, inventory, Inbox.get(player).getSenderBlacklist()),
				(player, buffer) -> {
					ListTag list = new ListTag();
					Inbox.get(player).getSenderBlacklist().forEach(e -> list.add(StringTag.valueOf(e)));
					CompoundTag nbt = new CompoundTag();
					nbt.put("list", list);
					buffer.writeNbt(nbt);
				}
		));
		register(EMAIL_Generate, new GuiConsumer(
				(guiID, inventory, player) -> new ContainerEmailGenerate(guiID, inventory),
				(player, buffer) -> {}
		));
	}


	@OnlyIn(Dist.CLIENT)
	public static void openGui(int ID){
		EmailMain.net.sendMessageToServer(new MsgOpenGui(ID));
	}

	public static boolean register(int id, GuiConsumer supplier){
		if (!GUIS.containsKey(id)) {
			GUIS.put(id, supplier);
			return true;
		}
		return false;
	}

	public static void openGui(int ID, ServerPlayer player) {
		try {
			NetworkHooks.openGui(player,
					new SimpleMenuProvider(
							(guiID, inventory, p) -> GUIS.containsKey(ID) && GUIS.get(ID) != null ? GUIS.get(ID).container.createMenu(guiID, inventory, player) : null,
							Component.nullToEmpty(null)
					),
					buffer -> {
						if (GUIS.containsKey(ID) && GUIS.get(ID) != null){
							GUIS.get(ID).extraDataWriter.accept(player, buffer);
						}
			});
		}catch (SizeReport.ToBigException ignored){}
	}

	public static record GuiConsumer(MenuConstructor container, BiConsumer<ServerPlayer, FriendlyByteBuf> extraDataWriter) { }
}
