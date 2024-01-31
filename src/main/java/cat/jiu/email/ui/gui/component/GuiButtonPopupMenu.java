package cat.jiu.email.ui.gui.component;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.util.List;

public class GuiButtonPopupMenu extends GuiScreen {
    public final List<GuiButton> buttons;
    protected boolean visible = false,
                    resetBtnWeight = true;
    public final Scroll<List<GuiButton>> scroll;
    private IDrawEvent event;

    public GuiButtonPopupMenu() {
        this(Lists.newArrayList());
    }

    public GuiButtonPopupMenu(List<GuiButton> buttons) {
        this.buttons = buttons;
        this.scroll = new Scroll<>(this.buttons);
    }

    public GuiButtonPopupMenu setResetBtnWeight(boolean resetBtnWeight) {
        this.resetBtnWeight = resetBtnWeight;
        return this;
    }

    public GuiButtonPopupMenu setDrawEvent(IDrawEvent event) {
        this.event = event;
        return this;
    }

    int createX = 0;
    int createY = 0;

    public void setCreatePoint(int createX, int createY) {
        this.createX = createX;
        this.createY = createY;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;

        int btnY = this.createY;
        int width = getWidth();
        for (GuiButton btn : buttons) {
            btn.visible = visible;
            if (visible) {
                if (this.resetBtnWeight){
                    btn.width = width;
                }
                btn.x = this.createX;
                btn.y = btnY;
                btnY += btn.height;
            }
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void drawPopupMenu(Minecraft mc, int x, int y, float partialTicks) {
        if (this.visible) {
            int btnY = this.createY;
            for (int i : this.scroll.getShows()) {
                GuiButton btn = this.buttons.get(i);
                btn.y = btnY;
                btn.drawButton(mc, x, y, partialTicks);
                if (this.event != null) {
                    this.event.draw(btn);
                }
                btnY += btn.height;
            }
            GuiButton btn = this.buttons.get(this.scroll.getShows()[this.scroll.getShows().length - 1]);
            this.drawHorizontalLine(btn.x, btn.x + btn.width - 2, btn.y + btn.height, Color.BLACK.getRGB());
        }
    }

    public int getHeight() {
        int height = 0;
        for (int show : this.scroll.getShows()) {
            height += this.buttons.get(show).height;
        }
        return height;
    }

    public int getWidth() {
        int width = 0;
        for (GuiButton btn : this.buttons) {
            width = Math.max(width, btn.width);
        }
        return width;
    }

    public boolean mouseClicked(Minecraft mc, int mouseX, int mouseY, int mouseButton) {
        boolean flag = false;
        if (mouseButton == 0 && this.isVisible()) {
            for (GuiButton btn : this.buttons) {
                if (btn.mousePressed(mc, mouseX, mouseY)) {
                    GuiScreenEvent.ActionPerformedEvent.Pre event = new GuiScreenEvent.ActionPerformedEvent.Pre(this, btn, this.buttons);
                    if (MinecraftForge.EVENT_BUS.post(event))
                        break;
                    btn = event.getButton();
                    btn.playPressSound(mc.getSoundHandler());
                    btn.mouseReleased(mouseX, mouseY);
                    flag = true;
                    MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.ActionPerformedEvent.Post(this, btn, this.buttons));
                }
            }
        }
//			this.setVisible(false);
        return flag;
    }

    @Override
    public <T extends GuiButton> T addButton(T buttonIn) {
        this.buttons.add(buttonIn);
        this.scroll.init();
        return buttonIn;
    }

    public boolean scroll(GuiContainer gui) {
        int key = Mouse.getEventDWheel();
        int x = (Mouse.getEventX() * gui.width / gui.mc.displayWidth) - gui.getGuiLeft();
        int y = (gui.height - Mouse.getEventY() * gui.height / gui.mc.displayHeight - 1) - gui.getGuiTop();
        return this.scroll(x,y,key);
    }

    public boolean scroll(int mouseX, int mouseY, int key) {
        if (this.visible && isInRange(mouseX, mouseY, this.createX, this.createY, this.getWidth(), this.getHeight())) {
            int page = 0;

            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                page += 2;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
                page += 1;
            }
            if (key == 120) {
                this.scroll.go(-1 - page);
                return true;
            } else if (key == -120) {
                this.scroll.go(1 + page);
                return true;
            }
        }
        return false;
    }

    static boolean isInRange(int mouseX, int mouseY, int x, int y, int width, int height) {
        return (mouseX >= x && mouseY >= y) && (mouseX <= x + width && mouseY <= y + height);
    }

    public static interface IDrawEvent {
        void draw(GuiButton button);
    }
}
