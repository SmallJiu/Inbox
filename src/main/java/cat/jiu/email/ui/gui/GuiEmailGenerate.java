package cat.jiu.email.ui.gui;

import cat.jiu.core.api.element.ISound;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Sound;
import cat.jiu.core.util.element.Text;
import cat.jiu.core.util.timer.Timer;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.container.ContainerEmailGenerate;
import cat.jiu.email.ui.gui.component.*;
import cat.jiu.email.util.*;
import cat.jiu.email.util.client.ShowInboxGui;
import cat.jiu.formless.utils.client.AudioSystem;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiEmailGenerate extends AbstractContainerScreen<ContainerEmailGenerate> {
	public static final ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_generate.png");
	public static final ResourceLocation EXPIRATION = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_expiration.png");
    private EditBox titleField, localSound;
    private final EditBox[] textFields = new EditBox[5];
    private final GuiTime expiration = new GuiTime(this, false);
	private final GuiButtonPopupMenu mcSounds = new GuiButtonPopupMenu();
	private GuiCheckbox useMCSound;
	private GuiButton mcSoundBtn;

	public GuiEmailGenerate(ContainerEmailGenerate container, Inventory inventory) {
		super(container, inventory, Component.nullToEmpty(null));
		this.imageWidth = 176;
		this.imageHeight = 233;
		this.mcSounds.scroll.setShowCount(5);
		this.mcSounds.setResetBtnWeight(false);
	}
	
	@Override
	public void init() {
		super.init();
		this.addWidget(expiration);

        this.titleField = this.addRenderableWidget(new EditBox(this.font, this.getGuiLeft() + 38, this.getGuiTop() + 5, 109, 11, Component.nullToEmpty(null)));
		this.titleField.setTextColor(-1);
		this.titleField.setTextColorUneditable(-1);
		this.titleField.setMaxLength(100);
		this.titleField.setBordered(false);

		this.localSound = this.addRenderableWidget(new EditBox(this.font, this.titleField.x, this.titleField.y + this.titleField.getHeight() + 2, 95, 11, Component.nullToEmpty(null)));
		this.localSound.setTextColor(-1);
		this.localSound.setTextColorUneditable(-1);
		this.localSound.setMaxLength(100);
		this.localSound.setBordered(false);

        this.initText();

		this.addRenderableWidget(new GuiImageButton(this, this.leftPos + 149 + 28, this.topPos + 3, 16, 16, I18n.get("email.config.expiration"), 256, 256, 256, 256, btn->
			expiration.setEnable(!expiration.isEnable())
        )).setBackground(()->EXPIRATION);

		GuiImageButton btn = this.addRenderableWidget(new GuiImageButton(this, this.titleField.x+this.titleField.getWidth()+4, this.titleField.y -2, 22, this.titleField.getHeight()+2, I18n.get("info.email.generate"), 256, 256, 176, 9, 59, 50, b->
			this.generate()
        )).setBackground(()->BackGround);

		this.addRenderableWidget(new GuiImageButton(this, this.titleField.x+this.titleField.getWidth()+4, this.titleField.y -2+btn.getHeight()+2, 22, this.localSound.getHeight()+2, I18n.get("info.email.name"), 23, 15, 23, 15, b-> GuiHandler.openGui(GuiHandler.EMAIL_MAIN)))
				.setBackground(()->ShowInboxGui.inbox);

		this.mcSounds.scroll.collection.clear();
		this.mcSounds.scroll.init();
		Registry.SOUND_EVENT.forEach(soundEvent ->
			this.mcSounds.addButton(new GuiButton(0, 0, this.localSound.getWidth(), this.localSound.getHeight(), Component.nullToEmpty(String.valueOf(soundEvent.getLocation())), b->{
				if (this.mcSoundBtn.isActive()) {
					this.mcSoundBtn.setMessage(Component.nullToEmpty(String.valueOf(soundEvent.getLocation())));
				}
			}))
		);

		this.mcSoundBtn = this.addRenderableWidget(new GuiButton(this.localSound.x-1, this.localSound.y, this.localSound.getWidth()+2, this.localSound.getHeight()+2, this.mcSounds.scroll.collection.get(0).getMessage(), b->
				this.mcSounds.setVisible(!this.mcSounds.isVisible())
		));
		this.mcSoundBtn.visible = false;

		this.useMCSound = this.addRenderableWidget(new GuiCheckbox(this.localSound.x + this.localSound.getWidth() + 2, this.localSound.y, 13, 13, new TranslatableComponent("info.email.generate.sound.use_mc_sound"), false, false, ()->{
			this.mcSoundBtn.visible = this.useMCSound.selected();
			this.localSound.setVisible(!this.useMCSound.selected());
		}) {
			@Override
			public void renderButton(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
				super.renderButton(pPoseStack, pMouseX, pMouseY, pPartialTick);
				if (EmailUtils.isInRange(pMouseX, pMouseY, this.x, this.y, this.width, this.height)) {
					renderTooltip(pPoseStack, this.getMessage(), pMouseX, pMouseY);
				}
			}
		});
	}

	void generate(){
		if(!this.getMenu().isLock()) {
			String title = titleField.getValue();
			if(StringUtils.isEmpty(title)) {
				title = "info.email.default_title";
			}

			if(this.textsIsEmpty() && this.getMenu().isEmpty()) {
				this.setRenderText(I18n.get("info.email.error.empty_msgs_item"), Color.RED);
				return;
			}

			List<IText> msgs = Lists.newArrayList();
			if(!textsIsEmpty()) {
				for (EditBox textField : textFields) {
					String msg = textField.getValue();
					if (msg.isEmpty()) {
						msgs.add(Text.empty);
					}else {
						msgs.add(new Text(msg));
					}
				}
			}else {
				msgs.add(new Text("info.email.default_msg"));
			}
			Email email = new Email(new Text(title), new Text(Minecraft.getInstance().player.getName()), (ISound) null, null, msgs);

			long expiration = this.expiration.getTimeOfMillis();
			if(expiration>0) {
				email.setExpirationTime(new TimeMillis(expiration));
			}

			if (this.useMCSound.selected()) {
				email.setMcSound(new Sound(new Timer(59, 59, 19), Registry.SOUND_EVENT.get(new ResourceLocation(this.mcSoundBtn.getMessage().getString())), 1, 1, SoundSource.PLAYERS));
			}else if (!this.localSound.getValue().isEmpty()) {
				email.setNetworkLocalSound(new AudioSystem.Audio(this.localSound.getValue(), SoundSource.PLAYERS));
			}

			if(!EmailConfigs.isInfiniteSize()) {
				Email email_t = email.copy();
				if(!this.getMenu().isEmpty()) {
					email_t.addItems(this.getMenu().toItemList(true));
				}
				SizeReport report = EmailUtils.checkEmailSize(email_t);
				if(!SizeReport.SUCCESS.equals(report)) {
					if (this.getMenu().isLock()) this.getMenu().setLock(false);
					this.setRenderText(new Text("info.email.error.send.to_big", report.slot(), report.size()).format(), Color.RED);
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
			JsonParser.toJsonFile(EmailAPI.getTypePath() + filename, object, true);

			this.getMenu().setLock(false);
			this.setRenderText("Save to " + EmailAPI.getTypePath() + filename, Color.GREEN, Timer.parseTick(5, 0));
		}
	}
	
	private void initText() {
		for(int i = 0; i < this.textFields.length; i++) {
			this.textFields[i] = new EditBox(this.font, this.leftPos+9, this.topPos+34 + (12 * i), 155, 12, Component.nullToEmpty(null));
			
			EditBox field = this.textFields[i];
			field.setTextColor(Color.WHITE.getRGB());
			field.setTextColorUneditable(Color.WHITE.getRGB());
			field.setMaxLength(256);
			field.setBordered(false);
			this.addRenderableWidget(field);
		}
	}
	
	public boolean textsIsEmpty() {
		for (EditBox tf : this.textFields) {
			if (!StringUtils.isEmpty(tf.getValue())) return false;
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
		this.renderText = text;
		this.renderColor = color;
		this.renderTicks = ticks;
	}
	public void clearRenderText() {
		this.renderText = null;
		this.renderColor = null;
		this.renderTicks = 0;
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
			for (GuiEventListener listener : this.children()) {
				if(listener instanceof EditBox tf){
					if(tf.isFocused()){
						if(tf.charTyped(typedChar, keyCode)){
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

			for (EditBox tf : this.textFields) {
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
	public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(stack);
		super.render(stack, mouseX, mouseY, partialTicks);
		super.renderTooltip(stack, mouseX, mouseY);
	}

	@Override
	protected void renderBg(PoseStack stack, float partialTicks, int mouseX, int mouseY) {
		ClientUtil.bindTexture(BackGround);
		blit(stack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
		this.titleField.render(stack, mouseX, mouseY, partialTicks);
		for(EditBox tf : this.textFields) {
			tf.render(stack, mouseX, mouseY, partialTicks);
			EmailUtils.drawString(stack, this.font, tf.getValue().length()+"/"+tf.getMaxLength(), tf.x +tf.getWidth()+13, tf.y +2, (tf.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.WHITE).getRGB(), false);
		}
		this.expiration.render(stack, this.leftPos + 149 + 22 + 10, this.topPos + 20 + 80, partialTicks);
	}

	@Override
	protected void renderLabels(PoseStack stack, int mouseX, int mouseY) {
		fill(stack, 162, 100, 162 + 9, 100 + 9, this.getMenu().isLock() ? Color.RED.getRGB() : Color.GREEN.getRGB());

		EmailUtils.drawAlignRightString(stack, this.font, I18n.get("info.email.title") + ":", this.titleField.x-2,  this.titleField.y-2, (this.titleField.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.BLACK).getRGB(), false);
		EmailUtils.drawAlignRightString(stack, this.font, I18n.get("info.email.generate.sound") + ":", this.localSound.x-2,  this.localSound.y-2, (this.localSound.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.BLACK).getRGB(), false);

		if(this.renderTicks > 0 && this.renderText != null && this.renderColor != null) {
			if(this.renderTicks <= 0) this.clearRenderText();
			EmailUtils.drawString(stack, this.font, this.renderText, (88 - this.font.width(this.renderText) / 2), 138, this.renderColor.getRGB(), false);
			this.renderTicks--;
		}
		if(this.getMenu().renderTicks > 0 && this.getMenu().renderText != null && this.getMenu().renderColor != null) {
			if(this.getMenu().renderTicks <= 0) this.getMenu().clearRenderText();
			EmailUtils.drawString(stack, this.font, this.getMenu().renderText, (88 - this.font.width(this.getMenu().renderText) / 2), 138, this.getMenu().renderColor.getRGB(), false);
			this.getMenu().renderTicks--;
		}
	}
}
