package cat.jiu.email.ui.gui;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.client.AudioSystem;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.core.util.element.Text;
import cat.jiu.core.util.element.sound.SoundMC;
import cat.jiu.core.util.timer.Timer;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.element.Email;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.container.ContainerEmailGenerate;
import cat.jiu.email.ui.gui.component.*;
import cat.jiu.email.util.*;
import cat.jiu.email.util.client.ShowInboxGui;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiEmailGenerate extends ContainerScreen<ContainerEmailGenerate> {
	public static final ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/inbox_generate.png");
    private TextFieldWidget titleField, localSound;
    private final TextFieldWidget[] textFields = new TextFieldWidget[5];
    private final GuiTime expiration = new GuiTime(this, false);
	private final GuiButtonPopupMenu mcSounds = new GuiButtonPopupMenu();
	private GuiCheckbox useMCSound;
	private GuiButton mcSoundBtn;

	public GuiEmailGenerate(ContainerEmailGenerate container, PlayerInventory inventory) {
		super(container, inventory, ITextComponent.getTextComponentOrEmpty(null));
		this.xSize = 176;
		this.ySize = 233;
		this.mcSounds.scroll.setShowCount(5);
		this.mcSounds.setResetBtnWeight(true);
	}
	
	public ContainerEmailGenerate getMenu() {
		return this.container;
	}
	
	@Override
	public void init() {
		super.init();
		this.addListener(expiration);

        this.titleField = this.addButton(new TextFieldWidget(this.font, this.getGuiLeft() + 38, this.getGuiTop() + 5, 109, 11, ITextComponent.getTextComponentOrEmpty(null)));
		this.titleField.setTextColor(-1);
		this.titleField.setDisabledTextColour(-1);
		this.titleField.setMaxStringLength(100);
		this.titleField.setEnableBackgroundDrawing(false);

		this.localSound = this.addButton(new TextFieldWidget(this.font, this.titleField.x, this.titleField.y + this.titleField.getHeight() + 3, 95, 11, ITextComponent.getTextComponentOrEmpty(null)));
		this.localSound.setTextColor(-1);
		this.localSound.setDisabledTextColour(-1);
		this.localSound.setMaxStringLength(100);
		this.localSound.setEnableBackgroundDrawing(false);

        this.initText();

		this.addButton(new GuiImageButton(this, this.getGuiLeft() + 149 + 28, this.getGuiTop() + 3, 16, 16, I18n.format("inbox.config.expiration"), 256, 256, 256, 256, btn->
			expiration.setEnable(!expiration.isEnable())
        )).setBackground(()->GuiEmailSend.EXPIRATION);

		GuiImageButton btn = this.addButton(new GuiImageButton(this, this.titleField.x +this.titleField.getWidth()+4, this.titleField.y -2, 22, this.titleField.getHeight()+2, I18n.format("info.inbox.generate"), 256, 256, 176, 9, 59, 50, b->
			this.generate()
        )).setBackground(()->BackGround);

		this.addButton(new GuiImageButton(this, this.titleField.x +this.titleField.getWidth()+4, this.titleField.y -2+btn.getHeight()+2, 22, this.localSound.getHeight()+2, I18n.format("info.inbox.name"), 23, 15, 23, 15, b-> GuiHandler.openGui(GuiHandler.EMAIL_MAIN)))
				.setBackground(()->ShowInboxGui.inbox);

		this.mcSounds.scroll.collection.clear();
		this.mcSounds.scroll.init();
		ForgeRegistries.SOUND_EVENTS.forEach(soundEvent ->
			this.mcSounds.addButton(new GuiButton(0, 0, this.localSound.getWidth()+2, this.localSound.getHeight()+2, ITextComponent.getTextComponentOrEmpty(String.valueOf(soundEvent.getName())), b->{
				if (this.mcSoundBtn.visible) {
					this.mcSoundBtn.setMessage(ITextComponent.getTextComponentOrEmpty(String.valueOf(soundEvent.getName())));
					this.mcSounds.setVisible(false);
				}
			}))
		);
		this.mcSoundBtn = this.addButton(new GuiButton(this.localSound.x -1, this.localSound.y -1, this.localSound.getWidth()+2, this.localSound.getHeight()+2, this.mcSounds.scroll.collection.get(0).getMessage(), b->
				this.mcSounds.setVisible(!this.mcSounds.isVisible())
		));
		this.mcSoundBtn.visible = false;
		int x = this.mcSoundBtn.x + this.mcSoundBtn.getWidth()/2;
		x -= this.mcSounds.getWidth()/2;
		this.mcSounds.setCreatePoint(x, this.mcSoundBtn.y +this.mcSoundBtn.getHeight());

		this.useMCSound = this.addButton(new GuiCheckbox(this.localSound.x + this.localSound.getWidth() + 2, this.localSound.y, 12, 11, new TranslationTextComponent("info.inbox.generate.sound.use_mc_sound"), false, false, ()->{
			this.mcSoundBtn.visible = this.useMCSound.isChecked();
			this.localSound.setVisible(!this.useMCSound.isChecked());
			if (!this.mcSoundBtn.visible) {
				this.mcSounds.setVisible(false);
			}
		}) {
			@Override
			public void renderWidget(MatrixStack stack, int pMouseX, int pMouseY, float pPartialTick) {
				super.renderWidget(stack, pMouseX, pMouseY, pPartialTick);
				if (EmailUtils.isInRange(pMouseX, pMouseY, this.x, this.y, this.width, this.height)) {
					RenderUtils.drawComponentTooltip(stack, Collections.singletonList(this.getMessage()), pMouseX, pMouseY);
				}
			}
		});
	}

	void generate(){
		if(!this.getMenu().isLock()) {
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

			long expiration = this.expiration.getTimeOfMillis();
			if(expiration>0) {
				email.setExpirationTime(new TimeMillis(expiration));
			}

			if (this.useMCSound.isChecked()) {
				email.setMcSound(new SoundMC()
						.setDuration(59, 59, 19)
						.setSoundEvent(ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(this.mcSoundBtn.getMessage().getString())))
						.setSoundChannel(SoundCategory.PLAYERS)
				);
			}else if (!this.localSound.getText().isEmpty()) {
				email.setExternalSound(new AudioSystem.Audio(this.localSound.getText(), SoundCategory.PLAYERS));
			}

			if(!EmailConfigs.isInfiniteSize()) {
				Email email_t = email.copy();
				if(!this.getMenu().isEmpty()) {
					email_t.addItems(this.getMenu().toItemList(true));
				}
				SizeReport report = EmailUtils.checkEmailSize(email_t);
				if(!SizeReport.SUCCESS.equals(report)) {
					if (this.getMenu().isLock()) this.getMenu().setLock(false);
					this.setRenderText(new Text("info.inbox.error.send.to_big", report.slot, report.size).format(), Color.RED);
					return;
				}
			}
			if (!this.getMenu().isEmpty()) {
				email.addItems(this.getMenu().toItemList(true));
			}
			this.getMenu().setLock(true);
			String filename = email.getTitle().format() + "-" + System.currentTimeMillis() + ".json";

			JsonObject object = email.writeTo(JsonObject.class);
			object.remove("time");
			if (!email.hasAttachment()) {
				JsonArray array = new JsonArray();
				for (ResourceLocation resourceLocation : IAttachment.REGISTRY.getIDs()) {
					array.add(String.valueOf(resourceLocation));
				}
				object.add("attachments", new JsonArray());
				object.add("AllAttachmentID", array);
			}
			String file = EmailAPI.getGlobalDataPath() + "emails/" + filename;
			JsonParser.toJsonFile(file, object, true);

			this.getMenu().setLock(false);
//			try {
				this.setRenderText("Save to " + file, Color.GREEN, Timer.parseTick(10, 0));
//			} catch (IOException ignored) {}
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
		if(!this.getMenu().isLock()){
			if (this.useMCSound.mouseClicked(mouseX, mouseY, mouseButton)) {
				return true;
			}
			if (this.mcSounds.mouseClicked(this.getMinecraft(), (int) mouseX, (int) mouseY, mouseButton)) {
				return true;
			}
			return super.mouseClicked(mouseX, mouseY, mouseButton);
		}

		return super.mouseClicked(mouseX, mouseY, mouseButton);
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
	public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
		if (this.mcSounds.scroll((int) pMouseX, (int) pMouseY, (int)pDelta)) {
			return true;
		}
		return super.mouseScrolled(pMouseX, pMouseY, pDelta);
	}

	@Override
	public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(stack);
		super.render(stack, mouseX, mouseY, partialTicks);
		super.renderHoveredTooltip(stack, mouseX, mouseY);
		if(this.getMenu().renderTimer != null && !this.getMenu().renderTimer.isDone() && this.getMenu().renderText != null && this.getMenu().renderColor != null) {
			if(this.getMenu().renderTimer.isDone()) {
				this.clearRenderText();
			}
			RenderUtils.drawStringTooltip(stack, Collections.singletonList(this.getMenu().renderText), mouseX, mouseY);
		}
		this.titleField.renderWidget(stack, mouseX, mouseY, partialTicks);
		this.expiration.render(stack, this.getGuiLeft() + 149 + 22 + 10, this.getGuiTop() + 20 + 80, partialTicks);
		this.mcSounds.drawPopupMenu(stack, mouseX, mouseY, partialTicks);
		if (mcSoundBtn.visible && mcSoundBtn.isMouseOver(mouseX, mouseY)) {
			RenderUtils.drawComponentTooltip(stack, Collections.singletonList(mcSoundBtn.getMessage()), mouseX, mouseY);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack stack, float partialTicks, int mouseX, int mouseY) {
		RenderUtils.draw(stack, BackGround, this.getGuiLeft(), this.getGuiTop(), this.getXSize(), this.getYSize(), 0, 0);
		RenderUtils.fill(stack, this.getGuiLeft() + this.getXSize() - 14, this.getGuiTop() - 15 + this.getYSize()/2, 9, 9, this.getMenu().isLock() ? Color.RED.getRGB() : Color.GREEN.getRGB());

		EmailUtils.drawAlignRightString(stack, I18n.format("info.inbox.title") + ":", this.titleField.x -2,  this.titleField.y, (this.titleField.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.BLACK).getRGB(), false);
		EmailUtils.drawAlignRightString(stack, I18n.format("info.inbox.generate.sound") + ":", this.localSound.x -2,  this.localSound.y, (this.localSound.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.BLACK).getRGB(), false);

		if (!this.mcSounds.isVisible()) for(TextFieldWidget tf : this.textFields) {
			RenderUtils.drawString(stack, tf.getText().length()+"/"+tf.getMaxStringLength(), tf.x +tf.getWidth()+13, tf.y +2, (tf.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.WHITE).getRGB(), true);
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(MatrixStack stack, int mouseX, int mouseY) {
	}
}
