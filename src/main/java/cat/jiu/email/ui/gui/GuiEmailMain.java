package cat.jiu.email.ui.gui;

import cat.jiu.core.api.element.ISound;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.timer.MillisTimer;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.event.InboxDrawEvent;
import cat.jiu.email.event.InboxDrawEvent.Type;
import cat.jiu.email.event.InboxFilterEvent;
import cat.jiu.email.event.InboxPlaySoundEvent;
import cat.jiu.email.net.msg.MsgDeleteEmail;
import cat.jiu.email.net.msg.MsgReadEmail;
import cat.jiu.email.net.msg.MsgReceiveEmail;
import cat.jiu.email.net.msg.refresh.MsgRefreshInbox;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.ui.gui.component.GuiButton;
import cat.jiu.email.ui.gui.component.GuiImageButton;
import cat.jiu.email.ui.gui.component.GuiPopupMenu;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.client.EmailSenderSndSound;
import cat.jiu.email.util.client.GuiDynamicImage;

import com.google.common.collect.Lists;
import morph.avaritia.util.TextUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings({"deprecation"})
@OnlyIn(Dist.CLIENT)
public class GuiEmailMain extends AbstractContainerScreen<ContainerEmailMain> {
	public static ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_main.png");
	public static ResourceLocation load = new ResourceLocation(EmailMain.MODID, "textures/gui/load.png");

	private final ContainerEmailMain container = super.getMenu();
	private boolean isClose = false;
	private GuiPopupMenu popupMenu = new GuiPopupMenu();
	private GuiPopupMenu filterMenu = new GuiPopupMenu();
	private GuiImageButton refreshBtn, playSoundBtn;
	private final GuiDynamicImage loadImage = new GuiDynamicImage(load, 18, false, 32, 32, 0, 0, 16, 16, 32, 576);
	
	public GuiEmailMain(ContainerEmailMain container, Inventory inventory) {
		super(container, inventory, Component.nullToEmpty(null));
		this.imageWidth = EmailConfigs.Main.Size.Width.get();
		this.imageHeight = EmailConfigs.Main.Size.Height.get();
	}
	
	@Override
	public void init() {
		super.init();
		this.initTurnPageBtn(this.leftPos, this.topPos);
		this.initFunctionBtn(this.leftPos, this.topPos);
		this.initPopupMenu();
		this.initFilterMenu();
		
		String text = I18n.get("info.email.black");
		int weight = this.font.width(text);
		this.addRenderableWidget(new GuiButton(this.leftPos+190 - (weight + 4), this.topPos+145, weight + 4, super.font.lineHeight+3, Component.nullToEmpty(text), btn->
				GuiHandler.openGui(GuiHandler.EMAIL_BLACKLIST)
		));
		text = I18n.get("info.email.filter");
		weight = super.font.width(text);
		this.addRenderableWidget(this.filterButton = new GuiButton(this.leftPos+140 - (weight + 4), this.topPos+145, weight + 4, super.font.lineHeight+3, Component.nullToEmpty(text), btn->{
			this.filterMenu.setCreatePoint(this.filterButton.getX(), this.filterButton.getY() - this.filterMenu.height - 2);
			this.filterMenu.setVisible(!this.filterMenu.isVisible());
		}));

		this.refreshBtn = this.addRenderableWidget(new GuiImageButton(this, this.leftPos + 6, this.topPos + 6, 7, 7, ()->
			Component.translatable(refreshCoolingTicks <= 0 ? "info.email.refresh" : "info.email.refresh.cooling")
		, 256, 256, 111, 169, 55, 55, btn->{
			if(refreshCoolingTicks <= 0) {
				this.refresh();
				this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
		})).setBackground(()->BackGround);

//		Screen.blit(matrix, x+218+3, y+5, 10, 10, 1 + (this.isPlayingSound() ? 55 : 0), 169,  55, 55, 256, 256);
		this.playSoundBtn = this.addRenderableWidget(new GuiImageButton(this, this.leftPos + 218 + 3, this.topPos + 5, 10, 10, ()->null, 256, 256, -1, 169, 55, 55, btn->{
			if(this.currentEmail >=0 && this.getCurrentEmail()!=null && this.getCurrentEmail().hasSound()) {
				if(this.currentSound==null) {
					this.currentSound = new EmailSenderSndSound(this.getCurrentEmail().getSound(), this.currentEmail);
					MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Start(this.getCurrentEmail().getSound(), this.currentEmail));
					this.getMinecraft().getSoundManager().play(this.currentSound);
					this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					this.currentSoundCheck = 0;
					this.currentSoundLastTime = 0;
				}else {
					this.stopSound();
					this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				}
			}
		})).setBackground(()->BackGround);
		this.playSoundBtn.visible = false;
	}

	private void initTurnPageBtn(int x, int y) {
		this.addRenderableWidget(new GuiButton(x+82, y+19, 9, 20, Component.nullToEmpty("\u21e7"), btn-> goEmail(-1)));
		this.addRenderableWidget(new GuiButton(x+82, y+83, 9, 20, Component.nullToEmpty("\u21e9"), btn->goEmail(1)));
		this.addRenderableWidget(new GuiButton(x+222, y+31, 9, 20, Component.nullToEmpty("\u21e7"),btn->goMessage(-1)));
		this.addRenderableWidget(new GuiButton(x+222, y+83, 9, 20, Component.nullToEmpty("\u21e9"), btn->goMessage(1)));
	}
	
