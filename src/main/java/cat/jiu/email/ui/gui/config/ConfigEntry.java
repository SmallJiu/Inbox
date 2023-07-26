package cat.jiu.email.ui.gui.config;

import cat.jiu.email.util.EmailUtils;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ConfigEntry<T> {
    protected final ForgeConfigSpec.ConfigValue<T> value;
    protected final ForgeConfigSpec.ValueSpec spec;
    protected final T defaultValue;
    protected final String configName;
    protected final List<Widget> widgets = new ArrayList<>();
    protected Button undo, reset;

    protected ConfigEntry(ForgeConfigSpec.ConfigValue<T> value, ForgeConfigSpec.ValueSpec spec) {
        this.value = value;
        this.spec = spec;
        if(value!=null) this.setCacheValue(value.get());
        String name;
        if(this.spec==null){
            name = "Sub Entry";
        }else if(this.spec.getTranslationKey()!=null){
            name = I18n.format(this.spec.getTranslationKey());
            if(Objects.equals(name, this.spec.getTranslationKey())){
                name = this.value.getPath().get(this.value.getPath().size()-1);
            }
        }else {
            name = this.value.getPath().get(this.value.getPath().size()-1);
        }
        this.configName = name;

        T defaultValue;
        try {
            defaultValue = (T) spec.getDefault();
        } catch (Exception e) {
            defaultValue = null;
        }
        this.defaultValue = defaultValue;
    }

    protected final void addUndoAndReset(){
        if(this.getConfigWidget()!=null){
            this.undo = this.addWidget(new Button(this.getConfigWidget().x+this.getConfigWidget().getWidth()+2, 0, 20, 20, ITextComponent.getTextComponentOrEmpty("U"), btn->this.undo()));
            this.reset = this.addWidget(new Button(this.undo.x+this.undo.getWidth()+2, 0, 20, 20, ITextComponent.getTextComponentOrEmpty("R"), btn->this.reset()));
        }
    }

    public abstract void render(Screen gui, MatrixStack matrix, int x, int y, int mouseX, int mouseY);
    protected abstract T getCacheValue();
    protected abstract void setCacheValue(T newValue);
    protected abstract Widget getConfigWidget();

    protected void renderWidget(Screen gui, MatrixStack matrix, int x, int y, int mouseX, int mouseY){
        this.widgets.forEach(widget -> {
            widget.y = y;
            widget.render(matrix, mouseX, mouseY, 0);
        });
        if(this.undo!=null){
            this.undo.active = this.isChanged();
            if(this.reset.active && this.undo.isMouseOver(mouseX, mouseY)){
                gui.renderTooltip(matrix, new TranslationTextComponent("info.config.undo"), mouseX, mouseY);
            }
        }
        if(this.reset!=null){
            this.reset.active = this.isDefault();
            if(this.reset.active && this.reset.isMouseOver(mouseX, mouseY)){
                gui.renderTooltip(matrix, new TranslationTextComponent("info.config.reset"), mouseX, mouseY);
            }
        }
    }

    protected <T2 extends Widget> T2 addWidget(T2 w){
        this.widgets.add(w);
        return w;
    }

    public void undo(){
        this.setCacheValue(this.value.get());
    }
    public void reset(){
        this.setCacheValue(this.defaultValue);
    }
    public boolean isChanged() {
        return !Objects.equals(this.getCacheValue(), this.value.get());
    }
    public boolean isDefault() {
        return !Objects.equals(this.getCacheValue(), this.defaultValue);
    }
    public void save() {
        this.value.set(this.getCacheValue());
    }

    public int getWeight(){
        return 1;
    }

    public boolean mouseClick(double mouseX, double mouseY, int button){
        for (Widget widget : this.widgets) {
            if(widget.mouseClicked(mouseX, mouseY, button)){
                return true;
            }
        }
        return false;
    }
    public boolean charTyped(char codePoint, int modifiers) {
        for (Widget widget : this.widgets) {
            if(widget.charTyped(codePoint, modifiers)){
                return true;
            }
        }
        return false;
    }
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (Widget widget : this.widgets) {
            if(widget.keyPressed(keyCode, scanCode, modifiers)){
                return true;
            }
        }
        return false;
    }

    public void drawHoverText(Screen gui, MatrixStack matrix, int mouseX, int mouseY){

    }

    protected void drawCommentWithRange(Screen gui, MatrixStack matrix, int mouseX, int mouseY, int x, int y, int width, int height) throws Exception {
        if(EmailUtils.isInRange(mouseX, mouseY, x, y, width, height)){
            this.drawComment(gui, matrix, mouseX, mouseY);
        }
    }

    protected void drawComment(Screen gui, MatrixStack matrix, int mouseX, int mouseY) throws Exception {
        List<ITextComponent> comments = Lists.newArrayList();

        comments.add(ITextComponent.getTextComponentOrEmpty(TextFormatting.GREEN + this.configName));

        if(spec.getComment() != null){
            for (String s1 : I18n.format(this.spec.getComment()).split("\n")) {
                comments.add(ITextComponent.getTextComponentOrEmpty(TextFormatting.YELLOW + s1));
            }
            if(this.spec.getRange()!=null){
                comments.remove(comments.size()-1);
            }
            comments.add(ITextComponent.getTextComponentOrEmpty(""));
        }
        String range = null;
        if(this.spec.getRange()!=null){
            Class<?> clazz = null;
            for (Class<?> c : ForgeConfigSpec.class.getDeclaredClasses()) {
                if("Range".equalsIgnoreCase(c.getSimpleName())){
                    clazz = c;
                    break;
                }
            }

            Method getMin = clazz.getDeclaredMethod("getMin");
            getMin.setAccessible(true);
            Object min = getMin.invoke(this.spec.getRange());

            Method getMax = clazz.getDeclaredMethod("getMax");
            getMax.setAccessible(true);
            Object max = getMax.invoke(this.spec.getRange());

            comments.add(ITextComponent.getTextComponentOrEmpty(String.format("Min: %s", min)));
            comments.add(ITextComponent.getTextComponentOrEmpty(String.format("Max: %s", max)));
            range = String.format("range: %s ~ %s, ", min, max);
        }

        comments.add(ITextComponent.getTextComponentOrEmpty(TextFormatting.AQUA + String.format("[%sdefault: %s]", range!=null? range : "", this.spec.getDefault())));

        gui.renderWrappedToolTip(matrix, comments, mouseX+5, mouseY, gui.getMinecraft().fontRenderer);
    }
}
