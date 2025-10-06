package cat.jiu.email.ui;

import cat.jiu.core.util.Utils;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.configs.EmailConfigClient;
import cat.jiu.email.ui.gui.component.GuiImageButton;

import cat.jiu.email.configs.EmailConfigServer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(Dist.CLIENT)
public class InboxButton {
	public static final ResourceLocation inbox = Utils.location(EmailMain.MODID, "textures/gui/inbox_min.png");
	public static final ResourceLocation inbox_hover = Utils.location(EmailMain.MODID, "textures/gui/inbox_min_hover.png");
	static final ResourceLocation email = Utils.location(EmailMain.MODID, "textures/gui/email.png");

	static Button INSTANCE;

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onGuiClose(ScreenEvent.Closing event) {
		INSTANCE = null;
	}
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onGuiInit(ScreenEvent.Init.Post event) {
		Screen gui = event.getScreen();
		if(gui instanceof ChatScreen && EmailConfigClient.Enable_Chat_Button.get()) {
			event.addListener(new Button(gui, EmailConfigClient.Position.Inbox_Buttons.Chat_Gui_Button, EmailConfigClient.Position.Inbox_Buttons.Chat_Gui_Button_Size, I18n.get("info.inbox.name")));
		}else if(gui instanceof AbstractContainerScreen<?> con) {
			if(con instanceof InventoryScreen) {
				event.addListener(new Button(gui, EmailConfigClient.Position.Inbox_Buttons.Survival_Gui_Button, EmailConfigClient.Position.Inbox_Buttons.Survival_Gui_Button_Size, I18n.get("info.inbox.name")));
			}else if(con instanceof CreativeModeInventoryScreen) {
				event.addListener(new Button(gui, EmailConfigClient.Position.Inbox_Buttons.Creative_Tab_Button, EmailConfigClient.Position.Inbox_Buttons.Creative_Tab_Button_Size, I18n.get("info.inbox.name")));
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onGuiMouse(ScreenEvent.MouseScrolled.Pre event) {
		if (INSTANCE != null && INSTANCE.isDragging) {
			INSTANCE.size.set(INSTANCE.size.get() + (event.getScrollDelta() > 0 ? 0.05 : -0.05));
			if (INSTANCE.size.get() <= 0.15d) {
				INSTANCE.size.set(0.15d);
			}

			INSTANCE.setWidth((int) (23 * INSTANCE.size.get()));
			INSTANCE.setHeight((int) (15 * INSTANCE.size.get()));
			event.setCanceled(true);
		}
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onGuiMouseDragged(ScreenEvent.MouseDragged.Pre event) {
		if (INSTANCE != null) {
			if (event.getMouseButton() == 1 && EmailConfigClient.Lock_Inbox_Button_Dragging.get() && INSTANCE.isMouseOver(event.getMouseX(), event.getMouseY())) {
				INSTANCE.isDragging = true;
				event.setCanceled(true);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void onGuiKeyPressed(ScreenEvent.KeyPressed.Pre event) {
		if (INSTANCE != null && KeyBinds.KEY_BUTTON_DRAGGING.isClicked(event.getKeyCode(), event.getScanCode())) {
			EmailConfigClient.Lock_Inbox_Button_Dragging.set(!EmailConfigClient.Lock_Inbox_Button_Dragging.get());
			INSTANCE.displayTextTime = event.getScreen().getMinecraft().level.getLevelData().getGameTime() + 3 * 20;
			INSTANCE.displayText = Component.translatable(EmailConfigClient.Lock_Inbox_Button_Dragging.get() ? "info.inbox.key.dragging.info.unlock" : "info.inbox.key.dragging.info.lock");
			INSTANCE.displayText1 = EmailConfigClient.Lock_Inbox_Button_Dragging.get() ? Component.translatable("info.inbox.key.dragging.info.mouse_right") : null;
			event.setCanceled(true);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static class Button extends GuiImageButton {
		protected final Screen gui;
		protected final Progress progress = new Progress();
		protected final EmailConfigServer.Pos pos;
		protected final ForgeConfigSpec.DoubleValue size;
		protected boolean isDragging;

		protected long displayTextTime;
		protected Component displayText, displayText1;

		public Button(Screen gui, EmailConfigServer.Pos pos, ForgeConfigSpec.DoubleValue size, String buttonText) {
			super(gui, 0, 0, 20, 13, buttonText, 23, 15, 23, 15, b-> EmailAPI.openInbox());
			this.gui = gui;
			this.pos = pos;
			this.size = size;
			this.setBackground(()->this.isHovered() ? inbox_hover : inbox);

			int guiLeft = 0, guiTop = 0;
			if (this.gui instanceof AbstractContainerScreen<?> container) {
				guiLeft = container.getGuiLeft();
				guiTop = container.getGuiTop();
			}
			this.setX(guiLeft + pos.X.get());
			this.setY(guiTop + pos.Y.get());
			this.setWidth((int) (23 * size.get()));
			this.setHeight((int) (15 * size.get()));
			InboxButton.INSTANCE = this;
		}

		@Override
		public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
			if (this.isDragging) {
				this.isDragging = false;

				int guiLeft = 0, guiTop = 0;
				if (this.gui instanceof AbstractContainerScreen<?> container) {
					guiLeft = container.getGuiLeft();
					guiTop = container.getGuiTop();
				}
				this.pos.X.set(this.getX() - guiLeft);
				this.pos.Y.set(this.getY() - guiTop);
				this.saveConfig();
			}
			return super.mouseReleased(pMouseX, pMouseY, pButton);
		}

		protected void saveConfig() {
			EmailConfigServer.CONFIG_MAIN.save();
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
			super.renderWidget(graphics, mouseX, mouseY, partialTick);
			if(this.gui instanceof InventoryScreen inv) {
				this.setX(inv.getGuiLeft() + EmailConfigClient.Position.Inbox_Buttons.Survival_Gui_Button.X.get());
			}
			if(EmailMain.getUnread() > 0 || EmailMain.getUnaccepted() > 0) {
				this.drawHasEmailInfo(graphics, partialTick);
				this.progress.updata();
			}

			if (this.displayTextTime >= this.gui.getMinecraft().level.getLevelData().getGameTime()) {
				if (this.displayText1!=null) {
					graphics.renderTooltip(this.gui.getMinecraft().font, this.displayText1, this.gui.width / 2 - this.gui.getMinecraft().font.width(this.displayText1) / 2 - 10, this.gui.getMinecraft().font.lineHeight + 10 + this.gui.getMinecraft().font.lineHeight + 4);
				}
				graphics.renderTooltip(this.gui.getMinecraft().font, this.displayText, this.gui.width / 2 - this.gui.getMinecraft().font.width(this.displayText) / 2 - 10, this.gui.getMinecraft().font.lineHeight + 10);
			}

			if (this.isDragging) {
				this.setX(mouseX);
				this.setY(mouseY);
			}
		}

		protected void drawHasEmailInfo(GuiGraphics graphics, float partialTick) {
			if(this.gui instanceof ChatScreen) {
				graphics.blit(email, this.getX() - 25 + this.progress.progress - 1, this.getY() + 3, 92, 15, 17, 6);
			}else if(this.gui instanceof CreativeModeInventoryScreen) {
				graphics.blit(email, this.getX() + 20 + this.progress.progress + 1, this.getY() + 4, 73, 15, 15, 6);
			}else if (this.gui instanceof InventoryScreen) {
				graphics.blit(email, this.getX() + this.getWidth()/2 - 3, this.getY() - 26 + this.progress.progress, 88, 19, 6, 17);
			}
		}
	}

	public static class Progress {
		public int time = 0;
		public int progress = 8;
		public boolean reverse = false;

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
