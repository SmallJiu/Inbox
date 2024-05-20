package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.util.Arrays;

import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.gui.component.GuiButton;
import cat.jiu.email.ui.gui.component.GuiImageButton;

import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.MsgBlacklist;
import cat.jiu.email.net.msg.refresh.MsgRefreshBlacklist;
import cat.jiu.email.ui.container.ContainerInboxBlacklist;
import cat.jiu.email.util.ClientUtil;
import cat.jiu.email.util.EmailUtils;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class GuiBlacklist extends AbstractContainerScreen<ContainerInboxBlacklist> {
	public static final ResourceLocation bg = new ResourceLocation(EmailMain.MODID, "textures/gui/container/blacklist.png");

	protected int[] nameIndex;
	
	public GuiBlacklist(ContainerInboxBlacklist container, Inventory inventory) {
		super(container, inventory, new TranslatableComponent("info.email.black.info"));
		this.imageWidth = 160;
		this.imageHeight = 176;
		this.goName(0);
	}
	
	@Override
	public void init() {
		super.init();
		EmailMain.net.sendMessageToServer(new MsgRefreshBlacklist.Refresh());

		this.addRenderableWidget(new GuiButton(this.leftPos + 6, this.topPos + 159, 75, this.font.lineHeight + 4, new TranslatableComponent("info.email.black.back"), btn-> GuiHandler.openGui(GuiHandler.EMAIL_MAIN)));
		this.addRenderableWidget(new GuiButton(this.leftPos + 6 + 75, this.topPos + 159, 75, this.font.lineHeight + 4, new TranslatableComponent("info.email.black.add"), btn-> getMinecraft().setScreen(new GuiAddBlacklist(this.getMenu().getBlacklist()))));
		this.addRenderableWidget(new GuiButton(this.leftPos + this.width - 12 - 2, this.topPos + 3,
				11, this.font.lineHeight + 2, Component.nullToEmpty("R"), btn-> {
			if(this.getMenu().getBlacklist()!=null) {
				this.getMenu().getBlacklist().clear();
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
	protected void renderBg(PoseStack stack, float partialTicks, int mouseX, int mouseY) {
		super.renderBackground(stack);
		ClientUtil.bindTexture(bg);
		blit(stack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

		EmailUtils.drawString(stack,this.font, I18n.get("info.email.black.title"),
				this.leftPos + 5,
				this.topPos + 4, Color.WHITE.getRGB(), false);
		super.renderTooltip(stack, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(PoseStack stack, int mouseX, int mouseY) {
		this.children().forEach(listener -> {
			if(listener instanceof Button button && !(button instanceof GuiImageButton) && button.visible){
				hLine(stack, button.x - this.leftPos, button.x + button.getWidth() - 2 - this.leftPos, button.y + button.getHeight()-1 - this.topPos, (button.isHoveredOrFocused() ? Color.WHITE : Color.BLACK).getRGB());
			}
		});
		if(this.getMenu().getBlacklist()==null || this.getMenu().getBlacklist().isEmpty()) return;
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
				String name = this.getMenu().getBlacklist().get(this.currentShowName[i]);
				if(this.font.width(name) >= 130) {
					name = this.font.plainSubstrByWidth(name, 130) + "...";
				}
				EmailUtils.drawString(stack, this.font, name, x, y, Color.RED.getRGB(), false);
				EmailUtils.drawString(stack, this.font, "x", x + 140, y, Color.RED.getRGB(), false);
				y += this.font.lineHeight + 3;
			}
		}
		{
			int y = oriY;
			for(int i = 0; i < this.currentShowName.length; i++) {
				boolean remove = false;
				if(EmailUtils.isInRange(mouseX, mouseY, this.leftPos + x + 138, this.topPos + y, 9, 9)) {
					hLine(stack, x + 138, x + 138 + 9, y-1, Color.RED.getRGB());
					hLine(stack, x + 138, x + 138 + 9, y-1 + 9, Color.RED.getRGB());
					vLine(stack, x + 137 + 1, y-1, y+9, Color.RED.getRGB());
					vLine(stack, x + 137 + 1 + 9, y-1, y+9, Color.RED.getRGB());
					remove = true;
				}
				if(EmailUtils.isInRange(mouseX, mouseY, this.leftPos + x, this.topPos + y - 2, 150, 12)) {
					renderTooltip(stack, Component.nullToEmpty(remove ? I18n.get("info.email.black.remove") : this.getMenu().getBlacklist().get(this.currentShowName[i])), mouseX - this.leftPos, mouseY - this.topPos);
					break;
				}
				y += this.font.lineHeight + 3;
			}
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double key) {
        int page = 0;
    	if(EmailUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || EmailUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
    		page += 2;
    	}
    	if(EmailUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || EmailUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL)) {
    		page += 1;
    	}
    	if(key > 0) {
			this.goName(-1 - page);
			return true;
		}else if(key < 0) {
			this.goName(1 + page);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, key);
	}

	protected int namePage;
	protected int[] currentShowName;
	
	public void goName(int page) {
		if(this.getMenu().getBlacklist()==null || this.getMenu().getBlacklist().isEmpty()) return;
		
		this.nameIndex = new int[this.getMenu().getBlacklist().size()];
		for(int i = 0; i < this.nameIndex.length; i++) {
			this.nameIndex[i] = i;
		}
		if(this.getMenu().getBlacklist().size() > 12) {
			this.namePage += page;
			if(this.namePage > this.getMenu().getBlacklist().size()) this.namePage = this.getMenu().getBlacklist().size();
			if(this.namePage < 0) this.namePage = 0;
			int maxPage = this.getMenu().getBlacklist().size() - 12;
			if(this.namePage > maxPage) this.namePage = maxPage;
			
			this.currentShowName = Arrays.copyOfRange(this.nameIndex, this.namePage, 12 + this.namePage);
		}else {
			this.currentShowName = Arrays.copyOf(this.nameIndex, this.nameIndex.length);
		}
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if(this.getMenu().getBlacklist()==null || this.getMenu().getBlacklist().isEmpty()) return false;

		int y = 15;
		for (int j : this.currentShowName) {
			if (EmailUtils.isInRange(mouseX, mouseY, this.leftPos + 146, this.topPos +y, 10, 10)) {
				this.removeBlacklist(j);
				return true;
			}
			y += 12;
		}
		return false;
	}

	protected void removeBlacklist(int index) {
		if(this.getMenu().getBlacklist()==null) return;
		EmailMain.net.sendMessageToServer(new MsgBlacklist.Remove(this.getMenu().getBlacklist().get(index)));
	}
	
	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		if (keyCode == 1 || this.getMinecraft().options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, keyCode))) {
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
