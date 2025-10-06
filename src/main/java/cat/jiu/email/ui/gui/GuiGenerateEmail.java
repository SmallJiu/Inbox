package cat.jiu.email.ui.gui;

import cat.jiu.core.api.element.ISound;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.Utils;
import cat.jiu.core.util.client.AudioSystem;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.core.util.element.Text;
import cat.jiu.core.util.element.data.JsonData;
import cat.jiu.core.util.element.sound.SoundJmp123;
import cat.jiu.core.util.element.sound.SoundMC;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.api.AttachmentSendScreenWidget;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.email.element.Email;
import cat.jiu.email.ui.gui.component.*;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.SizeReport;
import cat.jiu.email.util.TimeMillis;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;

import java.awt.Color;
import java.io.File;
import java.util.*;

@OnlyIn(Dist.CLIENT)
public class GuiGenerateEmail extends Screen {
    protected final Screen parent;
    protected final GuiTime expiration = new GuiTime(true, true, true, true, true, false);
    protected final AttachmentWidget attachments = new AttachmentWidget(this.checkSendAttachment(), ()->this.info.setScrollDistance(0));
    protected EditBox title, name, sound;
    protected LockIconButton deletable;
    protected GuiImageButton tryPlaySound;
    protected Button generate;
    protected EmailInfo info;
    protected int startX = 2, startY = RenderUtils.fontHeight();
    protected boolean locked;
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public GuiGenerateEmail(Screen parent) {
        super(CommonComponents.EMPTY);
        this.parent = parent;
    }
    protected boolean checkSendAttachment(){
        return false;
    }

    @Override
    protected void init() {
        this.startX = 5;
        this.startY = RenderUtils.fontHeight();
        Window window = Minecraft.getInstance().getWindow();

        int shiftX = Math.max(
                RenderUtils.width(Component.translatable("inbox.config.sender").append(": ")),
                Math.max(
                        RenderUtils.width(Component.translatable("info.inbox.title").append(": ")),
                        RenderUtils.width(Component.translatable("info.inbox.generate.sound").append(": "))
                )
        );
        this.title = loadEditboxRenderConfig(this.addRenderableWidget(new EditBox(RenderUtils.getFontRenderer(), this.startX + shiftX, this.startY, window.getGuiScaledWidth() - this.startX - 75, RenderUtils.fontHeight() + 4, CommonComponents.EMPTY)));
        this.name = loadEditboxRenderConfig(this.addRenderableWidget(this.getNameEditbox()));
        this.sound = loadEditboxRenderConfig(this.addRenderableWidget(new EditBox(RenderUtils.getFontRenderer(), this.name.getX(), this.name.getY() + this.name.getHeight(), this.name.getWidth(), this.name.getHeight(), CommonComponents.EMPTY)));
        this.sound.setEditable(this.canSendSound());
        this.sound.visible = this.canSendSound();

        if (this.canSetEmailDeletable()){
            this.deletable = this.addRenderableWidget(new LockIconButton(this.name.getX() + this.name.getWidth() + 6, 2, b ->
                    this.deletable.setLocked(!this.deletable.isLocked())
            ));
            this.deletable.setMessage(Component.translatable("info.inbox.send.lock"));
            this.deletable.setTooltip(Tooltip.create(this.deletable.getMessage()));
        }

        if (this.canSendSound()) {
            int x = this.deletable == null ? this.name.getX() + this.name.getWidth() + 6 : this.deletable.getX(),
                    y = this.deletable == null ? 2 : this.deletable.getY();
            this.tryPlaySound = this.addRenderableWidget(new GuiImageButton(this, x, y + 20 + 3, 20, 20, ()->CommonComponents.EMPTY, 256, 256, 0, 169, 56, 56, b->
                    this.tryPlaySound()
            )).setBackground(()->GuiInbox.ICON);
            this.tryPlaySound.visible = false;
        }

        this.info = this.addRenderableWidget(new EmailInfo(this,
                this.startX, this.sound == null ? this.name.getY() + this.name.getHeight() : this.sound.getY() + this.sound.getHeight(),
                window.getGuiScaledWidth() - 10, window.getGuiScaledHeight() - 2 - RenderUtils.fontHeight() - (this.name.getY() + this.name.getHeight()) - 22
        ));
        this.attachments.attachments = this.info.attachments;
        this.attachments.init();
        this.attachments.visible = false;

        Component text = Component.translatable("info.inbox.black.back");
        Button btn = this.addRenderableWidget(GuiInbox.GuiButton.builder(text, b->
                        Minecraft.getInstance().setScreen(this.parent)
                )
                .pos(this.info.getRight() - RenderUtils.width(text) - 6, this.info.getBottom() + 2)
                .size(RenderUtils.width(text) + 6, RenderUtils.fontHeight() + 6)
                .build());

        if (this.canAddFolderButton()) {
            text = Component.literal("Folder");
            btn = this.addRenderableWidget(GuiInbox.GuiButton.builder(text, b ->
                            Util.getPlatform().openFile(new File(EmailAPI.getGlobalDataPath(), "emails/"))
                    )
                    .pos(btn.getX() - RenderUtils.width(text) - 6 - 2, btn.getY())
                    .size(RenderUtils.width(text) + 6, btn.getHeight())
                    .build());
        }

        text = this.getConfirmGenerateMessage();
        btn = this.generate = this.addRenderableWidget(GuiInbox.GuiButton.builder(text, b->
                        this.generate()
                )
                .pos(btn.getX() - RenderUtils.width(text) - 6 - 2, btn.getY())
                .size(RenderUtils.width(text) + 6, btn.getHeight())
                .build());
    }

