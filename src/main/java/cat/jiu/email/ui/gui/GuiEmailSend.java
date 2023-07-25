package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.util.List;

import com.google.common.collect.Lists;

import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.gui.component.GuiImageButton;
import cat.jiu.email.util.client.ShowInboxGui;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.ui.container.ContainerEmailSend;
import cat.jiu.email.ui.gui.component.GuiTime;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.TimeMillis;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class GuiEmailSend extends ContainerScreen<ContainerEmailSend> {
	public static final ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_send.png");
	public static final ResourceLocation EXPIRATION = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_expiration.png");
	private TextFieldWidget nameField;
    private TextFieldWidget titleField;
    private final TextFieldWidget[] textFields = new TextFieldWidget[5];
    private final GuiTime expiration = new GuiTime(this, false);
    
	public GuiEmailSend(ContainerEmailSend container, PlayerInventory inventory, ITextComponent t) {
		super(container, inventory, ITextComponent.getTextComponentOrEmpty(null));
		this.xSize = 176;
		this.ySize = 233;
	}
	
	@Override
	public void init() {
		super.init();
		this.addListener(expiration);
		this.nameField = this.addListener(new NameGuiTextField(this.font, this.getGuiLeft() + 39, this.getGuiTop() + 6, 109, 11));
        this.nameField.setTextColor(-1);
        this.nameField.setDisabledTextColour(-1);
        this.nameField.setMaxStringLength(100);
        this.nameField.setEnableBackgroundDrawing(false);
        
        this.titleField = this.addListener(new TextFieldWidget(this.font, this.getGuiLeft() + 39, this.getGuiTop() + 20, 109, 11, ITextComponent.getTextComponentOrEmpty(null)));
		this.titleField.setTextColor(-1);
		this.titleField.setDisabledTextColour(-1);
		this.titleField.setMaxStringLength(100);
		this.titleField.setEnableBackgroundDrawing(false);
        this.initText();

        this.addButton(new GuiImageButton(this, this.guiLeft + 149 + 28, this.guiTop + 3, 16, 16, I18n.format("email.config.expiration"), EXPIRATION, 256, 256, 256, 256, btn->
			expiration.setEnable(!expiration.isEnable())
        ));
        
        this.addButton(new GuiImageButton(this, this.nameField.x+this.nameField.getWidth()+1, this.nameField.y-2, 22, this.nameField.getHeight()+2, I18n.format("info.email.dispatch"), BackGround,  256, 256, 176, 9, 59, 50, btn-> {
			if(!container.isCooling() && !container.isLock()) {
				String name = nameField.getText();
				if(StringUtils.isNullOrEmpty(name)) {
					setRenderText(I18n.format("info.email.error.empty_name"), Color.RED);
					return;
				}
				String title = titleField.getText();
				if(StringUtils.isNullOrEmpty(title)) {
					title = "info.email.default_title";
				}

				if(textsIsEmpty() && container.isEmpty()) {
					setRenderText(I18n.format("info.email.error.empty_msgs_item"), Color.RED);
					return;
				}

				List<IText> msgs = Lists.newArrayList();
				if(!textsIsEmpty()) {
					for (TextFieldWidget textField : textFields) {
						String msg = textField.getText();
						if (msg.isEmpty()) {
							msgs.add(Text.empty);
							continue;
						}

						StringBuilder sb = new StringBuilder();
						for (int k = 0; k < msg.length(); k++) {
							char ch = msg.charAt(k);
							if (ch == '&' && k + 1 < msg.length()) {
								if (isFormatChar(msg.charAt(k + 1))) {
									sb.append('§');
								}
								sb.append(ch);
							} else {
								sb.append(ch);
							}
						}
						msgs.add(new Text(sb.toString()));
					}
				}else {
					msgs.add(new Text("info.email.default_msg"));
				}
				Email email = new Email(new Text(title), new Text(Minecraft.getInstance().player.getName()), null, null, msgs);

				long expiration = GuiEmailSend.this.expiration.getTimeOfMillis();
				if(expiration>0) {
					email.setExpirationTime(new TimeMillis(expiration));
				}

				EmailAPI.sendPlayerEmail(getMinecraft().player, name, email);
				clearRenderText();
			}
        }));

		this.addButton(new GuiImageButton(this, this.titleField.x+this.titleField.getWidth()+1, this.titleField.y-2, 22, this.nameField.getHeight()+2, I18n.format("info.email.name"), ShowInboxGui.inbox, 23, 15, 23, 15, b-> GuiHandler.openGui(GuiHandler.EMAIL_MAIN)));
	}
	
	private void initText() {
		for(int i = 0; i < this.textFields.length; i++) {
			this.textFields[i] = new TextFieldWidget(this.font, this.guiLeft+9, this.guiTop+34 + (12 * i), 155, 12, ITextComponent.getTextComponentOrEmpty(null));
			
			TextFieldWidget field = this.textFields[i];
			field.setTextColor(Color.WHITE.getRGB());
			field.setDisabledTextColour(Color.WHITE.getRGB());
			field.setMaxStringLength(256);
			field.setEnableBackgroundDrawing(false);
			this.addListener(field);
		}
	}
	
	private boolean isFormatChar(char c) {
		for(TextFormatting format : TextFormatting.values()) {
			if(format.formattingCode == c) return true;
		}
		return false;
	}
	
	public boolean textsIsEmpty() {
		for (TextFieldWidget tf : this.textFields) {
			if (!StringUtils.isNullOrEmpty(tf.getText())) return false;
		}
		return true;
	}
	
	public void setRenderText(String text) {
		this.setRenderText(text, Color.RED);
	}
	public void setRenderText(String text, Color color) {
		this.setRenderText(text, color, EmailUtils.parseTick(0,0,0,15, 0));
	}
	public void setRenderText(String text, Color color, long ticks) {
		this.renderText = text;
		this.renderColor = color;
		this.renderTicks = ticks;
	}
	public void clearRenderText() {
		this.renderText = null;
		this.renderColor = null;
		this.renderTicks = 0;
	}
	
	private long renderTicks = 0;
	private String renderText;
	private Color renderColor;

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if(!this.container.isLock()){
			return super.mouseClicked(mouseX, mouseY, mouseButton);
		}
		return true;
    }

	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		if(!this.container.isLock()){
			if(this.expiration.charTyped(typedChar, keyCode)){
				return true;
			}
			for (IGuiEventListener listener : this.getEventListeners()) {
				if(listener instanceof TextFieldWidget){
					if(((TextFieldWidget) listener).isFocused()){
						if(listener.charTyped(typedChar, keyCode)){
							return true;
						}
					}
				}else if(listener.charTyped(typedChar, keyCode)){
					return true;
				}
			}
			return super.charTyped(typedChar, keyCode);
		}
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if(!this.container.isLock()){
			if(this.expiration.keyPressed(keyCode, scanCode, modifiers)){
				return true;
			}
			boolean isFocused = false;
			boolean flag = false;

			if(this.nameField.isFocused()){
				isFocused = true;
				if(this.nameField.keyPressed(keyCode, scanCode, modifiers)){
					flag = true;
				}
			}
			if(this.titleField.isFocused()){
				isFocused = true;
				if(this.titleField.keyPressed(keyCode, scanCode, modifiers)){
					flag = true;
				}
			}

			for (TextFieldWidget tf : this.textFields) {
				if(tf.isFocused()){
					isFocused = true;
					if(tf.keyPressed(keyCode, scanCode, modifiers)){
						flag = true;
					}
				}
			}
			return flag || (isFocused && keyCode != 256) || super.keyPressed(keyCode, scanCode, modifiers);
		}
		return true;
	}

	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(matrix);
		super.render(matrix, mouseX, mouseY, partialTicks);
		super.renderHoveredTooltip(matrix, mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack matrix, float partialTicks, int mouseX, int mouseY) {
		this.getMinecraft().getTextureManager().bindTexture(BackGround);
		this.blit(matrix, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
		this.titleField.renderWidget(matrix, mouseX, mouseY, partialTicks);
		this.nameField.renderWidget(matrix, mouseX, mouseY, partialTicks);
		for(TextFieldWidget tf : this.textFields) {
			tf.renderWidget(matrix, mouseX, mouseY, partialTicks);
			this.font.drawString(matrix, tf.getText().length()+"/"+tf.getMaxStringLength(), tf.x+tf.getWidth()+13, tf.y+2, (tf.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.WHITE).getRGB());
		}
		this.expiration.render(matrix, this.guiLeft + 149 + 22 + 10, this.guiTop + 20 + 80, partialTicks);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(MatrixStack matrix, int mouseX, int mouseY) {
		if(EmailConfigs.Send.Enable_Inbox_Button.get()) {
			if(GuiEmailMain.isInRange(mouseX, mouseY, this.guiLeft+149, this.guiTop+20, 22, 10)) {
				this.renderTooltip(matrix, ITextComponent.getTextComponentOrEmpty(TextFormatting.RED + I18n.format("info.email.send.warn")), mouseX, this.guiTop+48);
			}
		}

		int color = this.container.isLock() ? Color.RED.getRGB() : Color.GREEN.getRGB();
		fill(matrix, 162, 100, 162 + 9, 100 + 9, color);

		drawAlignRightString(matrix, this.font, I18n.format("info.email.addressee") + ":", 36, 6, (this.nameField.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.BLACK).getRGB(), false);
		drawAlignRightString(matrix, this.font, I18n.format("info.email.title") + ":", 36, 21, (this.titleField.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.BLACK).getRGB(), false);

		if(this.renderTicks > 0 && this.renderText != null && this.renderColor != null) {
			if(this.renderTicks <= 0) clearRenderText();
			this.font.drawString(matrix, this.renderText, (88 - this.font.getStringWidth(this.renderText) / 2f), 138, this.renderColor.getRGB());
			this.renderTicks--;
		}
		if(this.container.renderTicks > 0 && this.container.renderText != null && this.container.renderColor != null) {
			if(this.container.renderTicks <= 0) this.clearRenderText();
			this.font.drawString(matrix, this.container.renderText, (88 - this.font.getStringWidth(this.container.renderText) / 2f), 138, this.container.renderColor.getRGB());
			this.container.renderTicks--;
		}
		if(this.container.isCooling()) {
			long millis = this.container.getCoolingMillis() - System.currentTimeMillis();
			long t_0 = millis % 1000;
			long t_t = t_0 / 50;

			long t_s = millis / 1000;
			long t_m = 0;
			if(t_s >= 60) {
				t_m = t_s / 60;
				t_s %= 60;
			}
			long t_h = 0;
			if(t_m >= 60) {
				t_h = t_m / 60;
				t_m %= 60;
			}
			long t_d = 0;
			if(t_h >= 24) {
				t_d = t_h / 24;
				t_h %= 24;
			}
			String d = t_d < 10 ? "0" + t_d : Long.toString(t_d);
			String h = t_h < 10 ? "0" + t_h : Long.toString(t_h);
			String m = t_m < 10 ? "0" + t_m : Long.toString(t_m);
			String s = t_s < 10 ? "0" + t_s : Long.toString(t_s);
			String t = t_t < 10 ? "0" + t_t : Long.toString(t_t);

			String text = I18n.format("info.email.cooling", d, h, m, s, t);
			this.font.drawString(matrix, text, this.xSize/2f - this.font.getStringWidth(text)/2f, 60, Color.RED.getRGB());
		}
		if(this.titleField.getText().isEmpty()) {
			this.font.drawString(matrix, I18n.format("info.email.default_title"), 39, 21, Color.BLACK.getRGB());
		}
		if(this.textsIsEmpty()) {
			this.font.drawString(matrix, I18n.format("info.email.default_msg"), 10, 34, Color.BLACK.getRGB());
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public static class NameGuiTextField extends TextFieldWidget {
		public NameGuiTextField(FontRenderer fontrenderer, int x, int y, int par5Width, int par6Height) {
			super(fontrenderer, x, y, par5Width, par6Height, ITextComponent.getTextComponentOrEmpty(null));
		}

		private int index = 0;

		@Override
		public boolean charTyped(char codePoint, int modifiers) {
			return super.charTyped(codePoint, modifiers);
		}

		@Override
		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if(this.isFocused()){
				if(keyCode == GLFW.GLFW_KEY_TAB) {
					List<? extends PlayerEntity> playerList = Minecraft.getInstance().player.getEntityWorld().getPlayers();
					ITextComponent name;

					if(this.index < playerList.size()) {
						name = playerList.get(this.index).getName();
						if(name.equals(Minecraft.getInstance().player.getName())
								&& EmailConfigs.Send.Enable_Send_To_Self.get()) {
							super.setText(name.getString());
							this.index++;
							return true;
						}
						super.setText(name.getString());
						this.index++;
					}else if(this.index < playerList.size() + 2) {
						this.absIndex++;
						if(this.absIndex==1) {
							super.setText("@p");
						}else if(this.absIndex==2) {
							super.setText("@a");
						}
						if(this.absIndex >= 2) {
							this.absIndex = 0;
							this.index = 0;
						}
					}
					return true;
				}
			}

			return super.keyPressed(keyCode, scanCode, modifiers);
		}

		protected int absIndex = 0;

		@Override
		public void renderWidget(MatrixStack matrix, int x, int y, float t) {
			super.renderWidget(matrix, x, y, t);
			if("@p".equals(this.getText())) {
				drawAlignRightString(matrix, Minecraft.getInstance().fontRenderer, I18n.format("info.email.@p"), this.x + this.width - 2, this.y, Color.BLACK.getRGB(), false);
			}else if("@a".equals(this.getText())) {
				drawAlignRightString(matrix, Minecraft.getInstance().fontRenderer, I18n.format("info.email.@a"), this.x + this.width - 2, this.y, Color.BLACK.getRGB(), false);
			}
		}
	}
	
	public static void drawAlignRightString(MatrixStack matrix, FontRenderer fr, String text, int x, int y, int color, boolean drawShadow) {
		for(int i = text.length(); i > 0; i--) {
			char c = text.charAt(i-1);
			float width = fr.getStringWidth(String.valueOf(c));
			x -= width;
			if (drawShadow) {
				fr.drawStringWithShadow(matrix, String.valueOf(c), x, y, color);
			}else {
				fr.drawString(matrix, String.valueOf(c), x, y, color);
			}
		}
	}
}
