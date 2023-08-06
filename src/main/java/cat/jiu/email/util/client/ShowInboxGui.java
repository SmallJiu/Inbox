package cat.jiu.email.util.client;

import cat.jiu.email.EmailMain;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.gui.component.GuiImageButton;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(Dist.CLIENT)
public class ShowInboxGui {

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onInitGui(ScreenEvent.Init.Post event) {
		Screen gui = event.getScreen();
		if(gui instanceof ChatScreen) {
			event.addListener(new ChatButton(gui, GuiHandler.EMAIL_MAIN, gui.width - 20 - 5, 5, I18n.get("info.email.name")));
			event.addListener(new ChatButton(gui, GuiHandler.EMAIL_SEND, gui.width - 20 - 5, 26, I18n.get("info.email.dispatch")));
		}else if(gui instanceof AbstractContainerScreen con) {
			if(con instanceof InventoryScreen) {
				event.addListener(new InventoryButton(gui, GuiHandler.EMAIL_MAIN,con.getGuiLeft()+27, con.getGuiTop() + 9, I18n.get("info.email.name")));
			}else if(con instanceof CreativeModeInventoryScreen) {
				event.addListener(new ChatButton(gui, GuiHandler.EMAIL_MAIN, con.getGuiLeft() + 145, con.getGuiTop() + 138, I18n.get("info.email.name")));
			}
		}
	}
	
	public static final ResourceLocation inbox = new ResourceLocation(EmailMain.MODID, "textures/gui/inbox_min.png");
	public static final ResourceLocation inbox_hover = new ResourceLocation(EmailMain.MODID, "textures/gui/inbox_min_hover.png");
	static final ResourceLocation email = new ResourceLocation(EmailMain.MODID, "textures/gui/email.png");

	@OnlyIn(Dist.CLIENT)
	private static class ChatButton extends GuiImageButton {
		private final int guiId;
		private final Screen gui;
		private final Progress progress = new Progress();
		public ChatButton(Screen gui, int guiId, int x, int y, String buttonText) {
			super(gui, x, y, 20, 13, buttonText, 23, 15, 23, 15, b-> GuiHandler.openGui(guiId));
			this.guiId = guiId;
			this.gui = gui;
			this.setBackground(()->this.isHovered() ? inbox_hover : inbox);
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			super.renderWidget(graphics, mouseX, mouseY, partialTick);
			if((EmailMain.getUnread() > 0 || EmailMain.getUnaccepted() > 0) && this.guiId == GuiHandler.EMAIL_MAIN) {
				if(this.gui instanceof ChatScreen) {
					graphics.blit(email, this.getX() - 25 + this.progress.progress - 1, this.getY() + 3, 92, 15, 17, 6);
				}else if(this.gui instanceof CreativeModeInventoryScreen) {
					graphics.blit(email, this.getX() + 7, this.getY() + 15 + this.progress.progress - 1, 88, 0, 6, 14);
				}
				this.progress.updata();
			}
			if(this.isHovered()) {
				graphics.renderTooltip(gui.getMinecraft().font, getMessage(), mouseX, mouseY);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static class InventoryButton extends GuiImageButton {
		private final Screen gui;
		private final Progress progress = new Progress();

		public InventoryButton(Screen gui, int guiId, int x, int y, String buttonText) {
			super(gui, x, y, 9, 6, buttonText, 23, 15, 23, 15, b-> GuiHandler.openGui(guiId));
			this.gui = gui;
			this.setBackground(()->this.isHovered() ? inbox_hover : inbox);
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			super.renderWidget(graphics, mouseX, mouseY, partialTick);
			if(this.visible) {
				if(this.gui instanceof InventoryScreen inv) {
					this.setX(inv.getGuiLeft() + 27);
				}

				if(EmailMain.getUnread() > 0 || EmailMain.getUnaccepted() > 0) {
					graphics.blit(email, this.getX() + 9 + this.progress.progress + 1, this.getY(), 73, 15, 15, 6);
					this.progress.updata();
				}
				if(this.isHovered()) {
					graphics.renderTooltip(this.gui.getMinecraft().font, getMessage(), mouseX, mouseY);
				}
			}
		}
	}

	static class Progress {
		int time = 0;
		int progress = 8;
		boolean reverse = false;

		public void updata(){
			if(this.progress <= 0) this.reverse = true;
			if(this.progress >= 8) this.reverse = false;
			if(this.time >= 5) {
				if(this.reverse) this.progress++;
				else this.progress--;
			}
			if(this.time >= 5) this.time = 0;
			this.time++;
		}
	}
}
