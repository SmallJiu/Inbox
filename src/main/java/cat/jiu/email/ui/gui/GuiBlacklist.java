package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.util.Arrays;

import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.gui.component.GuiImageButton;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;

import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.MsgBlacklist;
import cat.jiu.email.net.msg.refresh.MsgRefreshBlacklist;
import cat.jiu.email.ui.container.ContainerInboxBlacklist;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class GuiBlacklist extends ContainerScreen<ContainerInboxBlacklist> {
	public static final ResourceLocation bg = new ResourceLocation(EmailMain.MODID, "textures/gui/container/blacklist.png");

	protected int[] nameIndex;
	
	public GuiBlacklist(ContainerInboxBlacklist container, PlayerInventory inventory, ITextComponent t) {
		super(container, inventory, new TranslationTextComponent("info.email.black.info"));
		this.xSize = 160;
		this.ySize = 176;
		this.goName(0);
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@Override
	public void init() {
		super.init();
		EmailMain.net.sendMessageToServer(new MsgRefreshBlacklist.Refresh());

		this.addButton(new Button(this.guiLeft + 6, this.guiTop + 159, 75, this.font.FONT_HEIGHT + 4, new TranslationTextComponent("info.email.black.back"), btn-> GuiHandler.openGui(GuiHandler.EMAIL_MAIN)));
		this.addButton(new Button(this.guiLeft + 6 + 75, this.guiTop + 159, 75, this.font.FONT_HEIGHT + 4, new TranslationTextComponent("info.email.black.add"), btn-> getMinecraft().displayGuiScreen(new GuiAddBlacklist(container.getBlacklist()))));
		this.addButton(new Button(this.guiLeft + this.xSize - 12 - 2, this.guiTop + 3,
				11, this.font.FONT_HEIGHT + 2, ITextComponent.getTextComponentOrEmpty("R"), btn-> {
			if(container.getBlacklist()!=null) {
				container.getBlacklist().clear();
				currentShowName = null;
			}
			EmailMain.net.sendMessageToServer(new MsgRefreshBlacklist.Refresh());
			btn.visible = false;
			new Thread(()->{
				long s = 1;
				while(true) {
					if(s <= 0) {
						btn.visible = true;
						return;
					}
					try {
						Thread.sleep(1000);
						s--;
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}));
		this.goName(0);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack matrix, float partialTicks, int mouseX, int mouseY) {
		super.renderBackground(matrix);
		this.getMinecraft().getTextureManager().bindTexture(bg);
		blit(matrix, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);

		this.font.drawString(matrix, I18n.format("info.email.black.title"),
				this.guiLeft + 5,
				this.guiTop + 4, Color.BLACK.getRGB());
		super.renderHoveredTooltip(matrix, mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(MatrixStack matrix, int mouseX, int mouseY) {
		for(Widget btn : this.buttons) {
			if(!(btn instanceof GuiImageButton) && btn.visible){
				this.hLine(matrix, btn.x - this.guiLeft, btn.x + btn.getWidth() - 2 - this.guiLeft, btn.y + btn.getHeight()-1 - this.guiTop, (btn.isHovered() ? Color.WHITE : Color.BLACK).getRGB());
			}
		}
		if(this.container.getBlacklist()==null || this.container.getBlacklist().isEmpty()) return;
		if(this.currentShowName==null) {
			this.goName(0);
			if(this.currentShowName==null) {
				return;
			}
		}
		
		int x = 8;
		int oriY = 7 + 9;
		{
			int y = oriY;
			for(int i = 0; i < this.currentShowName.length; i++) {
				String name = this.container.getBlacklist().get(this.currentShowName[i]);
				if(this.font.getStringWidth(name) >= 130) {
					name = this.font.trimStringToWidth(name, 130) + "...";
				}
				this.font.drawString(matrix, name, x, y, Color.RED.getRGB());
				this.font.drawString(matrix, "x", x + 140, y, Color.RED.getRGB());
				y += this.font.FONT_HEIGHT + 3;
			}
		}
		{
			int y = oriY;
			for(int i = 0; i < this.currentShowName.length; i++) {
				boolean remove = false;
				if(GuiEmailMain.isInRange(mouseX, mouseY, this.guiLeft + x + 138, this.guiTop + y, 9, 9)) {
					this.hLine(matrix, x + 138, x + 138 + 9, y-1, Color.RED.getRGB());
					this.hLine(matrix, x + 138, x + 138 + 9, y-1 + 9, Color.RED.getRGB());
					this.vLine(matrix, x + 137 + 1, y-1, y+9, Color.RED.getRGB());
					this.vLine(matrix, x + 137 + 1 + 9, y-1, y+9, Color.RED.getRGB());
					remove = true;
				}
				if(GuiEmailMain.isInRange(mouseX, mouseY, this.guiLeft + x, this.guiTop + y - 2, 150, 12)) {
					this.renderTooltip(matrix, new StringTextComponent(remove ? I18n.format("info.email.black.remove") : this.container.getBlacklist().get(this.currentShowName[i])), mouseX - this.guiLeft, mouseY - this.guiTop);
					break;
				}
				y += this.font.FONT_HEIGHT + 3;
			}
		}
	}

	@SubscribeEvent
	public void onMouseScroll(GuiScreenEvent.MouseScrollEvent.Pre event) {
		int key = (int)event.getScrollDelta();
        int page = 0;
    	
    	if(EmailUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || EmailUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
    		page += 2;
    	}
    	if(EmailUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || EmailUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL)) {
    		page += 1;
    	}
    	
    	if(key > 0) {
			this.goName(-1 - page);
		}else if(key < 0) {
			this.goName(1 + page);
		}
	}

	protected int namePage;
	protected int[] currentShowName;
	
	public void goName(int page) {
		if(this.container.getBlacklist()==null || this.container.getBlacklist().isEmpty()) return;
		
		this.nameIndex = new int[this.container.getBlacklist().size()];
		for(int i = 0; i < this.nameIndex.length; i++) {
			this.nameIndex[i] = i;
		}
		if(this.container.getBlacklist().size() > 12) {
			this.namePage += page;
			if(this.namePage > this.container.getBlacklist().size()) this.namePage = this.container.getBlacklist().size();
			if(this.namePage < 0) this.namePage = 0;
			int maxPage = this.container.getBlacklist().size() - 12;
			if(this.namePage > maxPage) this.namePage = maxPage;
			
			this.currentShowName = Arrays.copyOfRange(this.nameIndex, this.namePage, 12 + this.namePage);
		}else {
			this.currentShowName = Arrays.copyOf(this.nameIndex, this.nameIndex.length);
		}
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if(this.container.getBlacklist()==null || this.container.getBlacklist().isEmpty()) return false;

		int y = 15;
		for (int j : this.currentShowName) {
			if (GuiEmailMain.isInRange(mouseX, mouseY, this.guiLeft + 146, this.guiTop +y, 10, 10)) {
				this.removeBlacklist(j);
				return true;
			}
			y += 12;
		}
		return false;
	}

	protected void removeBlacklist(int index) {
		if(this.container.getBlacklist()==null) return;
		EmailMain.net.sendMessageToServer(new MsgBlacklist.Remove(this.container.getBlacklist().get(index)));
	}
	
	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		if (keyCode == 1 || this.getMinecraft().gameSettings.keyBindInventory.isActiveAndMatches(InputMappings.getInputByCode(keyCode, keyCode))) {
			GuiHandler.openGui(GuiHandler.EMAIL_MAIN);
			return true;
        }else {
        	return super.charTyped(typedChar, keyCode);
        }
	}

	@Override
	public void onClose() {
		super.onClose();
		MinecraftForge.EVENT_BUS.unregister(this);
	}
}
