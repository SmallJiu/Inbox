package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.MsgBlacklist;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.text.TextFormatting;

public class GuiAddBlacklist extends GuiContainer {
	protected final GuiScreen parent;
	protected final List<String> blacklist;
	protected GuiTextField name;
	public GuiAddBlacklist(GuiScreen parent, List<String> blacklist) {
		super(new Container() {
			public boolean canInteractWith(EntityPlayer playerIn) {
				return true;
			}
		});
		this.parent = parent;
		this.blacklist = blacklist;
	}
	
	@Override
	public void initGui() {
		super.initGui();
		this.buttonList.clear();
		
		ScaledResolution sr = new ScaledResolution(this.mc);
		int x = sr.getScaledWidth() / 2;
        int y = sr.getScaledHeight() / 2;
		
		this.name = new GuiTextField(0, this.fontRenderer, x, y, 200, this.fontRenderer.FONT_HEIGHT+4);
		this.name.x = this.name.x - (this.name.width/2);
		this.name.setTextColor(-1);
        this.name.setDisabledTextColour(-1);
        this.name.setMaxStringLength(100);
        this.name.setEnableBackgroundDrawing(true);
		
        GuiButton btn = new GuiButton(1, x,y+this.fontRenderer.FONT_HEIGHT+6, this.name.width - 20,this.fontRenderer.FONT_HEIGHT+6, I18n.format("info.email.black.add")) {
			public void mouseReleased(int mouseX, int mouseY) {
				String name = GuiAddBlacklist.this.name.getText();
				if(!name.isEmpty() && (blacklist == null || !blacklist.contains(name))) {
					if(!EmailAPI.isInBlockReceiveWhitelist(name)) {
						EmailMain.net.sendMessageToServer(new MsgBlacklist.Add(name));
						Minecraft.getMinecraft().player.sendMessage(EmailUtils.createTextComponent(TextFormatting.GREEN, "info.email.black.add.success", name));
						mc.displayGuiScreen(parent);
					}else {
						Minecraft.getMinecraft().player.sendMessage(EmailUtils.createTextComponent(TextFormatting.YELLOW, "info.email.black.add.fail.whitelist"));
					}
				}
			}
		};
		btn.x = btn.x - (btn.width/2);
		this.buttonList.add(btn);
		this.addButton(new GuiButton(2,
				btn.x, btn.y + btn.height+2,
				btn.width, btn.height, I18n.format("info.email.black.back")) {
			public void mouseReleased(int mouseX, int mouseY) {
				mc.displayGuiScreen(parent);
			}
		});
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		super.renderHoveredToolTip(mouseX, mouseY);
		this.name.drawTextBox();
		for(int i = 0; i < this.buttonList.size(); i++) {
			GuiButton btn = this.buttonList.get(i);
			this.drawHorizontalLine(btn.x, btn.x + btn.width - 2, btn.y + btn.height-1, Color.BLACK.getRGB());
		}
	}
	
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		this.name.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if(!this.name.textboxKeyTyped(typedChar, keyCode)) {
			if (keyCode == 1 || this.mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
				this.mc.displayGuiScreen(this.parent);
	        }else {
	        	super.keyTyped(typedChar, keyCode);
	        }
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		ScaledResolution sr = new ScaledResolution(this.mc);
		int x = sr.getScaledWidth() / 2;
        int y = sr.getScaledHeight() / 2;
        
        String text = I18n.format("info.email.black.info");
        this.fontRenderer.drawString(text, x - (this.fontRenderer.getStringWidth(text)/2), y - (this.fontRenderer.FONT_HEIGHT + 4), Color.WHITE.getRGB());
	}
}
