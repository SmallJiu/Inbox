package cat.jiu.email.ui.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import org.lwjgl.input.Keyboard;

import com.google.common.collect.Lists;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.InboxText;
import cat.jiu.email.iface.IInboxText;
import cat.jiu.email.net.msg.MsgOpenGui;
import cat.jiu.email.ui.EmailGuiHandler;
import cat.jiu.email.ui.container.ContainerEmailSend;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
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

@SideOnly(Side.CLIENT)
public class GuiEmailSend extends GuiContainer {
	public static final ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/email_send.png");
	public static final ResourceLocation ANVIL_RESOURCE = new ResourceLocation("textures/gui/container/anvil.png");
    private GuiTextField nameField;
    private GuiTextField titleField;
    private final GuiTextField[] textFields;
    private int componentId = -1;
    private int nextID() {return componentId++;}
    private final ContainerEmailSend container;
    
	public GuiEmailSend(World world, EntityPlayer player) {
		super(new ContainerEmailSend(world, player));
		this.container = (ContainerEmailSend) super.inventorySlots;
		this.xSize = 176;
		this.ySize = 233;
		this.textFields = new GuiTextField[5];
	}
	
	@Override
	public void initGui() {
		super.initGui();
		this.nameField = new NameGuiTextField(nextID(), this.fontRenderer, this.getGuiLeft() + 39, this.getGuiTop() + 6, 109, 11);
        this.nameField.setTextColor(-1);
        this.nameField.setDisabledTextColour(-1);
        this.nameField.setMaxStringLength(100);
        this.nameField.setEnableBackgroundDrawing(false);
        
        this.titleField = new GuiTextField(nextID(), this.fontRenderer, this.getGuiLeft() + 39, this.getGuiTop() + 20, 109, 11);
        this.titleField.setTextColor(-1);
        this.titleField.setDisabledTextColour(-1);
        this.titleField.setMaxStringLength(100);
        this.titleField.setEnableBackgroundDrawing(false);
        this.initText();
        
        this.buttonList.clear();
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.buttonList.add(new GuiButton(nextID(), x + 149, y + 5, 22, 13, I18n.format("info.email.send")) {
        	public void mouseReleased(int mouseX, int mouseY) {
        		if(!container.isCooling()) {
        			if(container.isLock()) {
            			return;
            		}
            		String name = nameField.getText();
            		if(name==null || name.isEmpty()) {
            			setRenderText(I18n.format("info.email.error.empty_name"), Color.RED);
            			return;
            		}
            		String title = titleField.getText();
            		if(title==null || title.isEmpty()) {
            			title = "info.email.default_title";
            		}
            		
            		if(textsIsEmpty() && container.isEmpty()) {
            			setRenderText(I18n.format("info.email.error.empty_msgs_item"), Color.RED);
            			return;
            		}
            		
            		List<IInboxText> msgs = Lists.newArrayList();
            		if(!textsIsEmpty()) {
            			for(int i = 0; i < textFields.length; i++) {
    						String msg = textFields[i].getText();
    						if(msg.isEmpty()) {
								msgs.add(InboxText.empty);
								continue;
							}
							
							StringBuilder sb = new StringBuilder();
							for(int k = 0; k < msg.length(); k++) {
								char ch = msg.charAt(k);
								if(ch == '&' && k+1 < msg.length()) {
									if(isFormatChar(msg.charAt(k+1))) {
										sb.append('\u00a7');
									}
									sb.append(ch);
								}else {
									sb.append(ch);
								}
							}
							msgs.add(new InboxText(sb.toString()));
    					}
            		}else {
            			msgs.add(new InboxText("info.email.default_msg"));
            		}
            		EmailAPI.sendPlayerEmail(mc.player, name, new Email(new InboxText(title), new InboxText(Minecraft.getMinecraft().player.getName()), null, null, msgs));
            		clearRenderText();
        		}
        	}
        });
        if(EmailConfigs.Send.Enable_Inbox_Button) {
        	this.buttonList.add(new GuiButton(nextID(), x + 149, y + 20, 22, 10, I18n.format("info.email.receiving")) {
            	public void mouseReleased(int mouseX, int mouseY) {
            		if(container.isLock()) {
            			return;
            		}
            		EmailMain.net.sendMessageToServer(new MsgOpenGui(EmailGuiHandler.EMAIL_MAIN));
            	}
            });
        }
	}
	
