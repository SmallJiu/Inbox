package cat.jiu.email.ui.gui.component;

import java.awt.Color;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiClickButton extends GuiButton {
	private final IButtonClick event;
	public GuiClickButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, IButtonClick1 event) {
		super(buttonId, x, y, widthIn, heightIn, buttonText);
		this.event = event;
	}
	public GuiClickButton(int buttonId, int x, int y, String buttonText, IButtonClick1 event) {
		super(buttonId, x, y, buttonText);
		this.event = event;
	}
	public GuiClickButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, IButtonClick event) {
		super(buttonId, x, y, widthIn, heightIn, buttonText);
		this.event = event;
	}
	public GuiClickButton(int buttonId, int x, int y, String buttonText, IButtonClick event) {
		super(buttonId, x, y, buttonText);
		this.event = event;
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY) {
		if(this.event!=null) this.event.click(mouseX, mouseY);
	}
	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		super.drawButton(mc, mouseX, mouseY, partialTicks);
		if(this.visible) {
			this.drawHorizontalLine(this.x, this.x + this.width - 2, this.y + this.height-1, Color.BLACK.getRGB());
		}
	}
	public static interface IButtonClick {
		void click(int mouseX, int mouseY);
	}
	public static interface IButtonClick1 extends IButtonClick {
		void click();
		default void click(int mouseX, int mouseY){
			this.click();
		}
	}
}
