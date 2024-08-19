package cat.jiu.email.ui.container;

import cat.jiu.email.element.Inbox;
import cat.jiu.email.ui.GuiHandler;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ContainerEmailMain extends AbstractContainerMenu {
	public static final long EmailMaxSize = 2 * 1024 * 1024;
	private static final NonNullList<ItemStack> emptyStacks = NonNullList.withSize(16, ItemStack.EMPTY);

	private Inbox inbox;
	
	public ContainerEmailMain(int id, Inventory inv) {
		super(GuiHandler.main_TYPE.get(), id);
		this.inbox = Inbox.getEmpty(inv.player.getStringUUID());
	}
	
	private boolean refresh = true;
	private long inboxSize = -1;
	public void setRefresh(boolean refresh) {this.refresh = refresh;}
	public boolean isRefresh() {return refresh;}
	public Inbox getInbox() {return inbox;}
	public long getInboxSize() {return inboxSize;}
	public void setInbox(Inbox inbox) {
		this.inbox = inbox;
		this.inboxSize = inbox.getInboxSize();
		this.setRefresh(false);
	}
	public void updataInboxSize() {
		this.inboxSize = this.inbox.getInboxSize();
	}
	public boolean isEmptyStacks() {
		if (this.getCurrentEmail() != -1 && this.getInbox().getEmail(this.getCurrentEmail()) != null) {
			return this.getInbox().getEmail(this.getCurrentEmail()).hasItems();
		}
		return false;
	}
	
	private long currenEmail = -1;
	private long lastCurrenEmail = -1;
	public void setCurrenEmail(long emailID) {
		this.lastCurrenEmail = this.currenEmail;
		this.currenEmail = emailID;
	}
	public long getCurrentEmail() {return currenEmail;}
	public boolean isSameEmail() {
		boolean lag = this.currenEmail == this.lastCurrenEmail;
		if(!lag) this.lastCurrenEmail = this.currenEmail;
		return lag;
	}

	@Override
	public ItemStack quickMoveStack(Player p_38941_, int p_38942_) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player p_38874_) {
		return true;
	}
}
