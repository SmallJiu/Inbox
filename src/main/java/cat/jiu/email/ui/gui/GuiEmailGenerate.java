package cat.jiu.email.ui.gui;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.client.AudioSystem;
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
import cat.jiu.email.ui.InboxButton;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GuiEmailGenerate extends AbstractContainerScreen<ContainerEmailGenerate> {
	public static final ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/inbox_generate.png");
    private EditBox titleField, localSound;
    private final EditBox[] textFields = new EditBox[5];
    private final GuiTime expiration = new GuiTime(this, false);
	private final GuiButtonPopupMenu mcSounds = new GuiButtonPopupMenu();
	private GuiCheckbox useMCSound;
	private GuiButton mcSoundBtn;
	private LockIconButton lockBtn;
	private final Inventory playerInventory;
	private List<ItemStack> stacks;

	public GuiEmailGenerate(ContainerEmailGenerate container, Inventory inventory) {
		super(container, inventory, Component.empty());
		this.imageWidth = 176;
		this.imageHeight = 233;
		this.playerInventory = inventory;
		this.mcSounds.scroll.setShowCount(5);
		this.mcSounds.setResetBtnWeight(true);
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

		this.localSound = this.addRenderableWidget(new EditBox(this.font, this.titleField.getX(), this.titleField.getY() + this.titleField.getHeight() + 3, 95, 11, Component.nullToEmpty(null)));
		this.localSound.setTextColor(-1);
		this.localSound.setTextColorUneditable(-1);
		this.localSound.setMaxLength(100);
		this.localSound.setBordered(false);

        this.initText();

		Button expirationBtn = this.addRenderableWidget(new GuiImageButton(this, this.leftPos + 149 + 28, this.topPos - 5, 20, 20, I18n.get("inbox.config.expiration"), 256, 256, 256, 256, btn->
			expiration.setEnable(!expiration.isEnable())
        )).setBackground(()->GuiEmailSend.EXPIRATION);

		GuiImageButton btn = this.addRenderableWidget(new GuiImageButton(this, this.titleField.getX() +this.titleField.getWidth()+4, this.titleField.getY() -2, 22, this.titleField.getHeight()+2, I18n.get("info.inbox.generate"), 256, 256, 176, 9, 59, 50, b->
			this.generate()
        )).setBackground(()->BackGround);

		this.addRenderableWidget(new GuiImageButton(this, this.titleField.getX() +this.titleField.getWidth()+4, this.titleField.getY() -2+btn.getHeight()+2, 22, this.localSound.getHeight()+2, I18n.get("info.inbox.name"), 23, 15, 23, 15, b-> GuiHandler.openGui(GuiHandler.EMAIL_MAIN)))
				.setBackground(()-> InboxButton.inbox);

		this.mcSounds.scroll.collection.clear();
		this.mcSounds.scroll.init();
		ForgeRegistries.SOUND_EVENTS.forEach(soundEvent ->
			this.mcSounds.addButton(new GuiButton(0, 0, this.localSound.getWidth()+2, this.localSound.getHeight()+2, Component.nullToEmpty(String.valueOf(soundEvent.getLocation())), b->{
				if (this.mcSoundBtn.isActive()) {
					this.mcSoundBtn.setMessage(Component.nullToEmpty(String.valueOf(soundEvent.getLocation())));
					this.mcSounds.setVisible(false);
				}
			}))
		);
		this.mcSoundBtn = this.addRenderableWidget(new GuiButton(this.localSound.getX() -1, this.localSound.getY() -1, this.localSound.getWidth()+2, this.localSound.getHeight()+2, this.mcSounds.scroll.collection.get(0).getMessage(), b->
				this.mcSounds.setVisible(!this.mcSounds.isVisible())
		));
		this.mcSoundBtn.visible = false;
		this.mcSounds.setCreatePoint(this.mcSoundBtn.getX(), this.mcSoundBtn.getY() +this.mcSoundBtn.getHeight());

		this.useMCSound = this.addRenderableWidget(new GuiCheckbox(this.localSound.getX() + this.localSound.getWidth() + 2, this.localSound.getY(), 12, 11, Component.translatable("info.inbox.generate.sound.use_mc_sound"), false, false, ()->{
			this.mcSoundBtn.visible = this.useMCSound.selected();
			this.localSound.setVisible(!this.useMCSound.selected());
			if (!this.mcSoundBtn.visible) {
				this.mcSounds.setVisible(false);
			}
		}) {
			@Override
			public void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
				super.renderWidget(graphics, pMouseX, pMouseY, pPartialTick);
				if (EmailUtils.isInRange(pMouseX, pMouseY, this.getX(), this.getY(), this.width, this.height)) {
					graphics.renderTooltip(font, this.getMessage(), pMouseX, pMouseY);
				}
			}
		});
		if (EmailUtils.isOP(Minecraft.getInstance().player)) {
			this.lockBtn = this.addRenderableWidget(new LockIconButton(expirationBtn.getX(), expirationBtn.getY() + expirationBtn.getHeight() + 1, btn1->
					this.lockBtn.setLocked(!this.lockBtn.isLocked())
			));
			this.lockBtn.setTooltip(Tooltip.create(Component.translatable("info.inbox.send.lock")));
			this.lockBtn.setMessage(Component.translatable("info.inbox.send.lock"));

			MutableComponent info = Component.translatable("info.inbox.memorize_items.0").append("\n").append(Component.translatable("info.inbox.memorize_items.1"));
			Button saveItemsBtn = this.addRenderableWidget(new ImageButton(
					expirationBtn.getX() + expirationBtn.getWidth() + 2, expirationBtn.getY() + 3,
					15, 12, 182, 24, 0, AbstractWidget.WIDGETS_LOCATION,
					btn1->{
						if (Screen.hasShiftDown()) {
							if (this.stacks!=null) this.stacks.clear();
							btn1.setTooltip(Tooltip.create(info));
							return;
						}
						if (this.stacks==null) {
							this.stacks = new ArrayList<>();
						}
						this.stacks.clear();
						for (ItemStack stack : this.playerInventory.items) {
							if (!stack.isEmpty()) {
								this.stacks.add(stack);
							}
						}
						btn1.setTooltip(Tooltip.create(
								info.copy().append("\n\n").append(Component.translatable("info.inbox.memorize_items.2")).append("\n\n")
										.append(String.format("Count: %s items\n", this.stacks.size()))
										.append(String.format(" Time: %s", EmailUtils.dateFormat.format(new Date())))
						));
					}
			));
			if (this.stacks==null || this.stacks.isEmpty()) {
				saveItemsBtn.setTooltip(Tooltip.create(info));
			}else {
				saveItemsBtn.setTooltip(Tooltip.create(
						info.copy().append("\n\n").append(Component.translatable("info.inbox.memorize_items.2")).append("\n\n")
								.append(String.format("Count: %s items\n", this.stacks.size()))
								.append(String.format(" Time: %s", EmailUtils.dateFormat.format(new Date())))
				));
			}
		}
	}

	void generate(){
		if(!this.getMenu().isLock()) {
			String title = titleField.getValue();
			if(StringUtils.isEmpty(title)) {
				title = "info.inbox.default_title";
			}

			if(this.textsIsEmpty() && this.getMenu().isEmpty() && (this.stacks==null || this.stacks.isEmpty())) {
				this.setRenderText(I18n.get("info.inbox.error.empty_msgs_item"), Color.RED);
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
				msgs.add(new Text("info.inbox.default_msg"));
			}
			Email email = new Email(new Text(title), new Text(Minecraft.getInstance().player.getName())).addMessages(msgs);

			long expiration = this.expiration.getTimeOfMillis();
			if(expiration>0) {
				email.setExpirationTime(new TimeMillis(expiration));
			}

			if (this.useMCSound.selected()) {
				email.setMcSound(new SoundMC()
						.setDuration(59, 59, 19)
						.setSoundEvent(ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(this.mcSoundBtn.getMessage().getString())))
						.setSoundChannel(SoundSource.PLAYERS)
				);
			}else if (!this.localSound.getValue().isEmpty()) {
				email.setExternalSound(new AudioSystem.Audio(this.localSound.getValue(), SoundSource.PLAYERS));
			}

			if(!EmailConfigs.isInfiniteSize()) {
				Email email_t = email.copy();
				if(!this.getMenu().isEmpty()) {
					email_t.addItems(this.getMenu().toItemList(true));
				}
				SizeReport report = EmailUtils.checkEmailSize(email_t);
				if(!SizeReport.SUCCESS.equals(report)) {
					if (this.getMenu().isLock()) this.getMenu().setLock(false);
					this.setRenderText(I18n.get("info.inbox.error.send.to_big", report.slot(), report.size()), Color.RED);
					return;
				}
			}
			if (!this.getMenu().isEmpty()) {
				email.addItems(this.getMenu().toItemList(true));
			}
			if (this.stacks != null && !this.stacks.isEmpty()) {
				email.addItems(this.stacks);
				this.stacks.clear();
			}

			if (this.lockBtn != null) {
				email.setDeletable(!this.lockBtn.isLocked());
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

			File file = new File(EmailAPI.getGlobalDataPath(), "emails/"+filename);
			JsonParser.toJsonFile(file, object, true);

			this.getMenu().setLock(false);
			try {
				this.setRenderText("Save to " + file.getCanonicalPath(), Color.GREEN, Timer.parseTick(10, 0));
			} catch (IOException ignored) {
			}
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

	public void setRenderText(String text) {
		this.setRenderText(text, Color.RED);
	}
	public void setRenderText(String text, Color color) {
		this.setRenderText(text, color, EmailUtils.parseTick(0,0,0,25, 0));
	}
	public void setRenderText(String text, Color color, long ticks) {
		super.getMenu().setRenderText(text, color, ticks);
	}
	public void clearRenderText() {
		super.getMenu().clearRenderText();
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
	public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
		if (this.mcSounds.scroll((int) pMouseX, (int) pMouseY, (int)pDelta)) {
			return true;
		}
		return super.mouseScrolled(pMouseX, pMouseY, pDelta);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTicks);
		super.renderTooltip(graphics, mouseX, mouseY);
		if(this.getMenu().renderTimer != null && !this.getMenu().renderTimer.isDone() && this.getMenu().renderText != null && this.getMenu().renderColor != null) {
			if(this.getMenu().renderTimer.isDone()) {
				this.clearRenderText();
			}
			graphics.renderTooltip(this.font, this.font.split(Component.literal(this.getMenu().renderText), 128), mouseX, mouseY);
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		graphics.blit(BackGround, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
		this.titleField.render(graphics, mouseX, mouseY, partialTicks);
		for(EditBox tf : this.textFields) {
//			tf.render(graphics, mouseX, mouseY, partialTicks);
			graphics.drawString(this.font, tf.getValue().length()+"/"+tf.getMaxLength(), tf.getX() +tf.getWidth()+13, tf.getY() +2, (tf.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.WHITE).getRGB(), false);
		}
		this.expiration.render(graphics, this.leftPos + 149 + 22 + 10, this.topPos + 20 + 80, partialTicks);
		this.mcSounds.drawPopupMenu(graphics, mouseX, mouseY, partialTicks);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.fill(162, 100, 162 + 9, 100 + 9, this.getMenu().isLock() ? Color.RED.getRGB() : Color.GREEN.getRGB());

		EmailUtils.drawAlignRightString(graphics, this.font, I18n.get("info.inbox.title") + ":", this.titleField.getX() -2-this.leftPos,  this.titleField.getY() -2-this.topPos, (this.titleField.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.BLACK).getRGB(), false);
		EmailUtils.drawAlignRightString(graphics, this.font, I18n.get("info.inbox.generate.sound") + ":", this.localSound.getX() -2-this.leftPos,  this.localSound.getY() -2-this.topPos, (this.localSound.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.BLACK).getRGB(), false);
	}
}
