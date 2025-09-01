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
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.gui.component.GuiImageButton;
import cat.jiu.email.util.EmailUtils;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringUtil;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import net.minecraftforge.common.MinecraftForge;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuiScheduledEmail extends Screen {
    private EditBox currentEmailTitle, currentEmailLastTime;
    private ScheduledEmailList emailList;
    private ScheduledEmailInfo emailInfo;
    private Button deleteEmailBtn, addBtn, refreshBtn;
//    private final GuiDynamicImage loadImage = new GuiDynamicImage(GuiInbox.load, 18, false, 32, 32, 0, 0, 16, 16, 32, 576);
    private final List<ScheduledEmail> emails = new ArrayList<>();

    public GuiScheduledEmail() {
        super(Component.empty());
        EmailMain.NETWORK.sendMessageToServer(MsgRefreshScheduledEmail.REFRESH_MAIN);
    }

    @Override
    protected void init() {
        super.init();
        Font font = RenderUtils.getFontRenderer();
        int listWidth = EmailConfigClient.Email_List_Width.get();
        int x = 6, y = this.font.lineHeight + 6;

        this.emailList = this.addRenderableWidget(new ScheduledEmailList(this, listWidth, this.height - 35, x, y));
        x += this.emailList.getWidth() + 8;

        this.currentEmailTitle = this.addRenderableOnly(new EditBox(font, x+5, y+5, this.width - this.emailList.getWidth() - 20, font.lineHeight + 6, Component.literal("this is a title")));
        this.currentEmailTitle.setEditable(false);
        this.currentEmailTitle.setTextColor(Color.WHITE.getRGB());
        this.currentEmailTitle.setTextColorUneditable(Color.WHITE.getRGB());
        this.currentEmailTitle.setBordered(false);

        this.currentEmailLastTime = this.addRenderableOnly(new EditBox(font, this.currentEmailTitle.getX(), this.currentEmailTitle.getY() + this.currentEmailTitle.getHeight(), this.currentEmailTitle.getWidth(), this.currentEmailTitle.getHeight(), Component.literal("2024/13/32 25:61:61")));
        this.currentEmailLastTime.setEditable(false);
        this.currentEmailLastTime.setTextColor(Color.WHITE.getRGB());
        this.currentEmailLastTime.setTextColorUneditable(Color.WHITE.getRGB());
        this.currentEmailLastTime.setBordered(false);

        this.emailInfo = this.addRenderableWidget(new ScheduledEmailInfo(this, x, this.emailList.getHeight() - (this.currentEmailTitle.getHeight()*2) - 5));
        this.currentEmailTitle.setMaxLength(this.font.plainSubstrByWidth("888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888", this.emailInfo.getWidth() + 10).length());

        this.lastID = -1;

        this.initButtons(x, y);
        this.refresh();
    }

    protected void initButtons(int x, int y) {
        this.refreshBtn = this.addRenderableWidget(new GuiImageButton(this, this.emailList.getLeft(), this.emailList.getBottom()+1, RenderUtils.getFontRenderer().lineHeight*2+1, RenderUtils.getFontRenderer().lineHeight*2+1, ()->
                Component.translatable(refreshCoolingTicks <= 0 ? "info.inbox.refresh" : "info.inbox.refresh.cooling")
                , 256, 256, 111, 169, 55, 55, btn->
                this.refresh(()->{
                            this.setCurrentScheduledEmail(-1);
                            this.refreshBtn.visible = false;
//                            this.loadImage.visible = true;
                            this.emails.clear();
                            this.emailList.clearEntries();
                            this.emailInfo.clearMessage();
                            EmailMain.NETWORK.sendMessageToServer(MsgRefreshScheduledEmail.REFRESH_MAIN);
                        }, ()->
                                this.refreshBtn.visible = true
                )
        )).setBackground(()-> GuiInbox.BackGround);
//        this.loadImage.visible = false;
//        this.loadImage.width = this.refreshBtn.getWidth();
//        this.loadImage.height = this.refreshBtn.getHeight();

        int width = RenderUtils.getFontRenderer().width(Component.translatable("info.inbox.black.back"));
        Button btn = this.addRenderableWidget(GuiInbox.GuiButton.builder(
                        Component.translatable("info.inbox.black.back"),
                        b-> GuiHandler.openGui(GuiHandler.EMAIL_MAIN)
                )
                .pos(this.emailInfo.getRight() - width - 6, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.lineHeight + 6)
                .build());

        if (EmailUtils.isOP(Minecraft.getInstance().player)) {
            width = this.font.width(Component.translatable("info.inbox.black.add"));
            this.addBtn = btn = this.addRenderableWidget(GuiInbox.GuiButton.builder(
                            Component.translatable("info.inbox.black.add"),
                            b->
                                    Minecraft.getInstance().setScreen(new GuiGenerateScheduledEmail(()->GuiHandler.openGui(GuiHandler.EMAIL_Scheduled)))
                    )
                    .pos(btn.getX() - width - 8, this.emailInfo.getBottom() + 2)
                    .size(width + 6, this.font.lineHeight + 6)
                    .build());

            width = this.font.width(Component.translatable("info.inbox.delete"));
            btn = this.deleteEmailBtn = this.addRenderableWidget(GuiInbox.GuiButton.builder(
                            Component.translatable("info.inbox.delete"),
                            b-> {
                                if (this.getCurrentScheduledEmail()!=null && EmailUtils.isOP(Minecraft.getInstance().player)) {
                                    EmailMain.NETWORK.sendMessageToServer(new MsgScheduledEmail.Remove(this.getCurrentScheduledEmail().getId()));
                                    for (int i = 0; i < this.emails.size(); i++) {
                                        if (this.emails.get(i).getId() == this.getCurrentScheduledEmail().getId()) {
                                            this.deleteEmailBtn.visible = false;
                                            this.emails.remove(i);
                                            this.setCurrentScheduledEmail(-1);
                                            this.emailList.refresh();
                                            break;
                                        }
                                    }
                                }
                            }
                    )
                    .pos(btn.getX() - width - 8, this.emailInfo.getBottom() + 2)
                    .size(width + 6, this.font.lineHeight + 6)
                    .build());
            this.deleteEmailBtn.visible = false;
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
    public synchronized void refresh() {
        if (this.emailList!=null) {
            this.emailList.refresh();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        RenderUtils.drawString(graphics, I18n.get("info.inbox.scheduled"), this.emailList.getLeft(), 4, Color.WHITE.getRGB(), true);
        graphics.fill(this.emailInfo.getLeft(), this.emailInfo.getTop() - this.currentEmailTitle.getHeight()*2 - 5, this.emailInfo.getRight(), this.emailInfo.getTop(), 0xC0101010);
        EmailUtils.hLineGradient(graphics, false, this.currentEmailLastTime.getX(), this.currentEmailLastTime.getY() + this.currentEmailLastTime.getHeight() - 4, this.emailInfo.getRight(), this.currentEmailLastTime.getY() + this.currentEmailLastTime.getHeight() - 3, Color.YELLOW.getRGB(), 0);
        super.render(graphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(graphics, pMouseX, pMouseY);
        this.renderLabels(graphics, pMouseX, pMouseY);
    }

    protected void renderTooltip(GuiGraphics graphics, int pX, int pY) {
        for (int i = 0; i < this.emailList.children().size(); i++) {
            ScheduledEmailList.ScheduledEmailEntry entry = null;
            try {
                entry = this.emailList.children().get(i);
            }catch (Exception ignored){}

            if (entry!=null && EmailUtils.isInRange(pX, pY, this.emailList.getLeft(), this.emailList.getTop(), this.emailList.getWidth(), this.emailList.getHeight()) && entry.isMouseOver(pX, pY)) {
                List<String> tip = Lists.newArrayList();

                tip.add(entry.email.getAsEmail().getTitle().format());
                tip.add("");

                tip.add(I18n.get("info.inbox.scheduled.interval_time", this.getInterval(entry.email)));
                tip.add(I18n.get("info.inbox.scheduled.last_time", this.getLastTime(entry.email)));

                if (entry.email.getAsEmail().hasExpirationTime()) {
                    tip.add("");
                    tip.add(I18n.get("info.inbox.scheduled.expiration_time", ITimer.formatTimestamp(entry.email.getAsEmail().getExpirationTime().millis)));
                }

                if (entry.email.getAddressee().isCustomPlayers()) {
                    tip.add("");
                    if (EmailUtils.isOP(Minecraft.getInstance().player)) {
                        tip.add(I18n.get("info.inbox.scheduled.custom.players", entry.email.getCustomAddressee()));
                    }else {
                        tip.add(I18n.get("info.inbox.scheduled.custom"));
                    }
                }

                tip.add("");
                tip.add("ID: " + entry.email.getId());

                graphics.renderComponentTooltip(RenderUtils.getFontRenderer(), tip.stream().map(Component::literal).collect(Collectors.toList()), pX, pY);
                break;
            }
        }
    }

    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        if (this.getCurrentScheduledEmail()!=null) {
            this.currentEmailLastTime.setValue(I18n.get("info.inbox.scheduled.last_time", this.getCurrentLastTime()));
            if (this.getCurrentScheduledEmail().canSend()) {
                this.getCurrentScheduledEmail().refreshNextExecuteTime();
            }
        }else {
            this.currentEmailLastTime.setValue("");
        }
    }

    public Email getCurrentEmail() {
        if (this.getCurrentScheduledEmail()!=null) {
            return this.getCurrentScheduledEmail().getAsEmail();
        }
        return null;
    }
    public ScheduledEmail getCurrentScheduledEmail() {
        if (this.emailList.getSelected()!=null) {
            return this.emailList.getSelected().email;
        }
        return null;
    }
    public long getCurrentScheduledEmailID(){
        if (this.getCurrentScheduledEmail()!=null) {
            return this.getCurrentScheduledEmail().getId();
        }
        return -1;
    }

    public void addEmail(ScheduledEmail email) {
        this.emails.add(email);
    }

    public String getLastTime(ScheduledEmail email) {
        return ITimer.formatTimestamp(email.getNextExecuteTime() - System.currentTimeMillis(), false, true, true,true, true);
    }
    public String getCurrentLastTime() {
        if (this.getCurrentScheduledEmail()!=null) {
            return this.getLastTime(this.getCurrentScheduledEmail());
        }
        return "unknown";
    }
    public String getInterval(ScheduledEmail email) {
        return ITimer.formatTimestamp(email.getInterval().millis, false, true, true,true, true);
    }
    public String getCurrentInterval() {
        if (this.getCurrentScheduledEmail()!=null) {
            return this.getInterval(this.getCurrentScheduledEmail());
        }
        return "unknown";
    }

    private long lastID;
    public void setCurrentScheduledEmail(long id) {
        if (this.getCurrentScheduledEmailID()!=-1 && this.lastID == id) {
            this.emailList.setSelected(null);
            if (EmailUtils.isOP(Minecraft.getInstance().player)) {
                this.deleteEmailBtn.visible = false;
            }
            this.setCurrentScheduledEmail(-1);
            return;
        }
        ScheduledEmailList.ScheduledEmailEntry entry = null;
        for (ScheduledEmailList.ScheduledEmailEntry entry1 : this.emailList.children()) {
            if (entry1.email.getId() == id) {
                this.emailList.setSelected(entry1);
                entry = entry1;
                break;
            }
        }

        this.lastID = id;

        if (entry!=null) {
            Email email = entry.email.getAsEmail();
            this.currentEmailTitle.setValue(email.getTitle().format());
            this.emailInfo.clearMessage();
            if (EmailUtils.isOP(Minecraft.getInstance().player)) {
                this.deleteEmailBtn.visible = true;
            }

            int textMaxLength = this.currentEmailLastTime.getWidth()-13;
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

    @Override
    public void onClose() {
        super.onClose();
        GuiHandler.openGui(GuiHandler.EMAIL_MAIN);
    }

    private static class ScheduledEmailInfo extends ScrollPanel {
        private final GuiScheduledEmail parent;
        private final List<FormattedCharSequence> messages = new ArrayList<>();

        public ScheduledEmailInfo(GuiScheduledEmail parent, int x, int height) {
            super(parent.getMinecraft(), parent.currentEmailLastTime.getWidth(), height, parent.currentEmailLastTime.getY() + parent.currentEmailLastTime.getHeight(), x);
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
                int height = this.messages.size() * (this.parent.font.lineHeight + 2);
                height += this.parent.font.lineHeight + 2;

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

    public static class ScheduledEmailList extends ObjectSelectionList<ScheduledEmailList.ScheduledEmailEntry> {
        private final GuiScheduledEmail parent;

        public ScheduledEmailList(GuiScheduledEmail parent, int width, int height, int x, int y) {
            super(Minecraft.getInstance(), width, height, y, y + height, RenderUtils.getFontRenderer().lineHeight * 2 + 8);
            this.parent = parent;
            this.setLeftPos(x);
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
            this.setRenderHeader(false, 0);
        }

        public void refresh() {
            List<ScheduledEmailEntry> entries = new ArrayList<>();
            for (ScheduledEmail email : this.parent.emails) {
                entries.add(new ScheduledEmailEntry(this.parent, email, this.width));
            }
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
        public void addEntryToTop(ScheduledEmailEntry pEntry) {
            super.addEntryToTop(pEntry);
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

        public static class ScheduledEmailEntry extends ObjectSelectionList.Entry<ScheduledEmailList.ScheduledEmailEntry> {
            private final GuiScheduledEmail parent;
            private final ScheduledEmail email;
            private final int width;
            private Component note, interval;

            public ScheduledEmailEntry(GuiScheduledEmail parent, ScheduledEmail email, int width) {
                this.parent = parent;
                this.email = email;
                this.width = width;
                this.updata();
            }

            @Override
            public Component getNarration() {
                return Component.empty();
            }

            @Override
            public void render(GuiGraphics graphics, int index, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
                graphics.drawString(RenderUtils.getFontRenderer(), this.note, pLeft + 2, pTop + 4, Color.WHITE.getRGB());
                graphics.drawString(RenderUtils.getFontRenderer(), this.interval, pLeft + 2, pTop + RenderUtils.getFontRenderer().lineHeight + 2 + 3, Color.WHITE.getRGB());
//                EmailUtils.drawAlignRightString(graphics, RenderUtils.getFontRenderer(), this.state, pLeft + pWidth - 9 - 3, pTop + 4, 0, true);
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
                    this.parent.setCurrentScheduledEmail(this.email.getId());
                }
                return false;
            }

            public void updata() {
                String note;
                if (StringUtil.isNullOrEmpty(this.email.getNote())) {
                    note = this.email.getAsEmail().getTitle().format();
                }else {
                    note = this.email.getNote();
                }
                this.note = Component.literal(RenderUtils.getFontRenderer().plainSubstrByWidth(note, this.width));
                this.interval = Component.literal(ITimer.formatTimestamp(this.email.getInterval().millis, false, true, true,true, true));
            }
        }
    }
}
