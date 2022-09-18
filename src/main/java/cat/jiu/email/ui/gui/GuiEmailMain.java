package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.email.EmailMain;
import cat.jiu.email.net.msg.*;
import cat.jiu.email.ui.EmailGuiHandler;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailSenderSndSound;
import cat.jiu.email.util.EmailSound;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonToStackUtil;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
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
		this.buttonList.add(new GuiButton(nextID(), x+213, y+31, 9, 20, "\u21e7") {
			public void mouseReleased(int mouseX, int mouseY) {
				goText(-1);
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+213, y+83, 9, 20, "\u21e9") {
			public void mouseReleased(int mouseX, int mouseY) {
				goText(1);
			}
		});
		
		this.buttonList.add(new GuiButton(nextID(), x+4, y+106, 37, 12, I18n.format("info.email.delete_accept")) {
			public void mouseReleased(int mouseX, int mouseY) {
				EmailMain.net.sendMessageToServer(new MsgDelete.AllReceive());
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+4, y+119, 37, 12, I18n.format("info.email.delete_read")) {
			public void mouseReleased(int mouseX, int mouseY) {
				EmailMain.net.sendMessageToServer(new MsgDelete.AllRead());
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+4, y+132, 37, 12, I18n.format("info.email.dispatch")) {
			public void mouseReleased(int mouseX, int mouseY) {
				EmailMain.net.sendMessageToServer(new MsgOpenGui(EmailGuiHandler.EMAIL_SEND));
			}
		});
		
		this.buttonList.add(new GuiButton(nextID(), x+188, y+106, 35, 12, I18n.format("info.email.delete")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(currentMsg != -1) {
					EmailMain.net.sendMessageToServer(new MsgDelete.Delete(currentMsg));
				}
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+188, y+119, 35, 12, I18n.format("info.email.accept")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(currentMsg != -1) {
					if(container.getEmailSize()+55 >= 2097152L && !EmailUtils.isInfiniteSize()) {
						emailIsOutStorageSize();
						return;
					}
					EmailMain.net.sendMessageToServer(new MsgReceive.Receive(currentMsg));
				}
			}
		});

		this.buttonList.add(new GuiButton(nextID(), x+188, y+145, 35, 12, I18n.format("info.email.accept_all")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(!EmailUtils.isInfiniteSize()) {
					if(container.getEmailSize()+(EmailMain.getUn(container.getInbox(), "accept") * 55) >= 2097152L) {
						emailIsOutStorageSize();
						return;
					}
				}
				EmailMain.net.sendMessageToServer(new MsgReceive.All());
			}
		});
		this.buttonList.add(new GuiButton(nextID(), x+188, y+132, 35, 12, I18n.format("info.email.read_all")) {
			public void mouseReleased(int mouseX, int mouseY) {
				if(!EmailUtils.isInfiniteSize()) {
					if(container.getEmailSize()+(EmailMain.getUn(container.getInbox(), "read") * 51) > 2097152L) {
						emailIsOutStorageSize();
						return;
					}
				}
				
				EmailMain.net.sendMessageToServer(new MsgRead.All());
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
		for(int i = 0; i < 5; i++) {
			if(this.showMsg == null || i >= this.showMsg.length) break;
			if(EmailGuiHandler.isInRange(mouseX, mouseY, this.getGuiLeft() + Candidate_Email_X, this.getGuiTop() + Candidate_Email_Y + ((17 * i)), 54, 15)) {
				String curren = Integer.toString(this.showMsg[i]);
				if(this.container.getInbox().has(curren)) {
					JsonObject msg = this.container.getInbox().get(curren).getAsJsonObject();
					List<String> tip = Lists.newArrayList();
					
					tip.add(I18n.format(msg.get("title").getAsString()));
					tip.add("");
					tip.add(TextFormatting.GRAY + msg.get("time").getAsString());
					tip.add(I18n.format("info.email.main.from", I18n.format(msg.get("sender").getAsString())));
					
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
		}else if(EmailGuiHandler.isInRange(x, y, 213, 53, 8, 28) || EmailGuiHandler.isInRange(x, y, 87, 30, 125, 74)) {
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
		if(this.size == -1) this.size = this.container.getInbox().size()-(this.container.getInbox().has("dev") ? 1 : 0);
		if(this.msgKeyMap == null && this.size > 0) {
			int i = 0;
			this.msgKeyMap = new int[this.size];
			for(Entry<String, JsonElement> msg : this.container.getInbox().entrySet()) {
				if(msg.getKey().equals("dev")) {
					continue;
				}
				this.msgKeyMap[i] = Integer.parseInt(msg.getKey());
				i+=1;
			}
			this.msgKeyMap = this.reverseOrder(this.msgKeyMap);
			this.msgPage = 0;
			this.goMsg(0);
		}
		if(this.size != this.container.getInbox().size()-1) {
			this.size = this.container.getInbox().size()-1;
			int i = 0;
			this.msgKeyMap = new int[this.size];
			for(Entry<String, JsonElement> msg : this.container.getInbox().entrySet()) {
				if(msg.getKey().equals("dev")) {
					continue;
				}
				this.msgKeyMap[i] = Integer.parseInt(msg.getKey());
				i+=1;
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
		
		super.fontRenderer.drawString(I18n.format("info.email.storage"), 5, 148, Color.BLACK.getRGB());
		super.fontRenderer.drawString(Long.toString(this.container.getEmailSize()), 28, 144, Color.BLACK.getRGB());
		for(int i = 0; i < 9; i++) {
			super.fontRenderer.drawString("_", 27+(i*3), 145, Color.BLACK.getRGB());
		}
		
		super.fontRenderer.drawString((EmailUtils.isInfiniteSize() ? "Infinite" : "2097183"), 28, 152, Color.BLACK.getRGB());
		super.fontRenderer.drawString("Bytes", 56.3F, 152.0F, Color.BLACK.getRGB(), false);
		
		if(this.container.getInbox() == null || this.container.getInbox().size() < 1) return;
		this.init();
		if(this.showMsg == null) return;
		int unRead = 0;
		int unReceive = 0;
		for(Entry<String, JsonElement> msgs : this.container.getInbox().entrySet()) {
			if(msgs.getKey().equals("dev")) {
				continue;
			}
			JsonObject msg = msgs.getValue().getAsJsonObject();
			if(!msg.has("read")) {
				unRead++;
			}
			if(msg.has("items") && ! msg.has("accept")) {
				unReceive++;
			}
		}
		
		super.fontRenderer.drawString(I18n.format("info.email.un_read") + ": "+ unRead, 5, 6, Color.BLACK.getRGB());
		super.fontRenderer.drawString(I18n.format("info.email.un_accept") + ": " + unReceive, 38, 6, Color.BLACK.getRGB());
		
		for (int i = 0; i < this.showMsg.length; i++) {
			String id = Integer.toString(this.showMsg[i]);
			if(this.container.getInbox().has(id)) {
				JsonObject msg = this.container.getInbox().get(id).getAsJsonObject();
				
				if(msg.has("items")) {
					super.fontRenderer.drawString("$", Candidate_Email_X+44, Candidate_Email_Y + (17 * i), msg.has("accept") ? Color.GREEN.getRGB() :  Color.RED.getRGB());
				}
				super.fontRenderer.drawString("*", Candidate_Email_X+49, Candidate_Email_Y + (17 * i), msg.has("read") ? Color.GREEN.getRGB() : Color.RED.getRGB());
				
				super.drawCenteredString(fontRenderer, id, Candidate_Email_X-9, Candidate_Email_Y + 3 + (17 * i), Color.WHITE.getRGB());
				
				String sender = I18n.format(msg.get("sender").getAsString());
				if(this.fontRenderer.getStringWidth(sender) > EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender) {
					sender = this.fontRenderer.trimStringToWidth(sender, EmailConfigs.Main.Number_Of_Words.Candidate_Email.Sender) + "...";
				}
				super.drawString(fontRenderer, sender, Candidate_Email_X+1, Candidate_Email_Y + (17 * i), Color.WHITE.getRGB());
				super.fontRenderer.drawString(msg.get("time").getAsString().substring(2), Candidate_Email_X+1, Candidate_Email_Y + 7 + (17 * i), Color.BLACK.getRGB());
			}
		}
		
		if(this.currentMsg >= 0) {
			String curren = Integer.toString(this.currentMsg);
			if(this.container.getInbox().has(curren)) {
				JsonObject msg = this.getCurrentMsg();
				
				String sender = I18n.format(msg.get("sender").getAsString());
				if(this.fontRenderer.getStringWidth(sender) > EmailConfigs.Main.Number_Of_Words.Current_Email.Sender) {
					sender = this.fontRenderer.trimStringToWidth(sender, EmailConfigs.Main.Number_Of_Words.Current_Email.Sender) + "...";
				}
				String title = I18n.format(msg.get("title").getAsString());
				if(this.fontRenderer.getStringWidth(title) > EmailConfigs.Main.Number_Of_Words.Current_Email.Title) {
					title = this.fontRenderer.trimStringToWidth(title, EmailConfigs.Main.Number_Of_Words.Current_Email.Title) + "...";
				}
				super.fontRenderer.drawString(title, EmailConfigs.Main.Position.Current_Email.Title.X, EmailConfigs.Main.Position.Current_Email.Title.Y, Color.WHITE.getRGB());
				
				super.fontRenderer.drawString(sender, EmailConfigs.Main.Position.Current_Email.Sender.X, EmailConfigs.Main.Position.Current_Email.Sender.Y, Color.WHITE.getRGB());
				super.fontRenderer.drawString(msg.get("time").getAsString(), EmailConfigs.Main.Position.Current_Email.Title.Time.X, EmailConfigs.Main.Position.Current_Email.Title.Time.Y, Color.WHITE.getRGB());
				
				super.drawCenteredString(fontRenderer, Integer.toString(this.currentMsg), EmailConfigs.Main.Position.Current_Email.MsgID.X, EmailConfigs.Main.Position.Current_Email.MsgID.Y, Color.WHITE.getRGB());
				
				for(int i = 0; i < currentText.length; i++) {
					String line = Integer.toString(this.currentText[i]+1);
					super.fontRenderer.drawString(line, (EmailConfigs.Main.Position.Current_Email.Row.X - fontRenderer.getStringWidth(line) / 2),  EmailConfigs.Main.Position.Current_Email.Row.Y + (this.showSelectedSpacing * i), Color.BLACK.getRGB());
					super.fontRenderer.drawString(this.showText.get(this.currentText[i]),  EmailConfigs.Main.Position.Current_Email.Msg.X, EmailConfigs.Main.Position.Current_Email.Msg.Y + (this.showSelectedSpacing * i), Color.BLACK.getRGB());
				}
				
				if(msg.has("items")) {
					if(this.container.isEmpty() || this.container.getCurrenMsg() != this.currentMsg) {
						List<ItemStack> stacks = JsonToStackUtil.toStacks(msg.get("items"));
						if(stacks != null)this.container.putStack(stacks);
					}
				}else {
					if(!this.container.isEmpty()) {
						this.container.clear();
					}
				}
				if(msg.has("sound")) {
					GlStateManager.popMatrix();
					GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
					this.mc.getTextureManager().bindTexture(BackGround);
					int x = (this.width - this.xSize) / 2;
					int y = (this.height - this.ySize) / 2;
					this.drawTexturedModalRect(x+208, y+4, 227, 0, 13, 12);
					GlStateManager.pushMatrix();
				}
			}
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
	
	private ISound currentSound;
	
	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		this.stopSound();
	}
	
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if(this.container.getInbox()==null || this.showMsg == null || this.showMsg == null) return;
		if(this.currentMsg >=0
		&& this.getCurrentMsg().has("sound")
		&& EmailGuiHandler.isInRange(mouseX, mouseY, this.getGuiLeft() + 208, this.getGuiTop() + 4, 13, 12)) {
			if(this.currentSound==null) {
				this.currentSound = new EmailSenderSndSound(EmailSound.from(this.getCurrentMsg().get("sound").getAsJsonObject()));
				this.mc.getSoundHandler().playSound(this.currentSound);
				this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				return;
			}
		}
		for(int i = 0; i < 5; i++) {
			if(EmailGuiHandler.isInRange(mouseX, mouseY, this.getGuiLeft() + Candidate_Email_X, this.getGuiTop() + Candidate_Email_Y + (17 * i) - 1, 54, 15)) {
				if(i >= this.showMsg.length) break;
				JsonElement msgE = this.container.getInbox().get(Integer.toString(this.showMsg[i]));
				if(msgE != null) {
					JsonObject msg =  msgE.getAsJsonObject();
					if(!msg.getAsJsonObject().has("read")) {
						if(this.container.getEmailSize()+51 >= 2097152L && !EmailUtils.isInfiniteSize()) {
							emailIsOutStorageSize();
							return;
						}
						EmailMain.net.sendMessageToServer(new MsgRead(this.showMsg[i]));
					}
					this.clearRenderText();
					this.mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					this.currentMsg = this.showMsg[i];
					this.stopSound();
					this.container.setCurrenMsg(this.currentMsg);
					String curren = Integer.toString(this.currentMsg);
					if(this.container.getInbox().has(curren)) {
						if(msg.has("msgs")) {
							this.long_line_winding(msg.get("msgs").getAsJsonArray());
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
	}
	
	private void long_line_winding(JsonArray msgs) {
		this.showText.clear();
		for(int i = 0; i < msgs.size(); i++) {
			String msg = I18n.format(msgs.get(i).getAsString());
			if(this.fontRenderer.getStringWidth(msg) > EmailConfigs.Main.Number_Of_Words.Current_Email.Message) {
				char[] chs = msg.toCharArray();
				StringBuilder s = new StringBuilder();
				for(int k = 0; k < chs.length; k++) {
					s.append(chs[k]);
					String formatStr = s.toString();
					if(this.fontRenderer.getStringWidth(formatStr) > EmailConfigs.Main.Number_Of_Words.Current_Email.Message) {
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
	protected JsonObject getCurrentMsg() {
		return this.container.getInbox().get(String.valueOf(this.currentMsg)).getAsJsonObject();
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