    protected boolean canAddFolderButton() {
        return true;
    }
    protected Component getConfirmGenerateMessage(){
        return Component.translatable("info.inbox.generate");
    }
    protected boolean canSendSound(){
        return true;
    }
    protected EditBox getNameEditbox() {
        return new EditBox(RenderUtils.getFontRenderer(), this.title.getX(), this.title.getY() + this.title.getHeight(), this.title.getWidth(), this.title.getHeight(), CommonComponents.EMPTY);
    }
    protected Component getNameInfo(){
        return Component.translatable("inbox.config.sender");
    }
    protected boolean canRenderDefultName() {
        return true;
    }
    protected boolean canAddEmailExpiration(){
        return true;
    }
    protected boolean canSetEmailDeletable(){
        return true;
    }

    public static EditBox loadEditboxRenderConfig(EditBox box){
        box.setTextColor(-1);
        box.setTextColorUneditable(-1);
        box.setMaxLength(Integer.MAX_VALUE);
        box.setBordered(false);
        return box;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderEditboxMessage(graphics, this.title,
                Component.translatable("info.inbox.default_title"),
                Component.translatable("info.inbox.title").append(":")
        );
        if (this.canSendSound()){
            this.renderEditboxMessage(graphics, this.sound,
                    this.canSendSound() ? Component.translatable("info.inbox.generate.sound.info") : CommonComponents.EMPTY,
                    Component.translatable("info.inbox.generate.sound").append(":")
            );
        }
        this.renderEditboxMessage(graphics, this.name,
                this.canRenderDefultName() ? Minecraft.getInstance().player.getName() : CommonComponents.EMPTY,
                ((MutableComponent)this.getNameInfo()).append(":")
        );
//        RenderUtils.hLineGradient(graphics, false, this.startX, this.sender.getY() + this.sender.getHeight() - 2, this.sender.getWidth(), 1, Color.YELLOW.getRGB(), 0, 0);
        if (this.messae != null) {
            if (this.renderTime >= System.currentTimeMillis()) {
                RenderUtils.drawComponent(graphics, this.messae, 2, this.info.getY() + this.info.getHeight() + 3, Color.WHITE.getRGB(), true);
            }else {
                this.messae = null;
            }
        }
        if (this.canSendSound()){
            this.tryPlaySound.visible = !this.sound.getValue().isEmpty();
            this.renderSoundMessage(graphics, mouseX, mouseY);
        }

        if (this.locked) {
            RenderUtils.square(graphics, this.generate.getX(), this.generate.getY(), this.generate.getWidth(), this.generate.getHeight(), RenderUtils.rgb(255, 0, 0, 100), Color.RED.getRGB());
        }
    }
    public void renderEditboxMessage(GuiGraphics graphics, EditBox box, Component message, Component rightMessage) {
        RenderUtils.drawRightComponent(graphics, rightMessage, box.getX() - 2, box.getY(), Color.WHITE.getRGB(), true);
        if (box.getValue().isEmpty()) {
            RenderUtils.drawComponent(graphics, message, box.getX(), box.getY(), RenderUtils.rgb(128, 128, 128, 150), true);
        }
        RenderUtils.hLine(graphics, box.getX(), box.getY() + box.getHeight() - 4, box.getWidth(), RenderUtils.rgb(128, 128, 128, 150));
    }
    public void renderSoundMessage(GuiGraphics graphics, int mouseX, int mouseY) {
        this.tryPlaySound.setUOffset(this.tempSound == null ? 0 : this.tempSound.isPlayed() ? 55 : 0);
        if (this.tempSound != null) {
            if(this.tryPlaySound.isMouseOver(mouseX, mouseY)) {
                List<Component> hover = Lists.newArrayList();
                hover.add(Component.translatable("info.inbox.play_sound" + (this.tempSound.isPlayed() ? ".stop" : "")));
                if (this.tempSound.isPlayed() && this.tempSound instanceof SoundJmp123) {
                    AudioSystem.Audio audio = ((SoundJmp123) this.tempSound).getAudio();
                    if (audio.isCanLopping()) {
                        if (audio.getMaxLoopCount() > 1) {
                            ((MutableComponent)hover.get(0)).append(String.format("  %s / %s", audio.getLoopCount(), audio.getMaxLoopCount()));
                        }else if (audio.isInfiniteLoop()) {
                            ((MutableComponent)hover.get(0)).append(String.format("  %s / Infinite", audio.getLoopCount()));
                        }
                    }
                }

                if(this.tempSound.isPlayed()) {
                    if (this.tempSoundDuration == null) {
                        this.tempSoundDuration = GuiInbox.formatMs(this.tempSound.getFloatDuration());
                    }
                    hover.add(Component.literal(String.format("%s / %s", GuiInbox.formatMs(this.tempSound.getFloatElapse()), this.tempSoundDuration)));
                }else {
                    hover.add(Component.translatable("info.inbox.play_sound.category", Component.translatable("soundCategory."+ this.tempSound.getSoundChannel().getName().toLowerCase())));
                }
                graphics.renderComponentTooltip(this.font, hover, mouseX, mouseY);
            }
        }
    }

