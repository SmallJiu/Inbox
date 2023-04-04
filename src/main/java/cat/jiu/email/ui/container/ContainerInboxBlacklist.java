package cat.jiu.email.ui.container;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

public class ContainerInboxBlacklist extends Container {
	private List<String> list;
	
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
	public boolean canInteractWith(EntityPlayer playerIn) {
		return true;
	}
}
