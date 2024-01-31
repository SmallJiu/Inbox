package cat.jiu.email.ui.gui.component;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.item.ItemStack;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EmailSlot extends GuiSlot {
	protected int current = -1;
	protected final List<EmailType> emails = Lists.newArrayList();
	protected final List<ItemSlot> slots = Lists.newArrayList();
	
	public EmailSlot(EmailType[] emails, ItemSlot[] slots, Minecraft mc, int width, int height, int topIn, int bottomIn, int slotHeightIn) {
		super(mc, width, height, topIn, bottomIn, slotHeightIn);
		this.emails.addAll(Arrays.asList(emails));
		this.slots.addAll(Arrays.asList(slots));
		this.emails.sort((e0, e1) -> Long.compare(e1.id, e0.id));
	}
	
	@Override
	protected int getContentHeight() {
		return this.getSize() * this.slotHeight + 3;
	}

	@Override
	protected int getSize() {
		return this.emails.size();
	}

	@Override
	protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
		this.current = slotIndex;
		List<ItemStack> stacks = this.emails.get(this.current).email.getItems();
		int index = 0;
		for(int i = 0; i < stacks.size(); i++) {
			index = i;
			this.slots.get(i).stack = stacks.get(i);
		}
		for(int i = index+1; i < this.slots.size(); i++) {
			this.slots.get(i).stack = ItemStack.EMPTY;
		}
	}

	@Override
	protected boolean isSelected(int slotIndex) {
		return this.current == slotIndex;
	}

	@Override
	protected void drawBackground() {}

	@Override
	protected void drawSlot(int index, int xPos, int yPos, int height, int mouseX, int mouseY, float partialTicks) {
		this.mc.fontRenderer.drawString("ID: " + this.emails.get(index).id, xPos + 3, yPos + 2, Color.RED.getRGB());
		
	}
	
	public static class ItemSlot {
		public final int x,y;
		public ItemStack stack = ItemStack.EMPTY;
		public ItemSlot(int x, int y, ItemStack stack) {
			this.x = x;
			this.y = y;
			this.stack = stack;
		}
	}
}
