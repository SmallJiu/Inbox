package cat.jiu.email.ui.gui.config;

import cat.jiu.email.ui.gui.component.GuiButton;
import cat.jiu.email.ui.gui.config.entry.*;
import cat.jiu.email.util.EmailUtils;
import com.electronwill.nightconfig.core.Config;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.ScrollPanel;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class GuiConfig extends Screen {
    public final String configFile;
    protected final Screen parent;
    protected List<ConfigEntry<?>> entries;
    protected final ForgeConfigSpec spec;
    protected ConfigPanel panel;
    protected final String path;
    protected Button done, undo, reset;
    public GuiConfig(String file, Screen parent, ForgeConfigSpec spec) {
        this(file, parent, spec, null);
        this.setConfigEntries(this.createEntries(null, spec, spec.getValues().valueMap()));
    }
    public GuiConfig(String file, Screen parent, ForgeConfigSpec spec, String path) {
        super(Component.nullToEmpty(file));
        this.spec = spec;
        this.configFile = file;
        this.parent = parent;
        this.path = path;
    }

    public void setConfigEntries(List<ConfigEntry<?>> entries) {
        this.entries = entries;
    }
    public void addConfigEntry(ConfigEntry<?> value){
        if(this.entries==null) {
            this.entries = new ArrayList<>();
        }
        this.entries.add(value);
        this.entries.sort(Comparator.comparingInt(ConfigEntry::getWeight));
    }

    public ArrayList<ConfigEntry<?>> createEntries(String path, ForgeConfigSpec spec, Map<String, Object> configs){
        ArrayList<ConfigEntry<?>> entries = new ArrayList<>();
        configs.forEach((k,v)->{
            if(v instanceof Config){
                entries.add(new SubEntry(k, spec, ((Config) v), (path!=null? path : "") + (path!=null ? " > " : "") + k, this));
            }else if(v instanceof ForgeConfigSpec.ConfigValue<?> value){
                if(value.get() instanceof Boolean){
                    entries.add(new BooleanEntry((ForgeConfigSpec.BooleanValue) v, spec.get(value.getPath())));
                }else if(value.get() instanceof Enum){
                    entries.add(new EnumEntry<>((ForgeConfigSpec.EnumValue<? extends Enum<?>>) v, spec.get(value.getPath())));
                }else if(value.get() instanceof Number num){
                    if(num instanceof Integer){
                        entries.add(new IntEntry((ForgeConfigSpec.ConfigValue<Integer>) v, spec.get(value.getPath())));
                    }else if(num instanceof Long){
                        entries.add(new LongEntry((ForgeConfigSpec.ConfigValue<Long>) v, spec.get(value.getPath())));
                    }else if(num instanceof Float){
                        entries.add(new FloatEntry((ForgeConfigSpec.ConfigValue<Float>) v, spec.get(value.getPath())));
                    }else if(num instanceof Double){
                        entries.add(new DoubleEntry((ForgeConfigSpec.ConfigValue<Double>) v, spec.get(value.getPath())));
                    }
                } else if (value.get() instanceof String) {
                    entries.add(new StringEntry((ForgeConfigSpec.ConfigValue<String>) v, spec.get(value.getPath())));
                }
            }
        });
        entries.sort(Comparator.comparingInt(ConfigEntry::getWeight));
        return entries;
    }

    @Override
    protected void init() {
        super.init();
        this.panel = new ConfigPanel(Minecraft.getInstance(), this, this.entries);
        this.addWidget(this.panel);

        Window window = this.getMinecraft().getWindow();
        this.undo = this.addRenderableWidget(new GuiButton(window.getGuiScaledWidth()/2 - 50, window.getGuiScaledHeight() - 25, 100, 20, new TranslatableComponent("info.config.undo"), btn->
                this.entries.forEach(ConfigEntry::undo))
        );
        this.undo.active = false;
        this.done = this.addRenderableWidget(new GuiButton(this.undo.x - 102, this.undo.y, 100, 20, new TranslatableComponent(this.path==null? "info.config.save" : "info.config.done"), btn->{
            if(this.path==null){
                this.entries.forEach(ConfigEntry::save);
                this.spec.save();
                MinecraftForge.EVENT_BUS.post(new ConfigWriteEvent(this.configFile, this.spec));
            }
            this.getMinecraft().setScreen(this.parent);
        }));
        this.reset = this.addRenderableWidget(new GuiButton(this.undo.x + 102, this.undo.y, 100, 20, new TranslatableComponent("info.config.reset"), btn->
                this.entries.forEach(ConfigEntry::reset)
        ));
        this.reset.active = false;
    }

    @Override
    public void render(PoseStack stack, int mouseX, int mouseY, float partialTicks) {
        super.renderDirtBackground(0);
        super.render(stack, mouseX, mouseY, partialTicks);
        this.panel.render(stack, mouseX, mouseY, partialTicks);

        boolean changed = false;
        for (ConfigEntry<?> entry : this.entries) {
            if(entry.isChanged()){
                changed = true;
                break;
            }
        }
        boolean isDefault = false;
        for (ConfigEntry<?> entry : this.entries) {
            if(entry.isDefault()){
                isDefault = true;
                break;
            }
        }
        this.undo.active = changed;
        this.reset.active = changed || isDefault;
        if(this.path==null && this.done.isMouseOver(mouseX, mouseY)){
            renderTooltip(stack, new TranslatableComponent("info.config.save.0"), mouseX, mouseY);
        }

        drawString(stack, this.font, this.configFile, (this.getMinecraft().getWindow().getGuiScaledWidth() / 2) - (this.font.width(this.configFile)/2), 5, Color.WHITE.getRGB());
        if(this.font.width(this.path) > this.getMinecraft().getWindow().getGuiScaledWidth()/2){
            List<String> texts = EmailUtils.splitString(this.path, this.getMinecraft().getWindow().getGuiScaledWidth()/2);
            for (int i = 0; i < texts.size(); i++) {
                drawString(stack, this.font, texts.get(i), (this.getMinecraft().getWindow().getGuiScaledWidth() / 2) - (this.font.width(texts.get(i))/2), 5 + this.font.lineHeight+this.font.lineHeight*i, Color.WHITE.getRGB());
            }
        }else {
            drawString(stack, this.font, this.path, (this.getMinecraft().getWindow().getGuiScaledWidth() / 2) - (this.font.width(this.path)/2), 5 + this.font.lineHeight, Color.WHITE.getRGB());
        }

        this.entries.forEach(entry-> entry.drawHoverText(this, stack, mouseX, mouseY));
    }

    @Override
    public void onClose() {
        this.getMinecraft().setScreen(this.parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(this.panel.mouseClicked(mouseX, mouseY, button)){
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if(this.panel.charTyped(codePoint, modifiers)){
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(this.panel.keyPressed(keyCode, scanCode, modifiers)){
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    static class ConfigPanel extends ScrollPanel {
        private final List<ConfigEntry<?>> entries;
        private final Screen main;

        public ConfigPanel(Minecraft client, Screen main, List<ConfigEntry<?>> entries) {
            super(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight() - 60, 30, 0);
            this.entries = entries;
            this.main = main;
        }

        @Override
        protected int getContentHeight() {
            return this.entries.size() * 18 + ((this.entries.size()-1) * 2);
        }

        @Override
        protected void drawPanel(PoseStack stack, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {
            for (int i = 0; i < this.entries.size(); i++) {
                this.entries.get(i).render(this.main, stack, entryRight, relativeY + 22*i, mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            for (ConfigEntry<?> entry : this.entries) {
                if(entry.mouseClick(mouseX, mouseY, button)){
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            for (ConfigEntry<?> entry : this.entries) {
                if(entry.charTyped(codePoint, modifiers)){
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            for (ConfigEntry<?> entry : this.entries) {
                if(entry.keyPressed(keyCode, scanCode, modifiers)){
                    return true;
                }
            }
            return false;
        }

        @Override
        public NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }
        @Override
        public void updateNarration(NarrationElementOutput pNarrationElementOutput) {}
    }
}
