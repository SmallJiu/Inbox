package cat.jiu.email.ui.gui.config;

import cat.jiu.email.ui.gui.component.GuiButton;
import cat.jiu.email.util.EmailUtils;

import com.google.common.collect.Lists;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.client.gui.components.Button;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class ConfigEntry<T> {
    protected final ForgeConfigSpec.ConfigValue<T> value;
    protected final ForgeConfigSpec.ValueSpec spec;
    protected final T defaultValue;
    protected final String configName;
    protected final List<AbstractWidget> widgets = new ArrayList<>();
    protected Button undo, reset;

    protected ConfigEntry(ForgeConfigSpec.ConfigValue<T> value, ForgeConfigSpec.ValueSpec spec) {
        this.value = value;
        this.spec = spec;
        if(value!=null) this.setCacheValue(value.get());
        String name;
        if(this.spec==null){
            name = "Sub Entry";
        }else if(this.spec.getTranslationKey()!=null){
            name = I18n.get(this.spec.getTranslationKey());
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

    public void drawAlignRightString(GuiGraphics graphics, String text, int x, int y, int color, boolean drawShadow, Font font) {
        graphics.drawString(font, text, x - font.width(text), y, color);
    }

    protected final void addUndoAndReset(){
        if(this.getConfigWidget()!=null){
            this.undo = this.addWidget(new GuiButton(this.getConfigWidget().getX() +this.getConfigWidget().getWidth()+2, 0, 20, 20, Component.nullToEmpty("U"), btn->this.undo(), Supplier::get));
            this.reset = this.addWidget(new GuiButton(this.undo.getX() +this.undo.getWidth()+2, 0, 20, 20, Component.nullToEmpty("R"), btn->this.reset(), Supplier::get));
        }
    }

    public abstract void render(Screen gui, GuiGraphics graphics, int x, int y, int mouseX, int mouseY);
    protected abstract T getCacheValue();
    protected abstract void setCacheValue(T newValue);
    protected abstract AbstractWidget getConfigWidget();

    protected void renderWidget(Screen gui, GuiGraphics graphics, int x, int y, int mouseX, int mouseY){
        this.widgets.forEach(widget -> {
            widget.setY(y);
            widget.render(graphics, mouseX, mouseY, 0);
        });
        if(this.undo!=null){
            this.undo.active = this.isChanged();
            if(this.reset.active && this.undo.isMouseOver(mouseX, mouseY)){
                graphics.renderTooltip(gui.getMinecraft().font, Component.translatable("info.config.undo"), mouseX, mouseY);
            }
        }
        if(this.reset!=null){
            this.reset.active = this.isDefault();
            if(this.reset.active && this.reset.isMouseOver(mouseX, mouseY)){
                graphics.renderTooltip(gui.getMinecraft().font, Component.translatable("info.config.reset"), mouseX, mouseY);
            }
        }
    }

    protected <T2 extends AbstractWidget> T2 addWidget(T2 w){
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
        for (AbstractWidget widget : this.widgets) {
            if(widget.mouseClicked(mouseX, mouseY, button)){
                return true;
            }
        }
        return false;
    }
    public boolean charTyped(char codePoint, int modifiers) {
        for (AbstractWidget widget : this.widgets) {
            if(widget.charTyped(codePoint, modifiers)){
                return true;
            }
        }
        return false;
    }
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (AbstractWidget widget : this.widgets) {
            if(widget.keyPressed(keyCode, scanCode, modifiers)){
                return true;
            }
        }
        return false;
    }

    public void drawHoverText(Screen gui, GuiGraphics graphics, int mouseX, int mouseY){

    }

    protected void drawCommentWithRange(Screen gui, GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height) throws Exception {
        if(EmailUtils.isInRange(mouseX, mouseY, x, y, width, height)){
            this.drawComment(gui, graphics, mouseX, mouseY);
        }
    }

    protected void drawComment(Screen gui, GuiGraphics graphics, int mouseX, int mouseY) throws Exception {
        List<Component> comments = Lists.newArrayList();

        comments.add(Component.nullToEmpty(ChatFormatting.GREEN + this.configName));

        if(spec.getComment() != null){
            for (String s1 : I18n.get(this.spec.getComment()).split("\n")) {
                comments.add(Component.nullToEmpty(ChatFormatting.YELLOW + I18n.get(s1)));
            }
            if(this.spec.getRange()!=null){
                comments.remove(comments.size()-1);
            }
            comments.add(Component.nullToEmpty(""));
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

            comments.add(Component.nullToEmpty(String.format("Min: %s", min)));
            comments.add(Component.nullToEmpty(String.format("Max: %s", max)));
            range = String.format("range: %s ~ %s, ", min, max);
        }
        if(this.spec.needsWorldRestart()) {
            comments.add(Component.nullToEmpty(ChatFormatting.RED + I18n.get("info.config.world_restart")));
        }
        comments.add(Component.nullToEmpty(ChatFormatting.AQUA + String.format("[%sdefault: %s]", range!=null? range : "", this.spec.getDefault())));

        graphics.renderComponentTooltip(gui.getMinecraft().font, comments, mouseX+5, mouseY);
    }
}
