package cat.jiu.email.ui.container;

import java.util.List;

import cat.jiu.email.ui.GuiHandler;
import com.google.common.collect.Lists;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;

public class ContainerInboxBlacklist extends Container {
	private List<String> list;

    public ContainerInboxBlacklist(int windowId, PlayerInventory inv, List<String> list) {
		super(GuiHandler.blacklist_TYPE.get(), windowId);
		this.list = list;
    }

    public List<String> getBlacklist() {
		return list;
	}
	public void addBlacklist(String name) {
		if(this.list==null) {
			this.list = Lists.newArrayList();
		}
		this.list.add(name);
	}
	public void setBlacklist(List<String> list) {
		this.list = list;
	}

	@Override
	public boolean canInteractWith(PlayerEntity playerIn) {
		return true;
	}
}
