package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.google.common.collect.Lists;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.event.InboxDrawEvent;
import cat.jiu.email.event.InboxDrawEvent.Type;
import cat.jiu.email.event.InboxPlaySoundEvent;
import cat.jiu.email.iface.IInboxText;
import cat.jiu.email.net.msg.*;
import cat.jiu.email.net.msg.refresh.MsgRefreshInbox;
import cat.jiu.email.proxy.ClientProxy;
import cat.jiu.email.ui.EmailGuiHandler;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.TextUtils;
import cat.jiu.email.util.client.EmailSenderSndSound;
import cat.jiu.email.util.client.GuiDynamicImage;

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
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;

@SideOnly(Side.CLIENT)
public class GuiEmailMain extends GuiContainer {
	public static ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_main.png");
	public static ResourceLocation load = new ResourceLocation(EmailMain.MODID, "textures/gui/load.png");
	private final ContainerEmailMain container;
	private GuiPopupMenu popupMenu = new GuiPopupMenu();
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
		this.buttonList.clear();
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		
		this.initTurnPageBtn(x, y);
		this.initFuntionBtn(x, y);
		this.initPopupMenu();
		
		String text = I18n.format("info.email.black");
		int weight = this.fontRenderer.getStringWidth(text);
		this.buttonList.add(new GuiButton(nextID(), x+190 - (weight + 4), y+145, weight + 4, 12, text) {
			public void mouseReleased(int mouseX, int mouseY) {
				GuiEmailMain gui = GuiEmailMain.this;
				mc.displayGuiScreen(new GuiBlacklist(gui));
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
	
	private void initFuntionBtn(int x, int y) {
		this.buttonList.add(new GuiButton(nextID(), x+4, y+106, 43, 12, I18n.format("info.email.delete_accept")) {
			public void mouseReleased(int mouseX, int mouseY) {
				EmailMain.net.sendMessageToServer(new MsgDeleteEmail.AllReceive());
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+4, y+119, 43, 12, I18n.format("info.email.delete_read")) {
			public void mouseReleased(int mouseX, int mouseY) {
				EmailMain.net.sendMessageToServer(new MsgDeleteEmail.AllRead());
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+4, y+132, 43, 12, I18n.format("info.email.dispatch")) {
			public void mouseReleased(int mouseX, int mouseY) {
				EmailMain.net.sendMessageToServer(new MsgOpenGui(EmailGuiHandler.EMAIL_SEND));
			}
		});
		
		this.buttonList.add(new GuiButton(nextID(), x+192, y+106, 41, 12, I18n.format("info.email.delete")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(currentEmail != -1) {
					EmailMain.net.sendMessageToServer(new MsgDeleteEmail.Delete(currentEmail));
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
		});
		this.buttonList.add(new GuiButton(nextID(), x+192, y+145, 41, 12, I18n.format("info.email.accept_all")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(!EmailConfigs.isInfiniteSize()) {
					if(container.getInboxSize()+(container.getInbox().getUnReceived() * 55) >= 2097152L) {
						emailIsOutStorageSize();
						return;
					}
				}
				EmailMain.net.sendMessageToServer(new MsgReceiveEmail.All());
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+192, y+132, 41, 12, I18n.format("info.email.read_all")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(!EmailConfigs.isInfiniteSize()) {
					if(container.getInboxSize()+(container.getInbox().getUnRead() * 51) > 2097152L) {
						emailIsOutStorageSize();
						return;
					}
				}
				
				EmailMain.net.sendMessageToServer(new MsgReadEmail.All());
			}
		});
	}
	
	private void initPopupMenu() {
		this.popupMenu = new GuiPopupMenu();
		this.popupMenu.addPopupButton(new GuiButton(nextID(), 0, 0, 45, 12, I18n.format("info.email.delete")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(popupMenuCurrentEmail != -1) {
					EmailMain.net.sendMessageToServer(new MsgDeleteEmail.Delete(popupMenuCurrentEmail));
					popupMenu.setVisible(false);
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
		});
	}
	
	private long renderTick = 0;
	private String renderText;
	private Color renderColor;
	private int renderX;
	private int renderY;
	
	public void emailIsOutStorageSize() {
		this.setRenderText(EmailUtils.parseTick(0,0,0,10, 0), I18n.format("info.email.out_size"), Color.RED, 65, 145);
	}
	
	public void setRenderText(long showTick, @Nonnull String text, @Nonnull Color color, int x, int y) {
		this.renderTick = showTick;
		this.renderText = text;
		this.renderColor = color;
		this.renderX = x;
		this.renderY = y;
	}
	public void clearRenderText() {
		this.renderTick = 0;
	}
	
	private final int Candidate_Email_X = EmailConfigs.Main.Position.Candidate_Email.X;
	private final int Candidate_Email_Y = EmailConfigs.Main.Position.Candidate_Email.Y;
	
	private int size = -1;
	private long[] showEmails = null;
	private long[] emailIDs = null;
	private long currentEmail = -1;
	private int emailPage = 0;
	
	private void goEmail(int page) {
		if(this.emailIDs == null) return;
		
		if(this.size > 5) {
			this.emailPage += page;
			int maxPage = this.size - 5;
			if(this.emailPage > this.size) this.emailPage = this.size;
			if(this.emailPage < 0) this.emailPage = 0;
			if(this.emailPage > maxPage) this.emailPage = maxPage;
			
			this.showEmails = Arrays.copyOfRange(this.emailIDs, this.emailPage, 5 + this.emailPage);
		}else {
			this.showEmails = Arrays.copyOf(this.emailIDs, this.emailIDs.length);
		}
	}
	
	
	private int maxSelectedTextRows = EmailConfigs.Main.Selected_Text_Rows;
	public void setMaxSelectedTextRows(int maxrows) {this.maxSelectedTextRows = maxrows;}
	public int getMaxSelectedTextRows() {return maxSelectedTextRows;}
	
	private int showSelectedSpacing = EmailConfigs.Main.Selected_Text_Spacing;
	public void setSelectedTextSpacing(int spacing) {this.showSelectedSpacing = spacing;}
	public int getSelectedTextSpacing() {return showSelectedSpacing;}

	private List<Message> showMessages = Lists.newArrayList();
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
		if(this.size == -1) this.size = this.container.getInbox().emailCount();
		if(this.emailIDs == null && this.size > 0) {
			this.emailIDs = this.container.getInbox().getEmailIDs().stream()
								.sorted((l0, l1) -> l1.compareTo(l0))
								.mapToLong(Long::longValue).toArray();
			this.emailPage = 0;
			this.goEmail(0);
		}
		if(this.size != this.container.getInbox().emailCount()) {
			this.size = this.container.getInbox().emailCount();
			this.emailIDs = this.container.getInbox().getEmailIDs().stream()
								.sorted((l0, l1) -> l1.compareTo(l0))
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
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawDefaultBackground();
		GlStateManager.popMatrix();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(BackGround);
		this.drawTexturedModalRect((this.width - this.xSize) / 2, (this.height - this.ySize) / 2, 0, 0, this.xSize, this.ySize);
		GlStateManager.pushMatrix();
		if(this.currentSound!=null && this.currentSound.isDonePlaying()) {
			this.stopSound();
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
		super.renderHoveredToolTip(mouseX, mouseY);
		
		if(this.container.isRefresh()) return;
		if(isInRange(mouseX, mouseY, this.getGuiLeft() + 5, this.getGuiTop() + 5, 7, 7)) {
			super.drawHoveringText(I18n.format(this.refreshCoolingTicks <= 0 ? "info.email.refresh" : "info.email.refresh.cooling"), mouseX, mouseY);
		}
		for(GuiButton btn : this.buttonList) {
			this.drawHorizontalLine(btn.x, btn.x + btn.width - 2, btn.y + btn.height-1, Color.BLACK.getRGB());
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
							long last = time - System.currentTimeMillis();
							long s = last / 1000;
							long m = s / 60;
							s %= 60;
							long h = m / 60;
							m %= 60;
							long d = h / 24;
							h %= 24;
							tip.add(I18n.format("info.email.remain_expiration_time", d,h,m,s));
						}
					}
					
					super.drawHoveringText(tip, mouseX, mouseY+10);
					break;
				}
			}
		}
		this.popupMenu.drawPopupMenu(this.popupMenuCurrentEmail, this.mc, mouseX, mouseY, partialTicks);
	}
	
	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		if(this.container.isRefresh()) {
			int x = (this.width - this.xSize) / 2;
			int y = (this.height - this.ySize) / 2;
			this.loadImage.draw(x + 3, y + 146);
			this.drawCenteredString(I18n.format("info.email.refreshing"), x + 9 + 40, y + 150, Color.RED.getRGB());
			
		}else {
			MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.INBOX, Phase.START, this.container.getInbox(), -1, mouseX, mouseY));
			
			GlStateManager.popMatrix();
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			this.drawInbox(mouseX, mouseY);
			GlStateManager.pushMatrix();
			
			MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.INBOX, Phase.END, this.container.getInbox(), -1, mouseX, mouseY));
		}
	}
	
	protected void drawInbox(int mouseX, int mouseY) {
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		if(this.renderTick > 0) {
			super.fontRenderer.drawString(this.renderText, x+this.renderX, y+this.renderY, this.renderColor.getRGB());
			this.renderTick--;
		}
		
		// 展示邮箱所占大小
		super.fontRenderer.drawString(I18n.format("info.email.storage"), x+5, y+148, Color.BLACK.getRGB());
		Color sizeColor = Color.GREEN;
		if(!EmailConfigs.isInfiniteSize()) {
			long size = this.container.getInboxSize();
			if(size >= 1398122) {
				sizeColor = Color.RED;
			}else if(size >= 699061) {
				sizeColor = Color.YELLOW;
			}
		}
		
		int storageWidth = this.fontRenderer.getStringWidth(I18n.format("info.email.storage"))+5;
		String bytes = EmailConfigs.isInfiniteSize() ? TextUtils.makeFabulous("Infinite") : "2097183";
		int bytesWidth = this.mc.fontRenderer.getStringWidth(bytes);
		super.fontRenderer.drawString(bytes, x+6 + storageWidth, y+156, Color.BLACK.getRGB(), EmailConfigs.isInfiniteSize());
		super.fontRenderer.drawString("Bytes", x+7+bytesWidth+1+storageWidth, y+156, Color.BLACK.getRGB(), false);
		this.drawAlignRightString(String.valueOf(this.container.getInboxSize()), x+6+storageWidth+bytesWidth, y+145, sizeColor.getRGB(), true);
		this.drawHorizontalLine(x+5+storageWidth, x+5+bytesWidth+storageWidth, y+154, Color.BLACK.getRGB());
		
		this.mc.getTextureManager().bindTexture(BackGround);
		Gui.drawScaledCustomSizeModalRect(x + 6, y + 6, 111, 169, 55, 55, 6, 6, 256, 256);
		
		if(this.container.getInbox() == null
		|| this.container.getInbox().emailCount()<=0) return;
		this.init();
		if(this.showEmails == null) return;
		
		// 展示未读(*)与未领($)
		super.fontRenderer.drawString("$:"+ this.container.getInbox().getUnReceived(), x+25, y+3, Color.RED.getRGB());
		super.fontRenderer.drawString("*:" + this.container.getInbox().getUnRead(), x+50, y+3, Color.RED.getRGB());
		
		// 展示邮件列表 
		for (int i = 0; i < this.showEmails.length; i++) {
			if(this.container.getInbox().hasEmail(this.showEmails[i])) {
				Email email = this.container.getInbox().getEmail(this.showEmails[i]);
				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CANDIDATE, Phase.START, this.container.getInbox(), this.showEmails[i], mouseX, mouseY));
				
				if(email.hasItems()) {
					super.fontRenderer.drawString("$", x+Candidate_Email_X+51, y+Candidate_Email_Y + (19 * i) + 1, email.isReceived() ? Color.GREEN.getRGB() :  Color.RED.getRGB());
				}
				super.fontRenderer.drawString("*", x+Candidate_Email_X+56, y+Candidate_Email_Y + (19 * i) + 1, email.isRead() ? Color.GREEN.getRGB() : Color.RED.getRGB());
				
				super.drawCenteredString(fontRenderer, String.valueOf(this.showEmails[i]), x+Candidate_Email_X-8, y+Candidate_Email_Y + 5 + (19 * i), Color.WHITE.getRGB());
				
				String sender = email.getSender().format();
				if(this.fontRenderer.getStringWidth(sender) > EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender) {
					sender = this.fontRenderer.trimStringToWidth(sender, EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender) + "...";
				}
				super.fontRenderer.drawString(sender, x+Candidate_Email_X+1, y+Candidate_Email_Y + (19 * i)+1, Color.WHITE.getRGB());
				super.fontRenderer.drawString(email.getCreateTimeAsString().substring(5, email.getCreateTimeAsString().length()-3), x+Candidate_Email_X+1, y+Candidate_Email_Y + 10 + (19 * i), Color.BLACK.getRGB());
				
				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CANDIDATE, Phase.END, this.container.getInbox(), this.showEmails[i], mouseX, mouseY));
			}
		}
		
		if(this.currentEmail >= 0) {
			if(this.container.getInbox().hasEmail(this.currentEmail)) {
				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CURRENT, Phase.START, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
				Email email = this.getCurrentEmail();
				
				String sender = email.getSender().format();
				if(this.fontRenderer.getStringWidth(sender) > EmailConfigs.Main.Number_Of_Words.Current_Email.Sender) {
					sender = this.fontRenderer.trimStringToWidth(sender, EmailConfigs.Main.Number_Of_Words.Current_Email.Sender) + "...";
				}
				String title = email.getTitle().format();
				if(this.fontRenderer.getStringWidth(title) > EmailConfigs.Main.Number_Of_Words.Current_Email.Title-(email.hasSound()?13:0)) {
					title = this.fontRenderer.trimStringToWidth(title, EmailConfigs.Main.Number_Of_Words.Current_Email.Title-(email.hasSound()?13:0)) + "...";
				}
				
				super.fontRenderer.drawString(title, x+7+EmailConfigs.Main.Position.Current_Email.Title.X, y+EmailConfigs.Main.Position.Current_Email.Title.Y, Color.WHITE.getRGB());
				super.fontRenderer.drawString(sender, x+6+EmailConfigs.Main.Position.Current_Email.Sender.X, y+EmailConfigs.Main.Position.Current_Email.Sender.Y, Color.WHITE.getRGB());
				this.drawAlignRightString(email.getCreateTimeAsString().substring(2, email.getCreateTimeAsString().length()-3), x+5+ 64 +EmailConfigs.Main.Position.Current_Email.Title.Time.X, y+EmailConfigs.Main.Position.Current_Email.Title.Time.Y, Color.WHITE.getRGB(), false);
				
				super.drawCenteredString(fontRenderer, String.valueOf(this.currentEmail), x+7+EmailConfigs.Main.Position.Current_Email.MsgID.X, y+EmailConfigs.Main.Position.Current_Email.MsgID.Y, Color.WHITE.getRGB());
				
				// 绘制信息
				int msgIndex = -1;
				for(int i = 0; i < this.currentMsg.length; i++) {
					int index = this.currentMsg[i];
					Message msg = this.showMessages.get(index);
					
					if(msgIndex != msg.row) {
						super.fontRenderer.drawString(Integer.toString(msg.row+1), x+7+(EmailConfigs.Main.Position.Current_Email.Row.X - fontRenderer.getStringWidth(Integer.toString(msg.row)) / 2),  y-1+EmailConfigs.Main.Position.Current_Email.Row.Y + ((this.mc.fontRenderer.FONT_HEIGHT+EmailConfigs.Main.Selected_Text_Spacing) * i), Color.BLACK.getRGB());
						msgIndex = msg.row;
					}else {
						boolean unicodeFlag = super.fontRenderer.getUnicodeFlag();
						super.fontRenderer.setUnicodeFlag(false);
						
						if(index+1 >= this.showMessages.size()
						|| this.showMessages.get(index+1).row != msgIndex) {
							super.fontRenderer.drawString("\u255a", x+5+(EmailConfigs.Main.Position.Current_Email.Row.X - fontRenderer.getStringWidth(Integer.toString(msg.row)) / 2),  y-1+EmailConfigs.Main.Position.Current_Email.Row.Y + ((this.mc.fontRenderer.FONT_HEIGHT+EmailConfigs.Main.Selected_Text_Spacing) * i), Color.BLACK.getRGB());
						}else {
							super.fontRenderer.drawString("\u2560", x+5+(EmailConfigs.Main.Position.Current_Email.Row.X - fontRenderer.getStringWidth(Integer.toString(msg.row)) / 2),  y-1+EmailConfigs.Main.Position.Current_Email.Row.Y + ((this.mc.fontRenderer.FONT_HEIGHT+EmailConfigs.Main.Selected_Text_Spacing) * i), Color.BLACK.getRGB());
						}
						
						super.fontRenderer.setUnicodeFlag(unicodeFlag);
					}
					super.fontRenderer.drawString(msg.msg, x+7+EmailConfigs.Main.Position.Current_Email.Msg.X, y-1+EmailConfigs.Main.Position.Current_Email.Msg.Y + ((this.mc.fontRenderer.FONT_HEIGHT+EmailConfigs.Main.Selected_Text_Spacing) * i), Color.BLACK.getRGB());
				}
				
				if(email.hasItems()) {
					if(this.container.getCurrenEmail() == this.currentEmail) {
						this.container.putStack(email.getItems());
					}
				}else {
					if(!this.container.isEmpty()) {
						this.container.clear();
					}
				}
				
				if(email.hasSound()) {
					this.mc.getTextureManager().bindTexture(BackGround);
					
					// old
//					this.drawTexturedModalRect(x+218, y+4, 236, (this.currentSound != null ? 12 : 0), 13, 12);
					GlStateManager.color(1.0F,  1.0F,  1.0F);
					this.drawTexturedModalRect(x+218, y+4, 236, 0, 3, 12);

					Gui.drawScaledCustomSizeModalRect(x+218 + 3, y+5, 1 + (this.currentSound != null ? 55 : 0), 169, 55, 55, 10, 10, 256, 256);
					
					if(isInRange(mouseX, mouseY, x+218, y+4, 13, 12)) {
						this.drawHoveringText(I18n.format("info.email.play_sound" + (this.currentSound!=null?".stop":"")), mouseX, mouseY);
					}
				}
				
				MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.CURRENT, Phase.END, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
			}
		}
	}
	
	private EmailSenderSndSound currentSound;
	private boolean isClose = false;
	
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
		
		if(isInRange(mouseX, mouseY, this.getGuiLeft() + 5, this.getGuiTop() + 5, 7, 7)
		&& this.refreshCoolingTicks <= 0) {
			this.refresh();
			this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			return;
		}
		
		if(this.popupMenu.mouseClicked(this.mc, mouseX, mouseY, btn)) return;
		super.mouseClicked(mouseX, mouseY, btn);
		if(this.container.getInbox()==null || this.container.getInbox().emailCount()<=0 || this.showEmails == null) return;
		
		for(int index = 0; index < 5; index++) {
			if(isInRange(mouseX, mouseY, this.getGuiLeft() + Candidate_Email_X, this.getGuiTop() + Candidate_Email_Y + (19 * index), 60, 17)) {
				if(index >= this.showEmails.length) break;
				if(!this.container.getInbox().hasEmail(this.showEmails[index])) continue;
				if(btn == 0) {
					// 展示邮件
					this.showEmail(index);
				}else if(btn == 1) {
					// 展示子菜单
					this.popupMenuCurrentEmail = this.showEmails[index];
					this.popupMenu.setCreatePoint(mouseX, mouseY);
					this.popupMenu.setVisible(true);
					this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				}
				break;
			}
		}
		
		// 点击音效
		if(isInRange(mouseX, mouseY, this.getGuiLeft() + 218, this.getGuiTop() + 4, 13, 12) 
		&&	this.currentEmail >=0 && this.getCurrentEmail()!=null && this.getCurrentEmail().hasSound()) {
			if(this.currentSound==null) {
				this.currentSound = new EmailSenderSndSound(this.getCurrentEmail().getSound(), this.currentEmail);
				MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Start(this.getCurrentEmail().getSound(), this.currentEmail));
				this.mc.getSoundHandler().playSound(this.currentSound);
				this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}else {
				this.stopSound();
				this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			}
		}
		
		// 展示子菜单
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
			
			if(this.container.getInbox().hasEmail(this.currentEmail)) {
				if(email.hasMessages()) {
					this.formatMessage(email.getMsgs());
					this.msgKeyMap = new int[this.showMessages.size()];
					for(int j = 0; j < this.showMessages.size(); j++) {
						this.msgKeyMap[j] = j;
					}
					this.currentMsg = new int[this.showMessages.size() > this.maxSelectedTextRows ? this.maxSelectedTextRows : this.showMessages.size()];
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
	
	private void formatMessage(List<IInboxText> msgs) {
		this.showMessages.clear();
		int width = EmailConfigs.Main.Number_Of_Words.Current_Email.Message;
		
		if(EmailConfigs.Main.Enable_Vanilla_Wrap_Text) {
			for(int row = 0; row < msgs.size(); row++) {
				String msg = msgs.get(row).format();
				if(msg.startsWith("&il")) msg = "    " + msg.substring(3);
				List<Message> m = Lists.newArrayList();
				
				List<String> formatMsg = this.fontRenderer.listFormattedStringToWidth(msg, width);
				for(int index = 0; index < formatMsg.size(); index++) {
					m.add(new Message(row, index, formatMsg.get(index)));
				}
				this.showMessages.addAll(m);
			}
		}else {
			for(int row = 0; row < msgs.size(); row++) {
				String msg = msgs.get(row).format();
				if(msg.startsWith("&il")) msg = "    " + msg.substring(3);
				if(this.fontRenderer.getStringWidth(msg) >= width) {
					char[] chs = msg.toCharArray();
					StringBuilder s = new StringBuilder();
					int index = 0;
					for(int k = 0; k < chs.length; k++) {
						s.append(chs[k]);
						String formatStr = s.toString();
						if(this.fontRenderer.getStringWidth(formatStr) >= width) {
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
	
	private int refreshCoolingTicks = 0;
	private void refresh() {
		if(this.refreshCoolingTicks<=0) {
			EmailMain.net.sendMessageToServer(new MsgRefreshInbox());
			this.container.setRefresh(true);
			this.currentMsg = null;
			this.currentEmail = -1;
			this.currentSound = null;
			this.emailIDs = null;
			this.emailPage = -1;
			this.showEmails = null;
			this.popupMenuCurrentEmail = -1;
			this.refreshCoolingTicks = 5 * 20;
			new Thread(()->{
				while(!this.isClose && this.refreshCoolingTicks > 0) {
					try {
						Thread.sleep(50);
						this.refreshCoolingTicks--;
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
	
	protected Email getCurrentEmail() {
		return this.container.getInbox().getEmail(this.currentEmail);
	}
	
	public void stopSound() {
		if(this.currentSound != null) {
			if(this.mc.getSoundHandler().isSoundPlaying(this.currentSound)) {
				this.mc.getSoundHandler().stopSound(this.currentSound);
			}
			this.currentSound = null;
			MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Stop(this.currentEmail));
		}
	}
	
	public FontRenderer getFontRenderer() {
		return super.fontRenderer;
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
		for(int i = text.length(); i > 0; i--) {
			char c = text.charAt(i-1);
			float width = super.fontRenderer.getCharWidth(c);
			x -= width;
			super.fontRenderer.drawString(String.valueOf(c), x, y, color, drawShadow);
		}
	}
	public void drawCenteredString(String text, int x, int y, int color) {
		FontRenderer fr = super.fontRenderer;
		fr.drawString(text, (float)(x - fr.getStringWidth(text) / 2), (float)y, color, false);
	}
	public void drawCenteredStringWithShadow(String text, int x, int y, int color) {
		FontRenderer fr = super.fontRenderer;
		fr.drawString(text, (float)(x - fr.getStringWidth(text) / 2), (float)y, color, true);
	}
	
	public static boolean isInRange(int mouseX, int mouseY, int x, int y, int width, int height) {
		int maxX = x + width;
		int maxY = y + height;
		return (mouseX >= x && mouseY >= y) && (mouseX <= maxX && mouseY <= maxY);
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
