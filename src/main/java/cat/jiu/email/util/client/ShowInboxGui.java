package cat.jiu.email.util.client;

import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.MsgOpenGui;
import cat.jiu.email.ui.EmailGuiHandler;
import cat.jiu.email.ui.gui.GuiEmailMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@EventBusSubscriber
public class ShowInboxGui {
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public static void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
		GuiScreen gui = event.getGui();
		if(gui instanceof net.minecraft.client.gui.GuiChat) {
			event.getButtonList().add(new Button(gui, EmailGuiHandler.EMAIL_MAIN, event.getButtonList().size(), gui.width - 20 - 5, 5, I18n.format("info.email.name")));
			event.getButtonList().add(new Button(gui, EmailGuiHandler.EMAIL_SEND, event.getButtonList().size(), gui.width - 20 - 5, 26, I18n.format("info.email.dispatch")));
		}else if(gui instanceof net.minecraft.client.gui.inventory.GuiContainer) {
			net.minecraft.client.gui.inventory.GuiContainer con = (net.minecraft.client.gui.inventory.GuiContainer) gui;
			
			if(con instanceof net.minecraft.client.gui.inventory.GuiInventory) {
				event.getButtonList().add(new InventoryButton(gui, EmailGuiHandler.EMAIL_MAIN, event.getButtonList().size(),con.getGuiLeft()+27, con.getGuiTop() + 9, I18n.format("info.email.name")));
			}else if(con instanceof net.minecraft.client.gui.inventory.GuiContainerCreative) {
				event.getButtonList().add(new Button(gui, EmailGuiHandler.EMAIL_MAIN, event.getButtonList().size(), con.getGuiLeft() + 145, con.getGuiTop() + 138, I18n.format("info.email.name")));
			}
		}
	}
	
	static final ResourceLocation send_email = new ResourceLocation(EmailMain.MODID, "textures/gui/send_email.png");
	static final ResourceLocation send_email_hover = new ResourceLocation(EmailMain.MODID, "textures/gui/send_email_hover.png");
	static final ResourceLocation inbox = new ResourceLocation(EmailMain.MODID, "textures/gui/inbox_min.png");
	static final ResourceLocation inbox_hover = new ResourceLocation(EmailMain.MODID, "textures/gui/inbox_min_hover.png");
	static final ResourceLocation email = new ResourceLocation(EmailMain.MODID, "textures/gui/email.png");
	
	@SideOnly(Side.CLIENT)
	private static class Button extends GuiButton {
		private final int guiId;
		private final GuiScreen gui;
		public Button(GuiScreen gui, int guiId, int buttonId, int x, int y, String buttonText) {
			super(buttonId, x, y, 20, 13, buttonText);
			this.guiId = guiId;
			this.gui = gui;
		}
		
		int tick = 0;
		int i = 8;
		boolean lag = false;
		
		public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
			this.hovered = GuiEmailMain.isInRange(mouseX, mouseY, this.x, this.y, this.width, this.height);
			
			GlStateManager.pushMatrix();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			if(this.guiId == EmailGuiHandler.EMAIL_SEND) {
				mc.getTextureManager().bindTexture(this.hovered ? inbox_hover : inbox);
				mc.getTextureManager().bindTexture(this.hovered ? send_email : inbox);
			}else if(this.guiId == EmailGuiHandler.EMAIL_MAIN) {
				mc.getTextureManager().bindTexture(this.hovered ? inbox_hover : inbox);
			}
			drawModalRectWithCustomSizedTexture(this.x, this.y, 0, 0, this.width, this.height, this.width, this.height);
			GlStateManager.popMatrix();
			
			if((EmailMain.getUnread() > 0 || EmailMain.getUnaccept() > 0) && this.guiId == EmailGuiHandler.EMAIL_MAIN) {
				GlStateManager.pushMatrix();
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
				mc.getTextureManager().bindTexture(email);
				if(this.gui instanceof net.minecraft.client.gui.GuiChat) {
					super.drawTexturedModalRect(this.x - 25 + this.i - 1, this.y + 3, 92, 15, 17, 6);
				}else if(this.gui instanceof net.minecraft.client.gui.inventory.GuiContainerCreative) {
					super.drawTexturedModalRect(this.x + 7, this.y + 15 + this.i - 1, 88, 0, 6, 14);
				}
				GlStateManager.popMatrix();
				
				if(this.i <= 0) this.lag = true;
				if(this.i >= 8) this.lag = false;
				if(this.tick >= 5) {
					if(this.lag) this.i++;
					else this.i--;
				}
				if(this.tick >= 5) this.tick = 0;
				this.tick++;
			}
			if(GuiEmailMain.isInRange(mouseX, mouseY, this.x, this.y, this.width, this.height)) {
				this.gui.drawHoveringText(super.displayString, mouseX, mouseY);
			}
		}
		public void mouseReleased(int mouseX, int mouseY) {
			EmailMain.net.sendMessageToServer(new MsgOpenGui(this.guiId));
		}
	}
	
	@SideOnly(Side.CLIENT)
	private static class InventoryButton extends GuiButton {
		private final GuiScreen gui;
		private final int guiId;
		private boolean recipeBookOpen;
		public InventoryButton(GuiScreen gui, int guiId, int buttonId, int x, int y, String buttonText) {
			super(buttonId, x, y, 9, 6, buttonText);
			this.guiId = guiId;
			this.gui = gui;
			if(this.gui instanceof GuiInventory && ((GuiInventory) this.gui).func_194310_f().isVisible()) {
				this.recipeBookOpen = true;
			}
		}
		
		int tick = 0;
		int i = 8;
		boolean lag = false;
		
		@Override
		public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
			if(this.visible) {
				int x = this.x;
				if(this.gui instanceof GuiInventory) {
					GuiInventory inv = (GuiInventory) this.gui;
					if(inv.func_194310_f().isVisible()) {
						x = inv.getGuiLeft() + 27;
					}else {
						if(this.recipeBookOpen) {
							x = this.x - 77;
						}else {
							x = this.x;
						}
					}
				}
				
				this.hovered = GuiEmailMain.isInRange(mouseX, mouseY, x, this.y, this.width, this.height);
				
				GlStateManager.pushMatrix();
				GlStateManager.color(1.0F, 1.0F, 1.0F, 1F);
				mc.getTextureManager().bindTexture(this.hovered ? inbox_hover : inbox);
				drawModalRectWithCustomSizedTexture(x, this.y, 0, 0, this.width, this.height, this.width, this.height);
				GlStateManager.popMatrix();
				
				if(EmailMain.getUnread() > 0 || EmailMain.getUnaccept() > 0) {
					GlStateManager.pushMatrix();
					GlStateManager.color(1.0F, 1.0F, 1.0F, 1F);
					mc.getTextureManager().bindTexture(email);
					super.drawTexturedModalRect(x + 9 + this.i + 1, this.y, 73, 15, 15, 6);
					GlStateManager.popMatrix();
					
					if(this.i <= 0) this.lag = true;
					if(this.i >= 8) this.lag = false;
					if(this.tick >= 5) {
						if(this.lag) this.i++;
						else this.i--;
					}
					if(this.tick >= 5) this.tick = 0;
					this.tick++;
				}
				if(this.hovered) {
					this.gui.drawHoveringText(super.displayString, mouseX, mouseY);
				}
				this.mouseDragged(mc, mouseX, mouseY);
			}
		}
		
		public void mouseReleased(int mouseX, int mouseY) {
			EmailMain.net.sendMessageToServer(new MsgOpenGui(this.guiId));
		}
		
		public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
			int x = this.x;
			if(this.gui instanceof GuiInventory) {
				GuiInventory inv = (GuiInventory) this.gui;
				if(inv.func_194310_f().isVisible()) {
					x = inv.getGuiLeft() + 27;
				}else {
					if(this.recipeBookOpen) {
						x = this.x - 77;
					}else {
						x = this.x;
					}
				}
			}
			return this.enabled && this.visible && GuiEmailMain.isInRange(mouseX, mouseY, x, this.y, this.width, this.height);
		}
	}
}
