package cat.jiu.email.ui.gui;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.configs.EmailConfigClient;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.ScheduledEmail;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.net.msg.MsgScheduledEmail;
import cat.jiu.email.net.msg.refresh.MsgRefreshScheduledEmail;
import cat.jiu.email.ui.gui.component.GuiImageButton;
import cat.jiu.email.ui.gui.component.GuiTime;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.TimeMillis;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringUtil;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import net.minecraftforge.common.MinecraftForge;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GuiGenerateScheduledEmail extends Screen {
    private final Screen parent;
    private final HashMap<String, Email> pathMap = new HashMap<>();
    private EmailPathList emailList;
    private ScheduledEmailInfo emailInfo;
    private EditBox currentEmailTitle;
    private Button refreshBtn, confirmBtn;
//    private final GuiDynamicImage loadImage = new GuiDynamicImage(GuiInbox.load, 18, false, 32, 32, 0, 0, 16, 16, 32, 576);

    public GuiGenerateScheduledEmail(Screen parent) {
        super(CommonComponents.EMPTY);
        this.parent = parent;
        EmailMain.NETWORK.sendMessageToServer(MsgRefreshScheduledEmail.REFRESH_MAP);
    }

    @Override
    protected void init() {
        super.init();
        this.lastPath = null;
        Font font = RenderUtils.getFontRenderer();
        int listWidth = EmailConfigClient.Email_List_Width.get();
        int x = 6, y = this.font.lineHeight + 6;

        this.emailList = this.addRenderableWidget(new GuiGenerateScheduledEmail.EmailPathList(this, listWidth, this.height - 35, x, y));
        x += this.emailList.getWidth() + 8;

        this.currentEmailTitle = this.addRenderableOnly(new EditBox(font, x+5, y+5, this.width - this.emailList.getWidth() - 20, font.lineHeight + 6, Component.literal("this is a title")));
        this.currentEmailTitle.setEditable(false);
        this.currentEmailTitle.setTextColor(Color.WHITE.getRGB());
        this.currentEmailTitle.setTextColorUneditable(Color.WHITE.getRGB());
        this.currentEmailTitle.setBordered(false);

        this.emailInfo = this.addRenderableWidget(new GuiGenerateScheduledEmail.ScheduledEmailInfo(this, x, this.emailList.getHeight() - this.currentEmailTitle.getHeight() - 5));
        this.currentEmailTitle.setMaxLength(this.font.plainSubstrByWidth("888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888", this.emailInfo.getWidth() + 10).length());
        this.initButtons(x,y);

        this.emailList.refresh();
    }

    protected void initButtons(int x, int y) {
        this.refreshBtn = this.addRenderableWidget(new GuiImageButton(this, this.emailList.getLeft(), this.emailList.getBottom()+1, RenderUtils.getFontRenderer().lineHeight*2+1, RenderUtils.getFontRenderer().lineHeight*2+1, ()->
                Component.translatable(refreshCoolingTicks <= 0 ? "info.inbox.refresh" : "info.inbox.refresh.cooling")
                , 256, 256, 111, 169, 55, 55, btn->
                this.refresh(()->{
                            this.setCurrentPath(null);
                            this.refreshBtn.visible = false;
//                            this.loadImage.visible = true;
                            this.emailList.clearEntries();
                            this.emailInfo.clearMessage();
                            EmailMain.NETWORK.sendMessageToServer(MsgRefreshScheduledEmail.REFRESH_MAP);
                        }, ()-> {
                            this.refreshBtn.visible = true;
//                            this.loadImage.visible = false;
                        }
                )
        )).setBackground(()-> GuiInbox.BackGround);
//        this.loadImage.visible = false;
//        this.loadImage.width = this.refreshBtn.getWidth();
//        this.loadImage.height = this.refreshBtn.getHeight();

        int width = RenderUtils.getFontRenderer().width(Component.translatable("info.inbox.black.back"));
        Button btn = this.addRenderableWidget(GuiInbox.GuiButton.builder(
                        Component.translatable("info.inbox.black.back"),
                        b-> Minecraft.getInstance().setScreen(this.parent)
                )
                .pos(this.emailInfo.getRight() - width - 6, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.lineHeight + 6)
                .build());

        width = this.font.width(Component.translatable("info.inbox.scheduled.gen"));
        this.confirmBtn = btn = this.addRenderableWidget(GuiInbox.GuiButton.builder(
                        Component.translatable("info.inbox.scheduled.gen"),
                        b-> Minecraft.getInstance().setScreen(new ConfirmPanel(()->Minecraft.getInstance().setScreen(this)))
                )
                .pos(btn.getX() - width - 8, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.lineHeight + 6)
                .build());
        this.confirmBtn.visible = false;
        this.addRenderableWidget(this.confirmBtn);
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        RenderUtils.drawString(graphics, I18n.get("info.inbox.scheduled.gen"), this.emailList.getLeft(), 4, Color.WHITE.getRGB(), true);
        graphics.fill(this.emailInfo.getLeft(), this.emailList.getTop(), this.emailInfo.getRight(), this.emailInfo.getTop(), 0xC0101010);
        int i = 1;
        EmailUtils.hLineGradient(graphics, false, this.currentEmailTitle.getX(), this.currentEmailTitle.getY() + this.currentEmailTitle.getHeight() - i - 1, this.emailInfo.getRight(), this.currentEmailTitle.getY() + this.currentEmailTitle.getHeight() - i, Color.YELLOW.getRGB(), 0);
        super.render(graphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(graphics, pMouseX, pMouseY);
    }

    private void renderTooltip(GuiGraphics graphics, int pX, int pY) {
        for (int i = 0; i < this.emailList.children().size(); i++) {
            EmailPathList.EmailPathEntry entry = null;
            try {
                entry = this.emailList.children().get(i);
            }catch (Exception ignored){}

            if (entry!=null && EmailUtils.isInRange(pX, pY, this.emailList.getLeft(), this.emailList.getTop(), this.emailList.getWidth(), this.emailList.getHeight()) && entry.isMouseOver(pX, pY)) {
                List<String> tip = Lists.newArrayList();

                tip.add(entry.email.getTitle().format());
                tip.add(I18n.get("info.inbox.main.from", entry.email.getSender().format()));
                if (!entry.email.isDeletable()) {
                    tip.add(ChatFormatting.RED + I18n.get("info.inbox.send.lock.false"));
                }

                if (entry.email.hasExpirationTime()) {
                    tip.add(I18n.get("info.inbox.scheduled.expiration_time", ITimer.formatTimestamp(entry.email.getExpirationTime().millis)));
                }

                tip.add("");
                tip.add(I18n.get("info.inbox.scheduled.gen.path") + entry.path+".json");

                graphics.renderComponentTooltip(RenderUtils.getFontRenderer(), tip.stream().map(Component::literal).collect(Collectors.toList()), pX, pY);
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
            this.currentEmailTitle.setValue(email.getTitle().format());
            this.emailInfo.clearMessage();
            this.confirmBtn.visible = true;

            int textMaxLength = this.currentEmailTitle.getWidth()-13;
            for (IText message : email.getMessages()) {
                Component component = message.toTextComponent();

                List<FormattedCharSequence> texts;
                if (EmailConfigClient.Enable_Vanilla_Wrap_Text.get()) {
                    texts = Language.getInstance().getVisualOrder(font.getSplitter().splitLines(component, textMaxLength, Style.EMPTY));
                }else {
                    texts = EmailUtils.splitString(message.format(), textMaxLength).stream().map(e->FormattedCharSequence.backward(e, Style.EMPTY)).collect(Collectors.toList());
                }
                texts.forEach(this.emailInfo::addMessage);
            }
            this.emailInfo.setScrollDistance(0);
        }else {
            this.currentEmailTitle.setValue("");
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
    public void onClose() {
        super.onClose();
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class ScheduledEmailInfo extends ScrollPanel {
        private final GuiGenerateScheduledEmail parent;
        private final List<FormattedCharSequence> messages = new ArrayList<>();

        public ScheduledEmailInfo(GuiGenerateScheduledEmail parent, int x, int height) {
            super(parent.getMinecraft(), parent.currentEmailTitle.getWidth(), height, parent.currentEmailTitle.getY() + parent.currentEmailTitle.getHeight(), x);
            this.parent = parent;
        }

        public void addMessage(FormattedCharSequence message) {
            this.messages.add(message);
        }
        public void clearMessage() {
            this.messages.clear();
        }

        @Override
        protected int getContentHeight() {
            Email email = this.parent.getCurrentEmail();
            if (email != null) {
                int height = this.messages.size() * (RenderUtils.getFontRenderer().lineHeight + 2);
                height += RenderUtils.getFontRenderer().lineHeight + 2;

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
        protected void drawPanel(GuiGraphics graphics, int x, int y, Tesselator tess, int mouseX, int mouseY) {
            x = this.left + 5;
            Email email = this.parent.getCurrentEmail();
            if (email != null) {
                Font font = RenderUtils.getFontRenderer();

                for (FormattedCharSequence message : this.messages) {
                    if (message!=null) {
                        RenderSystem.enableBlend();
                        graphics.drawString(font, message, x, y, Color.WHITE.getRGB());
                        RenderSystem.disableBlend();
                    }
                    y+= font.lineHeight + 2;
                }

                y+= font.lineHeight + 2;
                for (IAttachment attachment : email.getAttachments()) {
                    AttachmentEvent.Render.Pre event = new AttachmentEvent.Render.Pre(email, attachment, graphics, font, this.width, this.height, x, y, mouseX, mouseY, this.top, this.bottom, this.left, this.right);
                    if (!MinecraftForge.EVENT_BUS.post(event)) {
                        attachment.render(event);
                        MinecraftForge.EVENT_BUS.post(new AttachmentEvent.Render.Post(email, attachment, graphics, font, this.width, this.height, x, y, mouseX, mouseY, this.top, this.bottom, this.left, this.right));
                    }
                    event.addY(4);
                    y = event.getY();
                }
            }
        }

        @Override
        public NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(NarrationElementOutput pNarrationElementOutput) {}


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

    public static class EmailPathList extends ObjectSelectionList<EmailPathList.EmailPathEntry> {
        private final HashMap<String, EmailPathEntry> entryMap = new HashMap<>();
        private final GuiGenerateScheduledEmail parent;

        public EmailPathList(GuiGenerateScheduledEmail parent, int width, int height, int x, int y) {
            super(Minecraft.getInstance(), width, height, y, y + height, RenderUtils.getFontRenderer().lineHeight + 8);
            this.parent = parent;
            this.setLeftPos(x);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
            this.setRenderHeader(false, 0);
        }

        public EmailPathEntry getEntry(String path) {
            if (entryMap.containsKey(path)) {
                return entryMap.get(path);
            }
            return null;
        }

        @Override
        public void addEntryToTop(EmailPathEntry pEntry) {
            super.addEntryToTop(pEntry);
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
        protected void renderList(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            try {
                super.renderList(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            }catch (Throwable ignored){}
        }

        @Override
        protected void renderSelection(GuiGraphics graphics, int y, int width, int height, int pOuterColor, int pInnerColor) {
            int x = this.x0 + (this.width - width) / 2;
            width -= 9;

            RenderUtils.draw(graphics, GuiInbox.BackGround, x, y, 7, 5, 168, 200); // 左上
            RenderUtils.draw(graphics, GuiInbox.BackGround, x, y + height - 3, 5, 7, 168, 225); //左下

            RenderUtils.draw(graphics, GuiInbox.BackGround, x + width - 4, y, 7, 7, 240, 200); // 右上
            RenderUtils.draw(graphics, GuiInbox.BackGround, x + width - 2, y + height - 3, 5, 7, 242, 225); // 右下

            RenderUtils.draw(graphics, GuiInbox.BackGround, x + 7, y, width - 11, 3, 175, 200, 1, 3, null); // 上
            RenderUtils.draw(graphics, GuiInbox.BackGround, x + 5, y + height - 3, width - 7, 7, 173, 225, 1, 7, null); // 下

            RenderUtils.draw(graphics, GuiInbox.BackGround, x, y + 5, 7, height - 8, 168, 205, 7, 1, null); //左
            RenderUtils.draw(graphics, GuiInbox.BackGround, x + width - 4, y + 7, 7, height - 10, 240, 207, 7, 1, null); //右
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
        protected void renderBackground(GuiGraphics pGuiGraphics) {
            this.parent.renderBackground(pGuiGraphics);
            pGuiGraphics.fill(this.getLeft(), this.getTop(), this.getRight(), this.getBottom(), 0xC0101010);
        }

        @Override
        public void enableScissor(GuiGraphics pGuiGraphics) {
            super.enableScissor(pGuiGraphics);
        }

        @Override
        public void clearEntries() {
            super.clearEntries();
        }

        public static class EmailPathEntry extends ObjectSelectionList.Entry<EmailPathEntry> {
            private final GuiGenerateScheduledEmail parent;
            private final String path;
            private final Email email;
            private final int width;
            private Component name;

            public EmailPathEntry(GuiGenerateScheduledEmail parent, String path, Email email, int width) {
                this.parent = parent;
                this.path = path;
                this.email = email;
                this.width = width;
                this.name = Component.literal(RenderUtils.getFontRenderer().plainSubstrByWidth(this.path, this.width - 12));
                this.parent.emailList.entryMap.put(path, this);
            }

            @Override
            public Component getNarration() {
                return Component.empty();
            }

            @Override
            public void render(GuiGraphics graphics, int index, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
                graphics.drawString(RenderUtils.getFontRenderer(), this.name, pLeft + 2, pTop + 4, Color.WHITE.getRGB());
            }

            @Override
            public void renderBack(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
                x -= 2;
                width -= 9;
                RenderUtils.draw(graphics, GuiInbox.BackGround, x, y, 3, 3, 168, 168); // 左上
                RenderUtils.draw(graphics, GuiInbox.BackGround, x, y + height + 1, 3, 3, 168, 197); //左下

                RenderUtils.draw(graphics, GuiInbox.BackGround, x + width, y, 3, 3, 244, 168); // 右上
                RenderUtils.draw(graphics, GuiInbox.BackGround, x + width, y + height + 1, 3, 3, 244, 197); // 右下

                RenderUtils.draw(graphics, GuiInbox.BackGround, x + 3, y, width - 3, 3, 171, 168, 1, 3, null); // 上
                RenderUtils.draw(graphics, GuiInbox.BackGround, x + 3, y + height + 1, width - 3, 3, 171, 197, 1, 3, null); // 下

                RenderUtils.draw(graphics, GuiInbox.BackGround, x, y + 3, 3, height - 2, 168, 171, 3, 1, null); //左
                RenderUtils.draw(graphics, GuiInbox.BackGround, x + width, y + 3, 3, height - 2, 244, 171, 3, 1, null); //右

                RenderUtils.draw(graphics, GuiInbox.BackGround, x + 3, y + 3, width - 3, height - 2, 171, 171, 1, 1, null); //右
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
        private final GuiTime time = new GuiTime(false, true, true, true, true, false);
        private EditBox note, custom_addressee;
        private Button timeBtn, addresseeBtn;
        private int currentAddressee = 0;

        public ConfirmPanel(Runnable parent) {
            super(CommonComponents.EMPTY);
            this.parent = parent;
            this.path = GuiGenerateScheduledEmail.this.getCurrentPath() + ".json";
        }

        @Override
        protected void init() {
            super.init();
            this.leftPos = (this.width - 176) / 2;
            this.topPos = (this.height - 166) / 2;
            this.time.setEnable(false);

            int
                    x = this.leftPos + 29,
                    y = this.topPos + 20 + RenderUtils.getFontRenderer().lineHeight + 2;

            this.time.setRenderPos(x + 156, y - 26);


            int width = RenderUtils.width(Component.translatable("info.inbox.scheduled.gen.interval.change")) + 4;
            this.timeBtn = this.addRenderableWidget(GuiInbox.GuiButton.builder(Component.translatable("info.inbox.scheduled.gen.interval.change"), btn->this.time.setEnable(!this.time.isEnable()))
                    .bounds(x, y, width, RenderUtils.getFontRenderer().lineHeight + 2)
                    .build());
            this.addWidget(this.time);

            width = RenderUtils.width(Component.translatable("info.inbox.scheduled.gen.addressee.0")) + 4;
            this.addresseeBtn = this.addRenderableWidget(GuiInbox.GuiButton.builder(Component.translatable("info.inbox.scheduled.gen.addressee.0"), btn->{
                        this.currentAddressee++;
                        if (this.currentAddressee>=3) {
                            this.currentAddressee = 0;
                        }
                        this.addresseeBtn.setMessage(Component.translatable("info.inbox.scheduled.gen.addressee."+this.currentAddressee));
                        this.custom_addressee.visible = this.currentAddressee==2;
                    })
                    .bounds(x, y += this.timeBtn.getHeight() + 4, width, RenderUtils.getFontRenderer().lineHeight + 2)
                    .build());
            this.addWidget(this.addresseeBtn);
            this.currentAddressee = 0;

            this.note = this.addRenderableWidget(new EditBox(RenderUtils.getFontRenderer(), x, y += this.addresseeBtn.getHeight() + 2, 131, RenderUtils.getFontRenderer().lineHeight + 2, Component.empty()));
            this.note.setMaxLength(Integer.MAX_VALUE);
            this.note.setBordered(false);

            this.custom_addressee = this.addRenderableWidget(new EditBox(RenderUtils.getFontRenderer(), this.leftPos + 5, y += (this.note.getHeight() + 2)*2 + 8, 160, RenderUtils.getFontRenderer().lineHeight + 2, Component.empty()));
            this.custom_addressee.setMaxLength(Integer.MAX_VALUE);
            this.custom_addressee.setBordered(false);
            this.custom_addressee.visible = false;

            this.addRenderableWidget(GuiInbox.GuiButton.builder(Component.translatable("info.inbox.confirm"), btn->this.gen())
                            .bounds(this.leftPos + 8, this.topPos + 166 - 20, 160, RenderUtils.getFontRenderer().lineHeight + 2)
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
                    .setNote(this.note.getValue());

            if (!StringUtil.isNullOrEmpty(this.custom_addressee.getValue())) {
                for (String name : this.custom_addressee.getValue().split(",")) {
                    email.addCustomAddressee(name);
                }
            }
            EmailMain.NETWORK.sendMessageToServer(new MsgScheduledEmail.Add(email));

            this.parent.run();
        }

        @Override
        public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
            this.renderBackground(graphics);

            RenderUtils.draw(graphics, GuiInbox.BackGround, this.leftPos, this.topPos, 176, 166, 0, 0);

            RenderUtils.drawCenteredString(graphics, I18n.get("info.inbox.scheduled.gen"), this.leftPos + (176/2), this.topPos + 5, Color.WHITE.getRGB(), true);
            int
                    x = this.leftPos + 29,
                    y = this.topPos + 20;
            RenderUtils.drawRightString(graphics, I18n.get("info.inbox.scheduled.gen.path"), x, y, Color.WHITE.getRGB(), true);
            RenderUtils.drawString(graphics, ChatFormatting.YELLOW + this.path, x, y, Color.WHITE.getRGB(), true);

            RenderUtils.drawRightString(graphics, I18n.get("info.inbox.scheduled.gen.interval"), x, y += RenderUtils.getFontRenderer().lineHeight + 4, Color.WHITE.getRGB(), true);
            String time = this.time.getTimeOfMillis() <= 0 ? I18n.get("info.inbox.scheduled.gen.interval.change.un") : ITimer.formatTimestamp(this.time.getTimeOfMillis(), false, true, true, true, true);
            RenderUtils.drawString(graphics, time, this.timeBtn.getX() + this.timeBtn.getWidth() + 3, this.timeBtn.getY() + 2, Color.WHITE.getRGB(), true);

            RenderUtils.drawRightString(graphics, I18n.get("info.inbox.addressee") + ": ", x, y += RenderUtils.getFontRenderer().lineHeight + 4, Color.WHITE.getRGB(), true);
            RenderUtils.drawString(graphics, I18n.get("info.inbox.scheduled.gen.addressee."+this.currentAddressee + ".info"), this.addresseeBtn.getX() + this.addresseeBtn.getWidth() + 3, this.addresseeBtn.getY() + 2, Color.WHITE.getRGB(), true);

            RenderUtils.drawRightString(graphics, I18n.get("info.inbox.scheduled.gen.note"), x, y += RenderUtils.getFontRenderer().lineHeight + 4, Color.WHITE.getRGB(), true);

            super.render(graphics, pMouseX, pMouseY, pPartialTick);

            this.time.render(graphics, this.leftPos + 176 + 5, this.topPos + 5, pPartialTick);
            graphics.hLine(this.note.getX(), this.note.getX() + this.note.getWidth(), this.note.getY() + this.note.getHeight() - 2, Color.LIGHT_GRAY.getRGB());

            if (this.currentAddressee == 2) {
                RenderUtils.drawString(graphics, I18n.get("info.inbox.scheduled.gen.addressee.2.custom"), this.custom_addressee.getX(), this.custom_addressee.getY() - RenderUtils.getFontRenderer().lineHeight - 3, Color.WHITE.getRGB(), true);
                graphics.hLine(this.custom_addressee.getX(), this.custom_addressee.getX() + this.custom_addressee.getWidth(), this.custom_addressee.getY() + this.custom_addressee.getHeight() - 2, Color.LIGHT_GRAY.getRGB());
            }
        }

        @Override
        public void onClose() {
            super.onClose();
            this.parent.run();
        }
    }
}
