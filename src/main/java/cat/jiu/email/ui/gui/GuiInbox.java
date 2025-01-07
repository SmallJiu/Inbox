package cat.jiu.email.ui.gui;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.ISound;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.client.GifDecoder;
import cat.jiu.core.util.element.sound.SoundMC;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.event.InboxFilterEvent;
import cat.jiu.email.event.InboxPlaySoundEvent;
import cat.jiu.email.net.msg.MsgDeleteEmail;
import cat.jiu.email.net.msg.MsgDisplayInbox;
import cat.jiu.email.net.msg.MsgReadEmail;
import cat.jiu.email.net.msg.MsgReceiveEmail;
import cat.jiu.email.net.msg.refresh.MsgRefreshInbox;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.gui.component.EmailListWidget;
import cat.jiu.email.ui.gui.component.GuiImageButton;
import cat.jiu.email.ui.gui.component.GuiPopupMenu;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.client.EmailSenderSndSound;
import cat.jiu.core.util.client.AudioSystem;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("all")
public class GuiInbox extends Screen {
    public static final ResourceLocation BackGround = new ResourceLocation(EmailMain.MODID, "textures/gui/container/inbox_bg.png");
    public static final ResourceLocation ICON = new ResourceLocation(EmailMain.MODID, "textures/gui/container/inbox_icon.png");
    @Deprecated
    public static final ResourceLocation load = new ResourceLocation(EmailMain.MODID, "textures/gui/load.png");
    public static final GifDecoder.GifTexture LOADING_GIF = GifDecoder.getTexture(EmailMain.class.getResourceAsStream("/assets/email/textures/gui/loading.gif"), -1);
    private EmailListWidget emailList;
    private EmailInfo emailInfo;
    private EditBox currentEmailTitle, currentEmailCreateTime;
    private Button deleteEmailBtn, acceptEmailBtn, filterEmailBtn, functionMenuBtn;
    private GuiImageButton playSoundBtn, refreshBtn;
    private final GuiPopupMenu filterMenu = new GuiPopupMenu(), functionMenu = new GuiPopupMenu();
    private final Inbox inbox = Inbox.getEmpty("");
    private long inboxSize = -1, currentEmailID = -1;

    public GuiInbox() {
        super(Component.nullToEmpty(null));
//        this.imageWidth = EmailConfigs.Main.Size.Width.get();
//        this.imageHeight = EmailConfigs.Main.Size.Height.get();
        this.filterMenu.setResetButtonSize(true, false);
        this.functionMenu.setResetButtonSize(true, false);
//        GuiInbox.loading.writeToFile(new File("C:/test.png"));
        ;
    }

    public static void display() {
        Minecraft.getInstance().setScreen(new GuiInbox());
        EmailMain.net.sendMessageToServer(new MsgDisplayInbox(true));
        refresh();
    }

    public static void refresh() {
        if (Minecraft.getInstance().screen instanceof GuiInbox) {
            EmailMain.net.sendMessageToServer(new MsgRefreshInbox());
        }
    }

    @Override
    protected void init() {
        Font font = this.getFont();
        int listWidth = EmailConfigs.Layout.Email_List_Width.get();
        int x = 6, y = this.font.lineHeight + 6;

        this.emailList = this.addRenderableWidget(new EmailListWidget(this, listWidth, this.height - 35, x, y));
        x += this.emailList.getWidth() + 8;

        this.currentEmailTitle = this.addRenderableOnly(new EditBox(font, x+5, y+5, this.width - this.emailList.getWidth() - 20, font.lineHeight + 6, Component.nullToEmpty("this is a title")));
        this.currentEmailTitle.setEditable(false);
        this.currentEmailTitle.setTextColor(Color.WHITE.getRGB());
        this.currentEmailTitle.setTextColorUneditable(Color.WHITE.getRGB());
        this.currentEmailTitle.setBordered(false);

        this.currentEmailCreateTime = this.addRenderableOnly(new EditBox(font, this.currentEmailTitle.getX(), this.currentEmailTitle.getY() + this.currentEmailTitle.getHeight(), this.currentEmailTitle.getWidth(), this.currentEmailTitle.getHeight(), Component.nullToEmpty("2024/13/32 25:61:61")));
        this.currentEmailCreateTime.setEditable(false);
        this.currentEmailCreateTime.setTextColor(Color.WHITE.getRGB());
        this.currentEmailCreateTime.setTextColorUneditable(Color.WHITE.getRGB());
        this.currentEmailCreateTime.setBordered(false);

        this.emailInfo = this.addRenderableWidget(new EmailInfo(this, x, this.emailList.getHeight() - (this.currentEmailTitle.getHeight()*2) - 5));
        this.currentEmailTitle.setMaxLength(this.font.plainSubstrByWidth("888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888", this.emailInfo.getWidth() + 10).length());
//        this.emailList.refreshList();
        this.initButtons(x, y);
    }