	private void initFunctionBtn(int x, int y) {
		this.addRenderableWidget(new GuiButton(x+4, y+106, 43, 12, Component.nullToEmpty(I18n.get("info.email.delete_accept")), btn -> {
			if(this.container.isRefresh()) return;
			EmailMain.net.sendMessageToServer(new MsgDeleteEmail.AllReceive());
		}));
		this.addRenderableWidget(new GuiButton(x+4, y+119, 43, 12, Component.nullToEmpty(I18n.get("info.email.delete_read")), btn-> {
			if(this.container.isRefresh()) return;
			EmailMain.net.sendMessageToServer(new MsgDeleteEmail.AllRead());
		}));
		this.addRenderableWidget(new GuiButton(x+4, y+132, 43, 12, Component.nullToEmpty(I18n.get("info.email.dispatch")), btn-> GuiHandler.openGui(GuiHandler.EMAIL_SEND)));
		this.addRenderableWidget(new GuiButton(x+192, y+106, 41, 12, Component.nullToEmpty(I18n.get("info.email.delete")), btn->{
			if(currentEmail != -1) {
//				Email email = getCurrentEmail();
//				if(email.hasItems() && !email.isReceived()) {
//					mc.setScreen(new GuiDeleteEmailConfirm(GuiEmailMain.this, new EmailType(currentEmail, email)));
//				}else {
					EmailMain.net.sendMessageToServer(new MsgDeleteEmail.Delete(currentEmail));
//				}
			}
		}));
		this.addRenderableWidget(new GuiButton(x+192, y+119, 41, 12, Component.nullToEmpty(I18n.get("info.email.accept")), btn->{
			if(currentEmail != -1) {
				if(container.getInboxSize()+55 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
					emailIsOutStorageSize();
					return;
				}
				EmailMain.net.sendMessageToServer(new MsgReceiveEmail.Receive(currentEmail));
			}
		}));
		this.addRenderableWidget(new GuiButton(x+192, y+145, 41, 12, Component.nullToEmpty(I18n.get("info.email.accept_all")),btn->{
			if(this.container.isRefresh()) return;
			if(!EmailConfigs.isInfiniteSize()) {
				if(container.getInboxSize()+(container.getInbox().getUnReceived() * 55L) >= 2097152L) {
					emailIsOutStorageSize();
				}
			}
			EmailMain.net.sendMessageToServer(new MsgReceiveEmail.All());
		}));
		this.addRenderableWidget(new GuiButton(x+192, y+132, 41, 12, Component.nullToEmpty(I18n.get("info.email.read_all")),btn->{
			if(this.container.isRefresh()) return;

			if(!EmailConfigs.isInfiniteSize()) {
				if(container.getInboxSize() + container.getInbox().getUnRead() * 51L > 2097152L) {
					emailIsOutStorageSize();
					return;
				}
			}
			EmailMain.net.sendMessageToServer(new MsgReadEmail.All());
		}));
	}
	
	private Button filterButton;
	private int currentFilter = 0;
	
	private void initFilterMenu() {
		this.filterMenu = new GuiPopupMenu();
		this.addFilter();
		MinecraftForge.EVENT_BUS.post(new InboxFilterEvent(this));
	}

	private void addFilter() {
		this.addFilter(I18n.get("info.email.filter.default"), email -> true);

		this.addFilter(I18n.get("info.email.filter.is_read"), Email::isRead);
		this.addFilter(I18n.get("info.email.filter.not_read"), email -> !email.isRead());

		this.addFilter(I18n.get("info.email.filter.has_sound"), Email::hasSound);

		this.addFilter(I18n.get("info.email.filter.has_item"), Email::hasItems);
		this.addFilter(I18n.get("info.email.filter.is_accept"), email -> email.hasItems() && email.isReceived());
		this.addFilter(I18n.get("info.email.filter.not_accept"), email -> email.hasItems() && !email.isReceived());

		this.addFilter(I18n.get("info.email.filter.has_expiration"), Email::hasExpirationTime);
		this.addFilter(I18n.get("info.email.filter.is_expiration"), email -> email.hasExpirationTime() && email.isExpiration());
		this.addFilter(I18n.get("info.email.filter.not_expiration"), email -> email.hasExpirationTime() && !email.isExpiration());
	}
	
	public void addFilter(String name, Predicate<Email> predicate) {
		final int filterID = this.filterMenu.getButtonSize();
		this.filterMenu.addPopupButton(new GuiButton(0, 0, 45, 12, Component.nullToEmpty(name), btn->{
			this.emailIDs = this.container.getInbox().getEmailIDs().stream()
					.filter(id -> predicate.test(this.container.getInbox().getEmail(id)))
					.sorted(Comparator.reverseOrder())
					.mapToLong(Long::longValue).toArray();
			goEmail(0);
			this.currentEmail = -1;
			this.currentFilter = filterID;
		}));
	}
	
