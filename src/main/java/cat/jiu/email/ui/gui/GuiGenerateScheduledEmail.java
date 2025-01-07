package cat.jiu.email.ui.gui;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.ScheduledEmail;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.net.msg.MsgScheduledEmail;
import cat.jiu.email.net.msg.refresh.MsgRefreshScheduledEmail;
import cat.jiu.email.ui.gui.component.GuiImageButton;
import cat.jiu.email.ui.gui.component.GuiTime;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.TimeMillis;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import io.netty.util.internal.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.*;
import net.minecraftforge.client.gui.ScrollPanel;
import net.minecraftforge.common.MinecraftForge;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GuiGenerateScheduledEmail extends Screen {
    private final Runnable parent;
    private final HashMap<String, Email> pathMap = new HashMap<>();
    private EmailPathList emailList;
    private ScheduledEmailInfo emailInfo;
    private TextFieldWidget currentEmailTitle;
    private Button refreshBtn, confirmBtn;
//    private final GuiDynamicImage loadImage = new GuiDynamicImage(GuiInbox.load, 18, false, 32, 32, 0, 0, 16, 16, 32, 576);

    public GuiGenerateScheduledEmail(Runnable parent) {
        super(ITextComponent.getTextComponentOrEmpty(null));
        this.parent = parent;
        EmailMain.net.sendMessageToServer(MsgRefreshScheduledEmail.REFRESH_MAP);
    }

    @Override
    protected void init() {
        super.init();
        this.lastPath = null;
        FontRenderer font = RenderUtils.getFontRenderer();
        int listWidth = EmailConfigs.Layout.Email_List_Width.get();
        int x = 6, y = this.font.FONT_HEIGHT + 6;

        this.emailList = this.addListener(new GuiGenerateScheduledEmail.EmailPathList(this, listWidth, this.height - 35, x, y));
        x += this.emailList.getWidth() + 8;

        this.currentEmailTitle = new TextFieldWidget(font, x+5, y+5, this.width - this.emailList.getWidth() - 20, font.FONT_HEIGHT + 6, new StringTextComponent("this is a title"));
        this.currentEmailTitle.setEnabled(false);
        this.currentEmailTitle.setTextColor(Color.WHITE.getRGB());
        this.currentEmailTitle.setDisabledTextColour(Color.WHITE.getRGB());
        this.currentEmailTitle.setEnableBackgroundDrawing(false);

        this.emailInfo = this.addListener(new GuiGenerateScheduledEmail.ScheduledEmailInfo(this, x, this.emailList.getHeight() - this.currentEmailTitle.getHeight() - 5));
        this.currentEmailTitle.setMaxStringLength(this.font.trimStringToWidth("888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888", this.emailInfo.getWidth() + 10).length());
        this.initButtons(x,y);

        this.emailList.refresh();
    }

    protected void initButtons(int x, int y) {
        this.refreshBtn = this.addButton(new GuiImageButton(this, this.emailList.getLeft(), this.emailList.getBottom()+1, RenderUtils.getFontRenderer().FONT_HEIGHT*2+1, RenderUtils.getFontRenderer().FONT_HEIGHT*2+1, ()->
                new TranslationTextComponent(refreshCoolingTicks <= 0 ? "info.inbox.refresh" : "info.inbox.refresh.cooling")
                , 256, 256, 111, 169, 55, 55, btn->
                this.refresh(()->{
                            this.setCurrentPath(null);
                            this.refreshBtn.visible = false;
//                            this.loadImage.visible = true;
                            this.emailList.clearEmails();
                            this.emailInfo.clearMessage();
                            EmailMain.net.sendMessageToServer(MsgRefreshScheduledEmail.REFRESH_MAP);
                        }, ()-> {
                            this.refreshBtn.visible = true;
//                            this.loadImage.visible = false;
                        }
                )
        )).setBackground(()-> GuiInbox.BackGround);
//        this.loadImage.visible = false;
//        this.loadImage.width = this.refreshBtn.getWidth();
//        this.loadImage.height = this.refreshBtn.getHeight();

        int width = RenderUtils.width(new TranslationTextComponent("info.inbox.black.back"));
        Button btn = this.addButton(GuiInbox.GuiButton.builder(
                        new TranslationTextComponent("info.inbox.black.back"),
                        b-> this.parent.run()
                )
                .pos(this.emailInfo.getRight() - width - 6, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.FONT_HEIGHT + 6)
                .build());

        width = RenderUtils.width(new TranslationTextComponent("info.inbox.scheduled.gen"));
        this.confirmBtn = btn = this.addButton(GuiInbox.GuiButton.builder(
                        new TranslationTextComponent("info.inbox.scheduled.gen"),
                        b-> Minecraft.getInstance().displayGuiScreen(new ConfirmPanel(()->Minecraft.getInstance().displayGuiScreen(this)))
                )
                .pos(btn.x - width - 8, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.FONT_HEIGHT + 6)
                .build());
        this.confirmBtn.visible = false;
        this.addButton(this.confirmBtn);
    }

    @Override
    public void render(MatrixStack stack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(stack);
        RenderUtils.drawString(stack, I18n.format("info.inbox.scheduled.gen"), this.emailList.getLeft(), 4, Color.WHITE.getRGB(), true);
        RenderUtils.fill(stack, this.emailInfo.getLeft(), this.emailList.getTop(), this.currentEmailTitle.getWidth(), this.currentEmailTitle.getHeight() + 5, 0xC0101010);
        RenderUtils.hLineGradient(stack, false, this.emailInfo.getLeft() + 5, this.emailInfo.getTop() - 3, this.emailInfo.getWidth() - 10, 1, Color.YELLOW.getRGB(), 0);

        this.emailList.render(stack, pMouseX, pMouseY, pPartialTick);
        this.emailInfo.render(stack, pMouseX, pMouseY, pPartialTick);
        super.render(stack, pMouseX, pMouseY, pPartialTick);
        this.currentEmailTitle.render(stack, pMouseX, pMouseY, pPartialTick);

        if (!this.refreshBtn.visible) GuiInbox.LOADING_GIF
                .setRenderPos(this.refreshBtn.x, this.refreshBtn.y - 5)
                .setRenderSize(this.refreshBtn.getWidth(), this.refreshBtn.getHeight())
                .render(stack);
        this.renderTooltip(stack, pMouseX, pMouseY);
    }

    private void renderTooltip(MatrixStack stack, int pX, int pY) {
        for (int i = 0; i < this.emailList.getEventListeners().size(); i++) {
            EmailPathList.EmailPathEntry entry = null;
            try {
                entry = this.emailList.getEventListeners().get(i);
            }catch (Exception ignored){}

            if (entry!=null && EmailUtils.isInRange(pX, pY, this.emailList.getLeft(), this.emailList.getTop(), this.emailList.getWidth(), this.emailList.getHeight()) && entry.isMouseOver(pX, pY)) {
                List<String> tip = Lists.newArrayList();

                tip.add(entry.email.getTitle().format());

                if (entry.email.hasExpirationTime()) {
                    tip.add("");
                    tip.add(I18n.format("info.inbox.scheduled.expiration_time", ITimer.formatTimestamp(entry.email.getExpirationTime().millis)));
                }

                tip.add("");
                tip.add(I18n.format("info.inbox.scheduled.gen.path") + entry.path+".json");

                RenderUtils.drawComponentTooltip(stack, tip.stream().map(ITextComponent::getTextComponentOrEmpty).collect(Collectors.toList()), pX, pY);
                break;
            }
        }
    }

    private static int refreshCoolingTicks = 0;
    private static boolean hasRefreshThread;
    protected void refresh(Runnable pre, Runnable done) {
        if(!hasRefreshThread) {
            refreshCoolingTicks = 5 * 20;
            hasRefreshThread = true;
            pre.run();
            new Thread(()->{
                while(refreshCoolingTicks > 0) {
                    try {
                        Thread.sleep(50);
                        refreshCoolingTicks--;
                    }catch(InterruptedException ignored) {}
                }
                hasRefreshThread = false;
                done.run();
            }).start();
        }
    }
    public void refresh(){
        if (this.emailList!=null) {
            this.emailList.refresh();
        }
    }

    private String lastPath;
    public void setCurrentPath(String path) {
        if (this.getCurrentPath()!=null && Objects.equals(this.lastPath, path)) {
            this.confirmBtn.visible = false;
            this.emailList.setSelected(null);
            this.setCurrentPath(null);
            return;
        }
        this.lastPath = path;
        EmailPathList.EmailPathEntry entry = this.emailList.getEntry(path);
        if (entry!=null) {
            Email email = entry.email;
            this.currentEmailTitle.setText(email.getTitle().format());
            this.emailInfo.clearMessage();
            this.confirmBtn.visible = true;

            int textMaxLength = this.currentEmailTitle.getWidth()-13;
            for (IText message : email.getMessages()) {
                ITextComponent component = message.toTextComponent();

                List<IReorderingProcessor> texts;
                if (EmailConfigs.Layout.Enable_Vanilla_Wrap_Text.get()) {
                    texts = LanguageMap.getInstance().func_244260_a(RenderUtils.getFontRenderer().getCharacterManager().func_238362_b_(component, textMaxLength, Style.EMPTY));
                }else {
                    texts = EmailUtils.splitString(message.format(), textMaxLength).stream().map(e->IReorderingProcessor.fromString(e, Style.EMPTY)).collect(Collectors.toList());
                }
                texts.forEach(this.emailInfo::addMessage);
            }
            this.emailInfo.setScrollDistance(0);
        }else {
            this.currentEmailTitle.setText("");
            this.emailInfo.clearMessage();
        }
    }

    public Email getCurrentEmail() {
        if (this.getCurrentEntry() != null) {
            return this.getCurrentEntry().email;
        }
        return null;
    }
    public String getCurrentPath() {
        if (this.getCurrentEntry() != null) {
            return this.getCurrentEntry().path;
        }
        return null;
    }

    public EmailPathList.EmailPathEntry getCurrentEntry(){
        return this.emailList.getSelected();
    }

    public void addPath(String path, Email email) {
        this.pathMap.put(path, email);
    }

    @Override
    public void closeScreen() {
        super.closeScreen();
        this.parent.run();
    }
    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == Minecraft.getInstance().gameSettings.keyBindInventory.getKey().getKeyCode()) {
            this.closeScreen();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class ScheduledEmailInfo extends ScrollPanel {
        private final GuiGenerateScheduledEmail parent;
        private final List<IReorderingProcessor> messages = new ArrayList<>();

        public ScheduledEmailInfo(GuiGenerateScheduledEmail parent, int x, int height) {
            super(parent.getMinecraft(), parent.currentEmailTitle.getWidth(), height, parent.currentEmailTitle.y + parent.currentEmailTitle.getHeight(), x);
            this.parent = parent;
        }

        public void addMessage(IReorderingProcessor message) {
            this.messages.add(message);
        }
        public void clearMessage() {
            this.messages.clear();
        }

        @Override
        protected int getContentHeight() {
            Email email = this.parent.getCurrentEmail();
            if (email != null) {
                int height = this.messages.size() * (RenderUtils.getFontRenderer().FONT_HEIGHT + 2);
                height += RenderUtils.getFontRenderer().FONT_HEIGHT + 2;

                for (IAttachment attachment : email.getAttachments()) {
                    AttachmentEvent.GetHeight event = new AttachmentEvent.GetHeight(email, attachment, RenderUtils.getFontRenderer(), this.width, this.height);
                    MinecraftForge.EVENT_BUS.post(event);
                    attachment.getHeight(event);
                    height += event.getHeight();
                    height += 4;
                }

                if (height < this.bottom - this.top - 8)
                    height = this.bottom - this.top - 8;
                return height;
            }
            return 1;
        }

        @Override
        protected void drawPanel(MatrixStack stack, int x, int y, Tessellator tess, int mouseX, int mouseY) {
            x = this.left + 5;
            Email email = this.parent.getCurrentEmail();
            if (email != null) {
                FontRenderer font = RenderUtils.getFontRenderer();

                for (IReorderingProcessor message : this.messages) {
                    if (message!=null) {
                        RenderSystem.enableBlend();
                        RenderUtils.drawSequence(stack, message, x, y, Color.WHITE.getRGB(), true);
                        RenderSystem.disableBlend();
                    }
                    y+= font.FONT_HEIGHT + 2;
                }

                y+= font.FONT_HEIGHT + 2;
                for (IAttachment attachment : email.getAttachments()) {
                    AttachmentEvent.Render.Pre event = new AttachmentEvent.Render.Pre(email, attachment, stack, font, this.width, this.height, x, y, mouseX, mouseY, this.top, this.bottom, this.left, this.right);
                    if (!MinecraftForge.EVENT_BUS.post(event)) {
                        attachment.render(event);
                        MinecraftForge.EVENT_BUS.post(new AttachmentEvent.Render.Post(email, attachment, stack, font, this.width, this.height, x, y, mouseX, mouseY, this.top, this.bottom, this.left, this.right));
                    }
                    event.addY(4);
                    y = event.getY();
                }
            }
        }


        public void setScrollDistance(float amount){
            this.scrollDistance = amount;
        }
        public float getScrollDistance(){
            return this.scrollDistance;
        }

        public int getLeft(){
            return this.left;
        }
        public int getTop(){
            return this.top;
        }

        public int getRight(){
            return this.right;
        }
        public int getBottom(){
            return this.bottom;
        }
        public int getWidth(){
            return this.width;
        }
        public int getHeight(){
            return this.height;
        }
    }

    public static class EmailPathList extends ExtendedList<EmailPathList.EmailPathEntry> {
        private final HashMap<String, EmailPathEntry> entryMap = new HashMap<>();
        private final GuiGenerateScheduledEmail parent;

        public EmailPathList(GuiGenerateScheduledEmail parent, int width, int height, int x, int y) {
            super(Minecraft.getInstance(), width, height, y, y + height, RenderUtils.getFontRenderer().FONT_HEIGHT + 8);
            this.parent = parent;
            this.setLeftPos(x);
            this.setRenderSelection(false);
            this.func_244605_b(false);
            this.func_244606_c(false);
            this.setRenderHeader(false, 0);
        }

        public EmailPathEntry getEntry(String path) {
            if (entryMap.containsKey(path)) {
                return entryMap.get(path);
            }
            return null;
        }

        public synchronized void refresh() {
            this.entryMap.clear();
            List<EmailPathEntry> entries = new ArrayList<>();
            this.parent.pathMap.forEach((path, email)->
                entries.add(new EmailPathEntry(this.parent, path, email, this.width))
            );
            this.replaceEntries(entries);
        }

        @Override
        protected void renderList(MatrixStack matrixStack, int x, int y, int mouseX, int mouseY, float partialTicks) {
            try {
                super.renderList(matrixStack, x, y, mouseX, mouseY, partialTicks);
            }catch (Throwable ignored){}
        }

        @Override
        public int getRowWidth() {
            return this.width;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width;
        }

        @Override
        protected void renderBackground(MatrixStack stack) {
            RenderUtils.fill(stack, this.getLeft(), this.getTop(), this.width, this.height, 0xC0101010);
        }

        public void clearEmails() {
            super.clearEntries();
        }

        public class EmailPathEntry extends ExtendedList.AbstractListEntry<EmailPathEntry> {
            private final GuiGenerateScheduledEmail parent;
            private final String path;
            private final Email email;
            private final int width;
            private ITextComponent name;

            public EmailPathEntry(GuiGenerateScheduledEmail parent, String path, Email email, int width) {
                this.parent = parent;
                this.path = path;
                this.email = email;
                this.width = width;
                this.name = new StringTextComponent(RenderUtils.getFontRenderer().trimStringToWidth(this.path, this.width - 12));
                this.parent.emailList.entryMap.put(path, this);
            }

            @Override
            public void render(MatrixStack stack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
                EmailPathList.EmailPathEntry entry = EmailPathList.this.getSelected();
                boolean isScroll = EmailPathList.this.getMaxScroll() > 0;
                if (entry != null && Objects.equals(entry.path, this.path)) {
                    this.renderSelection(stack, index, top, left, width, height, mouseX, mouseY, isScroll, isMouseOver, partialTicks);
                }else {
                    this.renderBack(stack, index, top, left, width, height, mouseX, mouseY, isScroll, isMouseOver, partialTicks);
                }
                RenderUtils.drawComponent(stack, this.name, left + 2, top + 4, Color.WHITE.getRGB(), true);
            }

            public void renderBack(MatrixStack stack, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isScroll, boolean isMouseOver, float partialTick) {
                x -= 2;
                width -= isScroll  ? 9 : 3;
                RenderUtils.draw(stack, GuiInbox.BackGround, x, y, 3, 3, 168, 168); // 左上
                RenderUtils.draw(stack, GuiInbox.BackGround, x, y + height + 1, 3, 3, 168, 197); //左下

                RenderUtils.draw(stack, GuiInbox.BackGround, x + width, y, 3, 3, 244, 168); // 右上
                RenderUtils.draw(stack, GuiInbox.BackGround, x + width, y + height + 1, 3, 3, 244, 197); // 右下

                RenderUtils.draw(stack, GuiInbox.BackGround, x + 3, y, width - 3, 3, 171, 168, 1, 3, null); // 上
                RenderUtils.draw(stack, GuiInbox.BackGround, x + 3, y + height + 1, width - 3, 3, 171, 197, 1, 3, null); // 下

                RenderUtils.draw(stack, GuiInbox.BackGround, x, y + 3, 3, height - 2, 168, 171, 3, 1, null); //左
                RenderUtils.draw(stack, GuiInbox.BackGround, x + width, y + 3, 3, height - 2, 244, 171, 3, 1, null); //右

                RenderUtils.draw(stack, GuiInbox.BackGround, x + 3, y + 3, width - 3, height - 2, 171, 171, 1, 1, null); //右
            }

            public void renderSelection(MatrixStack stack, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isScroll, boolean isMouseOver, float partialTick) {
                x -= 2;
                width -= isScroll ? 9 : 3;
                RenderUtils.draw(stack, GuiInbox.BackGround, x + 3, y + 3, width - 3, height - 2, 171, 171, 1, 1, null); //右

                RenderUtils.draw(stack, GuiInbox.BackGround, x, y, 7, 5, 168, 200); // 左上
                RenderUtils.draw(stack, GuiInbox.BackGround, x, y + height - 3, 5, 7, 168, 225); //左下

                RenderUtils.draw(stack, GuiInbox.BackGround, x + width - 4, y, 7, 7, 240, 200); // 右上
                RenderUtils.draw(stack, GuiInbox.BackGround, x + width - 2, y + height - 3, 5, 7, 242, 225); // 右下

                RenderUtils.draw(stack, GuiInbox.BackGround, x + 7, y, width - 11, 3, 175, 200, 1, 3, null); // 上
                RenderUtils.draw(stack, GuiInbox.BackGround, x + 5, y + height - 3, width - 7, 7, 173, 225, 1, 7, null); // 下

                RenderUtils.draw(stack, GuiInbox.BackGround, x, y + 5, 7, height - 8, 168, 205, 7, 1, null); //左
                RenderUtils.draw(stack, GuiInbox.BackGround, x + width - 4, y + 7, 7, height - 10, 240, 207, 7, 1, null); //右
            }

            @Override
            public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
                if (pButton == 0) {
                    this.parent.emailList.setSelected(this);
                    this.parent.setCurrentPath(this.path);
                }
                return false;
            }
        }
    }

    private class ConfirmPanel extends Screen {
        private final Runnable parent;
        private final String path;
        private int leftPos, topPos;
        private final GuiTime time = new GuiTime(this, false);
        private TextFieldWidget note, custom_addressee;
        private Button timeBtn, addresseeBtn;
        private int currentAddressee = 0;

        public ConfirmPanel(Runnable parent) {
            super(ITextComponent.getTextComponentOrEmpty(null));
            this.parent = parent;
            this.path = GuiGenerateScheduledEmail.this.getCurrentPath() + ".json";
        }

        @Override
        protected void init() {
            super.init();
            this.leftPos = (this.width - 176) / 2;
            this.topPos = (this.height - 166) / 2;

            int
                    x = this.leftPos + 29,
                    y = this.topPos + 20 + RenderUtils.getFontRenderer().FONT_HEIGHT + 2;

            int width = RenderUtils.width(new TranslationTextComponent("info.inbox.scheduled.gen.interval.change")) + 4;
            this.timeBtn = this.addButton(GuiInbox.GuiButton.builder(new TranslationTextComponent("info.inbox.scheduled.gen.interval.change"), btn->this.time.setEnable(!this.time.isEnable()))
                    .bounds(x, y, width, RenderUtils.getFontRenderer().FONT_HEIGHT + 2)
                    .build());
            this.children.add(this.time);

            width = RenderUtils.width(new TranslationTextComponent("info.inbox.scheduled.gen.addressee.0")) + 4;
            this.addresseeBtn = this.addButton(GuiInbox.GuiButton.builder(new TranslationTextComponent("info.inbox.scheduled.gen.addressee.0"), btn->{
                        this.currentAddressee++;
                        if (this.currentAddressee>=3) {
                            this.currentAddressee = 0;
                        }
                        this.addresseeBtn.setMessage(new TranslationTextComponent("info.inbox.scheduled.gen.addressee."+this.currentAddressee));
                        this.custom_addressee.visible = this.currentAddressee==2;
                    })
                    .bounds(x, y += this.timeBtn.getHeight() + 4, width, RenderUtils.getFontRenderer().FONT_HEIGHT + 2)
                    .build());
            this.addButton(this.addresseeBtn);
            this.currentAddressee = 0;

            this.note = this.addButton(new TextFieldWidget(RenderUtils.getFontRenderer(), x, y += this.addresseeBtn.getHeight() + 2, 131, RenderUtils.getFontRenderer().FONT_HEIGHT + 2, ITextComponent.getTextComponentOrEmpty(null)));
            this.note.setMaxStringLength(Integer.MAX_VALUE);
            this.note.setEnableBackgroundDrawing(false);

            this.addButton(this.custom_addressee = new TextFieldWidget(RenderUtils.getFontRenderer(), this.leftPos + 5, y += (this.note.getHeight() + 2)*2 + 8, 160, RenderUtils.getFontRenderer().FONT_HEIGHT + 2, ITextComponent.getTextComponentOrEmpty(null)));
            this.custom_addressee.setMaxStringLength(Integer.MAX_VALUE);
            this.custom_addressee.setEnableBackgroundDrawing(false);
            this.custom_addressee.visible = false;

            this.addButton(GuiInbox.GuiButton.builder(new TranslationTextComponent("info.inbox.confirm"), btn->this.gen())
                            .bounds(this.leftPos + 8, this.topPos + 166 - 20, 160, RenderUtils.getFontRenderer().FONT_HEIGHT + 2)
                    .build());
        }

        public void gen() {
            if (this.time.getTimeOfMillis() <= 0) {
                return;
            }
            ScheduledEmail email = new ScheduledEmail()
                    .setFilePath(this.path)
                    .setInterval(new TimeMillis(this.time.getTimeOfMillis()))
                    .setAddressee(ScheduledEmail.Addressee.get(this.currentAddressee))
                    .setNote(this.note.getText());

            if (!StringUtil.isNullOrEmpty(this.custom_addressee.getText())) {
                for (String name : this.custom_addressee.getText().split(",")) {
                    email.addCustomAddressee(name);
                }
            }
            EmailMain.net.sendMessageToServer(new MsgScheduledEmail.Add(email));

            this.parent.run();
        }

        @Override
        public void render(MatrixStack stack, int pMouseX, int pMouseY, float pPartialTick) {
            this.renderBackground(stack);

            RenderUtils.draw(stack, GuiInbox.BackGround, this.leftPos, this.topPos, 176, 166, 0, 0);

            RenderUtils.drawCenteredString(stack, I18n.format("info.inbox.scheduled.gen"), this.leftPos + (176/2), this.topPos + 5, Color.WHITE.getRGB(), true);
            int
                    x = this.leftPos + 29,
                    y = this.topPos + 20;
            RenderUtils.drawRightString(stack, I18n.format("info.inbox.scheduled.gen.path"), x, y, Color.WHITE.getRGB(), true);
            RenderUtils.drawString(stack, TextFormatting.YELLOW + this.path, x, y, Color.WHITE.getRGB(), true);

            RenderUtils.drawRightString(stack, I18n.format("info.inbox.scheduled.gen.interval"), x, y += RenderUtils.getFontRenderer().FONT_HEIGHT + 4, Color.WHITE.getRGB(), true);
            String time = this.time.getTimeOfMillis() <= 0 ? I18n.format("info.inbox.scheduled.gen.interval.change.un") : ITimer.formatTimestamp(this.time.getTimeOfMillis(), false, true, true, true, true);
            RenderUtils.drawString(stack, time, this.timeBtn.x + this.timeBtn.getWidth() + 3, this.timeBtn.y + 2, Color.WHITE.getRGB(), true);

            RenderUtils.drawRightString(stack, I18n.format("info.inbox.addressee") + ": ", x, y += RenderUtils.getFontRenderer().FONT_HEIGHT + 4, Color.WHITE.getRGB(), true);
            RenderUtils.drawString(stack, I18n.format("info.inbox.scheduled.gen.addressee."+this.currentAddressee + ".info"), this.addresseeBtn.x + this.addresseeBtn.getWidth() + 3, this.addresseeBtn.y + 2, Color.WHITE.getRGB(), true);

            RenderUtils.drawRightString(stack, I18n.format("info.inbox.scheduled.gen.note"), x, y += RenderUtils.getFontRenderer().FONT_HEIGHT + 4, Color.WHITE.getRGB(), true);

            super.render(stack, pMouseX, pMouseY, pPartialTick);

            this.time.render(stack, this.leftPos + 176 + 5, this.topPos + 5, pPartialTick);
            RenderUtils.hLine(stack, this.note.x, this.note.y + this.note.getHeight() - 2, this.note.getWidth(), Color.LIGHT_GRAY.getRGB());

            if (this.currentAddressee == 2) {
                RenderUtils.drawString(stack, I18n.format("info.inbox.scheduled.gen.addressee.2.custom"), this.custom_addressee.x, this.custom_addressee.y - RenderUtils.getFontRenderer().FONT_HEIGHT - 3, Color.WHITE.getRGB(), true);
                RenderUtils.hLine(stack, this.custom_addressee.x, this.custom_addressee.y + this.custom_addressee.getHeight() - 2, this.custom_addressee.getWidth(), Color.LIGHT_GRAY.getRGB());
            }
        }

        @Override
        public void closeScreen() {
            super.closeScreen();
            this.parent.run();
        }
    }
}
