package cat.jiu.email.ui.container;

import java.awt.Color;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Cooling;
import cat.jiu.email.net.msg.SendCooling;
import cat.jiu.email.ui.SendEmailCoolingEvent;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonToStackUtil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerEmailSend extends Container {
	private final EntityPlayer player;
	private final ItemStackHandler handler = new ItemStackHandler(16);
	
	public ContainerEmailSend(World world, EntityPlayer player) {
		this.player = player;
		this.addHandlerSlot(this.handler, 17, 100, 8, 2);
		this.addPlayerInventorySlot(8, 151);
		if(player instanceof EntityPlayerMP) {
			EntityPlayerMP mp = (EntityPlayerMP) player;
			if(Cooling.isCooling(player.getName())) {
				long m = Cooling.getCoolingTimeMillis(mp.getName());
				cooling = m;
				EmailMain.execute(a->EmailMain.net.sendMessageToPlayer(new SendCooling(m), mp));
			}
		}
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	protected void addHandlerSlot(ItemStackHandler handler, int x, int y, int slotWidth, int slotHeight) {
		int slotIndex = 0;
		for(int slotY = 0; slotY < slotHeight; slotY++) {
			for(int slotX = 0; slotX < slotWidth; slotX++) {
				if(slotIndex >= handler.getSlots()) return;
				this.addSlotToContainer(new SlotItemHandler(handler, slotIndex, x + 18 * slotX, y + (18 * slotY)));
				slotIndex += 1;
			}	
		}
	}
	
	protected void addPlayerInventorySlot(int x, int y) {
		int slotIndex = 0;
		for(int slotX = 0; slotX < 9; slotX++) {
			this.addSlotToContainer(new Slot(this.player.inventory, slotIndex, x + 18 * slotX, y + (18 * 2) + 22));
			slotIndex += 1;
		}
		for(int slotY = 0; slotY < 3; slotY++) {
			for(int slotX = 0; slotX < 9; slotX++) {
				this.addSlotToContainer(new Slot(this.player.inventory, slotIndex, x + 18 * slotX, y + (18 * slotY)));
				slotIndex += 1;
			}
		}
	}
	
	@SubscribeEvent
	public void cooling(SendEmailCoolingEvent event) {
		if(this.player.getName().equals(event.name)) {
			if(!this.player.world.isRemote) {
				EmailMain.execute(a->EmailMain.net.sendMessageToPlayer(new SendCooling(event.millis), (EntityPlayerMP) this.player));
			}
			cooling = event.millis;
		}
	}
	
	private static long cooling = 0;
	public boolean isCooling() {
		return cooling > System.currentTimeMillis();
	}
	
	public void setCooling(long millis) {
		cooling = millis;
	}
	
	public long getCoolingMillis() {return cooling;}
	
	boolean isLock = false;
	public boolean isLock() {return isLock;}
	public void setLock(boolean isSending) {
		this.isLock = isSending;
		for(IContainerListener listener : this.listeners) {
			listener.sendWindowProperty(this, 1001, this.isLock ? 1 : 0);
		}
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void updateProgressBar(int id, int data) {
		switch(id) {
			case 1001:
				this.isLock = data == 1;
				break;
		}
	}
	
	@Override
	public void onContainerClosed(EntityPlayer player) {
		super.onContainerClosed(player);
		if(!this.isEmpty() && !player.world.isRemote) {
			EmailUtils.spawnAsEntity(player, this.handler);
		}
	}
	
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		Slot slot = this.inventorySlots.get(index);

		if(slot == null || !slot.getHasStack()) {
			return ItemStack.EMPTY;
		}

		ItemStack newStack = slot.getStack(),
				  oldStack = newStack.copy();

		boolean isMerged = false;

		if(index < 16) {
			isMerged = super.mergeItemStack(newStack, 16, 52, true);
		}else {
			isMerged = super.mergeItemStack(newStack, 0, 16, false);
		}

		if(!isMerged) {
			return ItemStack.EMPTY;
		}
		
		return oldStack;
	}
	
	public boolean isEmpty() {
		for(int i = 0; i < this.handler.getSlots(); i++) {
			if(!this.handler.getStackInSlot(i).isEmpty()) return false;
		}
		return true;
	}
	
	/**
	 * will clear slots to create a JsonObject
	 */
	public JsonObject toItemArray(boolean copy) {
		List<ItemStack> stacks = Lists.newArrayList();
		for(int i = 0; i < this.handler.getSlots(); i++) {
			stacks.add(this.handler.getStackInSlot(i));
			if(!copy) {
				this.handler.setStackInSlot(i, ItemStack.EMPTY);
			}
		}
		return JsonToStackUtil.toJsonObject(stacks, false);
	}
	
	public List<ItemStack> toItemList(boolean copy) {
		List<ItemStack> stacks = Lists.newArrayList();
		for(int i = 0; i < this.handler.getSlots(); i++) {
			stacks.add(this.handler.getStackInSlot(i));
			if(!copy) {
				this.handler.setStackInSlot(i, ItemStack.EMPTY);
			}
		}
		return stacks;
	}
	
	public void putStack(List<ItemStack> items) {
		for(int i = 0; i < this.handler.getSlots(); i++) {
			ItemStack stack = i >= items.size() ? ItemStack.EMPTY : items.get(i);
			this.getSlot(i).putStack(stack);
		}
	}
	
	public long renderTicks = 0;
	public String renderText;
	public Color renderColor;
	
	public void setRenderText(String text) {
		this.setRenderText(text, Color.RED);
	}
	public void setRenderText(String text, Color color) {
		this.setRenderText(text, color, EmailUtils.parseTick(0,0,0,15, 0));
	}
	public void setRenderText(String text, Color color, long ticks) {
		this.renderText = text;
		this.renderColor = color;
		this.renderTicks = ticks;
	}
	public void clearRenderText() {
		this.renderText = null;
		this.renderColor = null;
		this.renderTicks = 0;
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}
}
