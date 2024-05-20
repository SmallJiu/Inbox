package cat.jiu.email.util.client;

import cat.jiu.email.EmailMain;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.gui.component.GuiImageButton;

import cat.jiu.email.util.EmailConfigs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
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
	public static void onInitGui(ScreenEvent.InitScreenEvent.Post event) {
		Screen gui = event.getScreen();
		if(gui instanceof ChatScreen && EmailConfigs.Enable_Chat_Button.get()) {
			event.addListener(new ChatButton(gui, GuiHandler.EMAIL_MAIN, gui.width - EmailConfigs.Main.Position.Inbox_Buttons.Chat_Gui_Button.X.get(), EmailConfigs.Main.Position.Inbox_Buttons.Chat_Gui_Button.Y.get(), I18n.get("info.email.name")));
			event.addListener(new ChatButton(gui, GuiHandler.EMAIL_SEND, gui.width - EmailConfigs.Main.Position.Inbox_Buttons.Chat_Gui_Button.X.get(), EmailConfigs.Main.Position.Inbox_Buttons.Chat_Gui_Button.Y.get()+23, I18n.get("info.email.dispatch")));
		}else if(gui instanceof AbstractContainerScreen<?> con) {
			if(con instanceof InventoryScreen) {
				event.addListener(new InventoryButton(gui, GuiHandler.EMAIL_MAIN,con.getGuiLeft()+EmailConfigs.Main.Position.Inbox_Buttons.Survival_Gui_Button.X.get(), con.getGuiTop() + EmailConfigs.Main.Position.Inbox_Buttons.Survival_Gui_Button.Y.get(), I18n.get("info.email.name")));
			}else if(con instanceof CreativeModeInventoryScreen) {
				event.addListener(new ChatButton(gui, GuiHandler.EMAIL_MAIN, con.getGuiLeft() + EmailConfigs.Main.Position.Inbox_Buttons.Creative_Tab_Button.X.get(), con.getGuiTop() + EmailConfigs.Main.Position.Inbox_Buttons.Creative_Tab_Button.Y.get(), I18n.get("info.email.name")));
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
			this.setBackground(()->this.isHoveredOrFocused() ? inbox_hover : inbox).setRefreshBackground(true);
		}

		@Override
		public void renderButton(PoseStack stack, int mouseX, int mouseY, float partialTick) {
			super.renderButton(stack, mouseX, mouseY, partialTick);
			if((EmailMain.getUnread() > 0 || EmailMain.getUnaccepted() > 0) && this.guiId == GuiHandler.EMAIL_MAIN) {
				Minecraft.getInstance().getTextureManager().bindForSetup(email);
				if(this.gui instanceof ChatScreen) {
					blit(stack, this.x - 25 + this.progress.progress - 1, this.y + 3, 92, 15, 17, 6);
				}else if(this.gui instanceof CreativeModeInventoryScreen) {
					blit(stack, this.x + 7, this.y + 15 + this.progress.progress - 1, 88, 0, 6, 14);
				}
				this.progress.updata();
			}
			if(this.isHoveredOrFocused()) {
				this.gui.renderTooltip(stack, this.hoveringText.get(), mouseX, mouseY);
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
			this.setBackground(()->this.isHoveredOrFocused() ? inbox_hover : inbox).setRefreshBackground(true);
		}

		@Override
		public void renderButton(PoseStack stack, int mouseX, int mouseY, float partialTick) {
			super.renderButton(stack, mouseX, mouseY, partialTick);
			if(this.visible) {
				if(this.gui instanceof InventoryScreen inv) {
					this.x = inv.getGuiLeft() + 27;
				}

				if(EmailMain.getUnread() > 0 || EmailMain.getUnaccepted() > 0) {
					Minecraft.getInstance().getTextureManager().bindForSetup(email);
					blit(stack, this.x + 9 + this.progress.progress + 1, this.y, 73, 15, 15, 6);
					this.progress.updata();
				}
				if(this.isHoveredOrFocused()) {
					this.gui.renderTooltip(stack, this.hoveringText.get(), mouseX, mouseY);
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
