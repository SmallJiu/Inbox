package cat.jiu.email.ui.gui.component;

import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.util.EmailUtils;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.List;

public class GuiButtonPopupMenu extends Screen {
    protected boolean visible = false,
                    resetBtnWeight = true;
    public final Scroll<List<Button>> scroll;
    private IDrawEvent event;

    public GuiButtonPopupMenu() {
        this(Lists.newArrayList());
    }

    public GuiButtonPopupMenu(List<Button> buttons) {
        super(ITextComponent.getTextComponentOrEmpty(null));
        this.scroll = new Scroll<>(buttons);
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

        if (visible) {
            int btnY = this.createY;
            int width = getWidth();
            for (Button btn : this.scroll.collection) {
                btn.visible = visible;
                if (this.resetBtnWeight) {
                    btn.setWidth(width);
                }
                btn.x = this.createX;
                btn.y = btnY;
                btnY += btn.getHeight();
            }
        }
    }

    public boolean isVisible() {
        return visible && !this.scroll.collection.isEmpty();
    }

    public void drawPopupMenu(MatrixStack stack, int x, int y, float partialTicks) {
        if (this.isVisible()) {
            int btnY = this.createY;
            for (int i : this.scroll.getShows()) {
                Button btn = this.scroll.collection.get(i);
                btn.y = btnY;
                btn.render(stack, x, y, partialTicks);
                if (this.event != null) {
                    this.event.draw(btn);
                }
//                if (isInRange(x,y, btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight())) {
//                    stack.hLine(btn.getX(), btn.getX()+btn.getWidth()-2, btn.getY(), Color.WHITE.getRGB());
//
//                    stack.vLine(btn.getX(), btn.getY(), btn.getY()+btn.getHeight(), Color.WHITE.getRGB());
//                    stack.vLine(btn.getX()+btn.getWidth()-2, btn.getY(), btn.getY()+btn.getHeight(), Color.WHITE.getRGB());
//                }

                btnY += btn.getHeight();
            }
//            Button btn1 = this.scroll.collection.get(this.scroll.getShows()[this.scroll.getShows().length - 1]);
//            stack.hLine(btn1.getX(), btn1.getX() + btn1.getWidth() - 1, btn1.getY() + btn1.getHeight()-1, Color.BLACK.getRGB());

//            for (int i : this.scroll.getShows()) {
//                Button btn = this.scroll.collection.get(i);
//                if (isInRange(x,y, btn.x, btn.y, btn.getWidth(), btn.getHeight())) {
//                    RenderUtils.hLine(stack, btn.x, btn.x+btn.getWidth()-1, btn.y+btn.getHeight()-1, Color.WHITE.getRGB());
//                }
//            }
        }
    }

    public int getHeight() {
        int height = 0;
        for (int show : this.scroll.getShows()) {
            height += this.scroll.collection.get(show).getHeight();
        }
        return height;
    }

    public int getWidth() {
        int width = 0;
        for (Button btn : this.scroll.collection) {
            width = Math.max(width, Math.max(btn.getWidth(), RenderUtils.width(btn.getMessage())));
        }
        return width;
    }

    public boolean mouseClicked(Minecraft mc, int mouseX, int mouseY, int mouseButton) {
        boolean flag = false;
        if (mouseButton == 0 && this.isVisible()) {
            for (Button btn : this.scroll.collection) {
                if (isInRange(mouseX, mouseY, btn.x, btn.y, btn.getWidth(), btn.getHeight())) {
                    btn.playDownSound(mc.getSoundHandler());
                    btn.onClick(mouseX, mouseY);
                    flag = true;
                }
            }
        }
//			this.setVisible(false);
        return flag;
    }

    public  <T extends Button> T addButton(T buttonIn) {
        this.scroll.collection.add(buttonIn);
        this.scroll.init();
        return buttonIn;
    }

    public boolean scroll(int mouseX, int mouseY, int key) {
        if (this.isVisible() && isInRange(mouseX, mouseY, this.createX, this.createY, this.getWidth(), this.getHeight())) {
            int page = 0;

            if (EmailUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || EmailUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
                page += 2;
            }
            if (EmailUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || EmailUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL)) {
                page += 1;
            }
            if (key == 1) {
                this.scroll.go(-1 - page);
                return true;
            } else if (key == -1) {
                this.scroll.go(1 + page);
                return true;
            }
        }
        return false;
    }

    static boolean isInRange(int mouseX, int mouseY, int x, int y, int width, int height) {
        return (mouseX >= x && mouseY >= y) && (mouseX <= x + width && mouseY <= y + height);
    }

    public interface IDrawEvent {
        void draw(Button button);
    }
}
