package cat.jiu.email.ui.gui;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.core.util.client.TextUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.ScheduledEmail;
import cat.jiu.email.net.msg.MsgScheduledEmail;
import cat.jiu.email.ui.gui.component.GuiFilterTextField;
import cat.jiu.email.ui.gui.component.GuiTime;
import cat.jiu.email.util.TimeMillis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringUtil;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;

public class GuiModifyScheduledEmail extends Screen {
    private final Screen parent;
    private final ScheduledEmail email;
    private final long id;
    private final String path;
    private int leftPos, topPos;
    private final GuiTime time = new GuiTime(false, true, true, true, true, false);
    private EditBox note, custom_addressee;
    private GuiFilterTextField idBox;
    private Button timeBtn, addresseeBtn;
    private int currentAddressee = 0;

    public GuiModifyScheduledEmail(ScheduledEmail email, Screen parent) {
        super(CommonComponents.EMPTY);
        this.email = email;
        this.parent = parent;
        this.id = email.getId();
        this.path = email.getAsFile().getName();
        this.time.setTime(email.getInterval());
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - 176) / 2;
        this.topPos = (this.height - 166) / 2;
        this.time.setEnable(false);


        this.idBox = this.addRenderableWidget(new GuiFilterTextField(String.valueOf(this.id), this.leftPos + 29, this.topPos + 20, 131, RenderUtils.getFontRenderer().lineHeight + 2));
        this.idBox.setMaxLength(Integer.MAX_VALUE);
        this.idBox.setBordered(false);
        this.idBox.setCanBeNegative(true);
        int
                x = this.leftPos + 29,
                y = this.topPos + 20 + RenderUtils.getFontRenderer().lineHeight + 2;
        y += RenderUtils.fontHeight() + 4;

        this.time.setRenderPos(x + 156, y - 26);

