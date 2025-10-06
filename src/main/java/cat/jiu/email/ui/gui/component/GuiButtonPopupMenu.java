package cat.jiu.email.ui.gui.component;

import cat.jiu.email.util.EmailUtils;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class GuiButtonPopupMenu extends AbstractWidget {
    protected boolean visible = false,
                    resetBtnWeight = true;
    public final Scroll<List<Button>> scroll;
    private IDrawEvent event;

    public GuiButtonPopupMenu() {
        this(Lists.newArrayList());
    }

    public GuiButtonPopupMenu(List<Button> buttons) {
        super(0, 0, 0, 0, CommonComponents.EMPTY);
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
    public void setSize(int width, int height) {
        for (Button button : this.scroll.collection) {
            button.setWidth(width);
            button.setHeight(height);
        }
    }

    int createX = 0;
    int createY = 0;

    public void setCreatePoint(int createX, int createY) {
        this.createX = createX;
        this.createY = createY;
        this.reloadPosition();
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) {
            this.reloadPosition();
        }
    }

    public void reloadPosition() {
        int btnY = this.createY;
        int width = getWidth();
        for (Button btn : this.scroll.collection) {
            btn.visible = visible;
            btn.active = visible;
            if (this.resetBtnWeight) {
                btn.setWidth(width);
            }
            btn.setX(this.createX);
            btn.setY(btnY);
            btnY += btn.getHeight();
        }
    }

    public boolean isVisible() {
        return visible && !this.scroll.collection.isEmpty();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.isVisible()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 8000);
            int btnY = this.createY;
            for (int i : this.scroll.getShows()) {
                Button btn = this.scroll.collection.get(i);
                btn.setY(btnY);
                btn.render(graphics, mouseX, mouseY, partialTick);
                if (this.event != null) {
                    this.event.draw(btn);
                }
//                if (isInRange(x,y, btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight())) {
//                    graphics.hLine(btn.getX(), btn.getX()+btn.getWidth()-2, btn.getY(), Color.WHITE.getRGB());
//
//                    graphics.vLine(btn.getX(), btn.getY(), btn.getY()+btn.getHeight(), Color.WHITE.getRGB());
//                    graphics.vLine(btn.getX()+btn.getWidth()-2, btn.getY(), btn.getY()+btn.getHeight(), Color.WHITE.getRGB());
//                }

                btnY += btn.getHeight() + 2;
            }
//            Button btn1 = this.scroll.collection.get(this.scroll.getShows()[this.scroll.getShows().length - 1]);
//            graphics.hLine(btn1.getX(), btn1.getX() + btn1.getWidth() - 1, btn1.getY() + btn1.getHeight()-1, Color.BLACK.getRGB());

//            for (int i : this.scroll.getShows()) {
//                Button btn = this.scroll.collection.get(i);
//                if (isInRange(mouseX, mouseY, btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight())) {
//                    graphics.hLine(btn.getX(), btn.getX()+btn.getWidth()-1, btn.getY()+btn.getHeight()-1, Color.WHITE.getRGB());
//                }
//            }
            graphics.pose().popPose();
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
            width = Math.max(width, Math.max(btn.getWidth(), Minecraft.getInstance().font.width(btn.getMessage())));
        }
        return width;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        boolean flag = false;
        if (mouseButton == 0 && this.isVisible()) {
            for (Button btn : this.scroll.collection) {
                if (isInRange(mouseX, mouseY, btn.getX(), btn.getY(), btn.getWidth(), btn.getHeight())) {
                    btn.playDownSound(Minecraft.getInstance().getSoundManager());
                    btn.onClick(mouseX, mouseY);
                    flag = true;
                }
            }
        }
        return flag;
    }

    public <T extends Button> T addButton(T buttonIn) {
        this.scroll.collection.add(buttonIn);
        this.scroll.init();
        return buttonIn;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double key) {
        if (this.isVisible() && isInRange(mouseX, mouseY, this.createX, this.createY, this.getWidth(), this.getHeight())) {
            int page = 0;

            if (EmailUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || EmailUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT)) {
                page += 2;
            }
            if (EmailUtils.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || EmailUtils.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL)) {
                page += 1;
            }
            if (key > 0) {
                this.scroll.go(-1 - page);
                return true;
            } else if (key < 0) {
                this.scroll.go(1 + page);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }

    static boolean isInRange(double mouseX, double mouseY, int x, int y, int width, int height) {
        return (mouseX >= x && mouseY >= y) && (mouseX <= x + width && mouseY <= y + height);
    }

    public interface IDrawEvent {
        void draw(Button button);
    }
}
