package cat.jiu.email.ui.gui.config.entry;

import cat.jiu.email.ui.gui.config.ConfigEntry;
import cat.jiu.email.util.EmailUtils;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.ForgeConfigSpec;

import java.awt.*;

public class BooleanEntry extends ConfigEntry<Boolean> {
    public static final ITextComponent trueText = ITextComponent.getTextComponentOrEmpty(TextFormatting.GREEN + "true"),
                                       falseText = ITextComponent.getTextComponentOrEmpty(TextFormatting.RED + "false");
    private final Button button;
    private boolean cache;
    public BooleanEntry(ForgeConfigSpec.BooleanValue value, ForgeConfigSpec.ValueSpec spec) {
        super(value, spec);
        this.button = this.addWidget(new Button(0,0,154, 20, cache ? trueText : falseText, btn->
            this.setCacheValue(!this.getCacheValue())
        ){
            @Override
            public ITextComponent getMessage() {
                return cache ? trueText : falseText;
            }
        });

        this.button.x = Minecraft.getInstance().getMainWindow().getScaledWidth()/2 - this.button.getWidth()/2 + this.button.getWidth() - this.button.getWidth()/2 - 2;
        this.addUndoAndReset();
    }

    @Override
    protected Widget getConfigWidget() {
        return this.button;
    }

    @Override
    public void render(Screen gui, MatrixStack matrix, int x, int y, int mouseX, int mouseY) {
        this.renderWidget(gui, matrix, x, y, mouseX, mouseY);
        EmailUtils.drawAlignRightString(matrix, this.configName, this.button.x-5, this.button.y+5, Color.WHITE.getRGB(), true);
    }

    @Override
    public void drawHoverText(Screen gui, MatrixStack matrix, int mouseX, int mouseY) {
        try {
            this.drawCommentWithRange(gui, matrix, mouseX, mouseY, this.button.x-5-gui.getMinecraft().fontRenderer.getStringWidth(this.configName), this.button.y+5, gui.getMinecraft().fontRenderer.getStringWidth(this.configName), Minecraft.getInstance().fontRenderer.FONT_HEIGHT);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean mouseClick(double mouseX, double mouseY, int button) {
        return super.mouseClick(mouseX, mouseY, button) || this.button.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected Boolean getCacheValue() {
        return this.cache;
    }

    @Override
    protected void setCacheValue(Boolean newValue) {
        this.cache = newValue;
    }

    @Override
    public void save() {
        this.value.set(this.cache);
    }
}
