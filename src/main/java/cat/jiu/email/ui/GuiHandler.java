package cat.jiu.email.ui;

import cat.jiu.core.util.client.Overlay;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.attachment.AttachmentUndying;
import cat.jiu.email.net.msg.MsgOpenGui;
import cat.jiu.email.ui.gui.GuiBlacklist;
import cat.jiu.email.ui.gui.GuiGenerateEmail;
import cat.jiu.email.ui.gui.GuiScheduledEmail;
import cat.jiu.email.ui.gui.GuiSendEmail;
import cat.jiu.email.ui.gui.component.AttachmentInboxIcon;

import cat.jiu.email.EmailMain;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;

public class GuiHandler {
	public static final int EMAIL_MAIN = 0;
	public static final int EMAIL_SEND = 1;
	public static final int EMAIL_BLACKLIST = 2;
	public static final int EMAIL_Generate = 3;
	public static final int EMAIL_Scheduled = 4;

	@OnlyIn(Dist.CLIENT)
	public static void registerScreen() {
		ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, ()->new ConfigScreenHandler.ConfigScreenFactory((mc, parent)->
				new cat.jiu.core.util.client.config.GuiConfig(parent, EmailMain.MODID)
		));
		Overlay.register(AttachmentUndying.UndyingCountOverlay.INSTANCE);

		if (ModList.get().isLoaded("attributeslib")) {
			AttachmentInboxIcon.initIconType();
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static void openGui(int ID) {
        switch (ID) {
            case EMAIL_MAIN:
				EmailAPI.openInbox();
				break;
			case EMAIL_SEND:
				Minecraft.getInstance().setScreen(new GuiSendEmail(Minecraft.getInstance().screen));
				break;
			case EMAIL_BLACKLIST:
				Minecraft.getInstance().setScreen(new GuiBlacklist(Minecraft.getInstance().screen));
				break;
			case EMAIL_Generate:
				Minecraft.getInstance().setScreen(new GuiGenerateEmail(Minecraft.getInstance().screen));
				break;
			case EMAIL_Scheduled:
				Minecraft.getInstance().setScreen(new GuiScheduledEmail(Minecraft.getInstance().screen));
				break;
        }
	}

	public static void openGui(int ID, ServerPlayer player) {
		EmailMain.NETWORK.sendMessageToPlayer(new MsgOpenGui(ID), player);
	}
}
