package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.MsgDeleteEmail;
import cat.jiu.email.ui.gui.component.EmailSlot;
import cat.jiu.email.ui.gui.component.EmailType;
import cat.jiu.email.ui.gui.component.GuiClickButton;
import cat.jiu.email.ui.gui.component.EmailSlot.ItemSlot;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag.TooltipFlags;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class GuiDeleteEmailConfirm extends GuiContainer {
	public static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/recipe_book.png");
	
	protected final GuiScreen parent;
	protected final EmailType[] emails;
	protected final ItemSlot[] slots = new ItemSlot[16];
	protected EmailSlot emailSlot;
	public GuiDeleteEmailConfirm(GuiScreen parent, EmailType... emails) {
		super(new Container() {
			public boolean canInteractWith(EntityPlayer playerIn) {
				return true;
			}
		});
		this.parent = parent;
		this.emails = emails;
	}
	
	@Override
	public void initGui() {
		this.buttonList.clear();
		ScaledResolution sr = new ScaledResolution(this.mc);
		int x = sr.getScaledWidth() / 2;
        int y = sr.getScaledHeight() / 2;
        
		this.buttonList.add(new GuiClickButton(0, x - 100, y + y/2 + y/2/2, 100, 20, I18n.format("info.email.confirm"), (mousex,mousey)->{
			for(EmailType email : this.emails) {
				EmailMain.net.sendMessageToServer(new MsgDeleteEmail.Delete(email.id));
			}
			this.mc.displayGuiScreen(this.parent);
		}));
		
		this.buttonList.add(new GuiClickButton(1, x, y + y/2 + y/2/2, 100, 20, I18n.format("info.email.cancel"), (mousex,mousey)->{
			this.mc.displayGuiScreen(this.parent);
		}));
		
		int slotx = 0;
		int sloty = 0;
		for(int i = 0; i < this.slots.length; i++) {
			this.slots[i] = new ItemSlot(x - 75 + (18 * slotx), y + y/2 + y/2/2 - 50 + (18 * sloty), ItemStack.EMPTY);
			slotx++;
			if(i==7) {
				sloty++;
				slotx=0;
			}
		}
		
		this.emailSlot = new EmailSlot(emails, slots, mc, this.width , this.height, 32, this.height - 100, this.mc.fontRenderer.FONT_HEIGHT + 6);
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.emailSlot.drawScreen(mouseX, mouseY, partialTicks);
		super.drawScreen(mouseX, mouseY, partialTicks);
		for(int i = 0; i < this.buttonList.size(); i++) {
			GuiButton btn = this.buttonList.get(i);
			this.drawHorizontalLine(btn.x, btn.x + btn.width - 2, btn.y + btn.height-1, Color.BLACK.getRGB());
		}
		
		for(int i = 0; i < this.slots.length; i++) {
			ItemSlot slot = this.slots[i];
			GlStateManager.pushMatrix();
			GlStateManager.color(1, 1, 1);
			this.mc.getTextureManager().bindTexture(GuiEmailMain.BackGround);
			this.drawTexturedModalRect(slot.x, slot.y, 47, 108, 18, 18);
			this.mc.getRenderItem().renderItemIntoGUI(slot.stack, slot.x+1, slot.y+1);
			GlStateManager.popMatrix();
		}
		
		for(int i = 0; i < this.slots.length; i++) {
			ItemSlot slot = this.slots[i];
			if(GuiEmailMain.isInRange(mouseX, mouseY, slot.x, slot.y, 18, 18) && !slot.stack.isEmpty()) {
				List<String> tooltip = Lists.newArrayList();
				tooltip.addAll(slot.stack.getTooltip(mc.player, mc.gameSettings.advancedItemTooltips ? TooltipFlags.ADVANCED : TooltipFlags.NORMAL));
				tooltip.add("Count: " + slot.stack.getCount());
				this.drawHoveringText(tooltip, mouseX, mouseY);
			}
		}
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		int x = new ScaledResolution(this.mc).getScaledWidth() / 2;
        this.fontRenderer.drawString(I18n.format("info.email.delete_email"), x - (this.fontRenderer.getStringWidth(I18n.format("info.email.delete_email"))/2), 18, Color.RED.getRGB());
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == 1 || this.mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
			this.mc.displayGuiScreen(this.parent);
        }else {
        	super.keyTyped(typedChar, keyCode);
        }
	}
	
	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		this.emailSlot.handleMouseInput();
	}
	
	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);
		this.emailSlot.actionPerformed(button);
	}
}