	private void initPopupMenu() {
		this.popupMenu = new GuiPopupMenu();
//		this.popupMenu.setWorldAndResolution(this.getMinecraft(), this.width, this.height);
		
		this.popupMenu.addPopupButton(new GuiButton(0, 0, 45, 12, Component.nullToEmpty(I18n.get("info.email.delete")),btn->{
			if(popupMenuCurrentEmail != -1) {
//				Email email = container.getInbox().getEmail(popupMenuCurrentEmail);
//				if(email.hasItems() && !email.isReceived()) {
//					mc.setScreen(new GuiDeleteEmailConfirm(GuiEmailMain.this, new EmailType(popupMenuCurrentEmail, email)));
//				}else {
					EmailMain.net.sendMessageToServer(new MsgDeleteEmail.Delete(popupMenuCurrentEmail));
//					popupMenu.setVisible(false);
//				}
			}
		}));
		this.popupMenu.addPopupButton(new GuiButton(0, 0, 45, 12, Component.nullToEmpty(I18n.get("info.email.accept")), btn->{
			if(popupMenuCurrentEmail != -1) {
				if(container.getInboxSize()+55 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
					emailIsOutStorageSize();
					return;
				}
				EmailMain.net.sendMessageToServer(new MsgReceiveEmail.Receive(popupMenuCurrentEmail));
			}
		}));
		this.popupMenu.addPopupButton(new GuiButton(0, 0, 45, 12, Component.nullToEmpty(I18n.get("info.email.read")),btn->{
			if(popupMenuCurrentEmail != -1) {
				if(container.getInboxSize()+51 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
					emailIsOutStorageSize();
					return;
				}
				EmailMain.net.sendMessageToServer(new MsgReadEmail(popupMenuCurrentEmail));
			}
		}));
		this.popupMenu.addPopupButton(new GuiButton(0, 0, 50, 12, Component.nullToEmpty(I18n.get("info.email.read_accept")), btn-> {
			if(popupMenuCurrentEmail != -1) {
				if(container.getInboxSize()+51+55 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
					emailIsOutStorageSize();
					return;
				}
				EmailMain.net.sendMessageToServer(new MsgReadEmail(popupMenuCurrentEmail));
				EmailMain.net.sendMessageToServer(new MsgReceiveEmail.Receive(popupMenuCurrentEmail));
			}
		}));
	}
	
	private int emailsSize = -1;
	private long[] showEmails = null;
	private long[] emailIDs = null;
	private long currentEmail = -1;
	private int emailPage = 0;
	
	public void goEmail(int page) {
		if(this.emailIDs == null) return;
		if(this.emailIDs.length > 5) {
			this.emailPage += page;
			
			if(this.emailPage < 0) {
				this.emailPage = 0;
			}
			if(this.emailPage > this.emailIDs.length - 5) {
				this.emailPage = this.emailIDs.length - 5;
			}
			
			this.showEmails = Arrays.copyOfRange(this.emailIDs, this.emailPage, this.emailPage + 5);
		}else {
			this.showEmails = this.emailIDs;
		}
	}
	
	private int maxSelectedTextRows = EmailConfigs.Main.Selected_Text_Rows.get();
	public void setMaxSelectedTextRows(int maxRows) {this.maxSelectedTextRows = maxRows;}
	public int getMaxSelectedTextRows() {return maxSelectedTextRows;}
	
	private int showSelectedSpacing = EmailConfigs.Main.Selected_Text_Spacing.get();
	public void setSelectedTextSpacing(int spacing) {this.showSelectedSpacing = spacing;}
	public int getSelectedTextSpacing() {return showSelectedSpacing;}

	private final List<Message> showMessages = Lists.newArrayList();
	private int[] msgKeyMap = null;
	private int[] currentMsg = null;
	private int msgPage = 0;
	
	private void goMessage(int page) {
		if(this.msgKeyMap == null) return;
		if(this.showMessages.size() > this.maxSelectedTextRows) {
			this.msgPage += page;
			int maxPage = this.showMessages.size() - this.maxSelectedTextRows;
			if(this.msgPage > this.showMessages.size()) this.msgPage = this.showMessages.size();
			if(this.msgPage < 0) this.msgPage = 0;
			if(this.msgPage > maxPage) this.msgPage = maxPage;
			this.currentMsg = Arrays.copyOfRange(this.msgKeyMap, this.msgPage, this.maxSelectedTextRows + this.msgPage);
		}else {
			this.currentMsg = Arrays.copyOf(this.msgKeyMap, this.msgKeyMap.length);
		}
	}
	
