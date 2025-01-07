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
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class GuiHandler {
	public static final DeferredRegister<ContainerType<?>> MENU_TYPE_REGISTER = DeferredRegister.create(ForgeRegistries.CONTAINERS, EmailMain.MODID);
	private static final Map<Integer, GuiConsumer<?>> GUIS = new HashMap<>();

//	public static final RegistryObject<ContainerType<ContainerEmailMain>> main_TYPE;
	public static final RegistryObject<ContainerType<ContainerEmailSend>> send_TYPE;
	public static final RegistryObject<ContainerType<ContainerInboxBlacklist>> blacklist_TYPE;
	public static final RegistryObject<ContainerType<ContainerEmailGenerate>> generate_TYPE;

	public static final int EMAIL_MAIN = 0;
	public static final int EMAIL_SEND = 1;
	public static final int EMAIL_BLACKLIST = 2;
	public static final int EMAIL_Generate = 3;
	public static final int EMAIL_Scheduled = 4;

	@OnlyIn(Dist.CLIENT)
	public static void registerScreen() {
		ScreenManager.<ContainerEmailSend, GuiEmailSend>registerFactory(GuiHandler.send_TYPE.get(), (container, inventory, title) -> new GuiEmailSend(container, inventory));
		ScreenManager.<ContainerInboxBlacklist, GuiBlacklist>registerFactory(GuiHandler.blacklist_TYPE.get(), (container, inventory, title) -> new GuiBlacklist(container, inventory));
//		MenuScreens.<ContainerEmailMain, GuiEmailMain>register(GuiHandler.main_TYPE.get(), (container, inventory, title) -> new GuiEmailMain(container, inventory));
		ScreenManager.<ContainerEmailGenerate, GuiEmailGenerate>registerFactory(GuiHandler.generate_TYPE.get(), (container, inventory, title) -> new GuiEmailGenerate(container, inventory));

		ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, ()->(mc, parent)->
				new cat.jiu.core.util.client.config.GuiConfig("/config/jiu/inbox/configs.toml", parent, EmailConfigs.CONFIG_MAIN)
		);
	}

	static {
		send_TYPE = register(EMAIL_SEND, "email_send_container", new GuiConsumer<>(
				(guiID, inventory, data) -> {
					ContainerEmailSend container = new ContainerEmailSend(guiID, inventory);
					try {
						CompoundNBT nbt = data.readCompoundTag();
						if(nbt!=null && nbt.contains("cooling")){
							container.setCooling(nbt.getLong("cooling"));
						}
					}catch (Exception ignored){}
					return container;
				},
				(player, buffer) -> {
					if(Cooling.isCooling(player.getName().getString())){
						CompoundNBT nbt = new CompoundNBT();
						nbt.putLong("cooling", Cooling.getCoolingTimeMillis(player.getName().getString()));
						buffer.writeCompoundTag(nbt);
					}
				}
		));

		blacklist_TYPE = register(EMAIL_BLACKLIST, "email_blacklist_container", new GuiConsumer<>(
				(guiID, inventory, data) -> {
					List<String> list = Lists.newArrayList();
					try {
						data.readCompoundTag().getList("list", 8).forEach(e->list.add(e.getString()));
					}catch (Exception ignored){}
					return new ContainerInboxBlacklist(guiID, inventory, list);
				},
				(player, buffer) -> {
					ListNBT list = new ListNBT();
					Inbox.get(player).getSenderBlacklist().forEach(e -> list.add(StringNBT.valueOf(e)));
					CompoundNBT nbt = new CompoundNBT();
					nbt.put("list", list);
					buffer.writeCompoundTag(nbt);
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
			Minecraft.getInstance().displayGuiScreen(new GuiScheduledEmail());
		}else {
			EmailMain.net.sendMessageToServer(new MsgOpenGui(ID));
		}
	}

	public static <T extends Container> RegistryObject<ContainerType<T>> register(int id, String name, GuiConsumer<T> supplier){
		if (!GUIS.containsKey(id)) {
			GUIS.put(id, supplier);
			return MENU_TYPE_REGISTER.register(name, ()->new ContainerType<>(supplier.container));
		}
		return null;
	}

	public static void openGui(int ID, ServerPlayerEntity player) {
		if (GUIS.containsKey(ID) && GUIS.get(ID) != null) {
			GuiConsumer<?> consumer = GUIS.get(ID);
			NetworkHooks.openGui(player,
					new SimpleNamedContainerProvider(
							(guiID, inventory, p) -> consumer.container.create(guiID, inventory),
							ITextComponent.getTextComponentOrEmpty(null)
					),
					buffer -> {
						if (consumer.extraDataWriter != null){
							consumer.extraDataWriter.accept(player, buffer);
						}
					});
		}
	}

	public static class GuiConsumer<T extends Container> {
		private final IContainerFactory<T> container;
		private final BiConsumer<ServerPlayerEntity, PacketBuffer> extraDataWriter;

		public GuiConsumer(IContainerFactory<T> container, BiConsumer<ServerPlayerEntity, PacketBuffer> extraDataWriter) {
			this.container = container;
			this.extraDataWriter = extraDataWriter;
		}
	}
}
