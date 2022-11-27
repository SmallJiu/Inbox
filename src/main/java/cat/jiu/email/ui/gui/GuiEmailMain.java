package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.stream.LongStream;

import javax.annotation.Nonnull;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.google.common.collect.Lists;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Text;
import cat.jiu.email.net.msg.*;
import cat.jiu.email.ui.EmailGuiHandler;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailSenderSndSound;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.Collections;

@SideOnly(Side.CLIENT)
public class GuiEmailMain extends GuiContainer {
	public static ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_main.png");
	private final ContainerEmailMain container;
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
		
		this.buttonList.add(new GuiButton(nextID(), x+76, y+19, 9, 20, "\u21e7") {
			public void mouseReleased(int mouseX, int mouseY) {
				goEmail(-1);
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+76, y+83, 9, 20, "\u21e9") {
			public void mouseReleased(int mouseX, int mouseY) {
				goEmail(1);
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+216, y+31, 9, 20, "\u21e7") {
			public void mouseReleased(int mouseX, int mouseY) {
				goMessage(-1);
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+216, y+83, 9, 20, "\u21e9") {
			public void mouseReleased(int mouseX, int mouseY) {
				goMessage(1);
			}
		});
		
		this.buttonList.add(new GuiButton(nextID(), x+4, y+106, 37, 12, I18n.format("info.email.delete_accept")) {
			public void mouseReleased(int mouseX, int mouseY) {
				EmailMain.net.sendMessageToServer(new MsgDeleteEmail.AllReceive());
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+4, y+119, 37, 12, I18n.format("info.email.delete_read")) {
			public void mouseReleased(int mouseX, int mouseY) {
				EmailMain.net.sendMessageToServer(new MsgDeleteEmail.AllRead());
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+4, y+132, 37, 12, I18n.format("info.email.dispatch")) {
			public void mouseReleased(int mouseX, int mouseY) {
				EmailMain.net.sendMessageToServer(new MsgOpenGui(EmailGuiHandler.EMAIL_SEND));
			}
		});
		
		this.buttonList.add(new GuiButton(nextID(), x+191, y+106, 35, 12, I18n.format("info.email.delete")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(currentEmail != -1) {
					EmailMain.net.sendMessageToServer(new MsgDeleteEmail.Delete(currentEmail));
				}
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+191, y+119, 35, 12, I18n.format("info.email.accept")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(currentEmail != -1) {
					if(container.getEmailSize()+55 >= 2097152L && !EmailUtils.isInfiniteSize()) {
						emailIsOutStorageSize();
						return;
					}
					EmailMain.net.sendMessageToServer(new MsgReceiveEmail.Receive(currentEmail));
				}
			}
		});

		this.buttonList.add(new GuiButton(nextID(), x+191, y+145, 35, 12, I18n.format("info.email.accept_all")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(!EmailUtils.isInfiniteSize()) {
					if(container.getEmailSize()+(container.getInbox().getUnReceived() * 55) >= 2097152L) {
						emailIsOutStorageSize();
						return;
					}
				}
				EmailMain.net.sendMessageToServer(new MsgReceiveEmail.All());
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+191, y+132, 35, 12, I18n.format("info.email.read_all")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(!EmailUtils.isInfiniteSize()) {
					if(container.getEmailSize()+(container.getInbox().getUnRead() * 51) > 2097152L) {
						emailIsOutStorageSize();
						return;
					}
				}
				
				EmailMain.net.sendMessageToServer(new MsgReadEmail.All());
			}
		});
	}
	
	private long renderTick = 0;
	private String renderText;
	private Color renderColor;
	private int renderX;
	private int renderY;
	
	public void emailIsOutStorageSize() {
		this.setRenderText(EmailUtils.parseTick(10, 0), I18n.format("info.email.out_size"), Color.RED, 65, 145);
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
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		super.renderHoveredToolTip(mouseX, mouseY);
		if(this.currentSound!=null && this.currentSound.isDonePlaying()) {
			this.stopSound();
		}
		for(int i = 0; i < 5; i++) {
			if(this.showEmails == null || i >= this.showEmails.length) break;
			if(EmailGuiHandler.isInRange(mouseX, mouseY, this.getGuiLeft() + Candidate_Email_X, this.getGuiTop() + Candidate_Email_Y + ((17 * i)), 54, 15)) {
				if(this.container.getInbox().hasEmail(this.showEmails[i])) {
					Email msg = this.container.getInbox().getEmail(this.showEmails[i]);
					List<String> tip = Lists.newArrayList();
					
					tip.add(I18n.format(msg.getTitle().getKey(), msg.getTitle().getArgs()));
					tip.add("");
					tip.add(TextFormatting.GRAY + msg.getTime());
					tip.add(I18n.format("info.email.main.from", msg.getSender().format()));
					
					super.drawHoveringText(tip, mouseX, mouseY+10);
					break;
				}
			}
		}
	}
	
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
	
	private List<String> showMessages = Lists.newArrayList();
	private int[] msgKeyMap = null;
	private int[] currentMsg = null;
	private int msgPage = 0;
	
	private int maxSelectedTextRows = EmailConfigs.Main.Selected_Text_Rows;
	public void setMaxSelectedTextRows(int maxrows) {this.maxSelectedTextRows = maxrows;}
	public int getMaxSelectedTextRows() {return maxSelectedTextRows;}
	
	private int showSelectedSpacing = EmailConfigs.Main.Selected_Text_Spacing;
	public void setSelectedTextSpacing(int spacing) {this.showSelectedSpacing = spacing;}
	public int getSelectedTextSpacing() {return showSelectedSpacing;}
	
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
	
	private int size = -1;
	private long[] showEmails = null;
	private long[] emailIDs = null;
	private long currentEmail = -1;
	private int emailPage = 0;
	
	private void init() {
		if(this.size == -1) this.size = this.container.getInbox().emailCount();
		if(this.emailIDs == null && this.size > 0) {
			this.emailIDs = this.getEmailIDs();
			this.emailPage = 0;
			this.goEmail(0);
		}
		if(this.size != this.container.getInbox().emailCount()) {
			this.size = this.container.getInbox().emailCount();
			this.emailIDs = this.getEmailIDs();
		}
	}
	private long[] getEmailIDs() {
		Long[] last = this.container.getInbox().getEmailIDs().toArray(new Long[0]);
		long[] result = new long[last.length];
		for(int i = 0; i < last.length; i++) {
			result[i] = last[i];
		}
		result = LongStream.of(result)
				.boxed()
				.sorted(Collections.reverseOrder())
				.mapToLong(Long::longValue)
				.toArray();
		return result;
	}
	
	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		int key = Mouse.getEventDWheel();
        int x = (Mouse.getEventX() * this.width / this.mc.displayWidth) - this.guiLeft;
        int y = (this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1) - this.guiTop;
		
        if(EmailGuiHandler.isInRange(x, y, 76, 41, 8, 40) || EmailGuiHandler.isInRange(x, y, 18, 19, 57, 86)) {
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
		}else if(EmailGuiHandler.isInRange(x, y, 216, 53, 8, 28) || EmailGuiHandler.isInRange(x, y, 87, 30, 128, 74)) {
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
		int left = (this.width - this.xSize) / 2;
		int top = (this.height - this.ySize) / 2;
		
		if(EmailGuiHandler.isInRange(this.lastClickX, this.lastClickY, left + 76, top + 41, 8, 40)
		|| EmailGuiHandler.isInRange(this.lastClickX, this.lastClickY, left + 18, top + 19, 57, 86)) {
			if(keyCode == Keyboard.KEY_UP) {
				this.goEmail(-1);
			}else if(keyCode == Keyboard.KEY_DOWN) {
				this.goEmail(1);
			}
		}else if(EmailGuiHandler.isInRange(this.lastClickX, this.lastClickY, left + 216, top + 53, 8, 28)
			|| EmailGuiHandler.isInRange(this.lastClickX, this.lastClickY, left + 87, top + 30, 128, 74)) {
			if(keyCode == Keyboard.KEY_UP) {
				this.goMessage(-1);
			}else if(keyCode == Keyboard.KEY_DOWN) {
				this.goMessage(1);
			}
		}
	}
	
	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		if(this.renderTick > 0) {
			super.fontRenderer.drawString(this.renderText, this.renderX, this.renderY, this.renderColor.getRGB());
			this.renderTick--;
		}
		
//		this.drawAlignRightNumber(8888888, 51, 158, Color.RED.getRGB());
		super.fontRenderer.drawString(I18n.format("info.email.storage"), 5, 148, Color.BLACK.getRGB());
		this.drawAlignRightNumber(this.container.getEmailSize(), 51, 144, Color.BLACK.getRGB());
		for(int i = 0; i < 9; i++) {
			super.fontRenderer.drawString("_", 27+(i*3), 145, Color.BLACK.getRGB());
		}
		
		super.fontRenderer.drawString((EmailUtils.isInfiniteSize() ? "Infinite" : "2097183"), 28, 152, Color.BLACK.getRGB());
		super.fontRenderer.drawString("Bytes", 56.3F, 152.0F, Color.BLACK.getRGB(), false);
		
		if(this.container.getInbox() == null
		|| this.container.getInbox().emailCount()<=0) return;
		this.init();
		if(this.showEmails == null) return;
		int unRead = this.container.getInbox().getUnRead();
		int unReceive = this.container.getInbox().getUnReceived();
		
		super.fontRenderer.drawString(I18n.format("info.email.un_read") + ": "+ unRead, 5, 6, Color.BLACK.getRGB());
		super.fontRenderer.drawString(I18n.format("info.email.un_accept") + ": " + unReceive, 38, 6, Color.BLACK.getRGB());
		
		for (int i = 0; i < this.showEmails.length; i++) {
			if(this.container.getInbox().hasEmail(this.showEmails[i])) {
				Email email = this.container.getInbox().getEmail(this.showEmails[i]);
				
				if(email.hasItems()) {
					super.fontRenderer.drawString("$", Candidate_Email_X+44, Candidate_Email_Y + (17 * i), email.isReceived() ? Color.GREEN.getRGB() :  Color.RED.getRGB());
				}
				super.fontRenderer.drawString("*", Candidate_Email_X+49, Candidate_Email_Y + (17 * i), email.isRead() ? Color.GREEN.getRGB() : Color.RED.getRGB());
				
				super.drawCenteredString(fontRenderer, String.valueOf(this.showEmails[i]), Candidate_Email_X-9, Candidate_Email_Y + 3 + (17 * i), Color.WHITE.getRGB());
				
				String sender = email.getSender().format();
				if(this.fontRenderer.getStringWidth(sender) > EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender) {
					sender = this.fontRenderer.trimStringToWidth(sender, EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender) + "...";
				}
				super.drawString(fontRenderer, sender, Candidate_Email_X+1, Candidate_Email_Y + (17 * i), Color.WHITE.getRGB());
				super.fontRenderer.drawString(email.getTime().substring(2), Candidate_Email_X+1, Candidate_Email_Y + 7 + (17 * i), Color.BLACK.getRGB());
			}
		}
		
		if(this.currentEmail >= 0) {
			if(this.container.getInbox().hasEmail(this.currentEmail)) {
				Email msg = this.getCurrentEmail();
				
				String sender = msg.getSender().format();
				if(this.fontRenderer.getStringWidth(sender) > EmailConfigs.Main.Number_Of_Words.Current_Email.Sender) {
					sender = this.fontRenderer.trimStringToWidth(sender, EmailConfigs.Main.Number_Of_Words.Current_Email.Sender) + "...";
				}
				String title = msg.getTitle().format();
				if(this.fontRenderer.getStringWidth(title) > EmailConfigs.Main.Number_Of_Words.Current_Email.Title-(msg.hasSound()?13:0)) {
					title = this.fontRenderer.trimStringToWidth(title, EmailConfigs.Main.Number_Of_Words.Current_Email.Title-(msg.hasSound()?13:0)) + "...";
				}
				super.fontRenderer.drawString(title, EmailConfigs.Main.Position.Current_Email.Title.X, EmailConfigs.Main.Position.Current_Email.Title.Y, Color.WHITE.getRGB());
				
				super.fontRenderer.drawString(sender, EmailConfigs.Main.Position.Current_Email.Sender.X, EmailConfigs.Main.Position.Current_Email.Sender.Y, Color.WHITE.getRGB());
				super.fontRenderer.drawString(msg.getTime(), EmailConfigs.Main.Position.Current_Email.Title.Time.X, EmailConfigs.Main.Position.Current_Email.Title.Time.Y, Color.WHITE.getRGB());
				
				super.drawCenteredString(fontRenderer, String.valueOf(this.currentEmail), EmailConfigs.Main.Position.Current_Email.MsgID.X, EmailConfigs.Main.Position.Current_Email.MsgID.Y, Color.WHITE.getRGB());
				
				for(int i = 0; i < currentMsg.length; i++) {
					String line = Integer.toString(this.currentMsg[i]+1);
					super.fontRenderer.drawString(line, (EmailConfigs.Main.Position.Current_Email.Row.X - fontRenderer.getStringWidth(line) / 2),  EmailConfigs.Main.Position.Current_Email.Row.Y + (this.showSelectedSpacing * i), Color.BLACK.getRGB());
					super.fontRenderer.drawString(this.showMessages.get(this.currentMsg[i]),  EmailConfigs.Main.Position.Current_Email.Msg.X, EmailConfigs.Main.Position.Current_Email.Msg.Y + (this.showSelectedSpacing * i), Color.BLACK.getRGB());
				}
				
				if(msg.hasItems()) {
					if(this.container.getCurrenMsg() == this.currentEmail) {
						this.container.putStack(msg.getItems());
					}
				}else {
					if(!this.container.isEmpty()) {
						this.container.clear();
					}
				}
				
				if(msg.hasSound()) {
					GlStateManager.popMatrix();
					GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
					this.mc.getTextureManager().bindTexture(BackGround);
					int x = (this.width - this.xSize) / 2;
					int y = (this.height - this.ySize) / 2;
					
					this.drawTexturedModalRect(x+211, y+4, 230, 0+(this.currentSound!=null?12:0), 13, 12);
					GlStateManager.pushMatrix();
				}
			}
		}
	}
	
	private void drawAlignRightNumber(Number num, int x, int y, int color) {
		int j = 0;
		String text = String.valueOf(num);
		for(int i = text.length(); i > 0; i--) {
			char c = text.charAt(i-1);
			float length = super.fontRenderer.getCharWidth(c);
			if(length<4) length++;
			super.fontRenderer.drawString(String.valueOf(c), x - (j * length-1) - 0.5F, (float)y, color, false);
			j++;
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		GlStateManager.popMatrix();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(BackGround);
		this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
		GlStateManager.pushMatrix();
	}
	
	private EmailSenderSndSound currentSound;
	
	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		this.stopSound();
	}
	
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		this.lastClickX = mouseX;
		this.lastClickY = mouseY;
		if(this.container.getInbox()==null || this.container.getInbox().emailCount()<=0 || this.showEmails == null) return;
		
		for(int i = 0; i < 5; i++) {
			if(EmailGuiHandler.isInRange(mouseX, mouseY, this.getGuiLeft() + Candidate_Email_X, this.getGuiTop() + Candidate_Email_Y + (17 * i) - 1, 54, 15)) {
				if(i >= this.showEmails.length) break;
				Email msg = this.container.getInbox().getEmail(this.showEmails[i]);
				if(msg != null) {
					if(!msg.isRead()) {
						if(this.container.getEmailSize()+51 >= 2097152L && !EmailUtils.isInfiniteSize()) {
							emailIsOutStorageSize();
							return;
						}
						EmailMain.net.sendMessageToServer(new MsgReadEmail(this.showEmails[i]));
					}
					this.clearRenderText();
					this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					if(this.showEmails[i] != this.currentEmail) this.stopSound();
					this.currentEmail = this.showEmails[i];
					this.container.setCurrenMsg(this.currentEmail);
					if(this.container.getInbox().hasEmail(this.currentEmail)) {
						if(msg.hasMessages()) {
							this.formatMessage(msg.getMsgs());
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
				break;
			}
		}
		if(this.currentEmail >=0 && this.getCurrentEmail()!=null && this.getCurrentEmail().hasSound()
		&& EmailGuiHandler.isInRange(mouseX, mouseY, this.getGuiLeft() + 211, this.getGuiTop() + 4, 13, 12)) {
			if(this.currentSound==null) {
				this.currentSound = new EmailSenderSndSound(this.getCurrentEmail().getSound(), this.currentEmail);
				this.mc.getSoundHandler().playSound(this.currentSound);
				this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return;
			}
		}
	}
	
	private void formatMessage(List<Text> msgs) {
		this.showMessages.clear();
		
		for(int i = 0; i < msgs.size(); i++) {
			String msg = msgs.get(i).format();
			if(this.fontRenderer.getStringWidth(msg) >= EmailConfigs.Main.Number_Of_Words.Current_Email.Message) {
				char[] chs = msg.toCharArray();
				StringBuilder s = new StringBuilder();
				for(int k = 0; k < chs.length; k++) {
					s.append(chs[k]);
					String formatStr = s.toString();
					if(this.fontRenderer.getStringWidth(formatStr) >= EmailConfigs.Main.Number_Of_Words.Current_Email.Message) {
						this.showMessages.add(formatStr);
						s.setLength(0);
					}
				}
				if(s.length() > 0) {
					this.showMessages.add(s.toString());
				}
			}else {
				this.showMessages.add(msg);
			}
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
		}
	}
}
