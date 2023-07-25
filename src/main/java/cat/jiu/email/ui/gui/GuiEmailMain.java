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
import cat.jiu.email.ui.gui.component.GuiImageButton;
import cat.jiu.email.ui.gui.component.GuiPopupMenu;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.client.EmailSenderSndSound;
import cat.jiu.email.util.client.GuiDynamicImage;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import morph.avaritia.util.TextUtils;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings({"deprecation", "unused"})
@OnlyIn(Dist.CLIENT)
public class GuiEmailMain extends ContainerScreen<ContainerEmailMain> {
	public static ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_main.png");
	public static ResourceLocation load = new ResourceLocation(EmailMain.MODID, "textures/gui/load.png");

	private final ContainerEmailMain container = super.getContainer();
	private boolean isClose = false;
	private GuiPopupMenu popupMenu = new GuiPopupMenu();
	private GuiPopupMenu filterMenu = new GuiPopupMenu();
	private GuiImageButton refreshBtn;
	private final GuiDynamicImage loadImage = new GuiDynamicImage(load, 18, false, 32, 32, 0, 0, 16, 16, 32, 576);
	
	public GuiEmailMain(ContainerEmailMain container, PlayerInventory inventory, ITextComponent t) {
		super(container, inventory, ITextComponent.getTextComponentOrEmpty(null));
		this.xSize = EmailConfigs.Main.Size.Width.get();
		this.ySize = EmailConfigs.Main.Size.Height.get();
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@Override
	public void init() {
		super.init();
		this.initTurnPageBtn(this.guiLeft, this.guiTop);
		this.initFunctionBtn(this.guiLeft, this.guiTop);
		this.initPopupMenu();
		this.initFilterMenu();
		
		String text = I18n.format("info.email.black");
		int weight = super.font.getStringWidth(text);
		this.addButton(new Button(this.guiLeft+190 - (weight + 4), this.guiTop+145, weight + 4, super.font.FONT_HEIGHT+3, new StringTextComponent(text), btn->
				GuiHandler.openGui(GuiHandler.EMAIL_BLACKLIST)
		));
		text = I18n.format("info.email.filter");
		weight = super.font.getStringWidth(text);
		this.addButton(this.filterButton = new Button(this.guiLeft+140 - (weight + 4), this.guiTop+145, weight + 4, super.font.FONT_HEIGHT+3, new StringTextComponent(text), btn->{
			this.filterMenu.setCreatePoint(this.filterButton.x, this.filterButton.y - this.filterMenu.height - 2);
			this.filterMenu.setVisible(!this.filterMenu.isVisible());
		}));

		this.addButton(this.refreshBtn = new GuiImageButton(this, this.guiLeft + 6, this.guiTop + 6, 7, 7, ()->
			new StringTextComponent(I18n.format(refreshCoolingTicks <= 0 ? "info.email.refresh" : "info.email.refresh.cooling"))
		, BackGround, 256, 256, 111, 169, 55, 55, btn->{
			if(refreshCoolingTicks <= 0) {
				this.refresh();
				this.getMinecraft().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				this.refreshBtn.visible = false;
			}
		}));
		if(refreshCoolingTicks>0){
			this.refreshBtn.visible = false;
		}
	}

	private void initTurnPageBtn(int x, int y) {
		this.addButton(new Button(x+82, y+19, 9, 20, new StringTextComponent("\u21e7"), btn-> goEmail(-1)));
		this.addButton(new Button(x+82, y+83, 9, 20, new StringTextComponent("\u21e9"), btn->goEmail(1)));
		this.addButton(new Button(x+222, y+31, 9, 20, new StringTextComponent("\u21e7"),btn->goMessage(-1)));
		this.addButton(new Button(x+222, y+83, 9, 20, new StringTextComponent("\u21e9"), btn->goMessage(1)));
	}
	
	private void initFunctionBtn(int x, int y) {
		this.addButton(new Button(x+4, y+106, 43, 12, new StringTextComponent(I18n.format("info.email.delete_accept")), btn -> {
			if(this.container.isRefresh()) return;
			EmailMain.net.sendMessageToServer(new MsgDeleteEmail.AllReceive());
		}));
		this.addButton(new Button(x+4, y+119, 43, 12, new StringTextComponent(I18n.format("info.email.delete_read")), btn-> {
			if(this.container.isRefresh()) return;
			EmailMain.net.sendMessageToServer(new MsgDeleteEmail.AllRead());
		}));
		this.addButton(new Button(x+4, y+132, 43, 12, new StringTextComponent(I18n.format("info.email.dispatch")), btn-> GuiHandler.openGui(GuiHandler.EMAIL_SEND)));
		this.addButton(new Button(x+192, y+106, 41, 12, new StringTextComponent(I18n.format("info.email.delete")), btn->{
			if(currentEmail != -1) {
//				Email email = getCurrentEmail();
//				if(email.hasItems() && !email.isReceived()) {
//					mc.setScreen(new GuiDeleteEmailConfirm(GuiEmailMain.this, new EmailType(currentEmail, email)));
//				}else {
					EmailMain.net.sendMessageToServer(new MsgDeleteEmail.Delete(currentEmail));
//				}
			}
		}));
		this.addButton(new Button(x+192, y+119, 41, 12, new StringTextComponent(I18n.format("info.email.accept")), btn->{
			if(currentEmail != -1) {
				if(container.getInboxSize()+55 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
					emailIsOutStorageSize();
					return;
				}
				EmailMain.net.sendMessageToServer(new MsgReceiveEmail.Receive(currentEmail));
			}
		}));
		this.addButton(new Button(x+192, y+145, 41, 12, new StringTextComponent(I18n.format("info.email.accept_all")),btn->{
			if(this.container.isRefresh()) return;
			if(!EmailConfigs.isInfiniteSize()) {
				if(container.getInboxSize()+(container.getInbox().getUnReceived() * 55L) >= 2097152L) {
					emailIsOutStorageSize();
				}
			}
			EmailMain.net.sendMessageToServer(new MsgReceiveEmail.All());
		}));
		this.addButton(new Button(x+192, y+132, 41, 12, new StringTextComponent(I18n.format("info.email.read_all")),btn->{
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
//		this.filterMenu.setWorldAndResolution(this.getMinecraft(), this.width, this.height);
		MinecraftForge.EVENT_BUS.post(new InboxFilterEvent(this));
	}
	
	@SubscribeEvent
	public void addFilter(InboxFilterEvent event) {
		event.addFilter(I18n.format("info.email.filter.default"), email -> true);
		
		event.addFilter(I18n.format("info.email.filter.is_read"), Email::isRead);
		event.addFilter(I18n.format("info.email.filter.not_read"), email -> !email.isRead());
		
		event.addFilter(I18n.format("info.email.filter.has_sound"), Email::hasSound);
		
		event.addFilter(I18n.format("info.email.filter.has_item"), Email::hasItems);
		event.addFilter(I18n.format("info.email.filter.is_accept"), email -> email.hasItems() && email.isReceived());
		event.addFilter(I18n.format("info.email.filter.not_accept"), email -> email.hasItems() && !email.isReceived());
		
		event.addFilter(I18n.format("info.email.filter.has_expiration"), Email::hasExpirationTime);
		event.addFilter(I18n.format("info.email.filter.is_expiration"), email -> email.hasExpirationTime() && email.isExpiration());
		event.addFilter(I18n.format("info.email.filter.not_expiration"), email -> email.hasExpirationTime() && !email.isExpiration());
	}
	
	public void addFilter(String name, Predicate<Email> predicate) {
		final int filterID = this.filterMenu.getButtonSize();
		this.filterMenu.addPopupButton(new Button(0, 0, 45, 12, new StringTextComponent(name), btn->{
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
		
		this.popupMenu.addPopupButton(new Button(0, 0, 45, 12, new StringTextComponent(I18n.format("info.email.delete")),btn->{
			if(popupMenuCurrentEmail != -1) {
				Email email = container.getInbox().getEmail(popupMenuCurrentEmail);
//				if(email.hasItems() && !email.isReceived()) {
//					mc.setScreen(new GuiDeleteEmailConfirm(GuiEmailMain.this, new EmailType(popupMenuCurrentEmail, email)));
//				}else {
					EmailMain.net.sendMessageToServer(new MsgDeleteEmail.Delete(popupMenuCurrentEmail));
					popupMenu.setVisible(false);
//				}
			}
		}));
		this.popupMenu.addPopupButton(new Button(0, 0, 45, 12, new StringTextComponent(I18n.format("info.email.accept")), btn->{
			if(popupMenuCurrentEmail != -1) {
				if(container.getInboxSize()+55 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
					emailIsOutStorageSize();
					return;
				}
				EmailMain.net.sendMessageToServer(new MsgReceiveEmail.Receive(popupMenuCurrentEmail));
			}
		}));
		this.popupMenu.addPopupButton(new Button(0, 0, 45, 12, new StringTextComponent(I18n.format("info.email.read")),btn->{
			if(popupMenuCurrentEmail != -1) {
				if(container.getInboxSize()+51 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
					emailIsOutStorageSize();
					return;
				}
				EmailMain.net.sendMessageToServer(new MsgReadEmail(popupMenuCurrentEmail));
			}
		}));
		this.popupMenu.addPopupButton(new Button(0, 0, 50, 12, new StringTextComponent(I18n.format("info.email.read_accept")), btn-> {
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

	@SubscribeEvent
	public void onMouseScroll(GuiScreenEvent.MouseScrollEvent.Pre event) {
		if(event.getGui().getClass() != this.getClass() || this.container.isRefresh()) return;
		MainWindow window = this.getMinecraft().getMainWindow();

		double key = event.getScrollDelta();
        double x = event.getMouseX() - this.guiLeft;
        double y = event.getMouseY() - this.guiTop;

		if(isInRange(x, y, 82, 41, 8, 40) || isInRange(x, y, 17, 10, 63, 90)) {
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
			event.setCanceled(true);
		}else if(isInRange(x, y, 221, 53, 8, 28) || isInRange(x, y, 92, 30, 128, 74)) {
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
			event.setCanceled(true);
		}
	}
	
	double lastClickX = 0;
	double lastClickY = 0;

	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		if(this.container.isRefresh()) return false;
		
		if(isInRange(this.lastClickX, this.lastClickY, this.guiLeft + 76, this.guiTop + 41, 8, 40)
		|| isInRange(this.lastClickX, this.lastClickY, this.guiLeft + 18, this.guiTop + 19, 57, 86)) {
			if(keyCode == GLFW.GLFW_KEY_UP) {
				this.goEmail(-1);
			}else if(keyCode == GLFW.GLFW_KEY_DOWN) {
				this.goEmail(1);
			}
		}else if(isInRange(this.lastClickX, this.lastClickY, this.guiLeft + 216, this.guiTop + 53, 8, 28)
			|| isInRange(this.lastClickX, this.lastClickY, this.guiLeft + 87, this.guiTop + 30, 128, 74)) {
			if(keyCode == GLFW.GLFW_KEY_UP) {
				this.goMessage(-1);
			}else if(keyCode == GLFW.GLFW_KEY_DOWN) {
				this.goMessage(1);
			}
		}
		return super.charTyped(typedChar, keyCode);
	}
	
	private final int Candidate_Email_X = EmailConfigs.Main.Position.Candidate_Email.X.get();
	private final int Candidate_Email_Y = EmailConfigs.Main.Position.Candidate_Email.Y.get();

	@Override
	public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
		super.render(matrix, mouseX, mouseY, partialTicks);
		this.popupMenu.drawPopupMenu(matrix, this.popupMenuCurrentEmail, this.getMinecraft(), mouseX, mouseY, partialTicks);
		this.filterMenu.drawPopupMenu(matrix, -1, this.getMinecraft(), mouseX, mouseY, partialTicks);
		super.renderHoveredTooltip(matrix, mouseX, mouseY);
	}

	int currentSoundCheck = 0;
	long currentSoundLastTime = 0;

	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack matrix, float partialTicks, int mouseX, int mouseY) {
		this.renderBackground(matrix);
//		GlStateManager.pushMatrix();
		this.getMinecraft().getTextureManager().bindTexture(BackGround);
		this.blit(matrix, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
//		GlStateManager.popMatrix();

		// TODO 检查播放的音效是否已停止
		if(this.currentSound!=null && this.currentSound.isDonePlaying()) {
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
		this.setRenderText(EmailUtils.parseTick(0,0,0,10, 0), true, I18n.format("info.email.out_size"), Color.RED);
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
	protected void drawGuiContainerForegroundLayer(MatrixStack matrix, int mouseX, int mouseY) {
		for(Widget btn : this.buttons) {
			if(!(btn instanceof GuiImageButton) && btn.visible){
				this.drawHorizontalLine(matrix, btn.x - this.guiLeft, btn.x + btn.getWidth() - 2 - this.guiLeft, btn.y + btn.getHeight()-1 - this.guiTop, (btn.isHovered() ? Color.WHITE : Color.BLACK).getRGB());
			}
		}
		if(this.container.isRefresh()) {
			this.loadImage.draw(matrix, 3, 146);
			this.drawStringWithShadow(matrix, I18n.format("info.email.refreshing"), 3 + this.loadImage.width, 146 + 5, Color.RED.getRGB());
		}else {
			MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.INBOX, TickEvent.Phase.START, this.container.getInbox(), -1, 0, 0));

			GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			this.drawInbox(matrix, mouseX, mouseY);
			GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

			MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.INBOX, TickEvent.Phase.END, this.container.getInbox(), -1, 0, 0));
		}
	}

	protected void drawInbox(MatrixStack matrix, int mouseX, int mouseY) {
		@Deprecated
		final int x=0, y=0;

		// TODO 绘制需要渲染的信息，目前Email中没有使用过此功能
		if(this.renderTimer != null && this.renderTimer.isStarted() && !this.renderTimer.isDone()) {
			if(super.font.getStringWidth(this.renderText)>this.xSize) {
				List<String> str = splitString(this.renderText, this.xSize);
				int y_t = y+this.ySize;
				for (String s : str) {
					this.drawStringWithShadow(matrix, s, x, y_t, this.renderColor.getRGB());
					y_t += super.font.FONT_HEIGHT;
				}
			}else {
				this.drawStringWithShadow(matrix, this.renderText, x, y+this.ySize, this.renderColor.getRGB());
			}
		}

		// TODO 绘制当前过滤器
		this.drawStringWithShadow(matrix, I18n.format("info.email.filter")+ ": " + this.filterMenu.getPopupButton(this.currentFilter).getMessage().getString(), x, y-super.font.FONT_HEIGHT, Color.WHITE.getRGB());

		// TODO 绘制邮箱所占网络包大小
//		super.font.drawString(matrix, I18n.format("info.email.storage"), x+5, y+148, Color.BLACK.getRGB());
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
		int bytesWidth = this.font.getStringWidth(bytes);
		super.font.drawString(matrix, bytes, x+6, y+156, Color.BLACK.getRGB());
		this.drawStringNoShadow(matrix, "Bytes", x+7+bytesWidth+1, y+156, Color.BLACK.getRGB());
		this.drawAlignRightString(matrix, String.valueOf(this.container.getInboxSize()), x+6+bytesWidth, y+145, sizeColor.getRGB(), false);
		this.drawHorizontalLine(matrix, x+5, x+5+bytesWidth, y+154, Color.BLACK.getRGB());

		// TODO 绘制刷新按钮
//		GlStateManager.pushMatrix();
//		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
//		this.getMinecraft().getTextureManager().bindTexture(BackGround);
//		blit(matrix, x + 6, y + 6, 6, 6, 111, 169, 55, 55, 256, 256);
//		GlStateManager.popMatrix();
//		if(isInRange(mouseX, mouseY, this.guiLeft + 5, this.guiTop + 5, 7, 7)) {
//			this.renderTooltip(matrix, new StringTextComponent(I18n.format(this.refreshCoolingTicks <= 0 ? "info.email.refresh" : "info.email.refresh.cooling")), mouseX - this.guiLeft, mouseY - this.guiTop);
//		}

		if(this.container.getInbox() == null
		|| this.container.getInbox().emailCount()<=0) return;
		this.initEmails();
		if(this.showEmails == null) return;

		// TODO 展示未读(*)与未领($)
		this.drawStringNoShadow(matrix, "$:"+ this.container.getInbox().getUnReceived(), x+25, y+3, Color.RED.getRGB());
		this.drawStringNoShadow(matrix, "*:" + this.container.getInbox().getUnRead(), x+50, y+3, Color.RED.getRGB());

		// TODO 绘制所选邮件
		if(this.currentEmail >= 0) {
			if(this.container.getInbox().hasEmail(this.currentEmail)) {
				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CURRENT, TickEvent.Phase.START, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
				Email email = this.getCurrentEmail();

				String sender = email.getSender().format();
				if(super.font.getStringWidth(sender) > EmailConfigs.Main.Number_Of_Words.Current_Email.Sender.get()) {
					sender = super.font.trimStringToWidth(sender, EmailConfigs.Main.Number_Of_Words.Current_Email.Sender.get()) + "...";
				}
				String title = email.getTitle().format();
				if(super.font.getStringWidth(title) > EmailConfigs.Main.Number_Of_Words.Current_Email.Title.get()-(email.hasSound()?13:0)) {
					title = super.font.trimStringToWidth(title, EmailConfigs.Main.Number_Of_Words.Current_Email.Title.get()-(email.hasSound()?13:0)) + "...";
				}

				this.drawStringNoShadow(matrix, title, x+7+EmailConfigs.Main.Position.Current_Email.Title.X.get(), y+EmailConfigs.Main.Position.Current_Email.Title.Y.get(), Color.WHITE.getRGB());
				this.drawStringNoShadow(matrix, sender, x+6+EmailConfigs.Main.Position.Current_Email.Sender.X.get(), y+EmailConfigs.Main.Position.Current_Email.Sender.Y.get(), Color.WHITE.getRGB());
				this.drawAlignRightString(matrix, email.getCreateTimeAsString().substring(2, email.getCreateTimeAsString().length()-3), x+5+ 64 +EmailConfigs.Main.Position.Current_Email.Time.X.get(), y+EmailConfigs.Main.Position.Current_Email.Time.Y.get(), Color.WHITE.getRGB(), false);

				this.drawCenteredStringWithShadow(matrix, String.valueOf(this.currentEmail), x+7+EmailConfigs.Main.Position.Current_Email.MsgID.X.get(), y+EmailConfigs.Main.Position.Current_Email.MsgID.Y.get(), Color.WHITE.getRGB());

				// TODO 绘制附带信息
				int msgIndex = -1;
				for(int i = 0; i < this.currentMsg.length; i++) {
					int index = this.currentMsg[i];
					Message msg = this.showMessages.get(index);

					if(msgIndex != msg.row) {
						this.drawStringNoShadow(matrix, Integer.toString(msg.row+1), x+7+(EmailConfigs.Main.Position.Current_Email.Row.X.get() - font.getStringWidth(Integer.toString(msg.row)) / 2),  y-1+EmailConfigs.Main.Position.Current_Email.Row.Y.get() + ((this.font.FONT_HEIGHT+EmailConfigs.Main.Selected_Text_Spacing.get()) * i), Color.BLACK.getRGB());
						msgIndex = msg.row;
					}else {
						boolean unicode = this.getMinecraft().getForceUnicodeFont();
						this.getMinecraft().gameSettings.forceUnicodeFont = false;
						if(index+1 >= this.showMessages.size()
								|| this.showMessages.get(index+1).row != msgIndex) {
							this.drawCenteredStringNoShadow(matrix, "\u255a", x+9+(EmailConfigs.Main.Position.Current_Email.Row.X.get() - font.getStringWidth(Integer.toString(msg.row)) / 2),  y-1+EmailConfigs.Main.Position.Current_Email.Row.Y.get() + ((this.font.FONT_HEIGHT+EmailConfigs.Main.Selected_Text_Spacing.get()) * i), Color.BLACK.getRGB());
						}else {
							this.drawCenteredStringNoShadow(matrix, "\u2560", x+9+(EmailConfigs.Main.Position.Current_Email.Row.X.get() - font.getStringWidth(Integer.toString(msg.row)) / 2),  y-1+EmailConfigs.Main.Position.Current_Email.Row.Y.get() + ((this.font.FONT_HEIGHT+EmailConfigs.Main.Selected_Text_Spacing.get()) * i), Color.BLACK.getRGB());
						}
						this.getMinecraft().gameSettings.forceUnicodeFont = unicode;
					}
					this.drawStringNoShadow(matrix, msg.msg, x+7+EmailConfigs.Main.Position.Current_Email.Msg.X.get(), y-1+EmailConfigs.Main.Position.Current_Email.Msg.Y.get() + ((this.font.FONT_HEIGHT+EmailConfigs.Main.Selected_Text_Spacing.get()) * i), Color.BLACK.getRGB());
				}

				// TODO 绘制附带音效
				if(email.hasSound()) {
					MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.SOUND, TickEvent.Phase.START, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
					if(!this.isPlayingSound() && this.currentSound != null) {
						this.stopSound();
					}
					GlStateManager.pushMatrix();
					GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
					this.getMinecraft().getTextureManager().bindTexture(BackGround);
					this.blit(matrix, x+218, y+4, 236, 0, 3, 12);
					Screen.blit(matrix, x+218+3, y+5, 10, 10,1 + (this.isPlayingSound() ? 55 : 0), 169,  55, 55, 256, 256);
					GlStateManager.popMatrix();

					if(isInRange(mouseX, mouseY, this.guiLeft + 221, this.guiTop + 5, 10, 10)) {
						List<ITextComponent> hover = Lists.newArrayList();
						hover.add(new StringTextComponent(I18n.format("info.email.play_sound" + (this.isPlayingSound() ? ".stop" : ""))));
						if(this.isPlayingSound()) {
							hover.add(new StringTextComponent(this.currentSound.time.toStringTime(false)));
						}
						this.renderWrappedToolTip(matrix, hover, mouseX - this.guiLeft, mouseY - this.guiTop, this.font);
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
			if(isInRange(mouseX, mouseY, this.guiLeft + Candidate_Email_X, this.guiTop + Candidate_Email_Y + ((19 * i)), 60, 17)) {
				if(this.container.getInbox().hasEmail(this.showEmails[i])) {
					Email email = this.container.getInbox().getEmail(this.showEmails[i]);
					List<String> tip = Lists.newArrayList();

					tip.add(email.getTitle().format());
					tip.add("");

					tip.add(TextFormatting.GRAY + email.getCreateTimeAsString());
					tip.add(I18n.format("info.email.main.from", email.getSender().format()));

					tip.add("");

					tip.add(I18n.format("info.email.email_size", email.getEmailNetworkSize()));
					if(email.getExpirationTime()!=null) {
						tip.add("");
						long time = email.getExpirationTimeAsTimestamp();
						if(System.currentTimeMillis() >= time) {
							tip.add(String.format("%s: %s", I18n.format("email.config.expiration"), TextFormatting.RED + I18n.format("email.config.expiration.ed")));
						}else {
							tip.add(I18n.format("info.email.remain_expiration_time", EmailUtils.formatTimestamp(time - System.currentTimeMillis())));
						}
					}

					visibleEmail = this.showEmails[i];
					this.renderWrappedToolTip(matrix, tip.stream().map(StringTextComponent::new).collect(Collectors.toList()), mouseX - this.guiLeft, mouseY - this.guiTop, this.font);
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
					identifier.append(email.isExpiration() ? TextFormatting.RED : TextFormatting.GREEN);
					identifier.append('#');
				}
				if(email.hasItems()) {
					identifier.append(email.isReceived() ? TextFormatting.GREEN : TextFormatting.RED);
					identifier.append('$');
				}
				identifier.append(email.isRead() ? TextFormatting.GREEN : TextFormatting.RED);
				identifier.append('*');

				this.drawAlignRightString(matrix, identifier.toString(), x+Candidate_Email_X+61, y+Candidate_Email_Y + (19 * i) + 1, Color.BLACK.getRGB(), false);

				this.drawCenteredStringWithShadow(matrix, String.valueOf(this.showEmails[i]), x+Candidate_Email_X-8, y+Candidate_Email_Y + 5 + (19 * i), (this.showEmails[i] == visibleEmail ? Color.CYAN : Color.WHITE).getRGB());

				String sender = email.getSender().format();
				if(super.font.getStringWidth(sender) > EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender.get()) {
					sender = super.font.trimStringToWidth(sender, EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender.get()) + "...";
				}
				this.drawStringNoShadow(matrix, sender, x+Candidate_Email_X+1, y+Candidate_Email_Y + (19 * i)+1, Color.WHITE.getRGB());
				this.drawStringNoShadow(matrix, email.getCreateTimeAsString().substring(5, email.getCreateTimeAsString().length()-3), x+Candidate_Email_X+1, y+Candidate_Email_Y + 10 + (19 * i), Color.BLACK.getRGB());

				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CANDIDATE, TickEvent.Phase.END, this.container.getInbox(), this.showEmails[i], mouseX, mouseY));
			}
		}
	}

	private EmailSenderSndSound currentSound;

	public boolean isPlayingSound() {
		if(this.currentSound != null) {
			return this.getMinecraft().getSoundHandler().isPlaying(this.currentSound) || !this.currentSound.isDonePlaying();
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
		if(this.getSlotUnderMouse() != null && this.getSlotUnderMouse().getHasStack()) {
			return false;
		}
		if(this.popupMenu.mouseClicked(x, y, btn)){
			return true;
		}
		if(this.filterMenu.mouseClicked(x, y, btn)){
			return true;
		}

		// TODO 刷新邮箱
//		if(isInRange(x, y, this.guiLeft + 5, this.guiTop + 5, 7, 7)
//		&& this.refreshCoolingTicks <= 0) {
//			this.refresh();
//			this.getMinecraft().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//			return true;
//		}
		if(this.container.getInbox()==null || this.container.getInbox().emailCount()<=0 || this.showEmails == null) return false;

		boolean showPopupMenu = false;
		// TODO 展示邮件或者子菜单
		for(int index = 0; index < 5; index++) {
			if(isInRange(x, y, this.guiLeft + Candidate_Email_X, this.guiTop + Candidate_Email_Y + (19 * index), 60, 17)) {
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
					this.getMinecraft().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				}
				break;
			}
		}

		// TODO 点击音效
		if(isInRange(x, y, this.guiLeft + 218, this.guiTop + 4, 13, 12)
		&&	this.currentEmail >=0 && this.getCurrentEmail()!=null && this.getCurrentEmail().hasSound()) {
			if(this.currentSound==null) {
				this.currentSound = new EmailSenderSndSound(this.getCurrentEmail().getSound(), this.currentEmail);
				MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Start(this.getCurrentEmail().getSound(), this.currentEmail));
				this.getMinecraft().getSoundHandler().play(this.currentSound);
				this.getMinecraft().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				this.currentSoundCheck = 0;
				this.currentSoundLastTime = 0;
			}else {
				this.stopSound();
				this.getMinecraft().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
		}

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
			this.getMinecraft().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));

			if(this.showEmails[index] != this.currentEmail) this.stopSound();
			this.currentEmail = this.showEmails[index];
			this.container.setCurrenEmail(this.currentEmail);

			// set current email items
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

				List<String> formatMsg = splitString(msg, width);
				for(int index = 0; index < formatMsg.size(); index++) {
					m.add(new Message(row, index, formatMsg.get(index)));
				}
				this.showMessages.addAll(m);
			}
		}else {
			for(int row = 0; row < msgs.size(); row++) {
				String msg = msgs.get(row).format();
				if(msg.startsWith("&il")) msg = "    " + msg.substring(3);
				if(super.font.getStringWidth(msg) >= width) {
					char[] chs = msg.toCharArray();
					StringBuilder s = new StringBuilder();
					int index = 0;
					for(int k = 0; k < chs.length; k++) {
						s.append(chs[k]);
						String formatStr = s.toString();
						if(super.font.getStringWidth(formatStr) >= width) {
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
	private void refresh() {
		if(refreshCoolingTicks<=0) {
			EmailMain.net.sendMessageToServer(new MsgRefreshInbox());
			this.container.setRefresh(true);
			this.currentMsg = null;
			this.currentEmail = -1;
			this.currentSound = null;
			this.emailIDs = null;
			this.emailPage = -1;
			this.showEmails = null;
			this.popupMenuCurrentEmail = -1;
			refreshCoolingTicks = 5 * 20;
			new Thread(()->{
				while(!this.isClose && refreshCoolingTicks > 0) {
					try {
						Thread.sleep(50);
						refreshCoolingTicks--;
					}catch(InterruptedException ignored) {}
				}
				this.refreshBtn.visible = true;
			}).start();
		}
	}

	protected Email getCurrentEmail() {
		return this.container.getInbox().getEmail(this.currentEmail);
	}

	public void stopSound() {
		if(this.currentSound != null) {
			this.getMinecraft().getSoundHandler().stop(this.currentSound);
			this.currentSound = null;
			MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Stop(this.currentEmail));
		}
	}

	public FontRenderer getFontRenderer() {
		return super.font;
	}
	public boolean isClose() {
		return isClose;
	}

	public void drawGradientRect(MatrixStack matrix, int left, int top, int right, int bottom, int color) {
		fill(matrix, left, top, right, bottom, color);
	}
	public void drawHorizontalLine(MatrixStack matrix, int startX, int endX, int y, int color) {
		super.hLine(matrix, startX, endX, y, color);
	}
	public void drawVerticalLine(MatrixStack matrix, int x, int startY, int endY, int color) {
		super.vLine(matrix, x, startY, endY, color);
	}
	public void drawAlignRightString(MatrixStack matrix, String text, int x, int y, int color, boolean drawShadow) {
		for(int i = text.length(); i > 0; i--) {
			if('§' == text.charAt(i-1)) {
				continue;
			}
			if(i-2>=0 && '§' == text.charAt(i-2)) {
				continue;
			}

			String c = String.valueOf(text.charAt(i-1));

			float width = super.font.getStringWidth(c);

			if(i-2 > 0) {
				boolean isColor;
				String s = text.charAt(i-3)+""+text.charAt(i-2);
				for(TextFormatting format : TextFormatting.values()) {
					isColor = format.toString().equals(s);
					if(isColor) {
						c = s + c;
						width = super.font.getStringWidth(c);
						break;
					}
				}
			}

			x -= width;
			if(drawShadow){
				this.drawStringWithShadow(matrix, c, x, y, color);
			}else {
				this.drawStringNoShadow(matrix, c, x, y, color);
			}
		}
	}

	public void drawCenteredStringNoShadow(MatrixStack matrix, String text, int x, int y, int color) {
		this.drawCenteredStringNoShadow(matrix, super.font, text, x, y, color);
	}
	public void drawCenteredStringWithShadow(MatrixStack matrix, String text, int x, int y, int color) {
		this.drawCenteredStringWithShadow(matrix, super.font, text, x, y, color);
	}

	public void drawCenteredStringNoShadow(MatrixStack matrix, FontRenderer fr, String text, int x, int y, int color) {
		fr.drawString(matrix, text, (float)(x - fr.getStringWidth(text) / 2), (float)y, color);
	}
	public void drawCenteredStringWithShadow(MatrixStack matrix, FontRenderer fr, String text, int x, int y, int color) {
		fr.drawStringWithShadow(matrix, text, (float)(x - fr.getStringWidth(text) / 2), (float)y, color);
	}

	public void drawStringNoShadow(MatrixStack matrix, String text, int x, int y, int color) {
		this.drawStringNoShadow(matrix, super.font, text, x, y, color);
	}
	public void drawStringWithShadow(MatrixStack matrix, String text, int x, int y, int color) {
		this.drawStringWithShadow(matrix, super.font, text, x, y, color);
	}
	public void drawStringNoShadow(MatrixStack matrix, FontRenderer fr, String text, int x, int y, int color) {
		fr.drawString(matrix, text, x, y, color);
	}
	public void drawStringWithShadow(MatrixStack matrix, FontRenderer fr, String text, int x, int y, int color) {
		fr.drawStringWithShadow(matrix, text, x, y, color);
	}

	// static
	
	public static boolean isInRange(double mouseX, double mouseY, int x, int y, int width, int height) {
		int maxX = x + width;
		int maxY = y + height;
		return (mouseX >= x && mouseY >= y) && (mouseX <= maxX && mouseY <= maxY);
	}
	
	public static List<String> splitString(String text, int textMaxLength) {
		FontRenderer fr = Minecraft.getInstance().fontRenderer;
		List<String> texts = Lists.newArrayList();
		if(fr.getStringWidth(text) >= textMaxLength) {
			StringBuilder s = new StringBuilder();
			for(int i = 0; i < text.length(); i++) {
				String str = s.toString();
				if(fr.getStringWidth(str) >= textMaxLength) {
					texts.add(str);
					s.setLength(0);
				}
				s.append(text.charAt(i));
			}
			if(s.length() > 0) {
				texts.add(s.toString());
			}
		}else {
			texts.add(text);
		}
		return texts;
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