    protected ISound tempSound;
    protected String tempSoundDuration;
    protected void tryPlaySound() {
        if (this.tempSound == null) {
            String sound = this.sound.getValue();
            if (!StringUtils.isEmpty(sound)) {
                if (sound.contains(":")) {
                    ResourceLocation location = Utils.location(sound);
                    if (location != null) {
                        SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(location);
                        if (event != null) {
                            this.tempSound = new SoundMC()
                                    .setDuration(59, 59, 19)
                                    .setSoundEvent(event)
                                    .setSoundChannel(SoundSource.PLAYERS);
                        }
                    }
                }
                if (this.tempSound == null) {
                    this.tempSound = new SoundJmp123(new AudioSystem.Audio(sound, SoundSource.PLAYERS));
                }
                this.tempSound.play();
            }
        }else {
            this.tempSound.stop();
            this.tempSound = null;
            this.tempSoundDuration = null;
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (this.locked) return false;
        this.setFocused(null);
        for(GuiEventListener wiget : this.children()) {
            wiget.setFocused(false);
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }
    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        return !this.locked && super.charTyped(pCodePoint, pModifiers);
    }
    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        return !this.locked && super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }
    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        return !this.locked && super.keyReleased(pKeyCode, pScanCode, pModifiers);
    }

    public boolean messageIsEmpty() {
        for (EmailInfo.Msg tf : this.info.messages.values()) {
            if (!StringUtils.isEmpty(tf.getValue())) return false;
        }
        return true;
    }
    protected void generate(){
        this.locked = true;
        String title = this.title.getValue();
        if(StringUtils.isEmpty(title)) {
            title = "info.inbox.default_title";
        }

        if(this.messageIsEmpty() && this.info.attachments.isEmpty()) {
            this.setMessage(Component.translatable("info.inbox.error.empty_msgs_item"), 5000);
            this.locked = false;
            return;
        }

        List<IText> msgs = Lists.newArrayList();
        if(!messageIsEmpty()) {
            for (EmailInfo.Msg textField : this.info.messages.values()) {
                String msg = textField.getValue();
                if (msg.isEmpty()) {
                    msgs.add(Text.empty);
                }else {
                    msgs.add(new Text(msg));
                }
            }
        }else {
            msgs.add(new Text("info.inbox.default_msg"));
        }
        Email email = new Email(new Text(title), this.name.getValue().isEmpty() ? new Text(Minecraft.getInstance().player.getName()) : new Text(this.name.getValue())).addMessages(msgs);

        long expiration = this.expiration.getTimeOfMillis();
        if(expiration>0) {
            email.setExpirationTime(new TimeMillis(expiration));
        }

        String sound = this.sound.getValue();
        if (!StringUtils.isEmpty(sound)) {
            ISound sound1 = null;
            if (sound.contains(":")) {
                ResourceLocation location = Utils.location(sound);
                if (location != null) {
                    SoundEvent event = ForgeRegistries.SOUND_EVENTS.getValue(location);
                    if (event != null) {
                        sound1 = new SoundMC()
                                .setDuration(59, 59, 19)
                                .setSoundEvent(event)
                                .setSoundChannel(SoundSource.PLAYERS);
                    }
                }
            }
            if (sound1 == null) {
                sound1 = new SoundJmp123(new AudioSystem.Audio(sound, SoundSource.PLAYERS));
            }
            email.setSound(sound1);
        }

        if (this.deletable != null) {
            email.setDeletable(!this.deletable.isLocked());
        }

        if (!this.info.attachments.isEmpty()) {
            for (EmailInfo.Attachment attachmentWidget : this.info.attachments.values()) {
                IAttachment attachment = attachmentWidget.attachment.newAttachmentInstance();
                if (attachment != null && !attachment.isEmpty()){
                    email.addAttachment(attachment);
                }
            }
        }

        if(!EmailConfigServer.isInfiniteSize()) {
            SizeReport report = EmailUtils.checkEmailSize(email);
            if(!SizeReport.SUCCESS.equals(report)) {
                this.setMessage(Component.translatable("info.inbox.error.send.to_big", report.slot(), report.size()), 5000);
                this.locked = false;
                return;
            }
        }
        this.onGenerateEmailSuccess(email);
        this.locked = false;
    }
    protected void onGenerateEmailSuccess(Email email) {
        String filename = email.getTitle().format() + "-" + System.currentTimeMillis() + ".json";

        JsonObject object = (JsonObject) email.write(JsonData.map()).getData();
        object.remove("time");
        if (!email.hasAttachments()) {
            JsonArray array = new JsonArray();
            for (ResourceLocation resourceLocation : IAttachment.REGISTRY.getIDs()) {
                array.add(String.valueOf(resourceLocation));
            }
            object.add("attachments", new JsonArray());
            object.add("AllAttachmentID", array);
        }

        File file = new File(EmailAPI.getGlobalDataPath(), "emails/"+filename);
        try {
            JsonUtils.toJsonFileThrow(file, object, true, EmailConfigServer.File_Charset.get());
            this.setMessage(Component.literal("Success! File: " + filename), 10000);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    Component messae;
    long renderTime;
    /**
     * @param renderTime unit: ms
     */
    public void setMessage(Component messae, long renderTime) {
        this.messae = messae;
        this.renderTime = System.currentTimeMillis() + renderTime;
    }

    @Override
    public void onClose() {
        super.onClose();
        Minecraft.getInstance().setScreen(this.parent);
        if (this.tempSound != null){
            this.tempSound.stop();
            this.tempSound = null;
        }
    }

    public static class EmailInfo extends ScrollPanel {
        private final GuiGenerateEmail parent;
        public final LinkedHashMap<UUID, Msg> messages = new LinkedHashMap<>();
        public Button addMessageButton;

        public final LinkedHashMap<UUID, Attachment> attachments = new LinkedHashMap<>();
        public Button addAttachmentButton;

        public EmailInfo(GuiGenerateEmail parent, int x, int y, int width, int height) {
            super(parent.getMinecraft(), width, height, y, x);
            this.parent = parent;
            this.addMessageButton = GuiInbox.GuiButton.builder(Component.literal("+++++++++++++ ").append(Component.translatable("info.inbox.generate.message")).append(" +++++++++++++"), b->{
                        UUID index = UUID.randomUUID();
                        this.messages.put(index, new Msg(GuiGenerateEmail.loadEditboxRenderConfig(new EditBox(
                                RenderUtils.getFontRenderer(), 0, 0, this.getWidth() - 8 - 13, RenderUtils.fontHeight() + 4, CommonComponents.EMPTY
                        )), Button.builder(Component.literal("X"), b0->
                                        this.messages.remove(index)
                                ).size(RenderUtils.fontHeight(), RenderUtils.fontHeight())
                                .build()));
                    })
                    .size(this.getWidth() - 8, RenderUtils.fontHeight() + 4)
                    .build();

            this.addAttachmentButton = GuiInbox.GuiButton.builder(Component.literal("+++++++++++++ ").append(Component.translatable("info.inbox.generate.attachment")).append(" +++++++++++++"), b->{
                        this.parent.attachments.visible = !this.parent.attachments.visible;
                        this.addAttachmentButton.visible = !this.addAttachmentButton.visible;
                    })
                    .size(this.getWidth() - 8, RenderUtils.fontHeight() + 4)
                    .build();
            this.parent.attachments.showButton = this.addAttachmentButton;

//            this.widgets.put(I18n.get("inbox.config.expiration"), this.parent.expiration);
        }
        public static class Msg {
            public final EditBox box;
            public final Button delete;
            public Msg(EditBox box, Button delete) {
                this.box = box;
                this.delete = delete;
            }
            public String getValue() {
                return this.box.getValue();
            }
        }
        public static class Attachment {
            public final AttachmentSendScreenWidget attachment;
            public final Button delete;
            public Attachment(AttachmentSendScreenWidget attachmentSendScreenWidget, Button delete) {
                this.attachment = attachmentSendScreenWidget;
                this.delete = delete;
            }
        }

        @Override
        protected int getContentHeight() {
            int height = this.addMessageButton.getHeight() + 6 + this.addAttachmentButton.getHeight() + 4;
            for (Msg message : this.messages.values()) {
                height += message.box.getHeight();
            }
            if (this.parent.attachments.visible){
                height += this.parent.attachments.getHeight();
            }
            if (this.parent.canAddEmailExpiration()){
                height += this.parent.expiration.getHeight();
            }
            for (Attachment widget : this.attachments.values()) {
                height += widget.attachment.getHeight()+4;
            }
            return Math.max(height + 3, this.getHeight() - 3);
        }

        @Override
        protected void drawPanel(GuiGraphics graphics, int x, int y, Tesselator tess, int mouseX, int mouseY) {
            graphics.enableScissor(this.getX(), this.getY(), this.getX()+this.getWidth(), this.getY()+this.getHeight());
            x = this.getX() + 2;

            if (this.parent.canAddEmailExpiration()){
                Component expiration = Component.translatable("info.inbox.scheduled.expiration_time", "");
                RenderUtils.drawComponent(graphics, expiration, x, y, Color.WHITE.getRGB(), true);
                this.parent.expiration.setPosition(x + RenderUtils.width(expiration), y);
                this.parent.expiration.render(graphics, mouseX, mouseY, 0);
                y += this.parent.expiration.getHeight() + 4;
            }

            for (Msg message : this.messages.values()) {
                message.box.setX(x);
                message.box.setY(y);
                message.box.render(graphics, mouseX, mouseY, 0);
                RenderUtils.hLine(graphics, message.box.getX(), message.box.getY() + message.box.getHeight() - 4, message.box.getWidth(), RenderUtils.rgb(128, 128, 128, 150));
                message.delete.setX(x + message.box.getWidth() + 2);
                message.delete.setY(y);
                message.delete.render(graphics, mouseX, mouseY, 0);
                y += message.box.getHeight();
            }

            this.addMessageButton.setPosition(x, y);
            this.addMessageButton.render(graphics, mouseX, mouseY, 0);
            y += this.addMessageButton.getHeight() + 7;

            for (Attachment attachment : this.attachments.values()) {
                attachment.attachment.setX(x);
                attachment.attachment.setY(y);
                attachment.attachment.render(graphics, mouseX, mouseY, 0);

                attachment.delete.setX(x + attachment.attachment.getWidth() + 2);
                attachment.delete.setY(y);
                attachment.delete.render(graphics, mouseX, mouseY, 0);
                y += attachment.attachment.getHeight() + 3;
            }

            if (this.parent.attachments.visible) {
                this.parent.attachments.setPosition(x, y);
                this.parent.attachments.setWidth(this.getWidth());
                this.parent.attachments.render(graphics, mouseX, mouseY, 0);
                y += this.parent.attachments.getHeight() + 4;
            }
            this.addAttachmentButton.setPosition(x, y);
            this.addAttachmentButton.render(graphics, mouseX, mouseY, 0);
            graphics.disableScissor();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.setFocused(null);
            boolean ret = super.mouseClicked(mouseX, mouseY, button);
            if (RenderUtils.inRange(mouseX, mouseY, this.getX(), this.getY(), this.getWidth(), this.getHeight())) {
                if (this.addAttachmentButton.mouseClicked(mouseX, mouseY, button)
                        || this.parent.attachments.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }else {
                    this.parent.attachments.visible = false;
                    this.addAttachmentButton.visible = true;
                }
                for (EmailInfo.Msg message : this.messages.values()) {
                    message.box.setFocused(false);
                    message.delete.setFocused(false);
                    if (message.box.mouseClicked(mouseX, mouseY, button)) {
                        this.setFocused(message.box);
                        return true;
                    }
                    if (message.delete.mouseClicked(mouseX, mouseY, button)) {
                        this.setFocused(message.delete);
                        return true;
                    }
                }
                if (this.addMessageButton.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                if (this.parent.canAddEmailExpiration() && this.parent.expiration.mouseClicked(mouseX, mouseY, button)) {
                    this.setFocused(this.parent.expiration);
                    return true;
                }
                for (Attachment attachment : this.attachments.values()) {
                    attachment.attachment.setFocused(false);
                    attachment.delete.setFocused(false);
                    if (attachment.attachment.mouseClicked(mouseX, mouseY, button)) {
                        this.setFocused(attachment.attachment);
                        return true;
                    }
                    if (attachment.delete.mouseClicked(mouseX, mouseY, button)) {
                        this.setFocused(attachment.delete);
                        return true;
                    }
                }
            }
            return ret;
        }
        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
            boolean scrollFocused = this.getFocused() != null && this.getFocused().mouseScrolled(mouseX, mouseY, scroll);
            if (this.canScroll()) {
                return scrollFocused || super.mouseScrolled(mouseX, mouseY, scroll);
            }
            return scrollFocused;
        }
        public boolean canScroll(){
            return (this.getContentHeight() + border) - height > 0;
        }

        public int getX() {
            return this.left;
        }
        public int getY() {
            return this.top;
        }
        public int getWidth() {
            return this.width;
        }
        public int getHeight() {
            return this.height;
        }
        public int getBottom() {
            return this.bottom;
        }
        public int getRight() {
            return this.right;
        }
        public void setScrollDistance(float scroll){
            this.scrollDistance = scroll;
        }
        @Override
        public NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }
        @Override
        public void updateNarration(NarrationElementOutput output) {}
    }

    public static class AttachmentWidget extends AttachmentSendScreenWidget.SubWidget {
        public final boolean checkCanSendAttachment;
        public final Runnable onClick;
        public Map<UUID, EmailInfo.Attachment> attachments;
        public Button showButton;

        public AttachmentWidget(boolean checkCanSendAttachment, Runnable onClick) {
            super(false, CommonComponents.EMPTY);
            this.setPosition(99999, 99999);
            this.checkCanSendAttachment = checkCanSendAttachment;
            this.onClick = onClick;
        }
        public void init() {
            if (this.attachments != null) {
                this.attachments.clear();
                this.widgets.clear();
                for (ResourceLocation id : AttachmentSendScreenWidget.REGISTRY.getIDs()) {
                    AttachmentSendScreenWidget.WidgetEntry wiget = AttachmentSendScreenWidget.REGISTRY.get(id);

                    if (this.checkCanSendAttachment && !wiget.canSendByNormalPlayer && !Minecraft.getInstance().player.isCreative()) {
                        if (!Utils.isOp()) {
                            continue;
                        }
                    }
                    UUID uuid = UUID.randomUUID();
                    this.addWiget(GuiInbox.GuiButton.builder(Component.literal(id.toString()), b->{
                        if (!this.attachments.containsKey(uuid)) {
                            this.attachments.put(uuid, new EmailInfo.Attachment(wiget.widgetEntryGetter.get(), Button.builder(Component.literal("X"), b0 -> {
                                        this.attachments.remove(uuid);
                                        b.visible = true;
                                    }).size(RenderUtils.fontHeight(), RenderUtils.fontHeight())
                                    .build()));
                            b.visible = false;
                        }
                        this.visible = false;
                        this.showButton.visible = true;
                        this.onClick.run();
                    }).build());
                }
            }
        }

        @Override
        public void setWidth(int pWidth) {
            super.setWidth(pWidth);
            for (PositionWiget widget : this.widgets) {
                widget.widget.setWidth(pWidth);
            }
        }
    }
}
