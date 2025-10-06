package cat.jiu.email.ui.container;

import java.util.List;

import cat.jiu.email.ui.GuiHandler;
import com.google.common.collect.Lists;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class ContainerInboxBlacklist extends AbstractContainerMenu {
	private List<String> list;

    public ContainerInboxBlacklist(int windowId, Inventory inv, List<String> list) {
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
	public boolean stillValid(Player p_38874_) {
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
		return ItemStack.EMPTY;
	}
}