	private void initEmails() {
		if(this.emailsSize == -1) this.emailsSize = this.container.getInbox().emailCount();
		if(this.emailIDs == null && this.emailsSize > 0) {
			this.emailIDs = this.container.getInbox().getEmailIDs().stream()
								.sorted(Comparator.reverseOrder())
								.mapToLong(Long::longValue).toArray();
			this.emailPage = 0;
			this.goEmail(0);
		}
		if(this.emailsSize != this.container.getInbox().emailCount()) {
			this.emailsSize = this.container.getInbox().emailCount();
			this.emailIDs = this.container.getInbox().getEmailIDs().stream()
								.sorted(Comparator.reverseOrder())
								.mapToLong(Long::longValue).toArray();
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double key) {
		x -= this.leftPos;
		y -= this.topPos;
		if(EmailUtils.isInRange(x, y, 82, 41, 8, 40) || EmailUtils.isInRange(x, y, 17, 10, 63, 90)) {
        	int page = 0;

        	if(EmailUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || EmailUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
        		page += 2;
        	}
        	if(EmailUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || EmailUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL)) {
        		page += 1;
        	}
        	
			if(key > 0) {
				this.goEmail(-1 - page);
			}else if(key < 0) {
				this.goEmail(1 + page);
			}
			return true;
		}else if(EmailUtils.isInRange(x, y, 221, 53, 8, 28) || EmailUtils.isInRange(x, y, 92, 30, 128, 74)) {
			int page = 0;

			if(EmailUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || EmailUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
				page += 2;
			}
			if(EmailUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || EmailUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL)) {
				page += 1;
			}
			if(key > 0) {
				this.goMessage(-1 - page);
			}else if(key < 0) {
				this.goMessage(1 + page);
			}
			return true;
		}
		return super.mouseScrolled(x, y, key);
	}
	
	double lastClickX = 0;
	double lastClickY = 0;

	@Override
	public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
		return super.keyPressed(pKeyCode, pScanCode, pModifiers);
	}

	@Override
	public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
		return super.keyReleased(pKeyCode, pScanCode, pModifiers);
	}

	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		if(this.container.isRefresh()) return false;
		
		if(EmailUtils.isInRange(this.lastClickX, this.lastClickY, this.leftPos + 76, this.topPos + 41, 8, 40)
		|| EmailUtils.isInRange(this.lastClickX, this.lastClickY, this.leftPos + 18, this.topPos + 19, 57, 86)) {
			if(keyCode == GLFW.GLFW_KEY_UP) {
				this.goEmail(-1);
			}else if(keyCode == GLFW.GLFW_KEY_DOWN) {
				this.goEmail(1);
			}
		}else if(EmailUtils.isInRange(this.lastClickX, this.lastClickY, this.leftPos + 216, this.topPos + 53, 8, 28)
			|| EmailUtils.isInRange(this.lastClickX, this.lastClickY, this.leftPos + 87, this.topPos + 30, 128, 74)) {
			if(keyCode == GLFW.GLFW_KEY_UP) {
				this.goMessage(-1);
			}else if(keyCode == GLFW.GLFW_KEY_DOWN) {
				this.goMessage(1);
			}
		}
		return super.charTyped(typedChar, keyCode);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);
		this.popupMenu.drawPopupMenu(graphics, this.popupMenuCurrentEmail, this.getMinecraft(), mouseX, mouseY, partialTicks);
		this.filterMenu.drawPopupMenu(graphics, -1, this.getMinecraft(), mouseX, mouseY, partialTicks);
		super.renderTooltip(graphics, mouseX, mouseY);
	}

	int currentSoundCheck = 0;
	long currentSoundLastTime = 0;

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		this.renderBackground(graphics);
//		GlStateManager.pushMatrix();
		graphics.blit(BackGround, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
//		GlStateManager.popMatrix();

		// TODO 检查播放的音效是否已停止
		if(this.currentSound!=null && this.currentSound.isStopped()) {
			this.stopSound();
		}
		if(this.currentSound!=null) {
			this.currentSoundCheck++;
			if(this.currentSoundCheck >= 20) {
				this.currentSoundCheck = 0;
				if(this.currentSoundLastTime == this.currentSound.time.getTicks()) {
					this.stopSound();
				}else {
					this.currentSoundLastTime = this.currentSound.time.getTicks();
				}
			}
		}
		this.refreshBtn.visible = refreshCoolingTicks <= 0;

		Email currentEmail = this.getCurrentEmail();
		if(currentEmail!=null){
			if(currentEmail.hasItems() && this.container.isEmptyStacks()){
				this.container.putStack(currentEmail.getItems());
			}else if(!currentEmail.hasItems() && !this.container.isEmptyStacks()) {
				this.container.clearStacks();
			}
		}
	}

	private MillisTimer renderTimer = null;
	private String renderText;
	private Color renderColor;

	public void emailIsOutStorageSize() {
		this.setRenderText(EmailUtils.parseTick(0,0,0,10, 0), true, I18n.get("info.email.out_size"), Color.RED);
	}

	@Deprecated
	public void setRenderText(long showTick, @Nonnull String text, @Nonnull Color color, int x, int y) {
		this.setRenderText(showTick, true, text, color);
	}
	/**
	 * @param showTime the text render time
	 * @param isTickTime true if show time is tick, false will be millis
	 * @param text the show text
	 * @param color this show text color
	 */
	public void setRenderText(long showTime, boolean isTickTime, @Nonnull String text, @Nonnull Color color) {
		this.renderTimer = new MillisTimer(isTickTime ? showTime*50 : showTime).start();
		this.renderText = text;
		this.renderColor = color;
	}
	public void clearRenderText() {
		this.renderTimer = null;
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		this.children().forEach(e->{
			if(e instanceof AbstractWidget widget){
				if(widget instanceof Button && !(widget instanceof GuiImageButton) && widget.visible){
					this.drawHorizontalLine(graphics, widget.getX() - this.leftPos, widget.getX() + widget.getWidth() - 2 - this.leftPos, widget.getY() + widget.getHeight()-1 - this.topPos, (widget.isHovered() ? Color.WHITE : Color.BLACK).getRGB());
				}
			}
		});
		if(this.container.isRefresh()) {
			this.loadImage.draw(graphics, 3, 146);
			this.drawStringWithShadow(graphics, I18n.get("info.email.refreshing"), 3 + this.loadImage.width, 146 + 5, Color.RED.getRGB());
		}else {
			MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.INBOX, TickEvent.Phase.START, this.container.getInbox(), -1, 0, 0));

			this.drawInbox(graphics, mouseX, mouseY);

			MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.INBOX, TickEvent.Phase.END, this.container.getInbox(), -1, 0, 0));
		}
	}

	private final int Candidate_Email_X = EmailConfigs.Main.Position.Candidate_Email.X.get();
	private final int Candidate_Email_Y = EmailConfigs.Main.Position.Candidate_Email.Y.get();

	protected void drawInbox(GuiGraphics graphics, int mouseX, int mouseY) {
		@Deprecated
		final int x=0, y=0;

		// TODO 绘制需要渲染的信息，目前Email中没有使用过此功能
		if(this.renderTimer != null && this.renderTimer.isStarted() && !this.renderTimer.isDone()) {
			if(super.font.width(this.renderText)>this.width) {
				List<String> str = EmailUtils.splitString(this.renderText, this.width);
				int y_t = y+this.height;
				for (String s : str) {
					this.drawStringWithShadow(graphics, s, x, y_t, this.renderColor.getRGB());
					y_t += super.font.lineHeight;
				}
			}else {
				this.drawStringWithShadow(graphics, this.renderText, x, y+this.height, this.renderColor.getRGB());
			}
		}

		// TODO 绘制当前过滤器
		this.drawStringWithShadow(graphics, I18n.get("info.email.filter")+ ": " + this.filterMenu.getPopupButton(this.currentFilter).getMessage().getString(), x, y-super.font.lineHeight, Color.WHITE.getRGB());

		// TODO 绘制邮箱所占网络包大小
//		super.font.drawString(matrix, I18n.get("info.email.storage"), x+5, y+148, Color.BLACK.getRGB());
		Color sizeColor = Color.GREEN;
		if(!EmailConfigs.isInfiniteSize()) {
			long size = this.container.getInboxSize();
			if (size >= 1398122) {
				sizeColor = Color.RED;
			} else if (size >= 699061) {
				sizeColor = Color.YELLOW;
			}
		}
		String bytes = EmailConfigs.isInfiniteSize() ? TextUtils.makeFabulous("Infinite") : "2097152";
		int bytesWidth = this.font.width(bytes);
		this.drawStringNoShadow(graphics, bytes, x+6, y+156, Color.BLACK.getRGB());
		this.drawStringNoShadow(graphics, "Bytes", x+7+bytesWidth+1, y+156, Color.BLACK.getRGB());
		this.drawAlignRightString(graphics, String.valueOf(this.container.getInboxSize()), x+6+bytesWidth, y+145, sizeColor.getRGB(), true);
		this.drawHorizontalLine(graphics, x+5, x+5+bytesWidth, y+154, Color.BLACK.getRGB());

		// TODO 绘制刷新按钮(已改为图片按钮形式)
//		GlStateManager.pushMatrix();
//		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
//		this.getMinecraft().getTextureManager().bindTexture(BackGround);
//		blit(matrix, x + 6, y + 6, 6, 6, 111, 169, 55, 55, 256, 256);
//		GlStateManager.popMatrix();
//		if(isInRange(mouseX, mouseY, this.leftPos + 5, this.topPos + 5, 7, 7)) {
//			this.renderTooltip(matrix, Component.nullToEmpty(I18n.get(this.refreshCoolingTicks <= 0 ? "info.email.refresh" : "info.email.refresh.cooling")), mouseX - this.leftPos, mouseY - this.topPos);
//		}

		if(this.container.getInbox() == null
		|| this.container.getInbox().emailCount()<=0) return;
		this.initEmails();
		if(this.showEmails == null) return;

		// TODO 展示未读(*)与未领($)
		this.drawStringNoShadow(graphics, "$:"+ this.container.getInbox().getUnReceived(), x+25, y+3, Color.RED.getRGB());
		this.drawStringNoShadow(graphics, "*:" + this.container.getInbox().getUnRead(), x+50, y+3, Color.RED.getRGB());

		// TODO 绘制所选邮件
		if(this.currentEmail >= 0) {
			if(this.container.getInbox().hasEmail(this.currentEmail)) {
				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CURRENT, TickEvent.Phase.START, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
				Email email = this.getCurrentEmail();

				String sender = email.getSender().format();
				if(super.font.width(sender) > EmailConfigs.Main.Number_Of_Words.Current_Email.Sender.get()) {
					sender = super.font.plainSubstrByWidth(sender, EmailConfigs.Main.Number_Of_Words.Current_Email.Sender.get()) + "...";
				}
				String title = email.getTitle().format();
				if(super.font.width(title) > EmailConfigs.Main.Number_Of_Words.Current_Email.Title.get()-(email.hasSound()?13:0)) {
					title = super.font.plainSubstrByWidth(title, EmailConfigs.Main.Number_Of_Words.Current_Email.Title.get()-(email.hasSound()?13:0)) + "...";
				}

				this.drawStringNoShadow(graphics, title, x+10+EmailConfigs.Main.Position.Current_Email.Title.X.get(), y+EmailConfigs.Main.Position.Current_Email.Title.Y.get(), Color.WHITE.getRGB());
				this.drawStringNoShadow(graphics, sender, x+6+EmailConfigs.Main.Position.Current_Email.Sender.X.get(), y+EmailConfigs.Main.Position.Current_Email.Sender.Y.get(), Color.WHITE.getRGB());
				this.drawAlignRightString(graphics, email.getCreateTimeAsString().substring(2, email.getCreateTimeAsString().length()-3), x+5+ 64 +EmailConfigs.Main.Position.Current_Email.Time.X.get(), y+EmailConfigs.Main.Position.Current_Email.Time.Y.get(), Color.WHITE.getRGB(), false);
				this.drawCenteredStringWithShadow(graphics, String.valueOf(this.currentEmail), x+7+EmailConfigs.Main.Position.Current_Email.MsgID.X.get(), y+EmailConfigs.Main.Position.Current_Email.MsgID.Y.get(), Color.WHITE.getRGB());

				// TODO 绘制附带信息
				int msgIndex = -1;
				for(int i = 0; i < this.currentMsg.length; i++) {
					int index = this.currentMsg[i];
					Message msg = this.showMessages.get(index);

					if(msgIndex != msg.row) {
						this.drawStringNoShadow(graphics, Integer.toString(msg.row+1), x+7+(EmailConfigs.Main.Position.Current_Email.Row.X.get() - font.width(Integer.toString(msg.row)) / 2),  y-1+EmailConfigs.Main.Position.Current_Email.Row.Y.get() + ((this.font.lineHeight+EmailConfigs.Main.Selected_Text_Spacing.get()) * i), Color.BLACK.getRGB());
						msgIndex = msg.row;
					}else {
						// 更改强制Unicode之后会直接黑屏，不知道为啥
//						boolean unicode = this.getMinecraft().options.forceUnicodeFont().get();
//						this.getMinecraft().options.forceUnicodeFont().set(false);
						if(index+1 >= this.showMessages.size()
						|| this.showMessages.get(index+1).row != msgIndex) {
							this.drawCenteredStringNoShadow(graphics, "\u255a", x+6+EmailConfigs.Main.Position.Current_Email.Row.X.get(),  y-1+EmailConfigs.Main.Position.Current_Email.Row.Y.get() + ((this.font.lineHeight+EmailConfigs.Main.Selected_Text_Spacing.get()) * i), Color.BLACK.getRGB());
						}else {
							this.drawCenteredStringNoShadow(graphics, "\u2560", x+6+EmailConfigs.Main.Position.Current_Email.Row.X.get(),  y-1+EmailConfigs.Main.Position.Current_Email.Row.Y.get() + ((this.font.lineHeight+EmailConfigs.Main.Selected_Text_Spacing.get()) * i), Color.BLACK.getRGB());
						}
//						this.getMinecraft().options.forceUnicodeFont().set(unicode);
					}
					this.drawStringNoShadow(graphics, msg.msg, x+7+EmailConfigs.Main.Position.Current_Email.Msg.X.get(), y-1+EmailConfigs.Main.Position.Current_Email.Msg.Y.get() + ((this.font.lineHeight+EmailConfigs.Main.Selected_Text_Spacing.get()) * i), Color.BLACK.getRGB());
				}

				this.playSoundBtn.visible = email.hasSound();
				// TODO 绘制附带音效
				if(email.hasSound()) {
					MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.SOUND, TickEvent.Phase.START, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
					if(!this.isPlayingSound() && this.currentSound != null) {
						this.stopSound();
					}

					graphics.blit(BackGround, x+218, y+4, 236, 0, 3, 12);
					this.playSoundBtn.setUOffset(1 + (this.isPlayingSound() ? 55 : 0));

					if(this.playSoundBtn.isMouseOver(mouseX, mouseY)) {
						List<Component> hover = Lists.newArrayList();
						hover.add(Component.translatable("info.email.play_sound" + (this.isPlayingSound() ? ".stop" : "")));
						if(this.isPlayingSound()) {
							hover.add(Component.nullToEmpty(this.currentSound.time.toStringTime(false)));
						}
						graphics.renderComponentTooltip(this.font, hover, mouseX - this.leftPos, mouseY - this.topPos);
					}
					MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.SOUND, TickEvent.Phase.END, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
				}
				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CURRENT, TickEvent.Phase.END, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
			}
		}

		long visibleEmail = -1;
		// TODO 绘制鼠标底下的候选邮件信息
		for(int i = 0; !this.popupMenu.isVisible() && i < 5; i++) {
			if(this.showEmails == null || i >= this.showEmails.length) break;
			if(EmailUtils.isInRange(mouseX, mouseY, this.leftPos + Candidate_Email_X, this.topPos + Candidate_Email_Y + ((19 * i)), 60, 17)) {
				if(this.container.getInbox().hasEmail(this.showEmails[i])) {
					Email email = this.container.getInbox().getEmail(this.showEmails[i]);
					List<String> tip = Lists.newArrayList();

					tip.add(email.getTitle().format());
					tip.add("");

					tip.add(ChatFormatting.GRAY + email.getCreateTimeAsString());
					tip.add(I18n.get("info.email.main.from", email.getSender().format()));

					tip.add("");

					tip.add(I18n.get("info.email.email_size", email.getEmailNetworkSize()));
					if(email.getExpirationTime()!=null) {
						tip.add("");
						long time = email.getExpirationTimeAsTimestamp();
						if(System.currentTimeMillis() >= time) {
							tip.add(String.format("%s: %s", I18n.get("email.config.expiration"), ChatFormatting.RED + I18n.get("email.config.expiration.ed")));
						}else {
							tip.add(I18n.get("info.email.remain_expiration_time", EmailUtils.formatTimestamp(time - System.currentTimeMillis())));
						}
					}

					visibleEmail = this.showEmails[i];
					graphics.renderComponentTooltip(this.font, tip.stream().map(Component::nullToEmpty).collect(Collectors.toList()), mouseX - this.leftPos, mouseY - this.topPos);
					break;
				}
			}
		}

		// TODO 展示邮件列表
		for (int i = 0; i < this.showEmails.length; i++) {
			if(this.container.getInbox().hasEmail(this.showEmails[i])) {
				Email email = this.container.getInbox().getEmail(this.showEmails[i]);
				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CANDIDATE, TickEvent.Phase.START, this.container.getInbox(), this.showEmails[i], mouseX, mouseY));

				StringBuilder identifier = new StringBuilder();
				if(email.getExpirationTime()!=null && email.getExpirationTime().millis>1) {
					identifier.append(email.isExpiration() ? ChatFormatting.RED : ChatFormatting.GREEN);
					identifier.append('#');
				}
				if(email.hasItems()) {
					identifier.append(email.isReceived() ? ChatFormatting.GREEN : ChatFormatting.RED);
					identifier.append('$');
				}
				identifier.append(email.isRead() ? ChatFormatting.GREEN : ChatFormatting.RED);
				identifier.append('*');

				this.drawAlignRightString(graphics, identifier.toString(), x+Candidate_Email_X+61, y+Candidate_Email_Y + (19 * i), Color.BLACK.getRGB(), false);

				this.drawCenteredStringWithShadow(graphics, String.valueOf(this.showEmails[i]), x+Candidate_Email_X-8, y+Candidate_Email_Y + 5 + (19 * i), (this.showEmails[i] == visibleEmail ? Color.CYAN : Color.WHITE).getRGB());

				String sender = email.getSender().format();
				if(super.font.width(sender) > EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender.get()) {
					sender = super.font.plainSubstrByWidth(sender, EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender.get()) + "...";
				}
				this.drawStringNoShadow(graphics, sender, x+Candidate_Email_X+1, y+Candidate_Email_Y + (19 * i)+1, Color.WHITE.getRGB());
				this.drawStringNoShadow(graphics, email.getCreateTimeAsString().substring(5, email.getCreateTimeAsString().length()-3), x+Candidate_Email_X+1, y+Candidate_Email_Y + 10 + (19 * i), Color.BLACK.getRGB());

				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CANDIDATE, TickEvent.Phase.END, this.container.getInbox(), this.showEmails[i], mouseX, mouseY));
			}
		}
	}

	private EmailSenderSndSound currentSound;

	public boolean isPlayingSound() {
		if(this.currentSound != null) {
			return this.getMinecraft().getSoundManager().isActive(this.currentSound) || !this.currentSound.isStopped();
		}
		return false;
	}

	public ISound getPlayingSound() {
		return this.getCurrentEmail().getSound().copy();
	}

	@Override
	public void onClose() {
		super.onClose();
		this.stopSound();
		this.isClose = true;
		MinecraftForge.EVENT_BUS.unregister(this);
	}

	private long popupMenuCurrentEmail = -1;

	@Override
	public boolean mouseClicked(double x, double y, int btn) {
		this.lastClickX = x;
		this.lastClickY = y;
		super.mouseClicked(x, y, btn);

		if(this.container.isRefresh()) return false;
		if(this.getSlotUnderMouse() != null && this.getSlotUnderMouse().hasItem()) {
			return false;
		}
		if(this.popupMenu.mouseClicked(x, y, btn)){
			return true;
		}
		if(this.filterMenu.mouseClicked(x, y, btn)){
			return true;
		}

		// TODO 刷新邮箱(已改为图片按钮形式)
//		if(isInRange(x, y, this.leftPos + 5, this.topPos + 5, 7, 7)
//		&& this.refreshCoolingTicks <= 0) {
//			this.refresh();
//			this.getMinecraft().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//			return true;
//		}
		if(this.container.getInbox()==null || this.container.getInbox().emailCount()<=0 || this.showEmails == null) return false;

		boolean showPopupMenu = false;
		// TODO 展示邮件或者子菜单
		for(int index = 0; index < 5; index++) {
			if(EmailUtils.isInRange(x, y, this.leftPos + Candidate_Email_X, this.topPos + Candidate_Email_Y + (19 * index), 60, 17)) {
				if(index >= this.showEmails.length) break;
				if(!this.container.getInbox().hasEmail(this.showEmails[index])) continue;
				if(btn == 0) {
					// TODO 展示邮件
					this.showEmail(index);
				}else if(btn == 1) {
					// TODO 展示子菜单
					this.popupMenuCurrentEmail = this.showEmails[index];
					this.popupMenu.setCreatePoint(x, y);
					this.popupMenu.setVisible(true);
					showPopupMenu = true;
					this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				}
				break;
			}
		}

		// TODO 点击音效 #playSoundBtn
//		if(this.currentEmail >=0 && this.getCurrentEmail()!=null && this.getCurrentEmail().hasSound()) {
//			if(this.currentSound==null) {
//				this.currentSound = new EmailSenderSndSound(this.getCurrentEmail().getSound(), this.currentEmail);
//				MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Start(this.getCurrentEmail().getSound(), this.currentEmail));
//				this.getMinecraft().getSoundManager().play(this.currentSound);
//				this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//				this.currentSoundCheck = 0;
//				this.currentSoundLastTime = 0;
//			}else {
//				this.stopSound();
//				this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//			}
//		}

		if(!showPopupMenu) {
			this.popupMenu.setVisible(false);
		}
		if(!this.filterButton.isMouseOver(x,y) && this.filterMenu.isVisible()){
			this.filterMenu.setVisible(false);
		}
		return false;
	}

	private void showEmail(int index) {
		Email email = this.container.getInbox().getEmail(this.showEmails[index]);
		if(email != null) {
			if(!email.isRead()) {
				if(this.container.getInboxSize()+51 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
					emailIsOutStorageSize();
					return;
				}
				EmailMain.net.sendMessageToServer(new MsgReadEmail(this.showEmails[index]));
			}
			this.clearRenderText();
			this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));

			if(this.showEmails[index] != this.currentEmail) this.stopSound();
			this.currentEmail = this.showEmails[index];
			this.container.setCurrenEmail(this.currentEmail);

			// TODO 设置邮件物品
			this.container.clearStacks();
			if(email.hasItems()) {
				this.container.putStack(email.getItems());
			}

			if(this.container.getInbox().hasEmail(this.currentEmail)) {
				if(email.hasMessages()) {
					this.formatMessage(email.getMessages());
					this.msgKeyMap = new int[this.showMessages.size()];
					for(int j = 0; j < this.showMessages.size(); j++) {
						this.msgKeyMap[j] = j;
					}
					this.currentMsg = new int[Math.min(this.showMessages.size(), this.maxSelectedTextRows)];
					for(int j = 0; j < this.showMessages.size(); j++) {
						if(j >= this.currentMsg.length) break;
						this.currentMsg[j] = j;
					}
				}else {
					this.msgKeyMap = new int[0];
					this.currentMsg = new int[0];
					this.showMessages.clear();
				}
				this.msgPage = 0;
			}
		}
	}

	private void formatMessage(List<IText> msgs) {
		this.showMessages.clear();
		int width = EmailConfigs.Main.Number_Of_Words.Current_Email.Message.get();

		if(EmailConfigs.Main.Enable_Vanilla_Wrap_Text.get()) {
			for(int row = 0; row < msgs.size(); row++) {
				String msg = msgs.get(row).format();
				if(msg.startsWith("&il")) msg = "    " + msg.substring(3);
				List<Message> m = Lists.newArrayList();

				List<String> formatMsg = EmailUtils.splitString(msg, width);
				for(int index = 0; index < formatMsg.size(); index++) {
					m.add(new Message(row, index, formatMsg.get(index)));
				}
				this.showMessages.addAll(m);
			}
		}else {
			for(int row = 0; row < msgs.size(); row++) {
				String msg = msgs.get(row).format();
				if(msg.startsWith("&il")) msg = "    " + msg.substring(3);
				if(super.font.width(msg) >= width) {
					char[] chs = msg.toCharArray();
					StringBuilder s = new StringBuilder();
					int index = 0;
					for(int k = 0; k < chs.length; k++) {
						s.append(chs[k]);
						String formatStr = s.toString();
						if(super.font.width(formatStr) >= width) {
							this.showMessages.add(new Message(row, index++, formatStr));
							s.setLength(0);
						}
					}
					if(s.length() > 0) {
						this.showMessages.add(new Message(row, index++, s.toString()));
					}
				}else {
					this.showMessages.add(new Message(row, 0, msg));
				}
			}
		}
	}

	private static int refreshCoolingTicks = 0;
	private static boolean hasRefreshThread;
	private void refresh() {
		if(refreshCoolingTicks<=0) {
			this.refreshBtn.visible = false;
			this.container.setRefresh(true);
			this.currentMsg = null;
			this.currentEmail = -1;
			this.currentSound = null;
			this.emailIDs = null;
			this.emailPage = -1;
			this.showEmails = null;
			this.popupMenuCurrentEmail = -1;
			refreshCoolingTicks = 5 * 20;
			EmailMain.net.sendMessageToServer(new MsgRefreshInbox());
			if(!hasRefreshThread){
				new Thread(()->{
					while(refreshCoolingTicks > 0) {
						try {
							Thread.sleep(50);
							refreshCoolingTicks--;
						}catch(InterruptedException ignored) {}
					}
					this.refreshBtn.visible = true;
					hasRefreshThread = false;
				}).start();
			}
		}
	}

	protected Email getCurrentEmail() {
		return this.container.getInbox().getEmail(this.currentEmail);
	}

	public void stopSound() {
		if(this.currentSound != null) {
			this.getMinecraft().getSoundManager().stop(this.currentSound);
			this.currentSound = null;
			MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Stop(this.currentEmail));
		}
	}

	public Font getFont() {
		return this.font;
	}
	public boolean isClose() {
		return isClose;
	}

	public void drawGradientRect(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
		graphics.fill(left, top, right, bottom, color);
	}
	public void drawHorizontalLine(GuiGraphics graphics, int startX, int endX, int y, int color) {
		graphics.hLine(startX, endX, y, color);
	}
	public void drawVerticalLine(GuiGraphics graphics, int x, int startY, int endY, int color) {
		graphics.vLine(x, startY, endY, color);
	}
	public void drawAlignRightString(GuiGraphics graphics, String text, int x, int y, int color, boolean drawShadow) {
		EmailUtils.drawAlignRightString(graphics, this.getFont(), text, x, y, color, drawShadow);
	}

	public void drawCenteredStringNoShadow(GuiGraphics graphics, String text, int x, int y, int color) {
		this.drawCenteredStringNoShadow(graphics, super.font, text, x, y, color);
	}
	public void drawCenteredStringWithShadow(GuiGraphics graphics, String text, int x, int y, int color) {
		this.drawCenteredStringWithShadow(graphics, super.font, text, x, y, color);
	}

	public void drawCenteredStringNoShadow(GuiGraphics graphics, Font fr, String text, int x, int y, int color) {
		graphics.drawString(fr, text, (float)(x - fr.width(text) / 2), (float)y, color, false);
	}
	public void drawCenteredStringWithShadow(GuiGraphics graphics, Font fr, String text, int x, int y, int color) {
		graphics.drawString(fr, text, (float)(x - fr.width(text) / 2), (float)y, color, true);
	}

	public void drawStringNoShadow(GuiGraphics graphics, String text, int x, int y, int color) {
		this.drawStringNoShadow(graphics, super.font, text, x, y, color);
	}
	public void drawStringWithShadow(GuiGraphics graphics, String text, int x, int y, int color) {
		this.drawStringWithShadow(graphics, super.font, text, x, y, color);
	}
	public void drawStringNoShadow(GuiGraphics graphics, Font fr, String text, int x, int y, int color) {
		graphics.drawString(fr, text, x, y, color, false);
	}
	public void drawStringWithShadow(GuiGraphics graphics, Font fr, String text, int x, int y, int color) {
		graphics.drawString(fr, text, x, y, color, true);
	}

	private static class Message {
		public final int row;
		public final int index;
		public final String msg;
		public Message(int row, int index, String msg) {
			this.row = row;
			this.index = index;
			this.msg = msg;
		}
		@Override
		public String toString() {
			return "row:"+row+",index:"+index+", msg:"+msg;
		}
	}
}