    protected void initButtons(int x, int y) {
        this.playSoundBtn = this.addRenderableWidget(new GuiImageButton(this, this.currentEmailTitle.getX() + this.currentEmailTitle.getWidth() - this.currentEmailTitle.getHeight()*2 - 5, this.currentEmailTitle.getY(), this.currentEmailTitle.getHeight()*2 - 6, this.currentEmailTitle.getHeight()*2 - 6, ()->null, 256, 256, 0, 169, 56, 56, btn->
                this.playSound()
        )).setBackground(()->ICON);
        this.playSoundBtn.visible = false;

        int width = this.font.width(Component.translatable("info.inbox.filter.default"));
        this.filterEmailBtn = this.addRenderableWidget(GuiButton.builder(Component.translatable("info.inbox.filter.default"), b-> {
                    this.filterMenu.setVisible(!this.filterMenu.isVisible());
                    this.renderEmptyTitle = !this.renderEmptyTitle;
                    this.currentEmailTitle.setValue("");
                    this.currentEmailCreateTime.setValue("");
                    if (this.getCurrentEmailID()!=-1) {
                        this.setCurrentEmail(this.getCurrentEmailID());
                    }
                })
                .pos(this.emailList.getLeft(), this.emailList.getTop() - y + 2)
                .size(this.emailList.getWidth(), this.font.lineHeight + 1)
                .build());
        this.addRenderableWidget(this.filterMenu);
        this.filterMenu.setPosition(this.filterEmailBtn.getX() + this.filterEmailBtn.getWidth() + 2, this.filterEmailBtn.getY());
        this.initFilterMenu();

        this.refreshBtn = this.addRenderableWidget(new GuiImageButton(this, this.emailList.getLeft(), this.emailList.getBottom()+1, this.getFont().lineHeight*2+1, this.getFont().lineHeight*2+1, ()->
                Component.translatable(refreshCoolingTicks <= 0 ? "info.inbox.refresh" : "info.inbox.refresh.cooling")
                , 256, 256, 111, 169, 55, 55, btn->
                this.refresh(()->{
                            this.stopSound();
                            this.setCurrentEmail(-1);
                            this.refreshBtn.visible = false;
                            this.renderEmptyTitle = true;
                            this.inbox.deleteAllEmail();
                            this.updataInboxSize();
                            this.emailList.clearEntries();
                            this.emailInfo.clearMessage();
                            EmailMain.net.sendMessageToServer(new MsgRefreshInbox());
                        }, ()-> this.refreshBtn.visible = true
                )
        )).setBackground(()->ICON);
        this.refreshBtn.visible = refreshCoolingTicks <= 0;

        Button btn = this.addRenderableWidget(GuiButton.builder(
                        Component.translatable("info.inbox.black.close"),
                        b-> this.getMinecraft().setScreen(null)
                )
                .pos(this.emailInfo.getRight() - width - 6, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.lineHeight + 6)
                .build());

        width = this.font.width(Component.translatable("info.inbox.menu"));
        this.functionMenuBtn = btn = this.addRenderableWidget(GuiButton.builder(
                        Component.translatable("info.inbox.menu"),
                        b-> {
                            this.functionMenu.setPosition((this.functionMenuBtn.getX() + this.functionMenuBtn.getWidth()/2) - this.functionMenu.getWidth()/2, this.functionMenuBtn.getY() - this.functionMenu.getHeight() - 5);
                            this.functionMenu.setVisible(!this.functionMenu.isVisible());
                        }
                )
                .pos(btn.getX() - width - 8, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.lineHeight + 6)
                .build());
        this.addRenderableWidget(this.functionMenu);
        this.initFunctionMenu();

        width = this.font.width(Component.translatable("info.inbox.delete"));
        btn = this.deleteEmailBtn = this.addRenderableWidget(GuiButton.builder(
                        Component.translatable("info.inbox.delete"),
                        b-> {
                            EmailMain.net.sendMessageToServer(new MsgDeleteEmail.Delete(this.getCurrentEmailID()));
                            for (int i = 0; i < this.emailList.children().size(); i++) {
                                if (this.emailList.children().get(i).getEmailId() == this.getCurrentEmailID()) {
                                    this.inbox.deleteEmail(this.getCurrentEmailID());
                                    this.updataInboxSize();
                                    this.emailList.refreshList();
                                    this.deleteEmailBtn.visible = false;
                                    this.acceptEmailBtn.visible = false;
                                    break;
                                }
                            }
                        }
                )
                .pos(btn.getX() - width - 8, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.lineHeight + 6)
                .build());
        this.deleteEmailBtn.visible = false;

        width = this.font.width(Component.translatable("info.inbox.accept"));
        btn = this.acceptEmailBtn = this.addRenderableWidget(GuiButton.builder(
                        Component.translatable("info.inbox.accept"),
                        b-> {
                            EmailMain.net.sendMessageToServer(new MsgReceiveEmail.Receive(this.getCurrentEmailID()));
                            if (this.emailList.getSelected()!=null) {
                                this.acceptEmailBtn.visible = false;
                                this.emailList.getSelected().getEmail().receive(getMinecraft().player);
                                this.updataInboxSize();
                            }
                        }
                )
                .pos(btn.getX() - width - 8, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.lineHeight + 6)
                .build());
        this.acceptEmailBtn.visible = false;
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(graphics);
        graphics.fillGradient(this.emailInfo.getLeft(), this.emailInfo.getTop() - this.currentEmailTitle.getHeight()*2 - 5, this.emailInfo.getRight(), this.emailInfo.getTop(), 0xC0101010, 0xC0101010);
        EmailUtils.hLineGradient(graphics, false, this.currentEmailCreateTime.getX(), this.currentEmailCreateTime.getY() + this.currentEmailCreateTime.getHeight() - 4, this.emailInfo.getRight(), this.currentEmailCreateTime.getY() + this.currentEmailCreateTime.getHeight() - 3, Color.YELLOW.getRGB(), 0);
        super.render(graphics, pMouseX, pMouseY, pPartialTick);

        graphics.drawString(this.getFont(), this.getInboxSize() +" Bytes", this.refreshBtn.getX() + this.refreshBtn.getWidth() + 4, this.refreshBtn.getY() + this.refreshBtn.getHeight()/2 - this.getFont().lineHeight/2, Color.WHITE.getRGB());
//        this.loadImage.draw(graphics, this.refreshBtn.getX(), this.refreshBtn.getY());
        if (!this.refreshBtn.visible) GuiInbox.LOADING_GIF
                .setRenderPos(this.refreshBtn.getX(), this.refreshBtn.getY() - 5)
                .setRenderSize(this.refreshBtn.getWidth(), this.refreshBtn.getHeight())
                .render(graphics);
        this.renderTooltip(graphics, pMouseX, pMouseY);
        this.updataData();
    }

