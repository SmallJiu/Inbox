package cat.jiu.email.ui.gui.config;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.ForgeConfigSpec;

public abstract class ConfigEntry {

    public abstract void render(MatrixStack matrix, int x, int y, int mouseX, int mouseY);
    public abstract ForgeConfigSpec.ConfigValue<?> getConfigValue();
    public abstract void undo();
    public abstract void reset();
    public abstract boolean isChanged();
    public abstract boolean isDefault();

    public int getWeight(){
        return 1;
    }
    public void save(){
        this.getConfigValue().save();
    }
    public boolean mouseClick(double mouseX, double mouseY, int button){
        return false;
    }
    public boolean charTyped(char codePoint, int modifiers) {return false;}
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public void drawAlignRightString(MatrixStack matrix, String text, int x, int y, int color, boolean drawShadow) {
        FontRenderer fontRenderer = Minecraft.getInstance().fontRenderer;
        for(int i = text.length(); i > 0; i--) {
            if('§' == text.charAt(i-1)) {
                continue;
            }
            if(i-2>=0 && '§' == text.charAt(i-2)) {
                continue;
            }

            String c = String.valueOf(text.charAt(i-1));

            float width = fontRenderer.getStringWidth(c);

            if(i-2 > 0) {
                boolean isColor;
                String s = text.charAt(i-3)+""+text.charAt(i-2);
                for(TextFormatting format : TextFormatting.values()) {
                    isColor = format.toString().equals(s);
                    if(isColor) {
                        c = s + c;
                        width = fontRenderer.getStringWidth(c);
                        break;
                    }
                }
            }

            x -= width;
            if(drawShadow){
                fontRenderer.drawStringWithShadow(matrix, c, x, y, color);
            }else {
                fontRenderer.drawString(matrix, c, x, y, color);
            }
        }
    }
}
