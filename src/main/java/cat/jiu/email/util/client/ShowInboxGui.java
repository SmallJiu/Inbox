package cat.jiu.email.util.client;

import cat.jiu.email.EmailMain;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.util.EmailUtils;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import net.minecraft.util.text.StringTextComponent;
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
		if(gui instanceof ChatScreen) {
			event.addWidget(new ChatButton(gui, GuiHandler.EMAIL_MAIN, gui.width - 20 - 5, 5, I18n.format("info.email.name")));
			event.addWidget(new ChatButton(gui, GuiHandler.EMAIL_SEND, gui.width - 20 - 5, 26, I18n.format("info.email.dispatch")));
		}else if(gui instanceof ContainerScreen) {
			ContainerScreen<?> con = (ContainerScreen<?>) gui;
			if(con instanceof InventoryScreen) {
				event.addWidget(new InventoryButton(gui, GuiHandler.EMAIL_MAIN,con.getGuiLeft()+27, con.getGuiTop() + 9, I18n.format("info.email.name")));
			}else if(con instanceof CreativeScreen) {
				event.addWidget(new ChatButton(gui, GuiHandler.EMAIL_MAIN, con.getGuiLeft() + 145, con.getGuiTop() + 138, I18n.format("info.email.name")));
			}
		}
	}
	
	public static final ResourceLocation inbox = new ResourceLocation(EmailMain.MODID, "textures/gui/inbox_min.png");
	public static final ResourceLocation inbox_hover = new ResourceLocation(EmailMain.MODID, "textures/gui/inbox_min_hover.png");
	static final ResourceLocation email = new ResourceLocation(EmailMain.MODID, "textures/gui/email.png");

	@OnlyIn(Dist.CLIENT)
	private static class ChatButton extends Button {
		private final int guiId;
		private final Screen gui;
		private final Minecraft mc = Minecraft.getInstance();

		public ChatButton(Screen gui, int guiId, int x, int y, String buttonText) {
			super(x, y, 20, 13, new StringTextComponent(buttonText), b-> GuiHandler.openGui(guiId));
			this.guiId = guiId;
			this.gui = gui;
		}
		
		int tick = 0;
		int i = 8;
		boolean lag = false;

		@Override
		public void renderWidget(MatrixStack matrix, int mouseX, int mouseY, float tick) {
			boolean hovered = EmailUtils.isInRange(mouseX, mouseY, this.x, this.y, this.width, this.height);

			GlStateManager.pushMatrix();
			GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
//			if(this.guiId == EmailGuiHandler.EMAIL_SEND) {
//				mc.getTextureManager().bindTexture(hovered ? inbox_hover : inbox);
//				mc.getTextureManager().bindTexture(hovered ? send_email : send_email_hover);
//			}else if(this.guiId == EmailGuiHandler.EMAIL_MAIN) {
				mc.getTextureManager().bindTexture(hovered ? inbox_hover : inbox);
//			}
			blit(matrix, this.x, this.y, 0, 0, this.width, this.height, this.width, this.height);
			GlStateManager.popMatrix();

			if((EmailMain.getUnread() > 0 || EmailMain.getUnaccepted() > 0) && this.guiId == GuiHandler.EMAIL_MAIN) {
				GlStateManager.pushMatrix();
				GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
				mc.getTextureManager().bindTexture(email);
				if(this.gui instanceof ChatScreen) {
					blit(matrix, this.x - 25 + this.i - 1, this.y + 3, 92, 15, 17, 6);
				}else if(this.gui instanceof CreativeScreen) {
					blit(matrix, this.x + 7, this.y + 15 + this.i - 1, 88, 0, 6, 14);
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
			if(hovered) {
				this.gui.renderTooltip(matrix, getMessage(), mouseX, mouseY);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	private static class InventoryButton extends Button {
		private final Screen gui;
		private boolean recipeBookOpen = false;
		private final Minecraft mc = Minecraft.getInstance();

		public InventoryButton(Screen gui, int guiId, int x, int y, String buttonText) {
			super(x, y, 9, 6, new StringTextComponent(buttonText), btn -> GuiHandler.openGui(guiId));
			this.gui = gui;
//			if(this.gui instanceof InventoryScreen && Minecraft.getInstance().player.getRecipeBook()!=null && ((InventoryScreen) this.gui).getRecipeBookComponent().isVisible()) {
//				this.recipeBookOpen = true;
//			}
		}
		
		int tick = 0;
		int i = 8;
		boolean lag = false;

		@Override
		public void renderWidget(MatrixStack matrix, int mouseX, int mouseY, float tick) {
			if(this.visible) {
				int x = this.x;
				if(this.gui instanceof InventoryScreen) {
					InventoryScreen inv = (InventoryScreen) this.gui;
//					if(Minecraft.getInstance().player.getRecipeBook()!=null && inv.getRecipeBookComponent().isVisible()) {
//						x = inv.getGuiLeft() + 27;
//					}else {
						if(this.recipeBookOpen) {
							x = this.x - 77;
						}else {
							x = this.x;
						}
//					}
				}

				boolean hovered = EmailUtils.isInRange(mouseX, mouseY, x, this.y, this.width, this.height);

				GlStateManager.pushMatrix();
				GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
				mc.getTextureManager().bindTexture(hovered ? inbox_hover : inbox);
				blit(matrix, x, this.y, 0, 0, this.width, this.height, this.width, this.height);
				GlStateManager.popMatrix();

				if(EmailMain.getUnread() > 0 || EmailMain.getUnaccepted() > 0) {
					GlStateManager.pushMatrix();
					GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
					mc.getTextureManager().bindTexture(email);
					blit(matrix, x + 9 + this.i + 1, this.y, 73, 15, 15, 6);
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
				if(hovered) {
					this.gui.renderTooltip(matrix, getMessage(), mouseX, mouseY);
				}
			}
		}
	}
}
