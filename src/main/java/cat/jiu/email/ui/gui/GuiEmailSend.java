package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cat.jiu.email.ui.gui.component.GuiButtonPopupMenu;
import cat.jiu.email.ui.gui.component.GuiPopupMenu;
import cat.jiu.email.util.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.input.Keyboard;

import com.google.common.collect.Lists;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.net.msg.MsgOpenGui;
import cat.jiu.email.ui.EmailGuiHandler;
import cat.jiu.email.ui.container.ContainerEmailSend;
import cat.jiu.email.ui.gui.component.GuiImageButton;
import cat.jiu.email.ui.gui.component.GuiTime;
import cat.jiu.email.util.client.ShowInboxGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class GuiEmailSend extends GuiContainer {
	public static final ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_send.png");
	public static final ResourceLocation EXPIRATION = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_expiration.png");
	private GuiTextField nameField, titleField;
    private final GuiTextField[] textFields;
    private final ContainerEmailSend container;
    private final GuiTime expiration = new GuiTime(false);
	private final GuiButtonPopupMenu addresseeHistory = new GuiButtonPopupMenu().setResetBtnWeight(false);
	private GuiButton addresseeHistoryBtn;
    
	public GuiEmailSend(World world, EntityPlayer player) {
		super(new ContainerEmailSend(world, player));
		this.container = (ContainerEmailSend) super.inventorySlots;
		this.xSize = 176;
		this.ySize = 233;
		this.addresseeHistory.scroll.setShowCount(5);
		this.textFields = new GuiTextField[5];
	}
	
	@Override
	public void initGui() {
		super.initGui();
		this.nameField = new NameGuiTextField(1, this.fontRenderer, this.getGuiLeft() + 39, this.getGuiTop() + 6, 95, 11);
        this.nameField.setTextColor(-1);
        this.nameField.setDisabledTextColour(-1);
        this.nameField.setMaxStringLength(100);
        this.nameField.setEnableBackgroundDrawing(false);
        
        this.titleField = new GuiTextField(2, this.fontRenderer, this.getGuiLeft() + 39, this.getGuiTop() + 20, 109, 11);
        this.titleField.setTextColor(-1);
        this.titleField.setDisabledTextColour(-1);
        this.titleField.setMaxStringLength(100);
        this.titleField.setEnableBackgroundDrawing(false);
        this.initText();

		this.addresseeHistory.setWorldAndResolution(this.mc, this.width, this.height);
		this.addresseeHistory.scroll.collection.clear();
		this.addresseeHistory.setCreatePoint(this.nameField.x - this.guiLeft, this.nameField.y + this.nameField.height - this.guiTop);

		{
			File jsonFile = new File(EmailAPI.globalEmailListPath);
			if(jsonFile.exists()) {
				JsonElement e = JsonUtil.parse(jsonFile);
				if(e != null && e.isJsonObject()) {
					JsonObject json = e.getAsJsonObject();
					if (json.has("history")) {
						JsonArray array = json.getAsJsonArray("history");
						for (int i = 0; i < array.size(); i++) {
							this.addresseeHistory.addButton(new GuiButton(
									i, 0, 0, this.nameField.width, this.nameField.height, array.get(i).getAsString()){
								@Override
								public void mouseReleased(int mouseX, int mouseY) {
									nameField.setText(this.displayString);
								}
							});
						}
						this.addresseeHistory.setVisible(this.addresseeHistory.isVisible());
					}
				}
			}
		}

        this.buttonList.clear();
        int x = super.guiLeft;
        int y = super.guiTop;
        
        this.addButton(new GuiImageButton(this, 3, x + 149 + 28, y + 3, 16, 16, I18n.format("email.config.expiration"), EXPIRATION, 256, 256, 256, 256) {
        	public void mouseReleased(int mouseX, int mouseY) {
        		expiration.setEnable(!expiration.isEnable());
        	}
        }).setImageWidth(16).setImageHeight(16);
        
        GuiImageButton sendBtn = this.addButton(new GuiImageButton(this, 4, x + 149, this.nameField.y-2, I18n.format("info.email.dispatch"), BackGround,  256, 256, 176, 9, 59, 50) {
        	public void mouseReleased(int mouseX, int mouseY) {
        		onSend();
        	}
        }).setImageWidth(22).setImageHeight(this.nameField.height+2);
        
        this.addButton(new GuiImageButton(this, 4, sendBtn.x, this.titleField.y-2, I18n.format("info.email.name"), ShowInboxGui.inbox, 23, 15, 23, 15) {
        	public void mouseReleased(int mouseX, int mouseY) {
        		EmailMain.net.sendMessageToServer(new MsgOpenGui(EmailGuiHandler.EMAIL_MAIN));
        	}
        }).setImageWidth(22).setImageHeight(this.nameField.height+2);

		this.addresseeHistoryBtn = this.addButton(new GuiImageButton(this, 5, this.nameField.x + this.nameField.width + 3, this.nameField.y, I18n.format("info.email.history"), BackGround,  256, 256, 194, 0, 9, 9) {
			public void mouseReleased(int mouseX, int mouseY) {
				addresseeHistory.setVisible(!addresseeHistory.isVisible());
			}
		}).setImageWidth(9).setImageHeight(9);
	}

	public void addAddresseeHistory(String name) {
		EmailAPI.addAddresseeHistory(name);
		boolean has = false;
		for (int i = 0; i < this.addresseeHistory.buttons.size(); i++) {
			if (this.addresseeHistory.buttons.get(i).displayString.equals(name)) {
				has = true;
				break;
			}
		}
		if (!has) {
			if (this.addresseeHistory.buttons.size() >= EmailConfigs.Send.Send_History_Max_Count) {
				this.addresseeHistory.buttons.remove(0);
			}
			this.addresseeHistory.addButton(new GuiButton(
					this.addresseeHistory.buttons.size(), 0, 0, this.nameField.width, this.nameField.height, name){
				@Override
				public void mouseReleased(int mouseX, int mouseY) {
					nameField.setText(this.displayString);
				}
			});
		}
	}
	
	private void initText() {
		int id = 5;
		for(int i = 0; i < this.textFields.length; i++) {
			this.textFields[i] = new GuiTextField(id++, this.fontRenderer, super.guiLeft+9, super.guiTop+34 + (12 * i), 155, 12);
			GuiTextField field = this.textFields[i];
			field.setTextColor(Color.WHITE.getRGB());
			field.setDisabledTextColour(Color.WHITE.getRGB());
			field.setMaxStringLength(256);
			field.setEnableBackgroundDrawing(false);
		}
	}
	
	public boolean textsIsEmpty() {
		for (GuiTextField textField : this.textFields) {
			if (!textField.getText().isEmpty()) return false;
		}
		return true;
	}
	
	public void setRenderText(String text) {
		this.setRenderText(text, Color.RED);
	}
	public void setRenderText(String text, Color color) {
		this.setRenderText(text, color, EmailUtils.parseTick(0,0,0,15, 0));
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
	
	private long renderTicks = 0;
	private String renderText;
	private Color renderColor;
	
	private int currenTextField = -1;
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
        
		this.titleField.drawTextBox();
        this.nameField.drawTextBox();
        for(GuiTextField tf : this.textFields) {
        	tf.drawTextBox();
        	Color color = Color.WHITE;
        	if(GuiEmailMain.isInRange(mouseX, mouseY, tf.x, tf.y, tf.width, tf.height)) {
        		color = Color.ORANGE;
        	}
    		this.fontRenderer.drawString(tf.getText().length()+"/"+tf.getMaxStringLength(), tf.x+tf.width+13, tf.y+2, color.getRGB(), true);
        }
        int x = super.guiLeft;
        int y = super.guiTop;
        
        this.expiration.drawTimeBox(x + 149 + 22 + 10, y + 20 + 80);
        
        if(EmailConfigs.Send.Enable_Inbox_Button) {
    		if(GuiEmailMain.isInRange(mouseX, mouseY, x+149, y+20, 22, 10)) {
    			super.drawHoveringText(TextFormatting.RED + I18n.format("info.email.send.warn"), mouseX, y+48);
    		}
        }
		super.renderHoveredToolTip(mouseX, mouseY);

//		List<String> list = new ArrayList<>();
//		for (int i = 0; i < this.addresseeHistory.getButtonSize(); i++) {
//			list.add(this.addresseeHistory.getPopupButton(i).displayString);
//		}
//		super.drawHoveringText(list, 0, 0);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		int x = super.guiLeft;
		int y = super.guiTop;
		
		GlStateManager.popMatrix();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(BackGround);
		super.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
		GlStateManager.pushMatrix();
		
		int color = this.container.isLock() ? Color.RED.getRGB() : Color.GREEN.getRGB(); 
		super.drawGradientRect(x + 162, y + 100, x + 162 + 9, y + 100 + 9, color, color);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		 GlStateManager.pushMatrix();
         GlStateManager.enableBlend();
         
         EmailUtils.drawAlignRightString(this.fontRenderer, I18n.format("info.email.addressee") + ":", 36, 6, (GuiEmailMain.isInRange(mouseX, mouseY, this.nameField.x, this.nameField.y, this.nameField.width, this.nameField.height) ? Color.ORANGE : Color.WHITE).getRGB(), true);
         EmailUtils.drawAlignRightString(this.fontRenderer, I18n.format("info.email.title") + ":", 36, 21, (GuiEmailMain.isInRange(mouseX, mouseY, this.titleField.x, this.titleField.y, this.titleField.width, this.titleField.height) ? Color.ORANGE : Color.WHITE).getRGB(), true);
		 
		 if(this.renderTicks > 0 && this.renderText != null && this.renderColor != null) {
			 if(this.renderTicks <= 0) clearRenderText();
			 super.fontRenderer.drawString(this.renderText, (88 - fontRenderer.getStringWidth(this.renderText) / 2), 138, this.renderColor.getRGB());
	         this.renderTicks--;
		 }
		 if(this.container.renderTicks > 0 && this.container.renderText != null && this.container.renderColor != null) {
			 if(this.container.renderTicks <= 0) this.clearRenderText();
			 super.fontRenderer.drawString(this.container.renderText, (88 - fontRenderer.getStringWidth(this.container.renderText) / 2), 138, this.container.renderColor.getRGB());
			 this.container.renderTicks--;
		 }
		 if(this.container.isCooling()) {
			 fontRenderer.drawString(I18n.format("info.email.cooling", EmailUtils.formatTimestamp(this.container.getCoolingMillis() - System.currentTimeMillis())), 45, 60, Color.RED.getRGB());
		 }
		 if(this.titleField.getText().isEmpty()) {
			 this.fontRenderer.drawString(I18n.format("info.email.default_title"), 39, 21, Color.BLACK.getRGB());
		 }
		 if(this.textsIsEmpty()) {
			 this.fontRenderer.drawString(I18n.format("info.email.default_msg"), 10, 34, Color.BLACK.getRGB());
		 }
		 GlStateManager.disableBlend();
         GlStateManager.popMatrix();
		this.addresseeHistory.drawPopupMenu(this.mc, mouseX-this.guiLeft, mouseY-this.guiTop, 0);
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		this.addresseeHistory.scroll(this);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if(this.container.isLock()) return;

		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (!this.addresseeHistoryBtn.mousePressed(this.mc, mouseX, mouseY) && this.addresseeHistory.isVisible()) {
			this.addresseeHistory.mouseClicked(mc, mouseX-this.guiLeft, mouseY-this.guiTop, mouseButton);
			this.addresseeHistory.setVisible(false);
			return;
		}

		this.expiration.mouseClick(mouseX, mouseY, mouseButton);

		boolean lag = false;
		lag = this.nameField.mouseClicked(mouseX, mouseY, mouseButton);
		if(lag) this.currenTextField = 0;

		lag = this.titleField.mouseClicked(mouseX, mouseY, mouseButton);
		if(lag) this.currenTextField = 1;
		for(int i = 0; i < this.textFields.length; i++) {
			lag = this.textFields[i].mouseClicked(mouseX, mouseY, mouseButton);
			if(lag) this.currenTextField = 1 + i + 1;
		}
	}

	private GuiTextField getTextField(int id) {
		switch(id) {
			case 0: return this.nameField;
			case 1: return this.titleField;
			case 2: return this.textFields[0];
			case 3: return this.textFields[1];
			case 4: return this.textFields[2];
			case 5: return this.textFields[3];
			case 6: return this.textFields[4];
			default: return null;
		}
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if(this.container.isLock() && (keyCode == Keyboard.KEY_E || keyCode == Keyboard.KEY_ESCAPE)) return;

		this.expiration.keyTyped(typedChar, keyCode);

		boolean flag = false;
		boolean lag = false;
		lag = this.nameField.textboxKeyTyped(typedChar, keyCode);
		if(lag) flag = true;
		lag = this.titleField.textboxKeyTyped(typedChar, keyCode);
		if(lag) flag = true;
		for(GuiTextField textField : this.textFields) {
			lag = textField.textboxKeyTyped(typedChar, keyCode);
			if(lag) flag = true;
		}

		if(keyCode == Keyboard.KEY_UP) {
			GuiTextField curren = this.getTextField(this.currenTextField-1);
			if(curren != null) {
				this.mouseClicked(curren.x + curren.width - 1, curren.y + curren.height - 1, 0);
				flag = true;
			}
		}

		if(keyCode == Keyboard.KEY_DOWN) {
			GuiTextField curren = this.getTextField(this.currenTextField+1);
			if(curren != null) {
				this.mouseClicked(curren.x + curren.width - 1, curren.y + curren.height - 1, 0);
				flag = true;
			}
		}

		if(!flag) {
			super.keyTyped(typedChar, keyCode);
		}
	}

	void onSend(){
		if(!container.isCooling() && !container.isLock()) {
			String name = nameField.getText();
			if(name.isEmpty()) {
				setRenderText(I18n.format("info.email.error.empty_name"), Color.RED);
				return;
			}
			String title = titleField.getText();
			if(title.isEmpty()) {
				title = "info.email.default_title";
			}

			if(textsIsEmpty() && container.isEmpty()) {
				setRenderText(I18n.format("info.email.error.empty_msgs_item"), Color.RED);
				return;
			}

			List<IText> msgs = Lists.newArrayList();
			if(!textsIsEmpty()) {
				for (GuiTextField textField : textFields) {
					if (!textField.getText().isEmpty()) {
						msgs.add(new Text(textField.getText()));
					}
				}
			}else {
				msgs.add(new Text("info.email.default_msg"));
			}
			Email email = new Email(new Text(title), new Text(Minecraft.getMinecraft().player.getName()), null, null, msgs);

			long expiration = GuiEmailSend.this.expiration.getTimeOfMillis();
			if(expiration>0) {
				email.setExpirationTime(new TimeMillis(expiration));
			}

			Email email_t = email.copy();
			if(!container.isEmpty()) {
				container.toItemList(true).forEach(email_t::addItem);
			}
			if(!EmailConfigs.isInfiniteSize()){
				SizeReport report = EmailUtils.checkEmailSize(email_t);
				if(!SizeReport.SUCCESS.equals(report)) {
					if (container.isLock()) container.setLock(false);
					setRenderText(I18n.format("info.email.error.send.to_big", report.slot, report.size), Color.RED);
					return;
				}
			}
			EmailAPI.sendPlayerEmail(mc.player, name, email);
			clearRenderText();
		}
	}

	@SideOnly(Side.CLIENT)
	public static class NameGuiTextField extends GuiTextField {
		public NameGuiTextField(int componentId, FontRenderer fontrendererObj, int x, int y, int par5Width, int par6Height) {
			super(componentId, fontrendererObj, x, y, par5Width, par6Height);
		}
		
		private int index = 0;
		@Override
		public boolean textboxKeyTyped(char typedChar, int keyCode) {
			boolean lag = super.textboxKeyTyped(typedChar, keyCode);
			if(!lag && this.isFocused() && keyCode == Keyboard.KEY_TAB) {
				List<EntityPlayer> playerList = Minecraft.getMinecraft().player.world.playerEntities;
				String name;
				
				if(this.index < playerList.size()) {
					name = playerList.get(this.index).getName();
					if(name.equals(Minecraft.getMinecraft().player.getName())
					&& EmailConfigs.Send.Enable_Send_To_Self) {
						super.setText(name);
						this.index++;
						return lag;
					}
					super.setText(name);
					lag = true;
					this.index++;
				}else if(this.index < playerList.size() + 2) {
					this.absIndex++;
					if(this.absIndex==1) {
						super.setText("@p");
					}else if(this.absIndex==2) {
						super.setText("@a");
					}
					if(this.absIndex >= 2) {
						this.absIndex = 0;
						this.index = 0;
					}
				}
			}
			return lag;
		}
		
		protected int absIndex = 0;
		
		@Override
		public void drawTextBox() {
			super.drawTextBox();
			if("@p".equals(this.getText())) {
				EmailUtils.drawAlignRightString(Minecraft.getMinecraft().fontRenderer, I18n.format("info.email.@p"), this.x + this.width - 2, this.y, Color.BLACK.getRGB(), false);
			}else if("@a".equals(this.getText())) {
				EmailUtils.drawAlignRightString(Minecraft.getMinecraft().fontRenderer, I18n.format("info.email.@a"), this.x + this.width - 2, this.y, Color.BLACK.getRGB(), false);
			}
		}
	}
}
