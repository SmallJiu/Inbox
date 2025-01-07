package cat.jiu.email.util.client;

import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.gui.component.GuiImageButton;

import cat.jiu.email.util.EmailConfigs;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(Dist.CLIENT)
public class ShowInboxGui {

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
		Screen gui = event.getGui();
		if(gui instanceof ChatScreen && EmailConfigs.Layout.Enable_Chat_Button.get()) {
			event.addWidget(new ChatButton(gui, GuiHandler.EMAIL_MAIN, gui.width - EmailConfigs.Layout.Position.Inbox_Buttons.Chat_Gui_Button.X.get(), EmailConfigs.Layout.Position.Inbox_Buttons.Chat_Gui_Button.Y.get(), I18n.format("info.inbox.name")));
			event.addWidget(new ChatButton(gui, GuiHandler.EMAIL_SEND, gui.width - EmailConfigs.Layout.Position.Inbox_Buttons.Chat_Gui_Button.X.get(), EmailConfigs.Layout.Position.Inbox_Buttons.Chat_Gui_Button.Y.get()+23, I18n.format("info.inbox.dispatch")));
		}else if(gui instanceof ContainerScreen<?>) {
			ContainerScreen con = (ContainerScreen) gui;
			if(gui instanceof InventoryScreen) {
				event.addWidget(new InventoryButton(gui, GuiHandler.EMAIL_MAIN,con.getGuiLeft()+EmailConfigs.Layout.Position.Inbox_Buttons.Survival_Gui_Button.X.get(), con.getGuiTop() + EmailConfigs.Layout.Position.Inbox_Buttons.Survival_Gui_Button.Y.get(), I18n.format("info.inbox.name")));
			}else if(gui instanceof CreativeScreen) {
				event.addWidget(new ChatButton(gui, GuiHandler.EMAIL_MAIN, con.getGuiLeft() + EmailConfigs.Layout.Position.Inbox_Buttons.Creative_Tab_Button.X.get(), con.getGuiTop() + EmailConfigs.Layout.Position.Inbox_Buttons.Creative_Tab_Button.Y.get(), I18n.format("info.inbox.name")));
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
		public void renderWidget(MatrixStack stack, int mouseX, int mouseY, float partialTick) {
			super.renderWidget(stack, mouseX, mouseY, partialTick);
			if((EmailMain.getUnread() > 0 || EmailMain.getUnaccepted() > 0) && this.guiId == GuiHandler.EMAIL_MAIN) {
				if(this.gui instanceof ChatScreen) {
					RenderUtils.draw(stack, email, this.x - 25 + this.progress.progress - 1, this.y + 3, 92, 15, 17, 6);
				}else if(this.gui instanceof CreativeScreen) {
					RenderUtils.draw(stack, email, this.x + 20 + this.progress.progress + 1, this.y + 4, 73, 15, 15, 6);
				}
				this.progress.updata();
			}
			if(this.isHovered()) {
				this.gui.renderTooltip(stack, getMessage(), mouseX, mouseY);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static class InventoryButton extends GuiImageButton {
		private final Screen gui;
		private final Progress progress = new Progress();

		public InventoryButton(Screen gui, int guiId, int x, int y, String buttonText) {
//			super(gui, x, y, 18, 10, buttonText, 23, 15, 23, 15, b-> GuiHandler.openGui(guiId));
			super(gui, x, y, 10, 6, buttonText, 23, 15, 23, 15, b-> GuiHandler.openGui(guiId));
			this.gui = gui;
			this.setBackground(()->this.isHovered() ? inbox_hover : inbox);
		}

		@Override
		public void renderWidget(MatrixStack stack, int mouseX, int mouseY, float partialTick) {
			super.renderWidget(stack, mouseX, mouseY, partialTick);
			if(this.visible) {
				if(this.gui instanceof InventoryScreen) {
					this.x = ((InventoryScreen) this.gui).getGuiLeft() + EmailConfigs.Layout.Position.Inbox_Buttons.Survival_Gui_Button.X.get();
				}

				if(EmailMain.getUnread() > 0 || EmailMain.getUnaccepted() > 0) {
					RenderUtils.draw(stack, email, this.x + 2, this.y - 26 + this.progress.progress, 88, 19, 6, 17);
					this.progress.updata();
				}
				if(this.isHovered()) {
					this.gui.renderTooltip(stack, getMessage(), mouseX, mouseY);
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
