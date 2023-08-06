package cat.jiu.email.ui.gui;

import java.awt.*;
import java.util.List;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.MsgBlacklist;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.gui.component.GuiImageButton;
import cat.jiu.email.util.EmailUtils;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class GuiAddBlacklist extends Screen {
	protected final List<String> blacklist;
	protected TextFieldWidget name;
	protected int guiLeft, guiTop, xSize, ySize;
	public GuiAddBlacklist(List<String> blacklist) {
		super(ITextComponent.getTextComponentOrEmpty(null));
		this.blacklist = blacklist;
	}

	@Override
	public void init() {
		super.init();
		this.guiLeft = (this.width + this.xSize) / 2;
		this.guiTop = (this.height + this.ySize) / 2;

		this.name = this.addListener(new TextFieldWidget(this.font, this.guiLeft, this.guiTop, 200, this.font.FONT_HEIGHT+4, ITextComponent.getTextComponentOrEmpty(null)));
		this.name.x = this.name.x - (this.name.getWidth()/2);
		this.name.setTextColor(-1);
        this.name.setDisabledTextColour(-1);
        this.name.setMaxStringLength(100);
        this.name.setEnableBackgroundDrawing(true);
		
        Button btn = new Button(this.guiLeft, this.guiTop+this.font.FONT_HEIGHT+6, this.name.getWidth() - 20,this.font.FONT_HEIGHT+6, new TranslationTextComponent("info.email.black.add"), b-> {
			String name = GuiAddBlacklist.this.name.getText();
			if(!name.isEmpty() && (blacklist == null || !blacklist.contains(name))) {
				if(!EmailAPI.isInBlockReceiveWhitelist(name)) {
					EmailMain.net.sendMessageToServer(new MsgBlacklist.Add(name));
				}else {
					Minecraft.getInstance().player.sendMessage(EmailUtils.createTextComponent(TextFormatting.YELLOW, "info.email.black.add.fail.whitelist"), Minecraft.getInstance().player.getUniqueID());
				}
			}
		});
		btn.x = btn.x - (btn.getWidth()/2);
		this.addButton(btn);
		this.addButton(new Button(btn.x, btn.y + btn.getHeight()+2, btn.getWidth(), btn.getHeight(), new TranslationTextComponent("info.email.black.back"), b-> GuiHandler.openGui(GuiHandler.EMAIL_BLACKLIST)));
	}

	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(matrix);
		super.render(matrix, mouseX, mouseY, partialTicks);
		for(Widget btn : this.buttons) {
			if(!(btn instanceof GuiImageButton) && btn.visible){
				this.hLine(matrix, btn.x, btn.x + btn.getWidth() - 2, btn.y + btn.getHeight()-1, (btn.isHovered() ? Color.WHITE : Color.BLACK).getRGB());
			}
		}
		this.name.renderWidget(matrix, mouseX, mouseY, partialTicks);

		ITextComponent text = ITextComponent.getTextComponentOrEmpty(I18n.format("info.email.black.info"));
		int x = this.guiLeft + this.xSize / 2;
		this.font.drawTextWithShadow(matrix, text, x - this.font.getStringPropertyWidth(text)/2F, this.name.y - this.font.FONT_HEIGHT - 10, Color.WHITE.getRGB());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
