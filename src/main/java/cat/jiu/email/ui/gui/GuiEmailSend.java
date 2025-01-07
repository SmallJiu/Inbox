package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.util.Collections;
import java.util.List;

import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.ui.gui.component.GuiButtonPopupMenu;
import cat.jiu.email.util.*;
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class GuiEmailSend extends ContainerScreen<ContainerEmailSend> {
	public static final ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/inbox_send.png");
	public static final ResourceLocation EXPIRATION = new ResourceLocation(EmailMain.MODID, "textures/gui/container/inbox_expiration.png");
	private TextFieldWidget nameField;
    private TextFieldWidget titleField;
    private final TextFieldWidget[] textFields = new TextFieldWidget[5];
    private final GuiTime expiration = new GuiTime(this, false);
	private final GuiButtonPopupMenu addresseeHistory = new GuiButtonPopupMenu();
	private GuiImageButton addresseeHistoryBtn;
    
	public GuiEmailSend(ContainerEmailSend container, PlayerInventory inventory) {
		super(container, inventory, ITextComponent.getTextComponentOrEmpty(null));
		this.xSize = 176;
		this.ySize = 233;
		this.addresseeHistory.scroll.setShowCount(5);
	}
	
	protected ContainerEmailSend getMenu() {
		return this.container;
	}
	
	@Override
	public void init() {
		super.init();
		this.addListener(expiration);
		this.nameField = this.addButton(new NameGuiTextField(this.font, this.getGuiLeft() + 39, this.getGuiTop() + 6, 95, 11));
        this.nameField.setTextColor(-1);
        this.nameField.setDisabledTextColour(-1);
        this.nameField.setMaxStringLength(100);
        this.nameField.setEnableBackgroundDrawing(false);
        
        this.titleField = this.addButton(new TextFieldWidget(this.font, this.getGuiLeft() + 39, this.getGuiTop() + 20, 109, 11, ITextComponent.getTextComponentOrEmpty(null)));
		this.titleField.setTextColor(-1);
		this.titleField.setDisabledTextColour(-1);
		this.titleField.setMaxStringLength(100);
		this.titleField.setEnableBackgroundDrawing(false);
        this.initText();

		this.addresseeHistory.scroll.collection.clear();
		this.addresseeHistory.setCreatePoint(this.nameField.x - this.getGuiLeft() - 2, this.nameField.y + this.nameField.getHeight() - this.getGuiTop());

		{
			if(EmailAPI.globalEmailCache.exists()) {
				JsonElement e = JsonParser.parse(EmailAPI.globalEmailCache);
				if(e != null && e.isJsonObject()) {
					JsonObject json = e.getAsJsonObject();
					if (json.has("history")) {
						json.getAsJsonArray("history").forEach(b->this.addAddresseeHistory(b.getAsString(), false));
						this.addresseeHistory.setVisible(this.addresseeHistory.isVisible());
					}
				}
			}
		}

		this.addresseeHistoryBtn = this.addButton(new GuiImageButton(this, this.nameField.x + this.nameField.getWidth() + 3, this.nameField.y, 9, 9, I18n.format("info.inbox.history"), 256, 256, 194, 0, 9, 9, b-> addresseeHistory.setVisible(!addresseeHistory.isVisible()))).setBackground(()->BackGround);

		this.addButton(new GuiImageButton(this, this.getGuiLeft() + 149 + 28, this.getGuiTop() + 3, 16, 16, I18n.format("inbox.config.expiration"), 256, 256, 256, 256, btn->
			expiration.setEnable(!expiration.isEnable())
        )).setBackground(()->EXPIRATION).visible = EmailUtils.isOP(Minecraft.getInstance().player);
        
        this.addButton(new GuiImageButton(this, this.addresseeHistoryBtn.x+this.addresseeHistoryBtn.getWidth()+4, this.nameField.y -2, 22, this.nameField.getHeight()+2, I18n.format("info.inbox.dispatch"), 256, 256, 176, 9, 59, 50, this::send)).setBackground(()->BackGround);

		this.addButton(new GuiImageButton(this, this.titleField.x +this.titleField.getWidth()+1, this.titleField.y -2, 22, this.nameField.getHeight()+2, I18n.format("info.inbox.name"), 23, 15, 23, 15, b-> GuiHandler.openGui(GuiHandler.EMAIL_MAIN)))
				.setBackground(()->ShowInboxGui.inbox);
	}

	protected void send(Button btn) {
		if(!this.getMenu().isCooling() && !this.getMenu().isLock()) {
			String name = nameField.getText();
			if(StringUtils.isEmpty(name)) {
				this.setRenderText(I18n.format("info.inbox.error.empty_name"), Color.RED);
				return;
			}
			String title = titleField.getText();
			if(StringUtils.isEmpty(title)) {
				title = "info.inbox.default_title";
			}

			if(this.textsIsEmpty() && this.getMenu().isEmpty()) {
				this.setRenderText(I18n.format("info.inbox.error.empty_msgs_item"), Color.RED);
				return;
			}

			List<IText> msgs = Lists.newArrayList();
			if(!textsIsEmpty()) {
				for (TextFieldWidget textField : textFields) {
					String msg = textField.getText();
					if (msg.isEmpty()) {
						msgs.add(Text.empty);
					}else {
						msgs.add(new Text(msg));
					}
				}
			}else {
				msgs.add(new Text("info.inbox.default_msg"));
			}
			Email email = new Email(new Text(title), new Text(Minecraft.getInstance().player.getName())).addMessages(msgs);

			long expiration = GuiEmailSend.this.expiration.getTimeOfMillis();
			if(expiration>0) {
				email.setExpirationTime(new TimeMillis(expiration));
			}

			Email email_t = email.copy();
			if(!this.getMenu().isEmpty()) {
				this.getMenu().toItemList(true).forEach(email_t::addItem);
			}
			if(!EmailConfigs.isInfiniteSize()){
				SizeReport report = EmailUtils.checkEmailSize(email_t);
				if(!SizeReport.SUCCESS.equals(report)) {
					if (this.getMenu().isLock()) this.getMenu().setLock(false);
					this.setRenderText(new Text("info.inbox.error.send.to_big", report.slot, report.size).format(), Color.RED);
					return;
				}
			}
//				if(!this.getMenu().isEmpty()) {
//					this.getMenu().toItemList(true).forEach(email::addItem);
//				}
			EmailAPI.sendPlayerEmail(getMinecraft().player, name, email);
			this.addAddresseeHistory(name, true);
			clearRenderText();
		}
	}

	public void addAddresseeHistory(String name, boolean writeToFile) {
		if (writeToFile) {
			EmailAPI.addAddresseeHistory(name);
		}
		ITextComponent component = ITextComponent.getTextComponentOrEmpty(name);
		boolean has = false;
		for (Button button : this.addresseeHistory.scroll.collection) {
			if (button.getMessage().equals(component)) {
				has = true;
				break;
			}
		}
		if (!has) {
			if (this.addresseeHistory.scroll.collection.size() >= EmailConfigs.Send.Send_History_Max_Count.get()) {
				this.addresseeHistory.scroll.collection.remove(0);
			}
			this.addresseeHistory.addButton(GuiInbox.GuiButton.builder(component, b->{
						nameField.setText(name);
						nameField.setCursorPosition(0);
					})
					.pos(0, 0)
					.size(this.nameField.getWidth()+2, this.nameField.getHeight())
					.build());
		}
	}
	
	private void initText() {
		for(int i = 0; i < this.textFields.length; i++) {
			this.textFields[i] = new TextFieldWidget(this.font, this.getGuiLeft()+9, this.getGuiTop()+34 + (12 * i), 155, 12, ITextComponent.getTextComponentOrEmpty(null));
			
			TextFieldWidget field = this.textFields[i];
			field.setTextColor(Color.WHITE.getRGB());
			field.setDisabledTextColour(Color.WHITE.getRGB());
			field.setMaxStringLength(256);
			field.setEnableBackgroundDrawing(false);
			this.addButton(field);
		}
	}
	
	public boolean textsIsEmpty() {
		for (TextFieldWidget tf : this.textFields) {
			if (!StringUtils.isEmpty(tf.getText())) return false;
		}
		return true;
	}

	private long renderTicks = 0;
	private String renderText;
	private Color renderColor;
	public void setRenderText(String text) {
		this.setRenderText(text, Color.RED);
	}
	public void setRenderText(String text, Color color) {
		this.setRenderText(text, color, EmailUtils.parseTick(0,0,0,25, 0));
	}
	public void setRenderText(String text, Color color, long ticks) {
		this.getMenu().setRenderText(text, color, ticks);
	}
	public void clearRenderText() {
		this.getMenu().clearRenderText();
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (!this.addresseeHistoryBtn.isMouseOver(mouseX, mouseY)) {
			if (this.addresseeHistory.mouseClicked(this.getMinecraft(), (int) mouseX - this.getGuiLeft(), (int) mouseY - this.getGuiTop(), mouseButton)){
				this.addresseeHistory.setVisible(false);
				return true;
			}
		}
		if(!this.getMenu().isLock()){
			return super.mouseClicked(mouseX, mouseY, mouseButton);
		}
		return true;
    }

	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		if(!this.getMenu().isLock()){
			if(this.expiration.charTyped(typedChar, keyCode)){
				return true;
			}
			for (IGuiEventListener listener : this.getEventListeners()) {
				if(listener instanceof TextFieldWidget){
					if(((TextFieldWidget)listener).isFocused()){
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
		if(!this.getMenu().isLock()){
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
			if (keyCode == Minecraft.getInstance().gameSettings.keyBindInventory.getKey().getKeyCode()) {
				this.closeScreen();
				return true;
			}
			return flag || (isFocused && keyCode != 256) || super.keyPressed(keyCode, scanCode, modifiers);
		}
		return true;
	}
	
	@Override
	public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(stack);
		super.render(stack, mouseX, mouseY, partialTicks);
		super.renderHoveredTooltip(stack, mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack stack, float partialTicks, int mouseX, int mouseY) {
		RenderUtils.draw(stack, BackGround, this.getGuiLeft(), this.getGuiTop(), this.getXSize(), this.getYSize(), 0, 0);
		RenderUtils.fill(stack, this.getGuiLeft() + 162, this.getGuiTop() + 100, 9, 9, this.getMenu().isLock() ? Color.RED.getRGB() : Color.GREEN.getRGB());

		EmailUtils.drawAlignRightString(stack,I18n.format("info.inbox.addressee") + ":", this.getGuiLeft() + 36, this.getGuiTop() + 6, (this.nameField.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.BLACK).getRGB(), false);
		EmailUtils.drawAlignRightString(stack,I18n.format("info.inbox.title") + ":", this.getGuiLeft() + 36, this.getGuiTop() + 21, (this.titleField.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.BLACK).getRGB(), false);

		for(TextFieldWidget tf : this.textFields) {
			RenderUtils.drawString(stack, tf.getText().length()+"/"+tf.getMaxStringLength(), tf.x +tf.getWidth()+13, tf.y +2, (tf.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.WHITE).getRGB(), true);
		}
		if(this.getMenu().renderTicks > 0 && this.getMenu().renderText != null && this.getMenu().renderColor != null) {
			if(this.getMenu().renderTicks <= 0) this.getMenu().clearRenderText();
			RenderUtils.drawString(stack, this.getMenu().renderText, this.getGuiLeft() + (88 - this.font.getStringWidth(this.getMenu().renderText) / 2), this.getGuiTop() + 138, this.getMenu().renderColor.getRGB(), false);
			this.getMenu().renderTicks--;
		}
		if(this.getMenu().isCooling()) {
//		if(!this.getMenu().isCooling()) {
			long millis = this.getMenu().getCoolingMillis() - System.currentTimeMillis();
//			long millis = System.currentTimeMillis();

			String text = I18n.format("info.inbox.cooling", EmailUtils.formatTimestamp(millis));
			RenderUtils.drawCenteredString(stack, text, this.getGuiLeft() + this.getXSize()/2, this.getGuiTop() + 60, Color.RED.getRGB(), false);
		}
		if(!this.addresseeHistory.isVisible() && this.titleField.getText().isEmpty()) {
			RenderUtils.drawString(stack, I18n.format("info.inbox.default_title"), this.getGuiLeft() + 39, this.getGuiTop() + 21, Color.WHITE.getRGB(), true);
		}
		if(!this.addresseeHistory.isVisible() && this.textsIsEmpty()) {
			RenderUtils.drawString(stack, I18n.format("info.inbox.default_msg"), this.getGuiLeft() + 10, this.getGuiTop() + 34, Color.WHITE.getRGB(), true);
		}
		this.expiration.render(stack, this.getGuiLeft() + 149 + 22 + 10, this.getGuiTop() + 20 + 80, 0);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(MatrixStack stack, int mouseX, int mouseY) {
		this.addresseeHistory.drawPopupMenu(stack, mouseX - this.getGuiLeft(), mouseY - this.getGuiTop(), 0);
		if (net.minecraft.util.StringUtils.isNullOrEmpty(this.nameField.getText()) && this.nameField.isMouseOver(mouseX, mouseY)) {
			RenderUtils.drawStringTooltip(stack, Collections.singletonList(I18n.format("info.inbox.send.name")), mouseX - this.getGuiLeft(), mouseY);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static class NameGuiTextField extends TextFieldWidget {
		public NameGuiTextField(FontRenderer font, int x, int y, int par5Width, int par6Height) {
			super(font, x, y, par5Width, par6Height, ITextComponent.getTextComponentOrEmpty(null));
		}

		private int
				index = 0,
				absIndex = 0;

		@Override
		public boolean charTyped(char codePoint, int modifiers) {
			return super.charTyped(codePoint, modifiers);
		}

		@Override
		public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			if(this.isFocused()){
				if(keyCode == GLFW.GLFW_KEY_TAB) {
					List<? extends PlayerEntity> playerList = Minecraft.getInstance().player.getEntityWorld().getPlayers();
					if(this.index < playerList.size()) {
						ITextComponent name = playerList.get(this.index).getName();
						if(name.equals(Minecraft.getInstance().player.getName())
								&& EmailConfigs.Send.Enable_Send_To_Self.get()) {
							this.setText(name.getString());
							this.index++;
							return true;
						}
						this.setText(name.getString());
						this.index++;
					}else {
						if(EmailUtils.isOP(Minecraft.getInstance().player) && this.index < playerList.size() + 2) {
							this.absIndex++;
							if(this.absIndex==1) {
								this.setText("@p");
							}else if(this.absIndex==2) {
								this.setText("@a");
							}
							if(this.absIndex >= 2) {
								this.absIndex = 0;
								this.index = 0;
							}
						}else {
							this.absIndex = 0;
							this.index = 0;
						}
					}
					return true;
				}
			}

			return super.keyPressed(keyCode, scanCode, modifiers);
		}

		@Override
		public void renderWidget(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
			super.renderWidget(stack, mouseX, mouseY, partialTicks);
			if("@p".equals(this.getText())) {
				EmailUtils.drawAlignRightString(stack, I18n.format("info.inbox.@p"), this.x + this.width - 2, this.y, Color.BLACK.getRGB(), false);
			}else if("@a".equals(this.getText())) {
				EmailUtils.drawAlignRightString(stack, I18n.format("info.inbox.@a"), this.x + this.width - 2, this.y, Color.BLACK.getRGB(), false);
			}
		}
	}
}