	private void initText() {
		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;
		for(int i = 0; i < this.textFields.length; i++) {
			this.textFields[i] = new GuiTextField(nextID(), this.fontRenderer, x+9, y+34 + (12 * i), 155, 12);
			
			GuiTextField field = this.textFields[i];
			field.setTextColor(Color.WHITE.getRGB());
			field.setDisabledTextColour(Color.WHITE.getRGB());
			field.setMaxStringLength(256);
			field.setEnableBackgroundDrawing(false);
		}
	}
	
	private boolean isFormatChar(char c) {
		for(TextFormatting format : TextFormatting.values()) {
			if(format.formattingCode == c) return true;
		}
		return false;
	}
	
	public boolean textsIsEmpty() {
		for(int i = 0; i < this.textFields.length; i++) {
			String msg = this.textFields[i].getText();
			if(msg != null && !msg.isEmpty()) return false;
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
	
	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		 GlStateManager.pushMatrix();
         GlStateManager.enableBlend();
         
         this.fontRenderer.drawString(I18n.format("info.email.addressee") + ":", 6, 6, Color.BLACK.getRGB());
		 this.fontRenderer.drawString(I18n.format("info.email.title") + ":", 10, 21, Color.BLACK.getRGB());
		 
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
			 long millis = this.container.getCoolingMillis() - System.currentTimeMillis();
			 long t_0 = millis % 1000;
			 long t_mi = t_0 % 50;
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
			 String mi = t_t < 10 ? "0" + t_t : Long.toString(t_mi);
			 
			 fontRenderer.drawString(I18n.format("info.email.cooling", d, h, m, s, t, mi), 45, 60, Color.RED.getRGB());
		 }
		 if(this.titleField.getText().isEmpty()) {
			 this.fontRenderer.drawString(I18n.format("info.email.default_title"), 39, 21, Color.BLACK.getRGB());
		 }
		 if(this.textsIsEmpty()) {
			 this.fontRenderer.drawString(I18n.format("info.email.default_msg"), 10, 34, Color.BLACK.getRGB());
		 }
		 GlStateManager.disableBlend();
         GlStateManager.popMatrix();
	}
	
	private int currenTextField = -1;
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if(this.container.isLock()) return;
		
        super.mouseClicked(mouseX, mouseY, mouseButton);
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
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		super.renderHoveredToolTip(mouseX, mouseY);
		this.titleField.drawTextBox();
        this.nameField.drawTextBox();
        for(GuiTextField tf : this.textFields) {
        	tf.drawTextBox();
    		this.fontRenderer.drawString(tf.getText().length()+"/"+tf.getMaxStringLength(), tf.x+tf.width+13, tf.y+2, Color.WHITE.getRGB(), true);
        }
        if(EmailConfigs.Send.Enable_Inbox_Button) {
            int x = (this.width - this.xSize) / 2;
            int y = (this.height - this.ySize) / 2;
    		if(GuiEmailMain.isInRange(mouseX, mouseY, x+149, y+20, 22, 10)) {
    			super.drawHoveringText(TextFormatting.RED + I18n.format("info.email.send.warn"), mouseX, y+48);
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
		super.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
		GlStateManager.pushMatrix();
		
		super.drawTexturedModalRect(x + 163, y + 100, 176 + (this.container.isLock() ? 9 : 0), 0, 7, 7);
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
				drawAlignRightString(Minecraft.getMinecraft().fontRenderer, I18n.format("info.email.@p"), this.x + this.width - 2, this.y, Color.BLACK.getRGB(), false);
			}else if("@a".equals(this.getText())) {
				drawAlignRightString(Minecraft.getMinecraft().fontRenderer, I18n.format("info.email.@a"), this.x + this.width - 2, this.y, Color.BLACK.getRGB(), false);
			}
		}
	}
	
	public static void drawAlignRightString(FontRenderer fr, String text, int x, int y, int color, boolean drawShadow) {
		for(int i = text.length(); i > 0; i--) {
			char c = text.charAt(i-1);
			float width = fr.getCharWidth(c);
			x -= width;
			fr.drawString(String.valueOf(c), x, y, color, drawShadow);
		}
	}
}
