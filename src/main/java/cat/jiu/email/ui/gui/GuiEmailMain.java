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
import cat.jiu.email.net.msg.MsgOpenGui;
import cat.jiu.email.net.msg.MsgReadEmail;
import cat.jiu.email.net.msg.MsgReceiveEmail;
import cat.jiu.email.net.msg.refresh.MsgRefreshInbox;
import cat.jiu.email.proxy.ClientProxy;
import cat.jiu.email.ui.EmailGuiHandler;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.ui.gui.component.EmailType;
import cat.jiu.email.ui.gui.component.GuiClickButton;
import cat.jiu.email.ui.gui.component.GuiPopupMenu;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.client.EmailSound;
import cat.jiu.email.util.client.GuiDynamicImage;
import com.google.common.collect.Lists;
import morph.avaritia.util.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

@EventBusSubscriber(Side.CLIENT)
@SideOnly(Side.CLIENT)
public class GuiEmailMain extends GuiContainer {
	public static ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_main.png");
	public static ResourceLocation load = new ResourceLocation(EmailMain.MODID, "textures/gui/load.png");
	
	private final ContainerEmailMain container;
	private boolean isClose = false;
	private GuiPopupMenu popupMenu = new GuiPopupMenu();
	private GuiPopupMenu filterMenu = new GuiPopupMenu();
	private final GuiDynamicImage loadImage = new GuiDynamicImage(load, 18, false, 16, 16, 0, 0, 32, 32, 32, 576);
	private int id = -1;
	private int nextID() {return id++;}
	
	public GuiEmailMain() {
		super(new ContainerEmailMain());
		this.container = (ContainerEmailMain) super.inventorySlots;
		this.xSize = EmailConfigs.Main.Size.Width;
		this.ySize = EmailConfigs.Main.Size.Height;
	}
	
