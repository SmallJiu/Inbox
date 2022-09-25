package cat.jiu.email.ui.container;

import java.util.List;

import cat.jiu.email.element.Inbox;
import cat.jiu.email.util.EmailConfigs;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerEmailMain extends Container {
	public static final long EmailMaxSize = 2 * 1024 * 1024;
	private Inbox inbox;
	private static final NonNullList<ItemStack> emptyStacks = NonNullList.withSize(16, ItemStack.EMPTY);
	private final ItemStackHandler handler = new ItemStackHandler(16);
	
	public ContainerEmailMain() {
		int slotIndex = 0;
		for(int slotY = 0; slotY < 2; slotY++) {
			for(int slotX = 0; slotX < 8; slotX++) {
				if(slotIndex >= 16) return;
				this.addSlotToContainer(new SlotItemHandler(handler, slotIndex, EmailConfigs.Main.Position.Current_Email.Items.X + (18 * slotX), EmailConfigs.Main.Position.Current_Email.Items.Y + (18 * slotY)) {
					public boolean canTakeStack(EntityPlayer playerIn) {
						return false;
					}
				});
				slotIndex += 1;
			}	
		}
	}
	
	private long inboxSize = 0;
	public Inbox getInbox() {return inbox;}
	public long getEmailSize() {return inboxSize;}
	public void setMsgs(Inbox inbox) {
		this.inbox = inbox;
		this.inboxSize = inbox.getInboxSize();
	}
	
	public void putStack(List<ItemStack> items) {
		for(int i = 0; i < this.handler.getSlots(); i++) {
			ItemStack stack = i >= items.size() ? ItemStack.EMPTY : items.get(i);
			this.getSlot(i).putStack(stack);
		}
	}
	
	public boolean isEmpty() {
		for(int i = 0; i < this.handler.getSlots(); i++) {
			if(!this.handler.getStackInSlot(i).isEmpty()) return false;
		}
		return true;
	}

	public void clear() {
		this.putStack(emptyStacks);
	}
	
	private int currenMsg = -1;
	private int lastCurrenMsg = -1;
	public void setCurrenMsg(int msgID) {
		this.lastCurrenMsg = this.currenMsg;
		this.currenMsg = msgID;
	}
	public int getCurrenMsg() {return currenMsg;}
	public boolean isSameMsg() {
		boolean lag = this.currenMsg == this.lastCurrenMsg;
		if(!lag) this.lastCurrenMsg = this.currenMsg;;
		return lag;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}
}
