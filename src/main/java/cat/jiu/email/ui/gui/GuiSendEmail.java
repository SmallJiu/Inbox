package cat.jiu.email.ui.gui;

import cat.jiu.core.util.Utils;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.EmailSenderGroup;
import cat.jiu.email.net.msg.MsgSend0;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.ui.gui.component.NameEditbox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiSendEmail extends GuiGenerateEmail {
    public GuiSendEmail(Screen parent) {
        super(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.generate.active = !GuiInbox.INBOX.isSendCooling();
        if (GuiInbox.INBOX.isSendCooling()) {
            if (this.generate.getTooltip() == null) {
                this.generate.setTooltip(Tooltip.create(Component.translatable("info.inbox.cooling")));
            }
        }else {
            this.generate.setTooltip(null);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void onGenerateEmailSuccess(Email email) {
        EmailMain.NETWORK.sendMessageToServer(new MsgSend0(EmailSenderGroup.PLAYER, this.name.getValue(), email));
    }

    @Override
    protected boolean canSendSound() {
        return false;
    }

    @Override
    protected boolean canAddEmailExpiration() {
        return Utils.isOp();
    }

    @Override
    protected boolean canSetEmailDeletable() {
        return Utils.isOp();
    }

    @Override
    protected boolean canAddFolderButton() {
        return false;
    }

    @Override
    protected boolean checkSendAttachment() {
        return true;
    }

    @Override
    protected EditBox getNameEditbox() {
        NameEditbox box = new NameEditbox(RenderUtils.getFontRenderer(), this.title.getX(), this.title.getY() + this.title.getHeight(), this.title.getWidth(), this.title.getHeight());
        box.setTooltip(Tooltip.create(Component.translatable("info.inbox.send.name")));
        return box;
    }

    @Override
    protected Component getNameInfo() {
        return Component.translatable("info.inbox.addressee");
    }

    @Override
    protected boolean canRenderDefultName() {
        return false;
    }

    @Override
    protected Component getConfirmGenerateMessage() {
        return Component.translatable("info.inbox.dispatch");
    }
}
