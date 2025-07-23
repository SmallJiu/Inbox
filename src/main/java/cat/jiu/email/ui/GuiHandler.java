package cat.jiu.email.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.ui.gui.*;
import cat.jiu.email.ui.gui.component.AttachmentInboxIcon;
import com.google.common.collect.Lists;

import cat.jiu.email.element.Cooling;
import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.MsgOpenGui;
import cat.jiu.email.ui.container.*;

import com.tterrag.registrate.builders.MenuBuilder;
import com.tterrag.registrate.util.entry.MenuEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class GuiHandler {
	private static final Map<Integer, GuiConsumer<? extends AbstractContainerMenu>> GUIS = new HashMap<>();

	public static final int
			EMAIL_MAIN = 0,
			EMAIL_Scheduled = 1,
			SEND = 2,
			BLACKLIST = 3,
			Generate = 4
	;
	public static final GuiConsumer<ContainerEmailSend> send_TYPE = GuiHandler.<ContainerEmailSend, GuiEmailSend>register(SEND, "inbox_send", (type, guiID, inventory, buffer)->{
				ContainerEmailSend container = new ContainerEmailSend(guiID, inventory);
				try {
					CompoundTag nbt = buffer.readNbt();
					if(nbt!=null && nbt.contains("cooling")){
						container.setCooling(nbt.getLong("cooling"));
					}
				}catch (Exception ignored){}
				return container;
			}, (container, inventory, name)->
					new GuiEmailSend(container, inventory)
			, (player, buffer)->{
				if(Cooling.isCooling(player.getName().getString())){
					CompoundTag nbt = new CompoundTag();
					nbt.putLong("cooling", Cooling.getCoolingTimeMillis(player.getName().getString()));
					buffer.writeNbt(nbt);
				}
	});
	public static final GuiConsumer<ContainerInboxBlacklist> blacklist_TYPE = GuiHandler.<ContainerInboxBlacklist, GuiBlacklist>register(BLACKLIST, "inbox_blacklist", (type, windowId, inventory, buffer)->{
				List<String> list = Lists.newArrayList();
				try {
					buffer.readNbt().getList("list", 8).forEach(e->list.add(e.getAsString()));
				}catch (Exception ignored){}
				return new ContainerInboxBlacklist(windowId, inventory, list);
			}, (menu, inventory, name)->
					new GuiBlacklist(menu, inventory)
			, (player, buffer) -> {
	});
	public static final GuiConsumer<ContainerEmailGenerate> generate_TYPE = GuiHandler.<ContainerEmailGenerate, GuiEmailGenerate>register(Generate, "email_generate_container",
			(type, windowId, inventory, buffer)-> new ContainerEmailGenerate(windowId, inventory),
			(container, inventory, displayName) -> new GuiEmailGenerate(container, inventory),
			null
	);

	@OnlyIn(Dist.CLIENT)
	public static void registerScreen() {
		EmailMain.container().registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

		if (ModList.get().isLoaded("attributeslib")) {
			AttachmentInboxIcon.getIconType();
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static void openGui(int ID){
		if (ID == EMAIL_MAIN) {
			EmailAPI.openInbox();
		}else if (ID == EMAIL_Scheduled) {
			Minecraft.getInstance().setScreen(new GuiScheduledEmail());
		}else {
			EmailMain.net.sendMessageToServer(new MsgOpenGui(ID));
		}
	}

	public static void openGui(int id, ServerPlayer player) {
		if (GUIS.containsKey(id)) {
			GUIS.get(id).open(player);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends AbstractContainerMenu, SC extends Screen & MenuAccess<T>> GuiConsumer<T> register(int id, String name, MenuBuilder.ForgeMenuFactory<T> factory, MenuBuilder.ScreenFactory<T, SC> screenFactory, BiConsumer<ServerPlayer, RegistryFriendlyByteBuf> extraData){
		if (!GUIS.containsKey(id)) {
			GuiConsumer<T> entry = new GuiConsumer<>(EmailMain.registrate().object(name).menu(
							name, factory, () -> screenFactory
					).register(), extraData);
			GUIS.put(id, entry);
			return entry;
		}
		return (GuiConsumer<T>) GUIS.get(id);
	}

	public static class GuiConsumer<T extends AbstractContainerMenu> {
		public final MenuEntry<T> menuEntry;
		public final BiConsumer<ServerPlayer, RegistryFriendlyByteBuf> extraData;
		public GuiConsumer(MenuEntry<T> menuEntry, BiConsumer<ServerPlayer, RegistryFriendlyByteBuf> extraData) {
			this.menuEntry = menuEntry;
			this.extraData = extraData;
		}
		public MenuType<T> get() {
			return this.menuEntry.get();
		}
		public void open(ServerPlayer player) {
			this.open(player, CommonComponents.EMPTY);
		}
		public void open(ServerPlayer player, Component name) {
			if (this.extraData!=null) {
				this.menuEntry.open(player, name, buffer -> this.extraData.accept(player, buffer));
			}else {
				this.menuEntry.open(player, name);
			}
		}
	}
}