	@Override
	public void initGui() {
		super.initGui();
		this.id = -1;
		this.buttonList.clear();
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		
		this.initTurnPageBtn(x, y);
		this.initFunctionBtn(x, y);
		this.initPopupMenu();
		this.initFilterMenu();
		
		String text = I18n.format("info.email.black");
		int weight = super.fontRenderer.getStringWidth(text);
		this.buttonList.add(new GuiButton(nextID(), x+190 - (weight + 4), y+145, weight + 4, super.fontRenderer.FONT_HEIGHT+3, text) {
			public void mouseReleased(int mouseX, int mouseY) {
				GuiEmailMain gui = GuiEmailMain.this;
				mc.displayGuiScreen(new GuiBlacklist(gui));
			}
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(this.visible) {
					this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
				}
			}
		});
		text = I18n.format("info.email.filter");
		weight = super.fontRenderer.getStringWidth(text);
		this.buttonList.add(this.filterButton = new GuiButton(nextID(), x+140 - (weight + 4), y+145, weight + 4, super.fontRenderer.FONT_HEIGHT+3, text) {
			public void mouseReleased(int mouseX, int mouseY) {
				filterMenu.setCreatePoint(this.x+this.height+this.width, this.y);
				filterMenu.setVisible(!filterMenu.isVisible());
			}
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(this.visible) {
					this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
				}
			}
		});
	}
	
	private void initTurnPageBtn(int x, int y) {
		this.buttonList.add(new GuiButton(nextID(), x+82, y+19, 9, 20, "\u21e7") {
			public void mouseReleased(int mouseX, int mouseY) {
				goEmail(-1);
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+82, y+83, 9, 20, "\u21e9") {
			public void mouseReleased(int mouseX, int mouseY) {
				goEmail(1);
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+222, y+31, 9, 20, "\u21e7") {
			public void mouseReleased(int mouseX, int mouseY) {
				goMessage(-1);
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+222, y+83, 9, 20, "\u21e9") {
			public void mouseReleased(int mouseX, int mouseY) {
				goMessage(1);
			}
		});
	}
	
	private void initFunctionBtn(int x, int y) {
		this.buttonList.add(new GuiButton(nextID(), x+4, y+106, 43, 12, I18n.format("info.email.delete_accept")) {
			public void mouseReleased(int mouseX, int mouseY) {
				EmailMain.net.sendMessageToServer(new MsgDeleteEmail.AllReceive());
			}
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(this.visible) {
					this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
				}
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+4, y+119, 43, 12, I18n.format("info.email.delete_read")) {
			public void mouseReleased(int mouseX, int mouseY) {
				EmailMain.net.sendMessageToServer(new MsgDeleteEmail.AllRead());
			}
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(this.visible) {
					this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
				}
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+4, y+132, 43, 12, I18n.format("info.email.dispatch")) {
			public void mouseReleased(int mouseX, int mouseY) {
				EmailMain.net.sendMessageToServer(new MsgOpenGui(EmailGuiHandler.EMAIL_SEND));
			}
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(this.visible) {
					this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
				}
			}
		});
		
		this.buttonList.add(new GuiButton(nextID(), x+192, y+106, 41, 12, I18n.format("info.email.delete")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(currentEmail != -1) {
					Email email = getCurrentEmail();
					if(email.hasItems() && !email.isReceived()) {
						mc.displayGuiScreen(new GuiDeleteEmailConfirm(GuiEmailMain.this, new EmailType(currentEmail, email)));
					}else {
						EmailMain.net.sendMessageToServer(new MsgDeleteEmail.Delete(currentEmail));						
					}
				}
			}
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(this.visible) {
					this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
				}
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+192, y+119, 41, 12, I18n.format("info.email.accept")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(currentEmail != -1) {
					if(container.getInboxSize()+55 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
						emailIsOutStorageSize();
						return;
					}
					EmailMain.net.sendMessageToServer(new MsgReceiveEmail.Receive(currentEmail));
				}
			}
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(this.visible) {
					this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
				}
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+192, y+145, 41, 12, I18n.format("info.email.accept_all")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(!EmailConfigs.isInfiniteSize()) {
					if(container.getInboxSize()+((long)container.getInbox().getUnReceived() * 55L) >= 2097152L) {
						emailIsOutStorageSize();
						return;
					}
				}
				EmailMain.net.sendMessageToServer(new MsgReceiveEmail.All());
			}
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(this.visible) {
					this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
				}
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+192, y+132, 41, 12, I18n.format("info.email.read_all")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(!EmailConfigs.isInfiniteSize()) {
					if(container.getInboxSize()+((long)container.getInbox().getUnRead() * 51L) > 2097152L) {
						emailIsOutStorageSize();
						return;
					}
				}
				
				EmailMain.net.sendMessageToServer(new MsgReadEmail.All());
			}
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(this.visible) {
					this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
				}
			}
		});
	}
	
	private GuiButton filterButton;
	private int currentFilter = 0;
	
	private void initFilterMenu() {
		this.filterMenu = new GuiPopupMenu();
		this.filterMenu.setWorldAndResolution(this.mc, this.width, this.height);
		MinecraftForge.EVENT_BUS.post(new InboxFilterEvent(this));
	}
	
	@SubscribeEvent
	public static void addFilter(InboxFilterEvent event) {
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
		this.filterMenu.addPopupButton(new GuiClickButton(nextID(), 0, 0, 45, 12, name, (mouseX, mouseY)->{
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
		this.popupMenu.setWorldAndResolution(this.mc, this.width, this.height);
		
		this.popupMenu.addPopupButton(new GuiButton(nextID(), 0, 0, 45, 12, I18n.format("info.email.delete")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(popupMenuCurrentEmail != -1) {
					Email email = container.getInbox().getEmail(popupMenuCurrentEmail);
					if(email.hasItems() && !email.isReceived()) {
						mc.displayGuiScreen(new GuiDeleteEmailConfirm(GuiEmailMain.this, new EmailType(popupMenuCurrentEmail, email)));
					}else {
						EmailMain.net.sendMessageToServer(new MsgDeleteEmail.Delete(popupMenuCurrentEmail));
						popupMenu.setVisible(false);
					}
				}
			}
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(this.visible) {
					this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
				}
			}
		});
		this.popupMenu.addPopupButton(new GuiButton(nextID(), 0, 0, 45, 12, I18n.format("info.email.accept")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(popupMenuCurrentEmail != -1) {
					if(container.getInboxSize()+55 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
						emailIsOutStorageSize();
						return;
					}
					EmailMain.net.sendMessageToServer(new MsgReceiveEmail.Receive(popupMenuCurrentEmail));
				}
			}
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(this.visible) {
					this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
				}
			}
		});
		this.popupMenu.addPopupButton(new GuiButton(nextID(), 0, 0, 45, 12, I18n.format("info.email.read")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(popupMenuCurrentEmail != -1) {
					if(container.getInboxSize()+51 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
						emailIsOutStorageSize();
						return;
					}
					EmailMain.net.sendMessageToServer(new MsgReadEmail(popupMenuCurrentEmail));
				}
			}
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(this.visible) {
					this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
				}
			}
		});
		this.popupMenu.addPopupButton(new GuiButton(nextID(), 0, 0, 50, 12, I18n.format("info.email.read_accept")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(popupMenuCurrentEmail != -1) {
					if(container.getInboxSize()+51+55 >= 2097152L && !EmailConfigs.isInfiniteSize()) {
						emailIsOutStorageSize();
						return;
					}
					EmailMain.net.sendMessageToServer(new MsgReadEmail(popupMenuCurrentEmail));
					EmailMain.net.sendMessageToServer(new MsgReceiveEmail.Receive(popupMenuCurrentEmail));
				}
			}
			public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
				super.drawButton(mc, mouseX, mouseY, partialTicks);
				if(this.visible) {
					this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
				}
			}
		});
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
	
	private int maxSelectedTextRows = EmailConfigs.Main.Selected_Text_Rows;
	public void setMaxSelectedTextRows(int maxrows) {this.maxSelectedTextRows = maxrows;}
	public int getMaxSelectedTextRows() {return maxSelectedTextRows;}
	
	private int showSelectedSpacing = EmailConfigs.Main.Selected_Text_Spacing;
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
	
	private void init() {
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
	public void handleMouseInput() throws IOException {
		if(this.container.isRefresh()) return;
		super.handleMouseInput();
		int key = Mouse.getEventDWheel();
        int x = (Mouse.getEventX() * this.width / this.mc.displayWidth) - this.guiLeft;
        int y = (this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1) - this.guiTop;
		
        if(isInRange(x, y, 82, 41, 8, 40) || isInRange(x, y, 17, 10, 63, 90)) {
        	int page = 0;
        	
        	if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        		page += 2;
        	}
        	if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
        		page += 1;
        	}
        	
			if(key == 120) {
				this.goEmail(-1 - page);
			}else if(key == -120) {
				this.goEmail(1 + page);
			}
		}else if(isInRange(x, y, 221, 53, 8, 28) || isInRange(x, y, 92, 30, 128, 74)) {
			int page = 0;
        	
        	if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
        		page += 2;
        	}
        	if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
        		page += 1;
        	}
			if(key == 120) {
				this.goMessage(-1 - page);
			}else if(key == -120) {
				this.goMessage(1 + page);
			}
		}
	}
	
	int lastClickX = 0;
	int lastClickY = 0;
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		super.keyTyped(typedChar, keyCode);
		
		if(this.container.isRefresh()) return;
		
		int left = (this.width - this.xSize) / 2;
		int top = (this.height - this.ySize) / 2;
		
		if(isInRange(this.lastClickX, this.lastClickY, left + 76, top + 41, 8, 40)
		|| isInRange(this.lastClickX, this.lastClickY, left + 18, top + 19, 57, 86)) {
			if(keyCode == Keyboard.KEY_UP) {
				this.goEmail(-1);
			}else if(keyCode == Keyboard.KEY_DOWN) {
				this.goEmail(1);
			}
		}else if(isInRange(this.lastClickX, this.lastClickY, left + 216, top + 53, 8, 28)
			|| isInRange(this.lastClickX, this.lastClickY, left + 87, top + 30, 128, 74)) {
			if(keyCode == Keyboard.KEY_UP) {
				this.goMessage(-1);
			}else if(keyCode == Keyboard.KEY_DOWN) {
				this.goMessage(1);
			}
		}else if(ClientProxy.REFESH_INBOX.getKeyCode() == keyCode) {
			this.refresh();
		}
	}
	
	private final int Candidate_Email_X = EmailConfigs.Main.Position.Candidate_Email.X;
	private final int Candidate_Email_Y = EmailConfigs.Main.Position.Candidate_Email.Y;
	
	int currentSoundCheck = 0;
	long currentSoundLastTime = 0;
	
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
	 * @param showTime the show time
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
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		super.drawDefaultBackground();
		
		GlStateManager.popMatrix();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(BackGround);
		this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize);
		GlStateManager.pushMatrix();
		
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
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		if(this.container.isRefresh()) {
			@Deprecated
			int x = 0, y = 0;
			this.loadImage.draw(x + 3, y + 146);
			this.drawCenteredStringWithShadow(I18n.format("info.email.refreshing"), x + 9 + 40, y + 150, Color.RED.getRGB());
		}else {
			MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.INBOX, Phase.START, this.container.getInbox(), -1, mouseX, mouseY));
			
			GlStateManager.popMatrix();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			this.drawInbox(mouseX, mouseY);
			GlStateManager.pushMatrix();
			
			MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.INBOX, Phase.END, this.container.getInbox(), -1, mouseX, mouseY));
		}

		super.renderHoveredToolTip(mouseX, mouseY);
		this.popupMenu.drawPopupMenu(this.popupMenuCurrentEmail, this.mc, mouseX, mouseY, 0);
		this.filterMenu.drawPopupMenu(-1, this.mc, mouseX, mouseY, 0);
		
		if(this.container.isRefresh()) return;
		if(isInRange(mouseX, mouseY, this.getGuiLeft() + 5, this.getGuiTop() + 5, 7, 7)) {
			this.drawHoveringText(I18n.format(GuiEmailMain.refreshCoolingTicks <= 0 ? "info.email.refresh" : "info.email.refresh.cooling"), mouseX, mouseY);
		}
		for(int i = 0; !this.popupMenu.isVisible() && i < 5; i++) {
			if(this.showEmails == null || i >= this.showEmails.length) break;
			if(isInRange(mouseX, mouseY, this.getGuiLeft() + Candidate_Email_X, this.getGuiTop() + Candidate_Email_Y + ((19 * i)), 60, 17)) {
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
					
					this.drawHoveringText(tip, mouseX, mouseY+10);
					break;
				}
			}
		}
	}
	
	protected void drawInbox(int mouseX, int mouseY) {
		int x = this.guiLeft, y = this.guiTop;
		
		if(this.renderTimer != null && this.renderTimer.isStarted() && !this.renderTimer.isDone()) {
			if(super.fontRenderer.getStringWidth(this.renderText)>this.xSize) {
				List<String> str = super.fontRenderer.listFormattedStringToWidth(this.renderText, this.xSize);
				int stry = y+this.ySize;
				for (String s : str) {
					this.drawStringWithShadow(s, x, stry, this.renderColor.getRGB());
					stry += super.fontRenderer.FONT_HEIGHT;
				}
			}else {
				this.drawStringWithShadow(this.renderText, x, y+this.ySize, this.renderColor.getRGB());
			}
		}
		
		this.drawStringWithShadow(I18n.format("info.email.filter")+ ": " + this.filterMenu.getPopupButton(this.currentFilter).displayString, x, y-super.fontRenderer.FONT_HEIGHT, Color.WHITE.getRGB());
		
		// TODO 展示邮箱所占大小
//		super.fontRenderer.drawString(I18n.format("info.email.storage"), x+5, y+148, Color.BLACK.getRGB());
		Color sizeColor = Color.GREEN;
		if(!EmailConfigs.isInfiniteSize()) {
			long size = this.container.getInboxSize();
			if(size >= 1398122) {
				sizeColor = Color.RED;
			}else if(size >= 699061) {
				sizeColor = Color.YELLOW;
			}
		}
		
		int storageWidth = 0;
		String bytes = EmailConfigs.isInfiniteSize() ? TextUtils.makeFabulous("Infinite") : "2097183";
		int bytesWidth = this.mc.fontRenderer.getStringWidth(bytes);
		super.fontRenderer.drawString(bytes, x+6 + storageWidth, y+154, Color.GRAY.getRGB(), true);
		this.drawString("Bytes", x+7+bytesWidth+1+storageWidth, y+154, Color.BLACK.getRGB());
		this.drawAlignRightString(String.valueOf(this.container.getInboxSize()), x+6+storageWidth+bytesWidth, y+145, sizeColor.getRGB(), true);
		this.drawHorizontalLine(x+5+storageWidth, x+5+bytesWidth+storageWidth, y+154, Color.BLACK.getRGB());
		
		this.mc.getTextureManager().bindTexture(BackGround);
		Gui.drawScaledCustomSizeModalRect(x + 6, y + 6, 111, 169, 55, 55, 6, 6, 256, 256);
		
		if(this.container.getInbox() == null
		|| this.container.getInbox().emailCount()<=0) return;
		this.init();
		if(this.showEmails == null) return;
		
		// TODO 展示未读(*)与未领($)
		this.drawString("$:"+ this.container.getInbox().getUnReceived(), x+25, y+3, Color.RED.getRGB());
		this.drawString("*:" + this.container.getInbox().getUnRead(), x+50, y+3, Color.RED.getRGB());
		
		// TODO 展示邮件列表
		for (int i = 0; i < this.showEmails.length; i++) {
			if(this.container.getInbox().hasEmail(this.showEmails[i])) {
				Email email = this.container.getInbox().getEmail(this.showEmails[i]);
				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CANDIDATE, Phase.START, this.container.getInbox(), this.showEmails[i], mouseX, mouseY));
				
				StringBuilder identifier = new StringBuilder();
				if(email.getExpirationTime()!=null && email.getExpirationTime().millis>1) {
					identifier.append(System.currentTimeMillis() >= email.getExpirationTimeAsTimestamp() ? TextFormatting.RED : TextFormatting.GREEN);
					identifier.append('#');
				}
				if(email.hasItems()) {
					identifier.append(email.isReceived() ? TextFormatting.GREEN : TextFormatting.RED);
					identifier.append('$');
				}
				identifier.append(email.isRead() ? TextFormatting.GREEN : TextFormatting.RED);
				identifier.append('*');
				
				this.drawAlignRightString(identifier.toString(), x+Candidate_Email_X+61, y+Candidate_Email_Y + (19 * i) + 1, Color.BLACK.getRGB(), false);
				
				this.drawCenteredStringWithShadow(String.valueOf(this.showEmails[i]), x+Candidate_Email_X-8, y+Candidate_Email_Y + 5 + (19 * i), Color.WHITE.getRGB());
				
				String sender = email.getSender().format();
				if(super.fontRenderer.getStringWidth(sender) > EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender) {
					sender = super.fontRenderer.trimStringToWidth(sender, EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender) + "...";
				}
				this.drawString(sender, x+Candidate_Email_X+1, y+Candidate_Email_Y + (19 * i)+1, Color.WHITE.getRGB());
				this.drawString(email.getCreateTimeAsString().substring(5, email.getCreateTimeAsString().length()-3), x+Candidate_Email_X+1, y+Candidate_Email_Y + 10 + (19 * i), Color.BLACK.getRGB());
				
				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CANDIDATE, Phase.END, this.container.getInbox(), this.showEmails[i], mouseX, mouseY));
			}
		}
		
		if(this.currentEmail >= 0) {
			if(this.container.getInbox().hasEmail(this.currentEmail)) {
				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CURRENT, Phase.START, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
				Email email = this.getCurrentEmail();
				
				String sender = email.getSender().format();
				if(super.fontRenderer.getStringWidth(sender) > EmailConfigs.Main.Number_Of_Words.Current_Email.Sender) {
					sender = super.fontRenderer.trimStringToWidth(sender, EmailConfigs.Main.Number_Of_Words.Current_Email.Sender) + "...";
				}
				String title = email.getTitle().format();
				if(super.fontRenderer.getStringWidth(title) > EmailConfigs.Main.Number_Of_Words.Current_Email.Title-(email.hasSound()?13:0)) {
					title = super.fontRenderer.trimStringToWidth(title, EmailConfigs.Main.Number_Of_Words.Current_Email.Title-(email.hasSound()?13:0)) + "...";
				}
				
				this.drawString(title, x+7+EmailConfigs.Main.Position.Current_Email.Title.X, y+EmailConfigs.Main.Position.Current_Email.Title.Y, Color.WHITE.getRGB());
				this.drawString(sender, x+6+EmailConfigs.Main.Position.Current_Email.Sender.X, y+EmailConfigs.Main.Position.Current_Email.Sender.Y, Color.WHITE.getRGB());
				this.drawAlignRightString(email.getCreateTimeAsString().substring(2, email.getCreateTimeAsString().length()-3), x+5+ 64 +EmailConfigs.Main.Position.Current_Email.Title.Time.X, y+EmailConfigs.Main.Position.Current_Email.Title.Time.Y, Color.WHITE.getRGB(), false);
				
				this.drawCenteredStringWithShadow(String.valueOf(this.currentEmail), x+7+EmailConfigs.Main.Position.Current_Email.MsgID.X, y+EmailConfigs.Main.Position.Current_Email.MsgID.Y, Color.WHITE.getRGB());
				
				// TODO 绘制信息
				int msgIndex = -1;
				for(int i = 0; i < this.currentMsg.length; i++) {
					int index = this.currentMsg[i];
					Message msg = this.showMessages.get(index);
					
					if(msgIndex != msg.row) {
						this.drawString(Integer.toString(msg.row+1), x+7+(EmailConfigs.Main.Position.Current_Email.Row.X - fontRenderer.getStringWidth(Integer.toString(msg.row)) / 2),  y-1+EmailConfigs.Main.Position.Current_Email.Row.Y + ((this.mc.fontRenderer.FONT_HEIGHT+EmailConfigs.Main.Selected_Text_Spacing) * i), Color.BLACK.getRGB());
						msgIndex = msg.row;
					}else {
						boolean unicodeFlag = super.fontRenderer.getUnicodeFlag();
						super.fontRenderer.setUnicodeFlag(false);
						
						if(index+1 >= this.showMessages.size()
						|| this.showMessages.get(index+1).row != msgIndex) {
							this.drawString("\u255a", x+7+(EmailConfigs.Main.Position.Current_Email.Row.X - fontRenderer.getStringWidth("\u255a") / 2),  y+EmailConfigs.Main.Position.Current_Email.Row.Y + ((this.mc.fontRenderer.FONT_HEIGHT+EmailConfigs.Main.Selected_Text_Spacing) * i), Color.BLACK.getRGB());
						}else {
							this.drawString("\u2560", x+7+(EmailConfigs.Main.Position.Current_Email.Row.X - fontRenderer.getStringWidth("\u2560") / 2),  y-1+EmailConfigs.Main.Position.Current_Email.Row.Y + ((this.mc.fontRenderer.FONT_HEIGHT+EmailConfigs.Main.Selected_Text_Spacing) * i), Color.BLACK.getRGB());
						}
						
						super.fontRenderer.setUnicodeFlag(unicodeFlag);
					}
					this.drawString(msg.msg, x+7+EmailConfigs.Main.Position.Current_Email.Msg.X, y-1+EmailConfigs.Main.Position.Current_Email.Msg.Y + ((this.mc.fontRenderer.FONT_HEIGHT+EmailConfigs.Main.Selected_Text_Spacing) * i), Color.BLACK.getRGB());
				}
				
				if(email.hasSound()) {
					MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.SOUND, Phase.START, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
					if(!this.isPlayingSound() && this.currentSound != null) {
						this.stopSound();
					}
					this.mc.getTextureManager().bindTexture(BackGround);
					
					// old
//					this.drawTexturedModalRect(x+218, y+4, 236, (this.currentSound != null ? 12 : 0), 13, 12);
					GlStateManager.color(1.0F,  1.0F,  1.0F);
					this.drawTexturedModalRect(x+218, y+4, 236, 0, 3, 12);
					
					Gui.drawScaledCustomSizeModalRect(x+218 + 3, y+5, 1 + (this.isPlayingSound() ? 55 : 0), 169, 55, 55, 10, 10, 256, 256);
					
					if(isInRange(mouseX, mouseY, x+218, y+4, 13, 12)) {
						List<String> hover = Lists.newArrayList();
						hover.add(I18n.format("info.email.play_sound" + (this.isPlayingSound() ? ".stop" : "")));
						if(this.currentSound!=null) {
							hover.add(this.currentSound.time.toStringTime(false));
						}
						this.drawHoveringText(hover, mouseX, mouseY);
					}
					MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.SOUND, Phase.END, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
				}
				
				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CURRENT, Phase.END, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
			}
		}
	}
	
	private EmailSound currentSound;
	
	public boolean isPlayingSound() {
		if(this.currentSound != null) {
			return this.mc.getSoundHandler().isSoundPlaying(this.currentSound) || !this.currentSound.isDonePlaying();
		}
		return false;
	}
	
	public ISound getPlayingSound() {
		return this.getCurrentEmail().getSound().copy();
	}
	
	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		this.stopSound();
		this.isClose = true;
	}
	
	private long popupMenuCurrentEmail = -1;
	
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int btn) throws IOException {
		if(this.container.isRefresh()) return;
		this.lastClickX = mouseX;
		this.lastClickY = mouseY;

		if(this.popupMenu.mouseClicked(this.mc, mouseX, mouseY, btn)) return;
		
		if(this.filterMenu.mouseClicked(this.mc, mouseX, mouseY, btn)) return;
		if(!this.filterButton.mousePressed(this.mc, mouseX, mouseY)) {
			this.filterMenu.setVisible(false);
		}
		
		if(this.getSlotUnderMouse() != null && this.getSlotUnderMouse().getHasStack()) {
			return;
		}
		
		if(isInRange(mouseX, mouseY, this.getGuiLeft() + 5, this.getGuiTop() + 5, 7, 7)
		&& GuiEmailMain.refreshCoolingTicks <= 0) {
			this.refresh();
			this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return;
		}
		
		super.mouseClicked(mouseX, mouseY, btn);
		if(this.container.getInbox()==null || this.container.getInbox().emailCount()<=0 || this.showEmails == null) return;
		
		for(int index = 0; index < 5; index++) {
			if(isInRange(mouseX, mouseY, this.getGuiLeft() + Candidate_Email_X, this.getGuiTop() + Candidate_Email_Y + (19 * index), 60, 17)) {
				if(index >= this.showEmails.length) break;
				if(!this.container.getInbox().hasEmail(this.showEmails[index])) continue;
				if(btn == 0) {
					// TODO 展示邮件
					this.showEmail(index);
				}else if(btn == 1) {
					// TODO 展示子菜单
					this.popupMenuCurrentEmail = this.showEmails[index];
					this.popupMenu.setCreatePoint(mouseX, mouseY);
					this.popupMenu.setVisible(true);
					this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				}
				break;
			}
		}
		
		// TODO 点击音效
		if(isInRange(mouseX, mouseY, this.getGuiLeft() + 218, this.getGuiTop() + 4, 13, 12) 
		&&	this.currentEmail >=0 && this.getCurrentEmail()!=null && this.getCurrentEmail().hasSound()) {
			if(this.currentSound==null) {
				this.currentSound = new EmailSound(this.getCurrentEmail().getSound(), this.currentEmail);
				MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Start(this.getCurrentEmail().getSound(), this.currentEmail));
				this.mc.getSoundHandler().playSound(this.currentSound);
				this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				this.currentSoundCheck = 0;
				this.currentSoundLastTime = 0;
			}else {
				this.stopSound();
				this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
		}
		
		// TODO 展示子菜单
		boolean showPopupMenu = true;
		for(int index = 0; btn == 1 && index < 5; index++) {
			if((isInRange(this.lastClickX, this.lastClickY, this.getGuiLeft() + Candidate_Email_X, this.getGuiTop() + Candidate_Email_Y + (17 * index) - 1, 54, 15))) {
				if(index >= this.showEmails.length) break;
				showPopupMenu = false;
			}
		}
		if(showPopupMenu) {
			this.popupMenu.setVisible(false);
		}
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
			this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			
			if(this.showEmails[index] != this.currentEmail) this.stopSound();
			this.currentEmail = this.showEmails[index];
			this.container.setCurrenEmail(this.currentEmail);
			
			// TODO set current email items
			this.container.clearStacks();
			if(email.hasItems()) {
				this.container.putStack(email.getItems());
			}
			
			if(this.container.getInbox().hasEmail(this.currentEmail)) {
				if(email.hasMessages()) {
					this.formatMessage(email.getMsgs());
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
		int width = EmailConfigs.Main.Number_Of_Words.Current_Email.Message;
		
		if(EmailConfigs.Main.Enable_Vanilla_Wrap_Text) {
			for(int row = 0; row < msgs.size(); row++) {
				String msg = msgs.get(row).format();
				if(msg.startsWith("&il")) msg = "    " + msg.substring(3);
				List<Message> m = Lists.newArrayList();
				
				List<String> formatMsg = super.fontRenderer.listFormattedStringToWidth(msg, width);
				for(int index = 0; index < formatMsg.size(); index++) {
					m.add(new Message(row, index, formatMsg.get(index)));
				}
				this.showMessages.addAll(m);
			}
		}else {
			for(int row = 0; row < msgs.size(); row++) {
				String msg = msgs.get(row).format();
				if(msg.startsWith("&il")) msg = "    " + msg.substring(3);
				if(super.fontRenderer.getStringWidth(msg) >= width) {
					StringBuilder s = new StringBuilder();
					int index = 0;
					for (char ch : msg.toCharArray()) {
						s.append(ch);
						String formatStr = s.toString();
						if (super.fontRenderer.getStringWidth(formatStr) >= width) {
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
		if(GuiEmailMain.refreshCoolingTicks<=0) {
			this.container.setRefresh(true);
			this.currentMsg = null;
			this.currentEmail = -1;
			this.currentSound = null;
			this.emailIDs = null;
			this.emailPage = -1;
			this.showEmails = null;
			this.popupMenuCurrentEmail = -1;
			GuiEmailMain.refreshCoolingTicks = 5 * 20;
			EmailMain.net.sendMessageToServer(new MsgRefreshInbox());
			if(!hasRefreshThread) {
				hasRefreshThread = true;
				new Thread(()->{
					while(GuiEmailMain.refreshCoolingTicks > 0) {
						try {
							Thread.sleep(50);
							GuiEmailMain.refreshCoolingTicks--;
						}catch(InterruptedException e) {
							e.printStackTrace();
						}
					}
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
			this.mc.getSoundHandler().stopSound(this.currentSound);
			this.currentSound = null;
			MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Stop(this.currentEmail));
		}
	}

	// TODO public method
	
	public FontRenderer getFontRenderer() {
		return super.fontRenderer;
	}
	public boolean isClose() {
		return isClose;
	}
	
	@Override
	public void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
		super.drawGradientRect(left, top, right, bottom, startColor, endColor);
	}
	@Override
	public void drawHorizontalLine(int startX, int endX, int y, int color) {
		super.drawHorizontalLine(startX, endX, y, color);
	}
	@Override
	public void drawVerticalLine(int x, int startY, int endY, int color) {
		super.drawVerticalLine(x, startY, endY, color);
	}
	public void drawAlignRightString(String text, int x, int y, int color, boolean drawShadow) {
		EmailUtils.drawAlignRightString(this.fontRenderer, text, x, y, color, drawShadow);
	}
	
	public void drawCenteredString(String text, int x, int y, int color) {
		this.drawCenteredString(super.fontRenderer, text, x, y, color);
	}
	public void drawCenteredStringWithShadow(String text, int x, int y, int color) {
		this.drawCenteredStringWithShadow(super.fontRenderer, text, x, y, color);
	}
	@Override
	public void drawCenteredString(FontRenderer fr, String text, int x, int y, int color) {
		fr.drawString(text, (float)(x - fr.getStringWidth(text) / 2), (float)y, color, false);
	}
	public void drawCenteredStringWithShadow(FontRenderer fr, String text, int x, int y, int color) {
		fr.drawString(text, (float)(x - fr.getStringWidth(text) / 2), (float)y, color, true);
	}
	
	public void drawString(String text, int x, int y, int color) {
		this.drawString(super.fontRenderer, text, x, y, color);
	}
	public void drawStringWithShadow(String text, int x, int y, int color) {
		this.drawStringWithShadow(super.fontRenderer, text, x, y, color);
	}
	@Override
	public void drawString(FontRenderer fr, String text, int x, int y, int color) {
		fr.drawString(text, x, y, color, false);
	}
	public void drawStringWithShadow(FontRenderer fr, String text, int x, int y, int color) {
		fr.drawString(text, x, y, color, true);
	}
	
	// TODO static method
	
	public static boolean isInRange(int mouseX, int mouseY, int x, int y, int width, int height) {
		return (mouseX >= x && mouseY >= y) && (mouseX <= x + width && mouseY <= y + height);
	}
	
	public static List<String> splitString(String text, int textMaxLength) {
		FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
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
