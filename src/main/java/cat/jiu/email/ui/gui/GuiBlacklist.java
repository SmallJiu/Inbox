package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.MsgBlacklist;
import cat.jiu.email.net.msg.refresh.MsgRefreshBlacklist;
import cat.jiu.email.ui.container.ContainerInboxBlacklist;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

public class GuiBlacklist extends GuiContainer {
	public static final ResourceLocation bg = new ResourceLocation(EmailMain.MODID, "textures/gui/container/backlist.png");
	
	private int id = -1;
	private int nextID() {return id++;}
	protected final GuiScreen parent;
	protected final ContainerInboxBlacklist container;
	protected int[] nameIndex;
	
	public GuiBlacklist(GuiScreen parent) {
		super(new ContainerInboxBlacklist());
		this.xSize = 160;
		this.ySize = 176;
		this.container = (ContainerInboxBlacklist) super.inventorySlots;
		
		this.parent = parent;
		this.fontRenderer = Minecraft.getMinecraft().fontRenderer;
		this.goName(0);
	}
	
	protected void removeBlacklist(int index) {
		if(this.container.getBlacklist()==null) return;
		String name = this.container.getBlacklist().get(index);
		if(this.container.getBlacklist().contains(name)) {
			this.container.getBlacklist().remove(name);
			EmailMain.net.sendMessageToServer(new MsgBlacklist.Remove(name));
			this.goName(0);
			Minecraft.getMinecraft().player.sendMessage(EmailUtils.createTextComponent(TextFormatting.GREEN, "info.email.black.remove.success", name));
		}
	}
	
	@Override
	public void initGui() {
		super.initGui();
		this.buttonList.clear();
		
		if(this.container.getBlacklist()!=null) {
			this.container.getBlacklist().clear();
			this.currentShowName = null;
		}
		EmailMain.net.sendMessageToServer(new MsgRefreshBlacklist.Refresh());
		
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		this.addButton(new GuiButton(nextID(),
				x + 6,
				y + 159,
				75, this.fontRenderer.FONT_HEIGHT + 4, I18n.format("info.email.black.back")) {
			public void mouseReleased(int mouseX, int mouseY) {
				mc.displayGuiScreen(parent);
			}
		});
		this.addButton(new GuiButton(nextID(),
				x + 6 + 75,
				y + 159,
				75, this.fontRenderer.FONT_HEIGHT + 4, I18n.format("info.email.black.add")) {
			public void mouseReleased(int mouseX, int mouseY) {
				mc.displayGuiScreen(new GuiAddBlacklist(GuiBlacklist.this, container.getBlacklist()));
			}
		});
		
		this.addButton(new GuiButton(nextID(),
				x + this.xSize - 12 - 2, y + 3,
				11, this.fontRenderer.FONT_HEIGHT + 2, "R") {
			public void mouseReleased(int mouseX, int mouseY) {
				if(container.getBlacklist()!=null) {
					container.getBlacklist().clear();
					currentShowName = null;
				}
				EmailMain.net.sendMessageToServer(new MsgRefreshBlacklist.Refresh());
				this.enabled = false;
				new Thread(()->{
					long s = 1;
					while(true) {
						if(s <= 0) {
							this.enabled = true;
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
			}
		});
		this.goName(0);
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawDefaultBackground();
		GlStateManager.popMatrix();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(bg);
		this.drawTexturedModalRect((this.width - this.xSize) / 2, (this.height - this.ySize) / 2, 0, 0, this.xSize, this.ySize);
		GlStateManager.pushMatrix();
		this.fontRenderer.drawString(I18n.format("info.email.black.title"), 
				(this.width - this.xSize) / 2 + 5, 
				(this.height - this.ySize) / 2 + 4, Color.BLACK.getRGB());
		super.drawScreen(mouseX, mouseY, partialTicks);
		super.renderHoveredToolTip(mouseX, mouseY);
		
		for(int i = 0; i < this.buttonList.size(); i++) {
			GuiButton btn = this.buttonList.get(i);
			this.drawHorizontalLine(btn.x, btn.x + btn.width - 2, btn.y + btn.height-1, Color.BLACK.getRGB());
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		if(this.container.getBlacklist()==null || this.container.getBlacklist().isEmpty()) return;
		if(this.currentShowName==null) {
			this.goName(0);
			if(this.currentShowName==null) {
				return;
			}
		}
		
		int x = (this.width - this.xSize) / 2 + 8; 
		int oriY = (this.height - this.ySize) / 2 + 7 + 9;
		{
			int y = oriY;
			for(int i = 0; i < this.currentShowName.length; i++) {
				String name = this.container.getBlacklist().get(this.currentShowName[i]);
				if(this.fontRenderer.getStringWidth(name) >= 130) {
					name = this.fontRenderer.trimStringToWidth(name, 130) + "...";
				}
				this.fontRenderer.drawString(name, x, y, Color.RED.getRGB());
				this.fontRenderer.drawString("x", x + 140, y, Color.RED.getRGB());
				y += this.fontRenderer.FONT_HEIGHT + 3;
			}
		}
		{
			int y = oriY;
			for(int i = 0; i < this.currentShowName.length; i++) {
				boolean remove = false;
				if(GuiEmailMain.isInRange(mouseX, mouseY, x + 138, y, 9, 9)) {
					this.drawHorizontalLine(x + 138, x + 138 + 9, y-1, Color.RED.getRGB());
					this.drawHorizontalLine(x + 138, x + 138 + 9, y-1 + 9, Color.RED.getRGB());
					this.drawVerticalLine(x + 137 + 1, y-1, y+9, Color.RED.getRGB());
					this.drawVerticalLine(x + 137 + 1 + 9, y-1, y+9, Color.RED.getRGB());
					remove = true;
				}
				if(GuiEmailMain.isInRange(mouseX, mouseY, x, y - 2, 150, 12)) {
					this.drawHoveringText(remove ? I18n.format("info.email.black.remove") : this.container.getBlacklist().get(this.currentShowName[i]), mouseX, mouseY);
					break;
				}
				y += this.fontRenderer.FONT_HEIGHT + 3;
			}
		}
	}
	
	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int key = Mouse.getEventDWheel();
        int page = 0;
    	
    	if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
    		page += 2;
    	}
    	if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
    		page += 1;
    	}
    	
    	if(key == 120) {
			this.goName(-1 - page);
		}else if(key == -120) {
			this.goName(1 + page);
		}
	}
	
	protected int currentName = 0;
	protected int namePage;
	protected int[] currentShowName;
	
	protected void goName(int page) {
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
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if(this.container.getBlacklist()==null || this.container.getBlacklist().isEmpty()) return;
		
		int x = (this.width - this.xSize) / 2 + 8;
		int y = (this.height - this.ySize) / 2 + 7 + 9;
		for(int i = 0; i < this.currentShowName.length; i++) {
			if(GuiEmailMain.isInRange(mouseX, mouseY, x + 138, y, 9, 9)) {
				this.removeBlacklist(this.currentShowName[i]);
				break;
			}
			y += this.fontRenderer.FONT_HEIGHT + 3;
		}
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == 1 || this.mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
			this.mc.displayGuiScreen(this.parent);
        }else {
        	super.keyTyped(typedChar, keyCode);
        }
	}
}
