package cat.jiu.email.ui.gui.config;

import cat.jiu.email.EmailMain;

import cat.jiu.email.ui.gui.config.entry.BooleanEntry;
import cat.jiu.email.ui.gui.config.entry.EnumEntry;
import cat.jiu.email.ui.gui.config.entry.NumberEntry;
import cat.jiu.email.ui.gui.config.entry.SubEntry;
import com.electronwill.nightconfig.core.Config;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.gui.ScrollPanel;
import net.minecraftforge.common.ForgeConfigSpec;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

public class GuiConfig extends Screen {
    public final String configFile;
    private final Screen parent;
    private final List<ConfigEntry> entries;
    private ForgeConfigSpec spec;
    private ConfigPanel panel;
    private String path = "./";
    private Button done, undo, reset;
    public GuiConfig(String file, Screen parent, ForgeConfigSpec configSpec) {
        this(file, parent, configSpec, get(configSpec));
        this.spec = configSpec;
    }
    public GuiConfig(String file, Screen parent, ForgeConfigSpec spec, Map<String, Object> configs, String path) {
        super(ITextComponent.getTextComponentOrEmpty(path));
        this.configFile = file;
        this.path = path;
        this.parent = parent;
        this.entries = create(this.path, spec, configs);
    }
    public GuiConfig(String file, Screen parent, ForgeConfigSpec spec, Map<String, Object> configs) {
        super(ITextComponent.getTextComponentOrEmpty(EmailMain.MODID));
        this.configFile = file;
        this.parent = parent;
        this.entries = create(this.path, spec, configs);
    }

    static Map<String, Object> get(ForgeConfigSpec spec){
        try {
            Field c = spec.getClass().getDeclaredField("values");
            c.setAccessible(true);
            return ((Config) c.get(spec)).valueMap();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    public ArrayList<ConfigEntry> create(String path, ForgeConfigSpec spec, Map<String, Object> configs){
        ArrayList<ConfigEntry> entries = new ArrayList<>();
        configs.forEach((k,v)->{
            if(v instanceof Config){
                entries.add(new SubEntry(k, spec, ((Config) v), path + k + "/", this));
            }else if(v instanceof ForgeConfigSpec.ConfigValue){
                ForgeConfigSpec.ConfigValue value = (ForgeConfigSpec.ConfigValue) v;
                if(value.get() instanceof Boolean){
                    entries.add(new BooleanEntry((ForgeConfigSpec.BooleanValue) v, (ForgeConfigSpec.ValueSpec) spec.get(value.getPath())));
                }else if(value.get() instanceof Enum){
                    entries.add(new EnumEntry((ForgeConfigSpec.EnumValue<? extends Enum<?>>) v, (ForgeConfigSpec.ValueSpec) spec.get(value.getPath())));
                }else if(value.get() instanceof Number){
                    entries.add(new NumberEntry((ForgeConfigSpec.ConfigValue<Number>) v, (ForgeConfigSpec.ValueSpec) spec.get(value.getPath())));
                }
            }
        });
        entries.sort(Comparator.comparingInt(ConfigEntry::getWeight));
        return entries;
    }

    @Override
    protected void init() {
        super.init();
        this.panel = new ConfigPanel(Minecraft.getInstance(), this.entries);
        this.addListener(this.panel);

        MainWindow window = this.getMinecraft().getMainWindow();
        this.undo = this.addButton(new Button(window.getScaledWidth()/2 - 50, window.getScaledHeight() - 25, 100, 20, new TranslationTextComponent("info.config.undo"), btn->
                this.entries.forEach(ConfigEntry::undo)
        ));
        this.undo.active = false;
        this.done = this.addButton(new Button(this.undo.x - 102, this.undo.y, 100, 20, new TranslationTextComponent("info.config.done"), btn->{
            this.entries.forEach(ConfigEntry::save);
            if(this.spec != null) this.spec.save();
            this.getMinecraft().displayGuiScreen(this.parent);
        }));
        this.reset = this.addButton(new Button(this.undo.x + 102, this.undo.y, 100, 20, new TranslationTextComponent("info.config.reset"), btn->
            this.entries.forEach(ConfigEntry::reset)
        ));
        this.reset.active = false;
    }

    @Override
    public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTicks) {
        super.renderDirtBackground(0);
        super.render(matrix, mouseX, mouseY, partialTicks);
        this.panel.render(matrix, mouseX, mouseY, partialTicks);

        boolean changed = false;
        for (ConfigEntry entry : this.entries) {
            if(entry.isChanged()){
                changed = true;
                break;
            }
        }
        this.undo.active = changed;
        this.reset.active = changed;

        this.font.drawStringWithShadow(matrix, this.configFile, (this.getMinecraft().getMainWindow().getScaledWidth() / 2f) - (this.font.getStringWidth(this.configFile)/2f), 5, Color.WHITE.getRGB());
        if(this.font.getStringWidth(this.path) > this.getMinecraft().getMainWindow().getScaledWidth()/2){
            List<String> texts = splitString(this.path, this.getMinecraft().getMainWindow().getScaledWidth()/2);
            for (int i = 0; i < texts.size(); i++) {
                this.font.drawStringWithShadow(matrix, texts.get(i), (this.getMinecraft().getMainWindow().getScaledWidth() / 2f) - (this.font.getStringWidth(texts.get(i))/2f), 5 + this.font.FONT_HEIGHT+this.font.FONT_HEIGHT*i, Color.WHITE.getRGB());
            }
        }else {
            this.font.drawStringWithShadow(matrix, this.path, (this.getMinecraft().getMainWindow().getScaledWidth() / 2f) - (this.font.getStringWidth(this.path)/2f), 5 + this.font.FONT_HEIGHT, Color.WHITE.getRGB());
        }
    }

    @Override
    public void closeScreen() {
        super.closeScreen();
        this.getMinecraft().displayGuiScreen(this.parent);
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
        private final List<ConfigEntry> entries;

        public ConfigPanel(Minecraft client, List<ConfigEntry> entries) {
            super(client, client.getMainWindow().getScaledWidth(), client.getMainWindow().getScaledHeight() - 60, 30, 0);
            this.entries = entries;
        }

        @Override
        protected int getContentHeight() {
            return this.entries.size() * 18 + ((this.entries.size()-1) * 2);
        }

        @Override
        protected void drawPanel(MatrixStack matrix, int entryRight, int relativeY, Tessellator tess, int mouseX, int mouseY) {
            for (int i = 0; i < this.entries.size(); i++) {
                this.entries.get(i).render(matrix, entryRight, relativeY + 22*i, mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            for (ConfigEntry entry : this.entries) {
                if(entry.mouseClick(mouseX, mouseY, button)){
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            for (ConfigEntry entry : this.entries) {
                if(entry.charTyped(codePoint, modifiers)){
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            for (ConfigEntry entry : this.entries) {
                if(entry.keyPressed(keyCode, scanCode, modifiers)){
                    return true;
                }
            }
            return false;
        }
    }

    public static List<String> splitString(String text, int textMaxLength) {
        FontRenderer fr = Minecraft.getInstance().fontRenderer;
        List<String> texts = Lists.newArrayList();
        if(fr.getStringWidth(text) >= textMaxLength) {
            StringBuilder s = new StringBuilder();
            for(int i = 0; i < text.length(); i++) {
                String str = s.toString();
                if(fr.getStringWidth(str) >= textMaxLength) {
                    texts.add(str);
                    s.setLength(0);
                }
                s.append(text.charAt(i));
            }
            if(s.length() > 0) {
                texts.add(s.toString());
            }
        }else {
            texts.add(text);
        }
        return texts;
    }
}
