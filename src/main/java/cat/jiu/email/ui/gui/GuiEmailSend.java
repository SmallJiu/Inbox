package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.util.List;

import cat.jiu.email.util.SizeReport;
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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.ChatFormatting;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.apache.commons.lang3.StringUtils;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class GuiEmailSend extends AbstractContainerScreen<ContainerEmailSend> {
	public static final ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_send.png");
	public static final ResourceLocation EXPIRATION = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_expiration.png");
	private EditBox nameField;
    private EditBox titleField;
    private final EditBox[] textFields = new EditBox[5];
    private final GuiTime expiration = new GuiTime(this, false);
    
	public GuiEmailSend(ContainerEmailSend container, Inventory inventory) {
		super(container, inventory, Component.nullToEmpty(null));
		this.imageWidth = 176;
		this.imageHeight = 233;
	}
	
	@Override
	public void init() {
		super.init();
		this.addWidget(expiration);
		this.nameField = this.addRenderableWidget(new NameGuiTextField(this.font, this.getGuiLeft() + 39, this.getGuiTop() + 6, 109, 11));
        this.nameField.setTextColor(-1);
        this.nameField.setTextColorUneditable(-1);
        this.nameField.setMaxLength(100);
        this.nameField.setBordered(false);
        
        this.titleField = this.addRenderableWidget(new EditBox(this.font, this.getGuiLeft() + 39, this.getGuiTop() + 20, 109, 11, Component.nullToEmpty(null)));
		this.titleField.setTextColor(-1);
		this.titleField.setTextColorUneditable(-1);
		this.titleField.setMaxLength(100);
		this.titleField.setBordered(false);
        this.initText();

        this.addRenderableWidget(new GuiImageButton(this, this.leftPos + 149 + 28, this.topPos + 3, 16, 16, I18n.get("email.config.expiration"), 256, 256, 256, 256, btn->
			expiration.setEnable(!expiration.isEnable())
        )).setBackground(()->EXPIRATION);
        
        this.addRenderableWidget(new GuiImageButton(this, this.nameField.getX() +this.nameField.getWidth()+1, this.nameField.getY() -2, 22, this.nameField.getHeight()+2, I18n.get("info.email.dispatch"), 256, 256, 176, 9, 59, 50, btn-> {
			if(!this.getMenu().isCooling() && !this.getMenu().isLock()) {
				String name = nameField.getValue();
				if(StringUtils.isEmpty(name)) {
					this.setRenderText(I18n.get("info.email.error.empty_name"), Color.RED);
					return;
				}
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
				Email email = new Email(new Text(title), new Text(Minecraft.getInstance().player.getName()), null, null, msgs);

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
						this.setRenderText(new Text("info.email.error.send.to_big", report.slot(), report.size()).format(), Color.RED);
						return;
					}
				}
				EmailAPI.sendPlayerEmail(getMinecraft().player, name, email);
				clearRenderText();
			}
        })).setBackground(()->BackGround);

		this.addRenderableWidget(new GuiImageButton(this, this.titleField.getX() +this.titleField.getWidth()+1, this.titleField.getY() -2, 22, this.nameField.getHeight()+2, I18n.get("info.email.name"), 23, 15, 23, 15, b-> GuiHandler.openGui(GuiHandler.EMAIL_MAIN)))
				.setBackground(()->ShowInboxGui.inbox);
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
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTicks);
		super.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		graphics.blit(BackGround, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
		this.titleField.renderWidget(graphics, mouseX, mouseY, partialTicks);
		this.nameField.renderWidget(graphics, mouseX, mouseY, partialTicks);
		for(EditBox tf : this.textFields) {
			tf.renderWidget(graphics, mouseX, mouseY, partialTicks);
			graphics.drawString(this.font, tf.getValue().length()+"/"+tf.getMaxLength(), tf.getX() +tf.getWidth()+13, tf.getY() +2, (tf.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.WHITE).getRGB());
		}
		this.expiration.render(graphics, this.leftPos + 149 + 22 + 10, this.topPos + 20 + 80, partialTicks);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.fill(162, 100, 162 + 9, 100 + 9, this.getMenu().isLock() ? Color.RED.getRGB() : Color.GREEN.getRGB());

		EmailUtils.drawAlignRightString(graphics, this.font, I18n.get("info.email.addressee") + ":", 36, 6, (this.nameField.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.BLACK).getRGB(), false);
		EmailUtils.drawAlignRightString(graphics, this.font, I18n.get("info.email.title") + ":", 36, 21, (this.titleField.isMouseOver(mouseX, mouseY) ? Color.CYAN : Color.BLACK).getRGB(), false);

		if(this.renderTicks > 0 && this.renderText != null && this.renderColor != null) {
			if(this.renderTicks <= 0) this.clearRenderText();
			graphics.drawString(this.font, this.renderText, (88 - this.font.width(this.renderText) / 2), 138, this.renderColor.getRGB());
			this.renderTicks--;
		}
		if(this.getMenu().renderTicks > 0 && this.getMenu().renderText != null && this.getMenu().renderColor != null) {
			if(this.getMenu().renderTicks <= 0) this.getMenu().clearRenderText();
			graphics.drawString(this.font, this.getMenu().renderText, (88 - this.font.width(this.getMenu().renderText) / 2), 138, this.getMenu().renderColor.getRGB());
			this.getMenu().renderTicks--;
		}
		if(this.getMenu().isCooling()) {
			long millis = this.getMenu().getCoolingMillis() - System.currentTimeMillis();
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

			String text = I18n.get("info.email.cooling", d, h, m, s, t);
			graphics.drawString(this.font, text, this.imageWidth/2 - this.font.width(text)/2, 60, Color.RED.getRGB());
		}
		if(this.titleField.getValue().isEmpty()) {
			graphics.drawString(this.font, I18n.get("info.email.default_title"), 39, 21, Color.WHITE.getRGB());
		}
		if(this.textsIsEmpty()) {
			graphics.drawString(this.font, I18n.get("info.email.default_msg"), 10, 34, Color.WHITE.getRGB());
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static class NameGuiTextField extends EditBox {
		public NameGuiTextField(Font font, int x, int y, int par5Width, int par6Height) {
			super(font, x, y, par5Width, par6Height, Component.nullToEmpty(null));
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
					List<? extends Player> playerList = Minecraft.getInstance().player.level().players();
					Component name;

					if(this.index < playerList.size()) {
						name = playerList.get(this.index).getName();
						if(name.equals(Minecraft.getInstance().player.getName())
								&& EmailConfigs.Send.Enable_Send_To_Self.get()) {
							this.setValue(name.getString());
							this.index++;
							return true;
						}
						this.setValue(name.getString());
						this.index++;
					}else if(this.index < playerList.size() + 2) {
						this.absIndex++;
						if(this.absIndex==1) {
							this.setValue("@p");
						}else if(this.absIndex==2) {
							this.setValue("@a");
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
		public void renderWidget(GuiGraphics graphics, int x, int y, float t) {
			super.renderWidget(graphics, x, y, t);
			if("@p".equals(this.getValue())) {
				EmailUtils.drawAlignRightString(graphics, Minecraft.getInstance().font, I18n.get("info.email.@p"), this.getX() + this.width - 2, this.getY(), Color.BLACK.getRGB(), false);
			}else if("@a".equals(this.getValue())) {
				EmailUtils.drawAlignRightString(graphics, Minecraft.getInstance().font, I18n.get("info.email.@a"), this.getX() + this.width - 2, this.getY(), Color.BLACK.getRGB(), false);
			}
		}
	}
}
