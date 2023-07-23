package cat.jiu.email.ui;

import cat.jiu.email.element.Inbox;
import cat.jiu.email.ui.container.*;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

public class EmailGuiHandler {
	public static final int EMAIL_MAIN = 0;
	public static final int EMAIL_SEND = 1;
	public static final int EMAIL_BLACKLIST = 2;

	public static void openGui(int ID, ServerPlayerEntity player) {
		NetworkHooks.openGui(player, new INamedContainerProvider() {
			@Override
			public ITextComponent getDisplayName() {
				return ITextComponent.getTextComponentOrEmpty(null);
			}
			@Nullable
			@Override
			public Container createMenu(int windowID, PlayerInventory inventory, PlayerEntity player) {
				return getContainer(ID, windowID, inventory, player);
			}
		}, packetBuffer -> {
			if(ID == EMAIL_MAIN){
				packetBuffer.writeCompoundTag(Inbox.get(player).writeTo(CompoundNBT.class));
			}else if(ID == EMAIL_BLACKLIST){
				ListNBT list = new ListNBT();
				Inbox.get(player).getSenderBlacklist().forEach(e->list.add(StringNBT.valueOf(e)));
				CompoundNBT nbt = new CompoundNBT();
				nbt.put("list", list);
				packetBuffer.writeCompoundTag(nbt);
			}
		});
	}

	static Container getContainer(int ID, int windowID, PlayerInventory inventory, PlayerEntity player){
		switch (ID) {
			case EMAIL_MAIN: return new ContainerEmailMain(windowID, inventory, Inbox.get(player));
			case EMAIL_SEND: return new ContainerEmailSend(windowID, inventory);
			case EMAIL_BLACKLIST: return new ContainerInboxBlacklist(windowID, inventory, Inbox.get(player).getSenderBlacklist());
			default: return null;
		}
	}
}
