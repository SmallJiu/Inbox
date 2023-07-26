package cat.jiu.email.ui;

import javax.annotation.Nullable;
import java.util.List;

import cat.jiu.email.element.Cooling;
import com.google.common.collect.Lists;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.net.msg.MsgOpenGui;
import cat.jiu.email.ui.container.*;
import cat.jiu.email.ui.gui.GuiBlacklist;
import cat.jiu.email.ui.gui.GuiEmailMain;
import cat.jiu.email.ui.gui.GuiEmailSend;

import net.minecraft.client.gui.ScreenManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@SuppressWarnings("ALL")
public class GuiHandler {
	public static final DeferredRegister<ContainerType<?>> CONTAINER_TYPE_REGISTER = DeferredRegister.create(ForgeRegistries.CONTAINERS, EmailMain.MODID);
	public static final RegistryObject<ContainerType<ContainerInboxBlacklist>> blacklist_TYPE = registerType("email_blacklist_container", (windowId, inv, data) -> {
		List<String> list = Lists.newArrayList();
		data.readCompoundTag().getList("list", 8).forEach(e->list.add(e.getString()));
		return new ContainerInboxBlacklist(windowId, inv, list);
	});
	public static final RegistryObject<ContainerType<ContainerEmailSend>> send_TYPE = registerType("email_send_container", (windowId, inv, data) -> {
		ContainerEmailSend container = new ContainerEmailSend(windowId, inv);
		try {
			CompoundNBT nbt = data.readCompoundTag();
			if(nbt!=null && nbt.contains("cooling")){
				container.setCooling(nbt.getLong("cooling"));
			}
		}catch (Exception ignored){}
		return container;
	});
	public static final RegistryObject<ContainerType<ContainerEmailMain>> main_TYPE = registerType("email_main_container", (windowId, inv, data) ->
			new ContainerEmailMain(windowId, inv, Inbox.get(inv.player.getUniqueID(), data.readCompoundTag()))
	);
	private static <T extends Container> RegistryObject<ContainerType<T>> registerType(String id, IContainerFactory<T> factory) {
		return CONTAINER_TYPE_REGISTER.register(id, ()->IForgeContainerType.create(factory));
	}

	@OnlyIn(Dist.CLIENT)
	public static void registerScreen() {
		ScreenManager.<ContainerEmailSend, GuiEmailSend>registerFactory(GuiHandler.send_TYPE.get(), (container, inventory, title) -> new GuiEmailSend(container, inventory));
		ScreenManager.<ContainerInboxBlacklist, GuiBlacklist>registerFactory(GuiHandler.blacklist_TYPE.get(), (container, inventory, title) -> new GuiBlacklist(container, inventory));
		ScreenManager.<ContainerEmailMain, GuiEmailMain>registerFactory(GuiHandler.main_TYPE.get(), (container, inventory, title) -> new GuiEmailMain(container, inventory));
	}



	public static final int EMAIL_MAIN = 0;
	public static final int EMAIL_SEND = 1;
	public static final int EMAIL_BLACKLIST = 2;

	@OnlyIn(Dist.CLIENT)
	public static void openGui(int ID){
		EmailMain.net.sendMessageToServer(new MsgOpenGui(ID));
	}

	public static void openGui(int ID, ServerPlayerEntity player) {
		NetworkHooks.openGui(player, new INamedContainerProvider() {
			@Override
			public ITextComponent getDisplayName() {
				return StringTextComponent.EMPTY;
			}
			@Nullable
			@Override
			public Container createMenu(int windowID, PlayerInventory inventory, PlayerEntity player) {
				switch (ID) {
					case EMAIL_MAIN: return new ContainerEmailMain(windowID, inventory, Inbox.get(player));
					case EMAIL_SEND: return new ContainerEmailSend(windowID, inventory);
					case EMAIL_BLACKLIST: return new ContainerInboxBlacklist(windowID, inventory, Inbox.get(player).getSenderBlacklist());
					default: return null;
				}
			}
		}, buffer -> {
			if (ID == EMAIL_MAIN) {
				buffer.writeCompoundTag(Inbox.get(player).writeTo(CompoundNBT.class));
			} else if (ID == EMAIL_BLACKLIST) {
				ListNBT list = new ListNBT();
				Inbox.get(player).getSenderBlacklist().forEach(e -> list.add(StringNBT.valueOf(e)));
				CompoundNBT nbt = new CompoundNBT();
				nbt.put("list", list);
				buffer.writeCompoundTag(nbt);
			}else if(ID == EMAIL_SEND){
				if(Cooling.isCooling(player.getName().getString())){
					CompoundNBT nbt = new CompoundNBT();
					nbt.putLong("cooling", Cooling.getCoolingTimeMillis(player.getName().getString()));
					buffer.writeCompoundTag(nbt);
				}
			}
		});
	}
}
