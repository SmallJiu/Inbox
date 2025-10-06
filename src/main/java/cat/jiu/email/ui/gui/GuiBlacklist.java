package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.util.Arrays;

import cat.jiu.core.util.Utils;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.ui.gui.component.GuiButton;

import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.MsgBlacklist;
import cat.jiu.email.net.msg.refresh.MsgRefreshBlacklist;
import cat.jiu.email.util.EmailUtils;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class GuiBlacklist extends Screen {
	public static final ResourceLocation bg = Utils.location(EmailMain.MODID, "textures/gui/container/blacklist.png");

	protected final Screen parent;
	protected int[] nameIndex;
	protected int leftPos;
	protected int topPos;
	
	public GuiBlacklist(Screen parent) {
		super(Component.translatable("info.inbox.black.info"));
		this.parent = parent;
		this.goName(0);
	}
	
	@Override
	public void init() {
		EmailMain.NETWORK.sendMessageToServer(MsgRefreshBlacklist.REFRESH);

		this.leftPos = (this.width - 160) / 2;
		this.topPos = (this.height - 176) / 2;

		this.addRenderableWidget(new GuiButton(this.leftPos + 6, this.topPos + 159, 75, this.font.lineHeight + 4, Component.translatable("info.inbox.black.back"), btn-> Minecraft.getInstance().setScreen(this.parent)));
		this.addRenderableWidget(new GuiButton(this.leftPos + 6 + 75, this.topPos + 159, 75, this.font.lineHeight + 4, Component.translatable("info.inbox.black.add"), btn-> getMinecraft().setScreen(new GuiAddBlacklist(this, GuiInbox.INBOX.getSenderBlacklist()))));
		this.addRenderableWidget(new GuiButton(this.leftPos + 160 - 12 - 2, this.topPos + 3,
				11, RenderUtils.fontHeight() + 2, Component.literal("R"), btn-> {
			
			GuiInbox.INBOX.getSenderBlacklist().clear();
			currentShowName = null;
			
			EmailMain.NETWORK.sendMessageToServer(MsgRefreshBlacklist.REFRESH);
			btn.active = false;
			new Thread(()->{
				try {
					Thread.sleep(1000);
					btn.active = true;
				}catch(Exception ignored) {}
			}).start();
		}));
		this.goName(0);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(graphics);
		RenderUtils.draw(graphics, bg, this.leftPos, this.topPos, 160, 176, 0, 0);
		super.render(graphics, mouseX, mouseY, partialTicks);

		RenderUtils.drawString(graphics, I18n.get("info.inbox.black.title"),
				this.leftPos + 5,
				this.topPos + 4, Color.WHITE.getRGB(), true);
        try {
            this.renderLabels(graphics, mouseX, mouseY);
        } catch (Exception ignored) {}
    }

	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		if(GuiInbox.INBOX.getSenderBlacklist().isEmpty()) return;
		if(this.currentShowName==null) {
			this.goName(0);
			if(this.currentShowName==null) {
				return;
			}
		}

		int x = this.leftPos + 8;
		int oriY = this.topPos + 7 + 9;
		{
			int y = oriY;
			for(int i = 0; i < this.currentShowName.length; i++) {
				String name = GuiInbox.INBOX.getSenderBlacklist().get(this.currentShowName[i]);
				if(RenderUtils.width(name) >= 130) {
					name = this.font.plainSubstrByWidth(name, 130) + "...";
				}
				RenderUtils.drawString(graphics, name, x, y, Color.RED.getRGB(), false);
				RenderUtils.drawString(graphics, "X", x + 140, y, Color.RED.getRGB(), false);
				y += this.font.lineHeight + 3;
			}
		}
		{
			int y = oriY;
			for(int i = 0; i < this.currentShowName.length; i++) {
				boolean remove = false;
				if(EmailUtils.isInRange(mouseX, mouseY, x + 138, y, 9, 9)) {
					RenderUtils.hLine(graphics, x + 138, y-1, 9, Color.RED.getRGB());
					RenderUtils.hLine(graphics, x + 138, y-1+9, 9, Color.RED.getRGB());
					RenderUtils.vLine(graphics, x + 138, y-1, 9, Color.RED.getRGB());
					RenderUtils.vLine(graphics, x + 138 + 9, y-1, 9, Color.RED.getRGB());
					remove = true;
				}
				if(EmailUtils.isInRange(mouseX, mouseY, x, y - 2, 150, 12)) {
					RenderUtils.drawComponentTooltip(graphics, mouseX, mouseY, Component.nullToEmpty(remove ? I18n.get("info.inbox.black.remove") : GuiInbox.INBOX.getSenderBlacklist().get(this.currentShowName[i])));
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
		if(GuiInbox.INBOX.getSenderBlacklist().isEmpty()) return;
		
		this.nameIndex = new int[GuiInbox.INBOX.getSenderBlacklist().size()];
		for(int i = 0; i < this.nameIndex.length; i++) {
			this.nameIndex[i] = i;
		}
		if(GuiInbox.INBOX.getSenderBlacklist().size() > 12) {
			this.namePage += page;
			if(this.namePage > GuiInbox.INBOX.getSenderBlacklist().size()) this.namePage = GuiInbox.INBOX.getSenderBlacklist().size();
			if(this.namePage < 0) this.namePage = 0;
			int maxPage = GuiInbox.INBOX.getSenderBlacklist().size() - 12;
			if(this.namePage > maxPage) this.namePage = maxPage;
			
			this.currentShowName = Arrays.copyOfRange(this.nameIndex, this.namePage, 12 + this.namePage);
		}else {
			this.currentShowName = Arrays.copyOf(this.nameIndex, this.nameIndex.length);
		}
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if(GuiInbox.INBOX.getSenderBlacklist().isEmpty()) return false;

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
		EmailMain.NETWORK.sendMessageToServer(new MsgBlacklist.Remove(GuiInbox.INBOX.getSenderBlacklist().get(index)));
	}
	
	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		if (keyCode == 1 || this.getMinecraft().options.keyInventory.isActiveAndMatches(InputConstants.getKey(keyCode, keyCode))) {
			Minecraft.getInstance().setScreen(this.parent);
			return true;
        }else {
        	return super.charTyped(typedChar, keyCode);
        }
	}

	@Override
	public void onClose() {
		super.onClose();
		Minecraft.getInstance().setScreen(this.parent);
	}
}