        this.addRenderableWidget(GuiInbox.GuiButton.builder(Component.literal(this.path), btn->
                    Minecraft.getInstance().keyboardHandler.setClipboard("/email scheduled modify file " + this.id + " ")
                )
                .pos(this.leftPos + 29, this.topPos + 20 + RenderUtils.fontHeight() + 2)
                .size(140, RenderUtils.fontHeight()+2)
                .tooltip(Tooltip.create(CommonComponents.GUI_COPY_LINK_TO_CLIPBOARD.copy()
                                .append(CommonComponents.NEW_LINE)
                                .append(Component.literal("/email scheduled modify file " + this.id + " "))
                        ))
                .build());

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
                    this.addresseeBtn.setWidth(RenderUtils.width(this.addresseeBtn.getMessage()) + 6);
                    this.custom_addressee.visible = this.currentAddressee==2;
                })
                .bounds(x, y += this.timeBtn.getHeight() + 4, width, RenderUtils.getFontRenderer().lineHeight + 2)
                .build());
        this.addWidget(this.addresseeBtn);
        this.currentAddressee = this.email.getAddressee().ordinal();
        this.addresseeBtn.setMessage(Component.translatable("info.inbox.scheduled.gen.addressee."+this.currentAddressee));
        this.addresseeBtn.setWidth(RenderUtils.width(this.addresseeBtn.getMessage()) + 6);

        this.note = this.addRenderableWidget(new EditBox(RenderUtils.getFontRenderer(), x, y += this.addresseeBtn.getHeight() + 2, 131, RenderUtils.getFontRenderer().lineHeight + 2, Component.empty()));
        this.note.setMaxLength(Integer.MAX_VALUE);
        this.note.setBordered(false);
        this.note.setValue(this.email.getNote());

        this.custom_addressee = this.addRenderableWidget(new EditBox(RenderUtils.getFontRenderer(), this.leftPos + 5, y += (this.note.getHeight() + 2)*2 + RenderUtils.fontHeight()*2, 160, RenderUtils.getFontRenderer().lineHeight + 2, Component.empty()));
        this.custom_addressee.setMaxLength(Integer.MAX_VALUE);
        this.custom_addressee.setBordered(false);
        this.custom_addressee.visible = this.email.getAddressee().isCustomPlayers();
        this.custom_addressee.setValue(String.join(",", this.email.getCustomAddressee()));

        this.addRenderableWidget(GuiInbox.GuiButton.builder(Component.translatable("info.inbox.confirm"), btn->this.modify())
                .bounds(this.leftPos + 8, this.topPos + 166 - 20, 160, RenderUtils.getFontRenderer().lineHeight + 2)
                .build());
    }

    public void modify() {
        if (this.time.getTimeOfMillis() <= 0) {
            return;
        }
        ScheduledEmail email = this.email
                .setId(this.idBox.getAsNumber().longValue())
                .setFilePath(this.path)
                .setInterval(new TimeMillis(this.time.getTimeOfMillis()))
                .setAddressee(ScheduledEmail.Addressee.get(this.currentAddressee))
                .setNote(this.note.getValue());

        if (!StringUtil.isNullOrEmpty(this.custom_addressee.getValue())) {
            email.getCustomAddressee().clear();
            for (String name : this.custom_addressee.getValue().split(",")) {
                email.addCustomAddressee(name);
            }
        }
        EmailMain.NETWORK.sendMessageToServer(new MsgScheduledEmail.Modify(this.id, email));

        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(graphics);

        RenderUtils.draw(graphics, GuiInbox.BackGround, this.leftPos, this.topPos, 176, 166, 0, 0);

        RenderUtils.drawCenteredString(graphics, I18n.get("info.inbox.scheduled.modify"), this.leftPos + (176/2), this.topPos + 5, Color.WHITE.getRGB(), true);
        int
                x = this.leftPos + 29,
                y = this.topPos + 20;
        RenderUtils.drawRightString(graphics, "ID: ", x, y, Color.WHITE.getRGB(), true);
        y += RenderUtils.fontHeight() + 4;
        RenderUtils.drawRightString(graphics, I18n.get("info.inbox.scheduled.gen.path"), x, y, Color.WHITE.getRGB(), true);

        RenderUtils.drawRightString(graphics, I18n.get("info.inbox.scheduled.gen.interval"), x, y += RenderUtils.getFontRenderer().lineHeight + 4, Color.WHITE.getRGB(), true);
        String time = this.time.getTimeOfMillis() <= 0 ? I18n.get("info.inbox.scheduled.gen.interval.change.un") : ITimer.formatTimestamp(this.time.getTimeOfMillis(), false, true, true, true, true);
        RenderUtils.drawString(graphics, time, this.timeBtn.getX() + this.timeBtn.getWidth() + 3, this.timeBtn.getY() + 2, Color.WHITE.getRGB(), true);

        RenderUtils.drawRightString(graphics, I18n.get("info.inbox.addressee") + ": ", x, y += RenderUtils.getFontRenderer().lineHeight + 4, Color.WHITE.getRGB(), true);
        RenderUtils.drawString(graphics, I18n.get("info.inbox.scheduled.gen.addressee."+this.currentAddressee + ".info"), this.addresseeBtn.getX() + this.addresseeBtn.getWidth() + 3, this.addresseeBtn.getY() + 2, Color.WHITE.getRGB(), true);

        RenderUtils.drawRightString(graphics, I18n.get("info.inbox.scheduled.gen.note"), x, y += RenderUtils.getFontRenderer().lineHeight + 4, Color.WHITE.getRGB(), true);

        super.render(graphics, pMouseX, pMouseY, pPartialTick);

        this.time.render(graphics, this.leftPos + 176 + 5, this.topPos + 5, pPartialTick);
        RenderUtils.hLine(graphics, this.note.getX(), this.note.getY() + this.note.getHeight() - 2, this.note.getWidth(), Color.LIGHT_GRAY.getRGB());
        RenderUtils.hLine(graphics, this.idBox.getX(), this.idBox.getY() + this.idBox.getHeight() - 2, this.idBox.getWidth(), Color.LIGHT_GRAY.getRGB());

        if (this.currentAddressee == 2) {
            RenderUtils.drawString(graphics, Arrays.asList(
                    I18n.get("info.inbox.scheduled.gen.addressee.2.custom.0"),
                    I18n.get("info.inbox.scheduled.gen.addressee.2.custom.1")
            ), this.custom_addressee.getX(), this.custom_addressee.getY() - RenderUtils.getFontRenderer().lineHeight*2 - 3, Color.WHITE.getRGB(), true);
            graphics.hLine(this.custom_addressee.getX(), this.custom_addressee.getX() + this.custom_addressee.getWidth(), this.custom_addressee.getY() + this.custom_addressee.getHeight() - 2, Color.LIGHT_GRAY.getRGB());
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        Minecraft.getInstance().setScreen(this.parent);
    }
}
