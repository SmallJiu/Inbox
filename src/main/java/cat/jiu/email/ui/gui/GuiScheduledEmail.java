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
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.gui.component.GuiImageButton;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;
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
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.gui.ScrollPanel;
import net.minecraftforge.common.MinecraftForge;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuiScheduledEmail extends Screen {
    private TextFieldWidget currentEmailTitle, currentEmailLastTime;
    private ScheduledEmailList emailList;
    private ScheduledEmailInfo emailInfo;
    private Button deleteEmailBtn, addBtn, refreshBtn;
//    private final GuiDynamicImage loadImage = new GuiDynamicImage(GuiInbox.load, 18, false, 32, 32, 0, 0, 16, 16, 32, 576);
    private final List<ScheduledEmail> emails = new ArrayList<>();

    public GuiScheduledEmail() {
        super(ITextComponent.getTextComponentOrEmpty(null));
        EmailMain.net.sendMessageToServer(MsgRefreshScheduledEmail.REFRESH_MAIN);
    }

    @Override
    protected void init() {
        super.init();
        FontRenderer font = RenderUtils.getFontRenderer();
        int listWidth = EmailConfigs.Layout.Email_List_Width.get();
        int x = 6, y = this.font.FONT_HEIGHT + 6;

        this.emailList = this.addListener(new ScheduledEmailList(this, listWidth, this.height - 35, x, y));
        x += this.emailList.getWidth() + 8;

        this.currentEmailTitle = new TextFieldWidget(font, x+5, y+5, this.width - this.emailList.getWidth() - 20, font.FONT_HEIGHT + 6, ITextComponent.getTextComponentOrEmpty("this is a title"));
        this.currentEmailTitle.setEnabled(false);
        this.currentEmailTitle.setTextColor(Color.WHITE.getRGB());
        this.currentEmailTitle.setDisabledTextColour(Color.WHITE.getRGB());
        this.currentEmailTitle.setEnableBackgroundDrawing(false);

        this.currentEmailLastTime = new TextFieldWidget(font, this.currentEmailTitle.x, this.currentEmailTitle.y + this.currentEmailTitle.getHeight(), this.currentEmailTitle.getWidth(), this.currentEmailTitle.getHeight(), ITextComponent.getTextComponentOrEmpty("2024/13/32 25:61:61"));
        this.currentEmailLastTime.setEnabled(false);
        this.currentEmailLastTime.setTextColor(Color.WHITE.getRGB());
        this.currentEmailLastTime.setDisabledTextColour(Color.WHITE.getRGB());
        this.currentEmailLastTime.setEnableBackgroundDrawing(false);

        this.emailInfo = this.addListener(new ScheduledEmailInfo(this, x, this.emailList.getHeight() - (this.currentEmailTitle.getHeight()*2) - 5));
        this.currentEmailTitle.setMaxStringLength(this.font.trimStringToWidth("888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888", this.emailInfo.getWidth() + 10).length());

        this.lastID = -1;

        this.initButtons(x, y);
        this.refresh();
    }

    protected void initButtons(int x, int y) {
        this.refreshBtn = this.addButton(new GuiImageButton(this, this.emailList.getLeft(), this.emailList.getBottom()+1, RenderUtils.getFontRenderer().FONT_HEIGHT*2+1, RenderUtils.getFontRenderer().FONT_HEIGHT*2+1, ()->
                new TranslationTextComponent(refreshCoolingTicks <= 0 ? "info.inbox.refresh" : "info.inbox.refresh.cooling")
                , 256, 256, 111, 169, 55, 55, btn->
                this.refresh(()->{
                            this.setCurrentScheduledEmail(-1);
                            this.refreshBtn.visible = false;
//                            this.loadImage.visible = true;
                            this.emails.clear();
                            this.emailList.clearPaths();
                            this.emailInfo.clearMessage();
                            EmailMain.net.sendMessageToServer(MsgRefreshScheduledEmail.REFRESH_MAIN);
                        }, ()->
                                this.refreshBtn.visible = true
                )
        )).setBackground(()-> GuiInbox.ICON);
        this.refreshBtn.visible = refreshCoolingTicks <= 0;
//        this.loadImage.visible = false;
//        this.loadImage.width = this.refreshBtn.getWidth();
//        this.loadImage.height = this.refreshBtn.getHeight();

        int width = RenderUtils.width(new TranslationTextComponent("info.inbox.black.back"));
        Button btn = this.addButton(GuiInbox.GuiButton.builder(
                        new TranslationTextComponent("info.inbox.black.back"),
                        b-> GuiHandler.openGui(GuiHandler.EMAIL_MAIN)
                )
                .pos(this.emailInfo.getRight() - width - 6, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.FONT_HEIGHT + 6)
                .build());

        if (EmailUtils.isOP(Minecraft.getInstance().player)) {
            width = RenderUtils.width(new TranslationTextComponent("info.inbox.black.add"));
            this.addBtn = btn = this.addButton(GuiInbox.GuiButton.builder(
                            new TranslationTextComponent("info.inbox.black.add"),
                            b->
                                    Minecraft.getInstance().displayGuiScreen(new GuiGenerateScheduledEmail(()->GuiHandler.openGui(GuiHandler.EMAIL_Scheduled)))
                    )
                    .pos(btn.x - width - 8, this.emailInfo.getBottom() + 2)
                    .size(width + 6, this.font.FONT_HEIGHT + 6)
                    .build());

            width = RenderUtils.width(new TranslationTextComponent("info.inbox.delete"));
            btn = this.deleteEmailBtn = this.addButton(GuiInbox.GuiButton.builder(
                            new TranslationTextComponent("info.inbox.delete"),
                            b-> {
                                if (this.getCurrentScheduledEmail()!=null && EmailUtils.isOP(Minecraft.getInstance().player)) {
                                    EmailMain.net.sendMessageToServer(new MsgScheduledEmail.Remove(this.getCurrentScheduledEmail().getId()));
                                    for (int i = 0; i < this.emails.size(); i++) {
                                        if (this.emails.get(i).getId() == this.getCurrentScheduledEmail().getId()) {
                                            this.deleteEmailBtn.visible = false;
                                            this.emails.remove(i);
                                            this.setCurrentScheduledEmail(-1);
                                            this.emailList.refresh();
                                            this.emailList.setSelected(null);
                                            this.emailInfo.clearMessage();
                                            break;
                                        }
                                    }
                                }
                            }
                    )
                    .pos(btn.x - width - 8, this.emailInfo.getBottom() + 2)
                    .size(width + 6, this.font.FONT_HEIGHT + 6)
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
    public void render(MatrixStack stack, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(stack);

        RenderUtils.drawString(stack, I18n.format("info.inbox.scheduled"), this.emailList.getLeft(), 4, Color.WHITE.getRGB(), true);
        RenderUtils.fill(stack, this.emailInfo.getLeft(), this.emailList.getTop(), this.currentEmailTitle.getWidth(), this.currentEmailTitle.getHeight()*2 + 5, 0xC0101010);
        RenderUtils.hLineGradient(stack, false, this.emailInfo.getLeft() + 5, this.emailInfo.getTop() - 3, this.emailInfo.getWidth() - 10, 1, Color.YELLOW.getRGB(), 0);

        this.emailList.render(stack, pMouseX, pMouseY, pPartialTick);
        this.emailInfo.render(stack, pMouseX, pMouseY, pPartialTick);

        super.render(stack, pMouseX, pMouseY, pPartialTick);

        this.currentEmailTitle.render(stack, pMouseX, pMouseY, pPartialTick);
        this.currentEmailLastTime.render(stack, pMouseX, pMouseY, pPartialTick);

        if (!this.refreshBtn.visible) GuiInbox.LOADING_GIF
                .setRenderPos(this.refreshBtn.x, this.refreshBtn.y - 5)
                .setRenderSize(this.refreshBtn.getWidth(), this.refreshBtn.getHeight())
                .render(stack);

        this.renderTooltip(stack, pMouseX, pMouseY);
        this.renderLabels(stack, pMouseX, pMouseY);
    }

    protected void renderTooltip(MatrixStack stack, int pX, int pY) {
        for (int i = 0; i < this.emailList.getEventListeners().size(); i++) {
            ScheduledEmailList.ScheduledEmailEntry entry = null;
            try {
                entry = this.emailList.getEventListeners().get(i);
            }catch (Exception ignored){}

            if (entry!=null && EmailUtils.isInRange(pX, pY, this.emailList.getLeft(), this.emailList.getTop(), this.emailList.getWidth(), this.emailList.getHeight()) && entry.isMouseOver(pX, pY)) {
                List<String> tip = Lists.newArrayList();

                tip.add(entry.email.getAsEmail().getTitle().format());
                tip.add("");

                tip.add(I18n.format("info.inbox.scheduled.interval_time", this.getInterval(entry.email)));
                tip.add(I18n.format("info.inbox.scheduled.last_time", this.getLastTime(entry.email)));

                if (entry.email.getAsEmail().hasExpirationTime()) {
                    tip.add("");
                    tip.add(I18n.format("info.inbox.scheduled.expiration_time", ITimer.formatTimestamp(entry.email.getAsEmail().getExpirationTime().millis)));
                }

                if (entry.email.getAddressee().isCustomPlayers()) {
                    tip.add("");
                    if (EmailUtils.isOP(Minecraft.getInstance().player)) {
                        tip.add(I18n.format("info.inbox.scheduled.custom.players", entry.email.getCustomAddressee()));
                    }else {
                        tip.add(I18n.format("info.inbox.scheduled.custom"));
                    }
                }

                tip.add("");
                tip.add("ID: " + entry.email.getId());

                RenderUtils.drawComponentTooltip(stack, tip.stream().map(ITextComponent::getTextComponentOrEmpty).collect(Collectors.toList()), pX, pY);
                break;
            }
        }
    }

    protected void renderLabels(MatrixStack stack, int pMouseX, int pMouseY) {
        if (this.getCurrentScheduledEmail()!=null) {
            this.currentEmailLastTime.setText(I18n.format("info.inbox.scheduled.last_time", this.getCurrentLastTime()));
            if (this.getCurrentScheduledEmail().canSend()) {
                this.getCurrentScheduledEmail().refreshNextExecuteTime();
            }
        }else {
            this.currentEmailLastTime.setText("");
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
        for (ScheduledEmailList.ScheduledEmailEntry entry1 : this.emailList.getEventListeners()) {
            if (entry1.email.getId() == id) {
                this.emailList.setSelected(entry1);
                entry = entry1;
                break;
            }
        }

        this.lastID = id;

        if (entry!=null) {
            Email email = entry.email.getAsEmail();
            this.currentEmailTitle.setText(email.getTitle().format());
            this.emailInfo.clearMessage();
            if (EmailUtils.isOP(Minecraft.getInstance().player)) {
                this.deleteEmailBtn.visible = true;
            }

            int textMaxLength = this.currentEmailLastTime.getWidth()-13;
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == Minecraft.getInstance().gameSettings.keyBindInventory.getKey().getKeyCode()) {
            this.closeScreen();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void closeScreen() {
        super.closeScreen();
        GuiHandler.openGui(GuiHandler.EMAIL_MAIN);
    }

    private static class ScheduledEmailInfo extends ScrollPanel {
        private final GuiScheduledEmail parent;
        private final List<IReorderingProcessor> messages = new ArrayList<>();

        public ScheduledEmailInfo(GuiScheduledEmail parent, int x, int height) {
            super(parent.getMinecraft(), parent.currentEmailLastTime.getWidth(), height, parent.currentEmailLastTime.y + parent.currentEmailLastTime.getHeight(), x);
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
                int height = this.messages.size() * (this.parent.font.FONT_HEIGHT + 2);
                height += this.parent.font.FONT_HEIGHT + 2;

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

    public static class ScheduledEmailList extends ExtendedList<ScheduledEmailList.ScheduledEmailEntry> {
        private final GuiScheduledEmail parent;

        public ScheduledEmailList(GuiScheduledEmail parent, int width, int height, int x, int y) {
            super(Minecraft.getInstance(), width, height, y, y + height, RenderUtils.getFontRenderer().FONT_HEIGHT * 2 + 8);
            this.parent = parent;
            this.setLeftPos(x);
            this.setRenderSelection(false);
            this.func_244605_b(false);
            this.func_244606_c(false);
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

        public void clearPaths() {
            super.clearEntries();
        }

        public class ScheduledEmailEntry extends ExtendedList.AbstractListEntry<ScheduledEmailList.ScheduledEmailEntry> {
            private final GuiScheduledEmail parent;
            private final ScheduledEmail email;
            private final int width;
            private ITextComponent note, interval;

            public ScheduledEmailEntry(GuiScheduledEmail parent, ScheduledEmail email, int width) {
                this.parent = parent;
                this.email = email;
                this.width = width;
                this.updata();
            }

            @Override
            public void render(MatrixStack stack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTicks) {
                ScheduledEmailEntry entry = ScheduledEmailList.this.getSelected();
                boolean isScroll = ScheduledEmailList.this.getMaxScroll() > 0;
                if (entry != null && entry.email.getId() == this.email.getId()) {
                    this.renderSelection(stack, index, top, left, width, height, mouseX, mouseY, isScroll, isMouseOver, partialTicks);
                }else {
                    this.renderBack(stack, index, top, left, width, height, mouseX, mouseY, isScroll, isMouseOver, partialTicks);
                }
                RenderUtils.drawComponent(stack, this.note, left + 2, top + 4, Color.WHITE.getRGB(), true);
                RenderUtils.drawComponent(stack, this.interval, left + 2, top + RenderUtils.getFontRenderer().FONT_HEIGHT + 2 + 3, Color.WHITE.getRGB(), true);
//                EmailUtils.drawAlignRightString(graphics, RenderUtils.getFontRenderer(), this.state, pLeft + pWidth - 9 - 3, pTop + 4, 0, true);
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
                this.note = ITextComponent.getTextComponentOrEmpty(RenderUtils.getFontRenderer().trimStringToWidth(note, this.width));
                this.interval = ITextComponent.getTextComponentOrEmpty(ITimer.formatTimestamp(this.email.getInterval().millis, false, true, true,true, true));
            }
        }
    }
}
