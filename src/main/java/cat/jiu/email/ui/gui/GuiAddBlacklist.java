package cat.jiu.email.ui.gui;

import java.awt.*;
import java.util.List;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.MsgBlacklist;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.gui.component.GuiButton;
import cat.jiu.email.ui.gui.component.GuiImageButton;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiAddBlacklist extends Screen {
	protected final List<String> blacklist;
	protected EditBox name;
	protected int leftPos, topPos, xSize, ySize;
	public GuiAddBlacklist(List<String> blacklist) {
		super(Component.nullToEmpty(null));
		this.blacklist = blacklist;
	}

	@Override
	public void init() {
		super.init();
		this.leftPos = (this.width + this.xSize) / 2;
		this.topPos = (this.height + this.ySize) / 2;

		this.name = this.addRenderableWidget(new EditBox(this.font, this.leftPos, this.topPos, 200, this.font.lineHeight+4, Component.nullToEmpty(null)));
		this.name.setX(this.name.getX() - (this.name.getWidth()/2));
		this.name.setTextColor(-1);
        this.name.setTextColorUneditable(-1);
        this.name.setMaxLength(100);

		GuiButton btn = new GuiButton(this.leftPos, this.topPos+this.font.lineHeight+6, this.name.getWidth() - 20,this.font.lineHeight+6, Component.translatable("info.email.black.add"), b-> {
			String name = this.name.getValue();
			if(!name.isEmpty() && (blacklist == null || !blacklist.contains(name))) {
				if(!EmailAPI.isInBlockReceiveWhitelist(name)) {
					EmailMain.net.sendMessageToServer(new MsgBlacklist.Add(name));
				}else {
					Minecraft.getInstance().player.sendSystemMessage(EmailUtils.createTextComponent(ChatFormatting.YELLOW, "info.email.black.add.fail.whitelist"));
				}
			}
		});
		btn.setX(btn.getX() - (btn.getWidth()/2));
		this.addRenderableWidget(btn);
		this.addRenderableWidget(new GuiButton(btn.getX(), btn.getY() + btn.getHeight()+2, btn.getWidth(), btn.getHeight(), Component.translatable("info.email.black.back"), b-> GuiHandler.openGui(GuiHandler.EMAIL_BLACKLIST)));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTicks);
		this.children().forEach(listener -> {
			if(listener instanceof Button btn && !(btn instanceof GuiImageButton) && btn.visible){
				graphics.hLine(btn.getX(), btn.getX() + btn.getWidth() - 2, btn.getY() + btn.getHeight()-1, (btn.isHovered() ? Color.WHITE : Color.BLACK).getRGB());
			}
		});
		this.name.renderWidget(graphics, mouseX, mouseY, partialTicks);

		Component text = Component.nullToEmpty(I18n.get("info.email.black.info"));
		int x = this.leftPos + this.xSize / 2;
		graphics.drawString(this.font, text, x - this.font.width(text)/2, this.name.getY() - this.font.lineHeight - 10, Color.WHITE.getRGB());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
