package cat.jiu.email.ui.gui.component;

import cat.jiu.email.util.EmailUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class NameEditbox extends EditBox {
    public NameEditbox(Font font, int x, int y, int par5Width, int par6Height) {
        super(font, x, y, par5Width, par6Height, CommonComponents.EMPTY);
    }

    private int
            index = 0,
            absIndex = 0;

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(this.isFocused()){
            if(keyCode == GLFW.GLFW_KEY_TAB) {
                List<? extends Player> playerList = Minecraft.getInstance().player.level().players();
                if(this.index < playerList.size()) {
                    Component name = playerList.get(this.index).getName();
                    if(name.equals(Minecraft.getInstance().player.getName())) {
                        this.setValue(name.getString());
                        this.index++;
                        return true;
                    }
                    this.setValue(name.getString());
                    this.index++;
                }else {
                    if(EmailUtils.isOP(Minecraft.getInstance().player) && this.index < playerList.size() + 2) {
                        this.absIndex++;
                        if(this.absIndex==1) {
                            this.setValue("@p");
                        }else if(this.absIndex==2) {
                            this.setValue("@a");
                        }
                        if(this.absIndex >= 2) {
                            this.absIndex = 0;
                            this.index = 0;
                        }
                    }else {
                        this.absIndex = 0;
                        this.index = 0;
                    }
                }
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int x, int y, float t) {
        super.renderWidget(graphics, x, y, t);
        if("@p".equals(this.getValue())) {
            EmailUtils.drawAlignRightString(graphics, Minecraft.getInstance().font, I18n.get("info.inbox.@p"), this.getX() + this.width - 2, this.getY(), Color.WHITE.getRGB(), false);
        }else if("@a".equals(this.getValue())) {
            EmailUtils.drawAlignRightString(graphics, Minecraft.getInstance().font, I18n.get("info.inbox.@a"), this.getX() + this.width - 2, this.getY(), Color.WHITE.getRGB(), false);
        }
    }
}