    protected void renderTooltip(GuiGraphics graphics, int pX, int pY) {
        for (int i = 0; i < this.emailList.children().size(); i++) {
            EmailListWidget.EmailEntry entry = null;
            try {
                entry = this.emailList.children().get(i);
            }catch (Throwable ignored){}
            if (entry!=null && EmailUtils.isInRange(pX, pY, this.emailList.getLeft(), this.emailList.getTop(), this.emailList.getWidth(), this.emailList.getHeight()) && entry.isMouseOver(pX, pY)) {
                List<String> tip = Lists.newArrayList();

                tip.add(entry.getEmail().getTitle().format());
                tip.add("");

                tip.add(ChatFormatting.GRAY + entry.getEmail().getCreateTimeAsString());
                tip.add(I18n.get("info.inbox.main.from", entry.getEmail().getSender().format()));

                tip.add("");

                tip.add(I18n.get("info.inbox.email_size", entry.getEmail().getEmailNetworkSize()));
                if (entry.getEmail().getExpirationTime() != null) {
                    tip.add("");
                    if (entry.getEmail().isExpiration()) {
                        tip.add(String.format("%s: %s", I18n.get("inbox.config.expiration"), ChatFormatting.RED + I18n.get("inbox.config.expiration.ed")));
                    } else {
                        tip.add(I18n.get("info.inbox.remain_expiration_time", EmailUtils.getExpirationTime(entry.getEmail())));
                    }
                }
                tip.add(String.format("ID: %s", entry.getEmailId()));
                graphics.renderComponentTooltip(this.getFont(), tip.stream().map(Component::literal).collect(Collectors.toList()), pX, pY);
            }
        }
        Email email = this.getCurrentEmail();

        // TODO 绘制附带音效
        if (email != null) {
            if (email.hasSound()) {
                // TODO
//			    MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.SOUND, TickEvent.Phase.START, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
                if(!this.isPlayingSound() && this.currentSound != null) {
                    this.stopSound();
                }

                this.playSoundBtn.setUOffset(this.isPlayingSound() ? 55 : 0);

                if(this.playSoundBtn.isMouseOver(pX, pY)) {
                    List<Component> hover = Lists.newArrayList();
                    hover.add(Component.translatable("info.inbox.play_sound" + (this.isPlayingSound() ? ".stop" : "")));
                    if(this.isPlayingSound()) {
                        if (this.currentAudioUUID != null) {
                            if (this.currentAudioDuration == null) {
                                this.currentAudioDuration = formatMs(AudioSystem.getFloatDuration(this.currentAudioUUID));
                            }
                            hover.add(Component.literal(String.format("%s / %s", formatMs(AudioSystem.getFloatElapse(this.currentAudioUUID)), this.currentAudioDuration)));
                        }else if(this.currentSound!=null) {
                            hover.add(Component.literal(this.currentSound.time.toStringTime(false)));
                        }else {
                            InboxPlaySoundEvent.Tick event = new InboxPlaySoundEvent.Tick(this.getCurrentEmail());
                            MinecraftForge.EVENT_BUS.post(event);
                            hover.add(Component.literal(String.format("%s / %s", event.getElapse(), event.getDuration())));
                        }
                    }else {
                        hover.add(Component.translatable("info.inbox.play_sound.category", Component.translatable("soundCategory."+ (email.isExternalSound() ? email.getExternalSound().getCategory().getName() : email.getSound().getSoundChannel().getName()).toLowerCase())));
                    }
                    graphics.renderComponentTooltip(this.font, hover, pX, pY);
                }
//			    MinecraftForge.EVENT_BUS.post(new InboxDrawEvent(this, Type.SOUND, TickEvent.Phase.END, this.container.getInbox(), this.currentEmail, mouseX, mouseY));
            }
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (!super.mouseClicked(pMouseX, pMouseY, pButton)) {
            this.functionMenu.setVisible(false);
            this.filterMenu.setVisible(false);
            if (pButton == 1) {
                for (EmailListWidget.EmailEntry entry : this.emailList.children()) {
                    if (EmailUtils.isInRange(pMouseX, pMouseY, this.emailList.getLeft(), this.emailList.getTop(), this.emailList.getWidth(), this.emailList.getHeight()) && entry.isMouseOver(pMouseX, pMouseY)) {

                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        boolean ret = this.getFocused() != null && this.isDragging() && pButton == 0 && this.getFocused().mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
        return ret || super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    private void initFunctionMenu() {
        this.functionMenu.clearPopupButtons();
        this.addFunctionButton(I18n.get("info.inbox.dispatch"), ()-> GuiHandler.openGui(GuiHandler.EMAIL_SEND));
        this.addFunctionButton(I18n.get("info.inbox.delete_read"), ()-> {
            EmailMain.net.sendMessageToServer(new MsgDeleteEmail.AllRead());
            for (int i = 0; i < this.emailList.children().size(); i++) {
                EmailListWidget.EmailEntry entry = this.emailList.children().get(i);
                if(entry.getEmail().isRead() && !entry.getEmail().hasItems()) {
                    this.inbox.deleteEmail(entry.getEmailId());
                }
            }
            this.emailList.refreshList();
        });
        this.addFunctionButton(I18n.get("info.inbox.accept_all"), ()-> {
            EmailMain.net.sendMessageToServer(new MsgReceiveEmail.All());
            for (int i = 0; i < this.emailList.children().size(); i++) {
                EmailListWidget.EmailEntry entry = this.emailList.children().get(i);
                if(entry.getEmail().hasAttachment() && !entry.getEmail().isReceived()) {
                    entry.getEmail().receive(getMinecraft().player);
                }
            }
        });

        this.addFunctionButton(I18n.get("info.inbox.black"), ()-> GuiHandler.openGui(GuiHandler.EMAIL_BLACKLIST));
        this.addFunctionButton(I18n.get("info.inbox.scheduled"), () -> Minecraft.getInstance().setScreen(new GuiScheduledEmail()));

        if (EmailUtils.isOP(Minecraft.getInstance().player)) {
            this.addFunctionButton(I18n.get("info.inbox.generate"), ()-> GuiHandler.openGui(GuiHandler.EMAIL_Generate));
        }
    }

    public void addFunctionButton(String name, Runnable runnable) {
        this.functionMenu.addPopupButton(GuiButton.builder(Component.literal(name), b->runnable.run()).bounds(0, 0, this.getFont().width(name) + 6, this.getFont().lineHeight + 2).build());
    }

    private void initFilterMenu() {
        this.filterMenu.clearPopupButtons();
        this.addFilter();
		MinecraftForge.EVENT_BUS.post(new InboxFilterEvent(this));
    }

    private void addFilter() {
        this.addFilter(I18n.get("info.inbox.filter.default"), email -> true);

        this.addFilter(I18n.get("info.inbox.filter.is_read"), Email::isRead);
        this.addFilter(I18n.get("info.inbox.filter.not_read"), email -> !email.isRead());

        this.addFilter(I18n.get("info.inbox.filter.has_sound"), Email::hasSound);

        this.addFilter(I18n.get("info.inbox.filter.has_item"), Email::hasItems);
        this.addFilter(I18n.get("info.inbox.filter.is_accept"), email -> email.hasItems() && email.isReceived());
        this.addFilter(I18n.get("info.inbox.filter.not_accept"), email -> email.hasItems() && !email.isReceived());

        this.addFilter(I18n.get("info.inbox.filter.has_expiration"), Email::hasExpirationTime);
        this.addFilter(I18n.get("info.inbox.filter.is_expiration"), email -> email.hasExpirationTime() && email.isExpiration());
        this.addFilter(I18n.get("info.inbox.filter.not_expiration"), email -> email.hasExpirationTime() && !email.isExpiration());
    }

    boolean renderEmptyTitle = true;
    public void addFilter(String name, Predicate<Email> predicate) {
        this.filterMenu.addPopupButton(GuiButton.builder(Component.literal(name), b->{
            this.emailList.refreshList(predicate);
            this.filterEmailBtn.setMessage(Component.literal(name));
            this.filterMenu.setVisible(false);
            this.renderEmptyTitle = !this.renderEmptyTitle;
        }).bounds(0, 0, 45, 12).build());
    }

    public void setCurrentEmail(long id) {
        if (this.getCurrentEmailID() != -1 && this.getCurrentEmailID() == id) {
            this.emailList.setSelected(null);
            this.deleteEmailBtn.visible = false;
            this.acceptEmailBtn.visible = false;
            this.playSoundBtn.visible = false;
            this.setCurrentEmail(-1);
            return;
        }
        if (id != this.getCurrentEmailID()) {
            this.stopSound();
        }
        this.currentEmailID = id;
        if (id!= -1 && this.getCurrentEmail()!=null) {
            Email email = this.getCurrentEmail();
            this.currentEmailTitle.setValue(email.getTitle().format());
            this.currentEmailCreateTime.setValue(email.getCreateTimeAsString());
            this.emailInfo.clearMessage();

            try {
                this.deleteEmailBtn.visible = true;
                this.acceptEmailBtn.visible = email.hasAttachment() && !email.isReceived();
                this.playSoundBtn.visible = email.hasSound();
            }catch (Throwable e){ }

            if(!email.isRead()) {
                EmailMain.net.sendMessageToServer(new MsgReadEmail(id));
                email.setRead(true);
                this.updataInboxSize();
            }

            int textMaxLength = this.currentEmailCreateTime.getWidth()-13;
            for (IText message : email.getMessages()) {
                Component component = message.toTextComponent();

                List<FormattedCharSequence> texts;
                if (EmailConfigs.Layout.Enable_Vanilla_Wrap_Text.get()) {
                    texts = Language.getInstance().getVisualOrder(font.getSplitter().splitLines(component, textMaxLength, Style.EMPTY));
                }else {
                    texts = EmailUtils.splitString(message.format(), textMaxLength).stream().map(e->FormattedCharSequence.backward(e, Style.EMPTY)).collect(Collectors.toList());
                }
                texts.forEach(this.emailInfo::addMessage);
            }
            this.emailInfo.setScrollDistance(0);
        }else {
            this.currentEmailTitle.setValue("");
            this.currentEmailCreateTime.setValue("");
            this.emailInfo.clearMessage();
        }
    }

    private static String formatMs(float ms){
        return ITimer.formatTimestamp((long) ms, false, false, true, true, true);
    }

    private EmailSenderSndSound currentSound;
    private UUID currentAudioUUID;
    private String currentAudioDuration;
    public ISound getPlayingSound() {
        return this.getCurrentEmail().getSound().copy();
    }
    public AudioSystem.Audio getPlayingNetworkOrLocalSound(){
        return this.getCurrentEmail().getExternalSound();
    }
    public UUID getCurrentAudioUUID() {
        return currentAudioUUID;
    }

    public boolean isPlayingSound() {
        if(this.currentSound != null) {
            return this.getMinecraft().getSoundManager().isActive(this.currentSound) || !this.currentSound.isStopped();
        }else if (this.currentAudioUUID != null) {
            return !AudioSystem.isClose(this.currentAudioUUID) && !AudioSystem.isPaused(this.currentAudioUUID);
        }
        return false;
    }
    public void stopSound() {
        if (!MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Stop(this.getCurrentEmail()))) {
            if(this.currentSound != null) {
                this.getMinecraft().getSoundManager().stop(this.currentSound);
                this.currentSound = null;
            }
            if (this.currentAudioUUID != null) {
                AudioSystem.stop(this.currentAudioUUID);
                this.currentAudioUUID = null;
            }
        }
    }

    private void playSound(){
        if(this.getCurrentEmail()!=null && this.getCurrentEmail().hasSound()) {
            boolean played = false;

            if (!this.isPlayingSound()) {
                if (MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Start(this.getCurrentEmail()))) {
                    return;
                }
                if (this.getCurrentEmail().isExternalSound()) {
                    boolean check = true;
                    String file = this.getCurrentEmail().getExternalSound().getFile();
                    if (!(file.startsWith("http://") || file.startsWith("https://"))) {
                        if (!file.endsWith(".mp3") || !new File(file).exists()) {
//                            this.setRenderText(Timer.parseTick(10, 0), true, I18n.get("info.inbox.file_not_found", file), Color.RED);
                            check = false;
                        }
                    }
                    if (check) {
                        this.currentAudioUUID = AudioSystem.play(this.getCurrentEmail().getExternalSound());
                        this.currentAudioDuration = null;
                        played = true;
                    }
                }else if (this.currentSound == null && !MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Start(this.getCurrentEmail())) && this.getCurrentEmail().getSound() instanceof SoundMC) {
                    this.currentSound = new EmailSenderSndSound((SoundMC) this.getCurrentEmail().getSound(), this.getCurrentEmailID());
                    this.getMinecraft().getSoundManager().play(this.currentSound);
                    played = true;
                }
            }
            if (!played) {
                this.stopSound();
            }else {
                this.currentSoundCheck = 0;
                this.currentSoundLastTime = 0;
            }
//            this.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    public Font getFont() {
        return this.font;
    }

    public Email getCurrentEmail(){
        return this.inbox.getEmail(this.getCurrentEmailID());
    }
    public long getCurrentEmailID(){
        return this.currentEmailID;
    }

    public long getInboxSize() {
        return inboxSize;
    }
    public void updataInboxSize() {
        this.inboxSize = this.inbox.getInboxSize();
    }

    public Inbox getInbox() {
        return inbox;
    }

    public void addEmail(long id, Email emai) {
        this.inbox.setEmail(id, emai);
        this.updataInboxSize();
        long selected = -1;
        if (this.emailList.getSelected() != null) {
            selected = this.emailList.getSelected().getEmailId();
        }
        this.emailList.refreshList();
        this.setSelectEmail(selected, false);
    }

    public void setSelectEmail(long id, boolean updataInfo) {
        for (EmailListWidget.EmailEntry child : this.emailList.children()) {
            if (child.getEmailId() == id) {
                this.emailList.setSelected(child);
                break;
            }
        }
        if (updataInfo) {
            this.setCurrentEmail(id);
        }
    }

    private static int refreshCoolingTicks = 0;
    private static boolean hasRefreshThread;
    private void refresh(Runnable pre, Runnable done) {
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

    @Override
    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        double listScroll = this.emailList.getScrollAmount();
        float infoScroll = this.emailInfo.getScrollDistance();
        long currentEmail = this.getCurrentEmailID();

        super.resize(pMinecraft, pWidth, pHeight);

        this.emailList.setScrollAmount(listScroll);
        this.setCurrentEmail(-1);
        this.setSelectEmail(currentEmail, false);
        this.emailInfo.setScrollDistance(infoScroll);
        this.filterMenu.setVisible(this.filterMenu.isVisible());
        this.functionMenu.setVisible(this.functionMenu.isVisible());
    }

    @Override
    public void onClose() {
        super.onClose();
        this.stopSound();
        EmailMain.net.sendMessageToServer(new MsgDisplayInbox(false));
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == Minecraft.getInstance().options.keyInventory.getKey().getValue()) {
            this.onClose();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    private final Date now = new Date();
    int currentSoundCheck = 0;
    long currentSoundLastTime = 0;

    protected void updataData() {
        // TODO 显示未选择邮件时的信息
        if (this.renderEmptyTitle && this.getCurrentEmailID() == -1) {
            long last = System.currentTimeMillis();
            if (last - this.now.getTime() >= 1000) {
                this.now.setTime(last);
                this.currentEmailTitle.setValue(I18n.get("info.inbox.no_select_title"));
                this.currentEmailCreateTime.setValue(I18n.get("info.inbox.no_select_time", EmailUtils.dateFormat.format(this.now)));
            }
        }
        // TODO 检查播放的音效是否已停止
        if (this.currentSound!=null){
            if(this.currentSound.isStopped()) {
                this.stopSound();
            }
            this.currentSoundCheck++;
            if(this.currentSoundCheck >= 20) {
                this.currentSoundCheck = 0;
                if(this.currentSoundLastTime == this.currentSound.time.getTicks()) {
                    this.stopSound();
                }else {
                    this.currentSoundLastTime = this.currentSound.time.getTicks();
                }
            }
        }
        if (!this.isPlayingSound()) {
            this.currentSound = null;
            this.currentSoundCheck = 0;
            this.currentSoundLastTime = 0;

            this.currentAudioUUID = null;
            this.currentAudioDuration = null;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class EmailInfo extends ScrollPanel {
        private final GuiInbox parent;
        private final List<FormattedCharSequence> messages = new ArrayList<>();

        public EmailInfo(GuiInbox parent, int x, int height) {
            super(parent.getMinecraft(), parent.currentEmailCreateTime.getWidth(), height, parent.currentEmailCreateTime.getY() + parent.currentEmailCreateTime.getHeight(), x);
            this.parent = parent;
        }

        public void addMessage(FormattedCharSequence message) {
            this.messages.add(message);
        }
        public void clearMessage() {
            this.messages.clear();
        }

        @Override
        protected void drawPanel(GuiGraphics graphics, int x, int y, Tesselator tess, int mouseX, int mouseY) {
            x = this.left + 5;
            Email email = this.parent.getCurrentEmail();
            if (email != null) {
                Font font = this.parent.getFont();

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
                    AttachmentEvent.Render.Pre event = new AttachmentEvent.Render.Pre(email, attachment, graphics, this.parent.getFont(), this.width, this.height, x, y, mouseX, mouseY, this.top, this.bottom, this.left, this.right);
                    if (!MinecraftForge.EVENT_BUS.post(event)) {
                        attachment.render(event);
                        MinecraftForge.EVENT_BUS.post(new AttachmentEvent.Render.Post(email, attachment, graphics, this.parent.getFont(), this.width, this.height, x, y, mouseX, mouseY, this.top, this.bottom, this.left, this.right));
                    }
                    event.addY(4);
                    y = event.getY();
                }
            }
        }

        @Override
        protected int getContentHeight() {
            Email email = this.parent.getCurrentEmail();
            if (email != null) {
                int height = this.messages.size() * (this.parent.font.lineHeight + 2);
                height += this.parent.font.lineHeight + 2;

                for (IAttachment attachment : email.getAttachments()) {
                    AttachmentEvent.GetHeight event = new AttachmentEvent.GetHeight(email, attachment, this.parent.getFont(), this.width, this.height);

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
        protected int getScrollAmount() {
            return this.parent.font.lineHeight * 3;
        }
        @Override
        public NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(NarrationElementOutput pNarrationElementOutput) {

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

    public static class GuiButton extends Button {
        public static Builder builder(Component pMessage, OnPress pOnPress) {
            return new Builder(pMessage, pOnPress);
        }
        protected GuiButton(int pX, int pY, int pWidth, int pHeight, Component pMessage, OnPress pOnPress, CreateNarration pCreateNarration) {
            super(pX, pY, pWidth, pHeight, pMessage, pOnPress, pCreateNarration);
        }

        protected GuiButton(Builder builder) {
            this(builder.x, builder.y, builder.width, builder.height, builder.message, builder.onPress, builder.createNarration);
            setTooltip(builder.tooltip);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
            graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xC0101010);
            graphics.hLine(this.getX(), this.getX() + this.getWidth()-1, this.getY() + this.getHeight(), this.isMouseOver(pMouseX, pMouseY) ? Color.BLUE.getRGB() : Color.LIGHT_GRAY.getRGB());

            int i = getFGColor();
            this.renderString(graphics, Minecraft.getInstance().font, this.isMouseOver(pMouseX, pMouseY) ? Color.ORANGE.getRGB() : Color.LIGHT_GRAY.getRGB());
        }

        @OnlyIn(Dist.CLIENT)
        public static class Builder extends Button.Builder {
            private final Component message;
            private final OnPress onPress;
            @Nullable
            private Tooltip tooltip;
            private int x;
            private int y;
            private int width = 150;
            private int height = 20;
            private CreateNarration createNarration = Button.DEFAULT_NARRATION;

            public Builder(Component pMessage, OnPress pOnPress) {
                super(pMessage, pOnPress);
                this.message = pMessage;
                this.onPress = pOnPress;
            }

            public Builder pos(int pX, int pY) {
                this.x = pX;
                this.y = pY;
                return this;
            }

            public Builder width(int pWidth) {
                this.width = pWidth;
                return this;
            }

            public Builder size(int pWidth, int pHeight) {
                this.width = pWidth;
                this.height = pHeight;
                return this;
            }

            public Builder bounds(int pX, int pY, int pWidth, int pHeight) {
                return this.pos(pX, pY).size(pWidth, pHeight);
            }

            public Builder tooltip(@Nullable Tooltip pTooltip) {
                this.tooltip = pTooltip;
                return this;
            }

            public Builder createNarration(CreateNarration pCreateNarration) {
                this.createNarration = pCreateNarration;
                return this;
            }

            public GuiButton build() {
                return build1(GuiButton::new);
            }

            public GuiButton build1(java.util.function.Function<Builder, GuiButton> builder) {
                return builder.apply(this);
            }
        }
    }
}
