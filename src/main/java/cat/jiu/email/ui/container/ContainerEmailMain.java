package cat.jiu.email.ui.container;

import java.util.List;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.init.ContainerTypes;
import cat.jiu.email.util.EmailConfigs;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerEmailMain extends Container {
	public static final long EmailMaxSize = 2 * 1024 * 1024;
	private static final NonNullList<ItemStack> emptyStacks = NonNullList.withSize(16, ItemStack.EMPTY);
	
	private final ItemStackHandler handler = new ItemStackHandler(16);
	private Inbox inbox;
	
	public ContainerEmailMain(int id, PlayerInventory inv, Inbox inbox) {
		super(ContainerTypes.container_email_main.get(), id);
		this.inbox = inbox;
		if(this.inbox!=null){
			this.refresh = false;
			this.inboxSize = inbox.getInboxSize();
		}
		int slotIndex = 0;
		for(int slotY = 0; slotY < 2; slotY++) {
			for(int slotX = 0; slotX < 8; slotX++) {
				this.addSlot(new SlotItemHandler(handler, slotIndex, EmailConfigs.Main.Position.Current_Email.Items.X.get() + (18 * slotX), EmailConfigs.Main.Position.Current_Email.Items.Y.get() + (18 * slotY)) {
					@Override
					public boolean canTakeStack(PlayerEntity playerIn) {
						return false;
					}
				});
				slotIndex += 1;
			}	
		}
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
	public void putStack(List<ItemStack> items) {
		for(int i = 0; i < this.handler.getSlots(); i++) {
			ItemStack stack = i >= items.size() ? ItemStack.EMPTY : items.get(i);
			this.getSlot(i).putStack(stack);
		}
	}
	public boolean isEmptyStacks() {
		for(int i = 0; i < this.handler.getSlots(); i++) {
			if(!this.handler.getStackInSlot(i).isEmpty()) return false;
		}
		return true;
	}
	public void clearStacks() {
		this.putStack(emptyStacks);
	}
	
	private long currenEmail = -1;
	private long lastCurrenEmail = -1;
	public void setCurrenEmail(long emailID) {
		this.lastCurrenEmail = this.currenEmail;
		this.currenEmail = emailID;
	}
	public long getCurrenEmail() {return currenEmail;}
	public boolean isSameEmail() {
		boolean lag = this.currenEmail == this.lastCurrenEmail;
		if(!lag) this.lastCurrenEmail = this.currenEmail;;
		return lag;
	}

	@Override
	public boolean canInteractWith(PlayerEntity player) {
		return true;
	}
}
