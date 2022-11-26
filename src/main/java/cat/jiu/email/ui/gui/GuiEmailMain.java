package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

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
import java.util.Comparator;

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
				goMsg(-1);
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+76, y+83, 9, 20, "\u21e9") {
			public void mouseReleased(int mouseX, int mouseY) {
				goMsg(1);
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+216, y+31, 9, 20, "\u21e7") {
			public void mouseReleased(int mouseX, int mouseY) {
				goText(-1);
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+216, y+83, 9, 20, "\u21e9") {
			public void mouseReleased(int mouseX, int mouseY) {
				goText(1);
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
				if(currentMsg != -1) {
					EmailMain.net.sendMessageToServer(new MsgDeleteEmail.Delete(currentMsg));
				}
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+191, y+119, 35, 12, I18n.format("info.email.accept")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(currentMsg != -1) {
					if(container.getEmailSize()+55 >= 2097152L && !EmailUtils.isInfiniteSize()) {
						emailIsOutStorageSize();
						return;
					}
					EmailMain.net.sendMessageToServer(new MsgReceiveEmail.Receive(currentMsg));
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
			if(this.showMsg == null || i >= this.showMsg.length) break;
			if(EmailGuiHandler.isInRange(mouseX, mouseY, this.getGuiLeft() + Candidate_Email_X, this.getGuiTop() + Candidate_Email_Y + ((17 * i)), 54, 15)) {
				if(this.container.getInbox().hasEmail(this.showMsg[i])) {
					Email msg = this.container.getInbox().getEmail(this.showMsg[i]);
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
	
	private int[] showMsg = null;
	private int[] msgKeyMap = null;
	private int currentMsg = -1;
	private int msgPage = 0;
	
	private void goMsg(int page) {
		if(this.msgKeyMap == null) return;
		
		if(this.size > 5) {
			this.msgPage += page;
			int maxPage = this.size - 5;
			if(this.msgPage > this.size) this.msgPage = this.size;
			if(this.msgPage < 0) this.msgPage = 0;
			if(this.msgPage > maxPage) this.msgPage = maxPage;
			
			this.showMsg = Arrays.copyOfRange(this.msgKeyMap, this.msgPage, 5 + this.msgPage);
		}else {
			this.showMsg = Arrays.copyOf(this.msgKeyMap, this.msgKeyMap.length);
		}
	}
	
	private List<String> showText = Lists.newArrayList();
	private int[] textKeyMap = null;
	private int[] currentText = null;
	private int textPage = 0;
	
	private int maxSelectedTextRows = EmailConfigs.Main.Selected_Text_Rows;
	public void setMaxSelectedTextRows(int maxrows) {this.maxSelectedTextRows = maxrows;}
	public int getMaxSelectedTextRows() {return maxSelectedTextRows;}
	
	private int showSelectedSpacing = EmailConfigs.Main.Selected_Text_Spacing;
	public void setSelectedTextSpacing(int spacing) {this.showSelectedSpacing = spacing;}
	public int getSelectedTextSpacing() {return showSelectedSpacing;}
	
	private void goText(int page) {
		if(this.textKeyMap == null) return;
		if(this.showText.size() > this.maxSelectedTextRows) {
			this.textPage += page;
			int maxPage = this.showText.size() - this.maxSelectedTextRows;
			if(this.textPage > this.showText.size()) this.textPage = this.showText.size();
			if(this.textPage < 0) this.textPage = 0;
			if(this.textPage > maxPage) this.textPage = maxPage;
			this.currentText = Arrays.copyOfRange(this.textKeyMap, this.textPage, this.maxSelectedTextRows + this.textPage);
		}else {
			this.currentText = Arrays.copyOf(this.textKeyMap, this.textKeyMap.length);
		}
	}
	
	private int[] reverseOrder(int[] array) {
		array = IntStream.of(array)
					.boxed()
					.sorted(Comparator.reverseOrder())
					.mapToInt(Integer::intValue)
					.toArray();
		return array;
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
				this.goMsg(-1 - page);
			}else if(key == -120) {
				this.goMsg(1 + page);
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
				this.goText(-1 - page);
			}else if(key == -120) {
				this.goText(1 + page);
			}
		}
	}
	
	private int size = -1;
	
	private void init() {
		if(this.size == -1) this.size = this.container.getInbox().emailCount();
		if(this.msgKeyMap == null && this.size > 0) {
			this.msgKeyMap = new int[this.container.getInbox().emailCount()];
			for(int i = 0; i < this.container.getInbox().emailCount(); i++) {
				this.msgKeyMap[i] = i;
			}
			this.msgKeyMap = this.reverseOrder(this.msgKeyMap);
			this.msgPage = 0;
			this.goMsg(0);
		}
		if(this.size != this.container.getInbox().emailCount()) {
			this.size = this.container.getInbox().emailCount();
			this.msgKeyMap = new int[this.container.getInbox().emailCount()];
			for(int i = 0; i < this.container.getInbox().emailCount(); i++) {
				this.msgKeyMap[i] = i;
			}
			this.msgKeyMap = this.reverseOrder(this.msgKeyMap);
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
		if(this.showMsg == null) return;
		int unRead = this.container.getInbox().getUnRead();
		int unReceive = this.container.getInbox().getUnReceived();
		
		super.fontRenderer.drawString(I18n.format("info.email.un_read") + ": "+ unRead, 5, 6, Color.BLACK.getRGB());
		super.fontRenderer.drawString(I18n.format("info.email.un_accept") + ": " + unReceive, 38, 6, Color.BLACK.getRGB());
		
		for (int i = 0; i < this.showMsg.length; i++) {
			if(this.container.getInbox().hasEmail(this.showMsg[i])) {
				Email email = this.container.getInbox().getEmail(this.showMsg[i]);
				
				if(email.hasItems()) {
					super.fontRenderer.drawString("$", Candidate_Email_X+44, Candidate_Email_Y + (17 * i), email.isReceived() ? Color.GREEN.getRGB() :  Color.RED.getRGB());
				}
				super.fontRenderer.drawString("*", Candidate_Email_X+49, Candidate_Email_Y + (17 * i), email.isRead() ? Color.GREEN.getRGB() : Color.RED.getRGB());
				
				super.drawCenteredString(fontRenderer, Integer.toString(this.showMsg[i]), Candidate_Email_X-9, Candidate_Email_Y + 3 + (17 * i), Color.WHITE.getRGB());
				
				String sender = email.getSender().format();
				if(this.fontRenderer.getStringWidth(sender) > EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender) {
					sender = this.fontRenderer.trimStringToWidth(sender, EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender) + "...";
				}
				super.drawString(fontRenderer, sender, Candidate_Email_X+1, Candidate_Email_Y + (17 * i), Color.WHITE.getRGB());
				super.fontRenderer.drawString(email.getTime().substring(2), Candidate_Email_X+1, Candidate_Email_Y + 7 + (17 * i), Color.BLACK.getRGB());
			}
		}
		
		if(this.currentMsg >= 0) {
			if(this.container.getInbox().hasEmail(this.currentMsg)) {
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
				
				super.drawCenteredString(fontRenderer, Integer.toString(this.currentMsg), EmailConfigs.Main.Position.Current_Email.MsgID.X, EmailConfigs.Main.Position.Current_Email.MsgID.Y, Color.WHITE.getRGB());
				
				for(int i = 0; i < currentText.length; i++) {
					String line = Integer.toString(this.currentText[i]+1);
					super.fontRenderer.drawString(line, (EmailConfigs.Main.Position.Current_Email.Row.X - fontRenderer.getStringWidth(line) / 2),  EmailConfigs.Main.Position.Current_Email.Row.Y + (this.showSelectedSpacing * i), Color.BLACK.getRGB());
					super.fontRenderer.drawString(this.showText.get(this.currentText[i]),  EmailConfigs.Main.Position.Current_Email.Msg.X, EmailConfigs.Main.Position.Current_Email.Msg.Y + (this.showSelectedSpacing * i), Color.BLACK.getRGB());
				}
				
				if(msg.hasItems()) {
					if(this.container.getCurrenMsg() == this.currentMsg) {
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
		if(this.container.getInbox()==null || this.container.getInbox().emailCount()<=0 || this.showMsg == null) return;
		
		for(int i = 0; i < 5; i++) {
			if(EmailGuiHandler.isInRange(mouseX, mouseY, this.getGuiLeft() + Candidate_Email_X, this.getGuiTop() + Candidate_Email_Y + (17 * i) - 1, 54, 15)) {
				if(i >= this.showMsg.length) break;
				Email msg = this.container.getInbox().getEmail(this.showMsg[i]);
				if(msg != null) {
					if(!msg.isRead()) {
						if(this.container.getEmailSize()+51 >= 2097152L && !EmailUtils.isInfiniteSize()) {
							emailIsOutStorageSize();
							return;
						}
						EmailMain.net.sendMessageToServer(new MsgReadEmail(this.showMsg[i]));
					}
					this.clearRenderText();
					this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					if(this.showMsg[i] != this.currentMsg) this.stopSound();
					this.currentMsg = this.showMsg[i];
					this.container.setCurrenMsg(this.currentMsg);
					if(this.container.getInbox().hasEmail(this.currentMsg)) {
						if(msg.hasMessages()) {
							this.formatMessage(msg.getMsgs());
							this.textKeyMap = new int[this.showText.size()];
							for(int j = 0; j < this.showText.size(); j++) {
								this.textKeyMap[j] = j;
							}
							this.currentText = new int[this.showText.size() > this.maxSelectedTextRows ? this.maxSelectedTextRows : this.showText.size()];
							for(int j = 0; j < this.showText.size(); j++) {
								if(j >= this.currentText.length) break;
								this.currentText[j] = j;
							}
						}else {
							this.textKeyMap = new int[0];
							this.currentText = new int[0];
							this.showText.clear();
						}
						this.textPage = 0;
					}
				}
				break;
			}
		}
		if(this.currentMsg >=0 && this.getCurrentEmail()!=null && this.getCurrentEmail().hasSound()
		&& EmailGuiHandler.isInRange(mouseX, mouseY, this.getGuiLeft() + 211, this.getGuiTop() + 4, 13, 12)) {
			if(this.currentSound==null) {
				this.currentSound = new EmailSenderSndSound(this.getCurrentEmail().getSound(), this.currentMsg);
				this.mc.getSoundHandler().playSound(this.currentSound);
				this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return;
			}
		}
	}
	
	private void formatMessage(List<Text> msgs) {
		this.showText.clear();
		
		for(int i = 0; i < msgs.size(); i++) {
			String msg = msgs.get(i).format();
			if(this.fontRenderer.getStringWidth(msg) >= EmailConfigs.Main.Number_Of_Words.Current_Email.Message) {
				char[] chs = msg.toCharArray();
				StringBuilder s = new StringBuilder();
				for(int k = 0; k < chs.length; k++) {
					s.append(chs[k]);
					String formatStr = s.toString();
					if(this.fontRenderer.getStringWidth(formatStr) >= EmailConfigs.Main.Number_Of_Words.Current_Email.Message) {
						this.showText.add(formatStr);
						s.setLength(0);
					}
				}
				if(s.length() > 0) {
					this.showText.add(s.toString());
				}
			}else {
				this.showText.add(msg);
			}
		}
	}
	
	protected Email getCurrentEmail() {
		return this.container.getInbox().getEmail(this.currentMsg);
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
