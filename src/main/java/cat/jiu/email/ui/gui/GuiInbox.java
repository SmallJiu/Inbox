package cat.jiu.email.ui.gui;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.ISound;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.client.GifDecoder;
import cat.jiu.core.util.client.RenderUtils;
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
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.Language;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraftforge.client.gui.ScrollPanel;
import net.minecraftforge.common.MinecraftForge;

import java.awt.Color;
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
    private TextFieldWidget currentEmailTitle, currentEmailCreateTime;
    private Button deleteEmailBtn, acceptEmailBtn, filterEmailBtn, functionMenuBtn;
    private GuiImageButton playSoundBtn, refreshBtn;
    private final GuiPopupMenu filterMenu = new GuiPopupMenu(), functionMenu = new GuiPopupMenu();
    private final Inbox inbox = Inbox.getEmpty("");
    private long inboxSize = -1, currentEmailID = -1;

    public GuiInbox() {
        super(new StringTextComponent(null));
//        this.imageWidth = EmailConfigs.Main.Size.Width.get();
//        this.imageHeight = EmailConfigs.Main.Size.Height.get();
        this.filterMenu.setResetButtonSize(true, false);
        this.functionMenu.setResetButtonSize(true, false);
//        GuiInbox.loading.writeToFile(new File("C:/test.png"));
        ;
    }

    public static void display() {
        Minecraft.getInstance().displayGuiScreen(new GuiInbox());
        EmailMain.net.sendMessageToServer(new MsgDisplayInbox(true));
        refresh();
    }

    public static void refresh() {
        if (Minecraft.getInstance().currentScreen instanceof GuiInbox) {
            EmailMain.net.sendMessageToServer(new MsgRefreshInbox());
        }
    }

    @Override
    protected void init() {
        FontRenderer font = this.getFont();
        int listWidth = EmailConfigs.Layout.Email_List_Width.get();
        int x = 6, y = this.font.FONT_HEIGHT + 6;

        this.addListener(this.emailList = new EmailListWidget(this, listWidth, this.height - 35, x, y));
        x += this.emailList.getWidth() + 8;

        this.currentEmailTitle = new TextFieldWidget(font, x+5, y+5, this.width - this.emailList.getWidth() - 20, font.FONT_HEIGHT + 6, new StringTextComponent("this is a title"));
        this.currentEmailTitle.setEnabled(false);
        this.currentEmailTitle.setTextColor(Color.WHITE.getRGB());
        this.currentEmailTitle.setDisabledTextColour(Color.WHITE.getRGB());
        this.currentEmailTitle.setEnableBackgroundDrawing(false);

        this.currentEmailCreateTime = new TextFieldWidget(font, this.currentEmailTitle.x, this.currentEmailTitle.y + this.currentEmailTitle.getHeight(), this.currentEmailTitle.getWidth(), this.currentEmailTitle.getHeight(), new StringTextComponent("2024/13/32 25:61:61"));
        this.currentEmailCreateTime.setEnabled(false);
        this.currentEmailCreateTime.setTextColor(Color.WHITE.getRGB());
        this.currentEmailCreateTime.setDisabledTextColour(Color.WHITE.getRGB());
        this.currentEmailCreateTime.setEnableBackgroundDrawing(false);

        this.addListener(this.emailInfo = new EmailInfo(this, x, this.emailList.getHeight() - (this.currentEmailTitle.getHeight()*2) - 5));
        this.currentEmailTitle.setMaxStringLength(this.font.trimStringToWidth("888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888", this.emailInfo.getWidth() + 10).length());
//        this.emailList.refreshList();
        this.initButtons(x, y);
    }

    protected void initButtons(int x, int y) {
        this.playSoundBtn = this.addButton(new GuiImageButton(this, this.currentEmailTitle.x + this.currentEmailTitle.getWidth() - this.currentEmailTitle.getHeight()*2 - 5, this.currentEmailTitle.y, this.currentEmailTitle.getHeight()*2 - 6, this.currentEmailTitle.getHeight()*2 - 6, ()->null, 256, 256, 0, 169, 56, 56, btn->
                this.playSound()
        )).setBackground(()->ICON);
        this.playSoundBtn.visible = false;

        int width = RenderUtils.width(new TranslationTextComponent("info.inbox.filter.default"));
        this.filterEmailBtn = this.addButton(GuiButton.builder(new TranslationTextComponent("info.inbox.filter.default"), b-> {
                    this.filterMenu.setVisible(!this.filterMenu.isVisible());
                    if (this.getCurrentEmailID()!=-1) {
                        this.setCurrentEmail(this.getCurrentEmailID());
                    }
                })
                .pos(this.emailList.getLeft(), this.emailList.getTop() - y + 2)
                .size(this.emailList.getWidth(), this.font.FONT_HEIGHT + 3)
                .build());
        this.addButton(this.filterMenu);
        this.filterMenu.setPosition(this.filterEmailBtn.x + this.filterEmailBtn.getWidth() + 2, this.filterEmailBtn.y);
        this.initFilterMenu();

        this.refreshBtn = this.addButton(new GuiImageButton(this, this.emailList.getLeft(), this.emailList.getBottom()+1, this.getFont().FONT_HEIGHT*2+1, this.getFont().FONT_HEIGHT*2+1, ()->
                new TranslationTextComponent(refreshCoolingTicks <= 0 ? "info.inbox.refresh" : "info.inbox.refresh.cooling")
                , 256, 256, 111, 169, 55, 55, btn->
                this.refresh(()->{
                            this.stopSound();
                            this.setCurrentEmail(-1);
                            this.refreshBtn.visible = false;
                            this.inbox.deleteAllEmail();
                            this.updataInboxSize();
                            this.emailList.clearEmails();
                            this.emailInfo.clearMessage();
                            EmailMain.net.sendMessageToServer(new MsgRefreshInbox());
                        }, ()-> this.refreshBtn.visible = true
                )
        )).setBackground(()->ICON);
        this.refreshBtn.visible = refreshCoolingTicks <= 0;

        width = RenderUtils.width(new TranslationTextComponent("info.inbox.black.close"));
        Button btn = this.addButton(GuiButton.builder(
                        new TranslationTextComponent("info.inbox.black.close"),
                        b-> this.getMinecraft().displayGuiScreen(null)
                )
                .pos(this.emailInfo.getRight() - width - 6, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.FONT_HEIGHT + 6)
                .build());

        width = RenderUtils.width(new TranslationTextComponent("info.inbox.menu"));
        this.functionMenuBtn = btn = this.addButton(GuiButton.builder(
                        new TranslationTextComponent("info.inbox.menu"),
                        b-> {
                            this.functionMenu.setPosition((this.functionMenuBtn.x + this.functionMenuBtn.getWidth()/2) - this.functionMenu.getWidth()/2, this.functionMenuBtn.y - this.functionMenu.getHeight() - 5);
                            this.functionMenu.setVisible(!this.functionMenu.isVisible());
                        }
                )
                .pos(btn.x - width - 8, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.FONT_HEIGHT + 6)
                .build());
        this.addButton(this.functionMenu);
        this.initFunctionMenu();

        width = RenderUtils.width(new TranslationTextComponent("info.inbox.delete"));
        btn = this.deleteEmailBtn = this.addButton(GuiButton.builder(
                        new TranslationTextComponent("info.inbox.delete"),
                        b-> {
                            EmailMain.net.sendMessageToServer(new MsgDeleteEmail.Delete(this.getCurrentEmailID()));
                            for (int i = 0; i < this.emailList.getEventListeners().size(); i++) {
                                if (this.emailList.getEventListeners().get(i).getEmailId() == this.getCurrentEmailID()) {
                                    this.inbox.deleteEmail(this.getCurrentEmailID());
                                    this.updataInboxSize();
                                    this.emailInfo.clearMessage();
                                    this.emailList.refreshList();
                                    this.emailList.setSelected(null);
                                    this.deleteEmailBtn.visible = false;
                                    this.acceptEmailBtn.visible = false;
                                    break;
                                }
                            }
                        }
                )
                .pos(btn.x - width - 8, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.FONT_HEIGHT + 6)
                .build());
        this.deleteEmailBtn.visible = false;

        width = RenderUtils.width(new TranslationTextComponent("info.inbox.accept"));
        btn = this.acceptEmailBtn = this.addButton(GuiButton.builder(
                        new TranslationTextComponent("info.inbox.accept"),
                        b-> {
                            EmailMain.net.sendMessageToServer(new MsgReceiveEmail.Receive(this.getCurrentEmailID()));
                            if (this.emailList.getSelected()!=null) {
                                this.acceptEmailBtn.visible = false;
                                this.emailList.getSelected().getEmail().receive(getMinecraft().player);
                                this.updataInboxSize();
                            }
                        }
                )
                .pos(btn.x - width - 8, this.emailInfo.getBottom() + 2)
                .size(width + 6, this.font.FONT_HEIGHT + 6)
                .build());
        this.acceptEmailBtn.visible = false;
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(stack);
        RenderUtils.fill(stack, this.emailInfo.getLeft(), this.emailList.getTop(), this.currentEmailTitle.getWidth(), this.currentEmailTitle.getHeight()*2 + 5, 0xC0101010);
        RenderUtils.hLineGradient(stack, false, this.emailInfo.getLeft() + 5, this.emailInfo.getTop() - 3, this.emailInfo.getWidth() - 10, 1, Color.YELLOW.getRGB(), 0);

        this.currentEmailTitle.render(stack, mouseX, mouseY, partialTicks);
        this.currentEmailCreateTime.render(stack, mouseX, mouseY, partialTicks);

        if (!this.refreshBtn.visible) GuiInbox.LOADING_GIF
                .setRenderPos(this.refreshBtn.x, this.refreshBtn.y - 5)
                .setRenderSize(this.refreshBtn.getWidth(), this.refreshBtn.getHeight())
                .render(stack);

        this.emailList.render(stack, mouseX, mouseY, partialTicks);
        super.render(stack, mouseX, mouseY, partialTicks);
        this.emailInfo.render(stack, mouseX, mouseY, partialTicks);

        RenderUtils.drawString(stack, this.getInboxSize() +" Bytes", this.refreshBtn.x + this.refreshBtn.getWidth() + 4, this.refreshBtn.y + this.refreshBtn.getHeight()/2 - this.getFont().FONT_HEIGHT/2, Color.WHITE.getRGB(), true);

        this.renderTooltip(stack, mouseX, mouseY);
        this.updataData();
    }

    protected void renderTooltip(MatrixStack stack, int pX, int pY) {
        for (int i = 0; i < this.emailList.getEventListeners().size(); i++) {
            EmailListWidget.EmailEntry entry = null;
            try {
                entry = this.emailList.getEventListeners().get(i);
            }catch (Throwable ignored){}
            if (entry!=null && EmailUtils.isInRange(pX, pY, this.emailList.getLeft(), this.emailList.getTop(), this.emailList.getWidth(), this.emailList.getHeight()) && entry.isMouseOver(pX, pY)) {
                List<String> tip = Lists.newArrayList();

                tip.add(entry.getEmail().getTitle().format());
                tip.add("");

                tip.add(TextFormatting.GRAY + entry.getEmail().getCreateTimeAsString());
                tip.add(I18n.format("info.inbox.main.from", entry.getEmail().getSender().format()));

                tip.add("");

                tip.add(I18n.format("info.inbox.email_size", entry.getEmail().getEmailNetworkSize()));
                if (entry.getEmail().getExpirationTime() != null) {
                    tip.add("");
                    if (entry.getEmail().isExpiration()) {
                        tip.add(String.format("%s: %s", I18n.format("inbox.config.expiration"), TextFormatting.RED + I18n.format("inbox.config.expiration.ed")));
                    } else {
                        tip.add(I18n.format("info.inbox.remain_expiration_time", EmailUtils.getExpirationTime(entry.getEmail())));
                    }
                }
                tip.add(String.format("ID: %s", entry.getEmailId()));
                RenderUtils.drawComponentTooltip(stack, tip.stream().map(StringTextComponent::new).collect(Collectors.toList()), pX, pY);
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
                    List<ITextComponent> hover = Lists.newArrayList();
                    hover.add(new TranslationTextComponent("info.inbox.play_sound" + (this.isPlayingSound() ? ".stop" : "")));
                    if(this.isPlayingSound()) {
                        if (this.currentAudioUUID != null) {
                            if (this.currentAudioDuration == null) {
                                this.currentAudioDuration = formatMs(AudioSystem.getFloatDuration(this.currentAudioUUID));
                            }
                            hover.add(new StringTextComponent(String.format("%s / %s", formatMs(AudioSystem.getFloatElapse(this.currentAudioUUID)), this.currentAudioDuration)));
                        }else if(this.currentSound!=null) {
                            hover.add(new StringTextComponent(this.currentSound.time.toStringTime(false)));
                        }else {
                            InboxPlaySoundEvent.Tick event = new InboxPlaySoundEvent.Tick(this.getCurrentEmail());
                            MinecraftForge.EVENT_BUS.post(event);
                            hover.add(new StringTextComponent(String.format("%s / %s", event.getElapse(), event.getDuration())));
                        }
                    }else {
                        hover.add(new TranslationTextComponent("info.inbox.play_sound.category", new TranslationTextComponent("soundCategory."+ (email.isExternalSound() ? email.getExternalSound().getCategory().getName() : email.getSound().getSoundChannel().getName()).toLowerCase())));
                    }
                    RenderUtils.drawComponentTooltip(stack, hover, pX, pY);
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
                for (EmailListWidget.EmailEntry entry : this.emailList.getEventListeners()) {
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
        boolean ret = this.getListener() != null && this.isDragging() && pButton == 0 && this.getListener().mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
        return ret || super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
    }

    private void initFunctionMenu() {
        this.functionMenu.clearPopupButtons();
        this.addFunctionButton(I18n.format("info.inbox.dispatch"), ()-> GuiHandler.openGui(GuiHandler.EMAIL_SEND));
        this.addFunctionButton(I18n.format("info.inbox.delete_read"), ()-> {
            EmailMain.net.sendMessageToServer(new MsgDeleteEmail.AllRead());
            for (int i = 0; i < this.emailList.getEventListeners().size(); i++) {
                EmailListWidget.EmailEntry entry = this.emailList.getEventListeners().get(i);
                if(entry.getEmail().isRead() && !entry.getEmail().hasItems()) {
                    this.inbox.deleteEmail(entry.getEmailId());
                }
            }
            this.emailList.refreshList();
        });
        this.addFunctionButton(I18n.format("info.inbox.accept_all"), ()-> {
            EmailMain.net.sendMessageToServer(new MsgReceiveEmail.All());
            for (int i = 0; i < this.emailList.getEventListeners().size(); i++) {
                EmailListWidget.EmailEntry entry = this.emailList.getEventListeners().get(i);
                if(entry.getEmail().hasAttachment() && !entry.getEmail().isReceived()) {
                    entry.getEmail().receive(getMinecraft().player);
                }
            }
        });

        this.addFunctionButton(I18n.format("info.inbox.black"), ()-> GuiHandler.openGui(GuiHandler.EMAIL_BLACKLIST));
        this.addFunctionButton(I18n.format("info.inbox.scheduled"), () -> Minecraft.getInstance().displayGuiScreen(new GuiScheduledEmail()));

        if (EmailUtils.isOP(Minecraft.getInstance().player)) {
            this.addFunctionButton(I18n.format("info.inbox.generate"), ()-> GuiHandler.openGui(GuiHandler.EMAIL_Generate));
        }
    }

    public void addFunctionButton(String name, Runnable runnable) {
        this.functionMenu.addPopupButton(GuiButton.builder(new TranslationTextComponent(name), b->runnable.run()).bounds(0, 0, this.getFont().getStringWidth(name) + 6, this.getFont().FONT_HEIGHT + 2).build());
    }

    private void initFilterMenu() {
        this.filterMenu.clearPopupButtons();
        this.addFilter();
		MinecraftForge.EVENT_BUS.post(new InboxFilterEvent(this));
    }

    private void addFilter() {
        this.addFilter(I18n.format("info.inbox.filter.default"), email -> true);

        this.addFilter(I18n.format("info.inbox.filter.is_read"), Email::isRead);
        this.addFilter(I18n.format("info.inbox.filter.not_read"), email -> !email.isRead());

        this.addFilter(I18n.format("info.inbox.filter.has_sound"), Email::hasSound);

        this.addFilter(I18n.format("info.inbox.filter.has_item"), Email::hasItems);
        this.addFilter(I18n.format("info.inbox.filter.is_accept"), email -> email.hasItems() && email.isReceived());
        this.addFilter(I18n.format("info.inbox.filter.not_accept"), email -> email.hasItems() && !email.isReceived());

        this.addFilter(I18n.format("info.inbox.filter.has_expiration"), Email::hasExpirationTime);
        this.addFilter(I18n.format("info.inbox.filter.is_expiration"), email -> email.hasExpirationTime() && email.isExpiration());
        this.addFilter(I18n.format("info.inbox.filter.not_expiration"), email -> email.hasExpirationTime() && !email.isExpiration());
    }

    boolean renderEmptyTitle = true;
    public void addFilter(String name, Predicate<Email> predicate) {
        this.filterMenu.addPopupButton(GuiButton.builder(new TranslationTextComponent(name), b->{
            this.emailList.refreshList(predicate);
            this.filterEmailBtn.setMessage(new TranslationTextComponent(name));
            this.filterMenu.setVisible(false);
            this.renderEmptyTitle = true;
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
            this.currentEmailTitle.setText(email.getTitle().format());
            this.currentEmailCreateTime.setText(email.getCreateTimeAsString());
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
                ITextComponent component = message.toTextComponent();

                List<IReorderingProcessor> texts;
                if (EmailConfigs.Layout.Enable_Vanilla_Wrap_Text.get()) {
                    texts = LanguageMap.getInstance().func_244260_a(this.getFont().getCharacterManager().func_238362_b_(component, textMaxLength, Style.EMPTY));
                }else {
                    texts = EmailUtils.splitString(message.format(), textMaxLength).stream().map(e->IReorderingProcessor.fromString(e, Style.EMPTY)).collect(Collectors.toList());
                }
                texts.forEach(this.emailInfo::addMessage);
            }
            this.emailInfo.setScrollDistance(0);
        }else {
            this.currentEmailTitle.setText("");
            this.currentEmailCreateTime.setText("");
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
            return this.getMinecraft().getSoundHandler().isPlaying(this.currentSound) || !this.currentSound.isDonePlaying();
        }else if (this.currentAudioUUID != null) {
            return !AudioSystem.isClose(this.currentAudioUUID) && !AudioSystem.isPaused(this.currentAudioUUID);
        }
        return false;
    }
    public void stopSound() {
        if (!MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Stop(this.getCurrentEmail()))) {
            if(this.currentSound != null) {
                this.getMinecraft().getSoundHandler().stop(this.currentSound);
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
//                            this.setRenderText(Timer.parseTick(10, 0), true, I18n.format("info.inbox.file_not_found", file), Color.RED);
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
                    this.getMinecraft().getSoundHandler().play(this.currentSound);
                    played = true;
                }
            }
            if (!played) {
                this.stopSound();
            }else {
                this.currentSoundCheck = 0;
                this.currentSoundLastTime = 0;
            }
//            this.getMinecraft().getSoundHandler().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    public FontRenderer getFont() {
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
        for (EmailListWidget.EmailEntry child : this.emailList.getEventListeners()) {
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
        if (pKeyCode == Minecraft.getInstance().gameSettings.keyBindInventory.getKey().getKeyCode()) {
            this.closeScreen();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    private final Date now = new Date();
    int currentSoundCheck = 0;
    long currentSoundLastTime = 0;

    protected void updataData() {
        // TODO 显示未选择邮件时的信息
        if (this.emailList!=null && this.emailList.getSelected() == null) {
            long last = System.currentTimeMillis();
            if (last - this.now.getTime() >= 1000) {
                this.now.setTime(last);
                this.currentEmailTitle.setText(I18n.format("info.inbox.no_select_title"));
                this.currentEmailCreateTime.setText(I18n.format("info.inbox.no_select_time", EmailUtils.dateFormat.format(this.now)));
            }
        }
        // TODO 检查播放的音效是否已停止
        if (this.currentSound!=null){
            if(this.currentSound.isDonePlaying()) {
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
        private final List<IReorderingProcessor> messages = new ArrayList<>();

        public EmailInfo(GuiInbox parent, int x, int height) {
            super(parent.getMinecraft(), parent.currentEmailCreateTime.getWidth(), height, parent.currentEmailCreateTime.y + parent.currentEmailCreateTime.getHeight(), x);
            this.parent = parent;
        }

        public void addMessage(IReorderingProcessor message) {
            this.messages.add(message);
        }
        public void clearMessage() {
            this.messages.clear();
        }

        @Override
        protected void drawPanel(MatrixStack stack, int x, int y, Tessellator tess, int mouseX, int mouseY) {
            x = this.left + 5;
            Email email = this.parent.getCurrentEmail();
            if (email != null) {
                FontRenderer font = this.parent.getFont();
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
                    AttachmentEvent.Render.Pre event = new AttachmentEvent.Render.Pre(email, attachment, stack, this.parent.getFont(), this.width, this.height, x, y, mouseX, mouseY, this.top, this.bottom, this.left, this.right);
                    if (!MinecraftForge.EVENT_BUS.post(event)) {
                        attachment.render(event);
                        MinecraftForge.EVENT_BUS.post(new AttachmentEvent.Render.Post(email, attachment, stack, this.parent.getFont(), this.width, this.height, x, y, mouseX, mouseY, this.top, this.bottom, this.left, this.right));
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
                int height = this.messages.size() * (this.parent.font.FONT_HEIGHT + 2);
                height += this.parent.font.FONT_HEIGHT + 2;

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
            return this.parent.font.FONT_HEIGHT * 3;
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
        public static Builder builder(ITextComponent pMessage, Button.IPressable pOnPress) {
            return new Builder(pMessage, pOnPress);
        }
        protected GuiButton(int pX, int pY, int pWidth, int pHeight, ITextComponent pMessage, Button.IPressable pOnPress) {
            super(pX, pY, pWidth, pHeight, pMessage, pOnPress);
        }

        protected GuiButton(Builder builder) {
            super(builder.x, builder.y, builder.width, builder.height, builder.message, builder.onPress, builder.tooltip);
        }

        @Override
        public void renderWidget(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
            RenderUtils.fill(stack, this.x, this.y, this.getWidth(), this.getHeight(), 0xC0101010);
            RenderUtils.hLine(stack, this.x, this.y + this.getHeight() - 1, this.getWidth(), this.isMouseOver(mouseX, mouseY) ? Color.BLUE.getRGB() : Color.LIGHT_GRAY.getRGB());

            RenderUtils.drawCenteredComponent(stack, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, this.isMouseOver(mouseX, mouseY) ? Color.ORANGE.getRGB() : Color.LIGHT_GRAY.getRGB(), true);
        }

        public static class Builder {
            private final ITextComponent message;
            private final Button.IPressable onPress;
            private Button.ITooltip tooltip;
            private int x;
            private int y;
            private int width = 150;
            private int height = 20;

            public Builder(ITextComponent pMessage, Button.IPressable pOnPress) {
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

            public Builder tooltip(Button.ITooltip pTooltip) {
                this.tooltip = pTooltip;
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
