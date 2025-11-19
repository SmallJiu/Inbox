package cat.jiu.email.ui;

import cat.jiu.core.util.client.Overlay;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.attachment.AttachmentUndying;
import cat.jiu.email.element.attachment.AttachmentWaypoint;
import cat.jiu.email.net.msg.MsgOpenGui;
import cat.jiu.email.ui.gui.GuiBlacklist;
import cat.jiu.email.ui.gui.GuiGenerateEmail;
import cat.jiu.email.ui.gui.GuiScheduledEmail;
import cat.jiu.email.ui.gui.GuiSendEmail;
import cat.jiu.email.ui.gui.component.AttachmentInboxIcon;

import cat.jiu.email.EmailMain;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import xaero.common.HudMod;
import xaero.common.effect.Effects;
import xaero.common.gui.GuiAddWaypoint;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.misc.Misc;
import xaero.common.misc.OptimizedMath;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.container.MinimapWorldContainer;
import xaero.hud.minimap.world.container.MinimapWorldRootContainer;

import java.util.Objects;

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
		registerModGenWaypoint();
	}

	@OnlyIn(Dist.CLIENT)
	private static void registerModGenWaypoint(){
		AttachmentWaypoint.ChoiceMapModScreen.registerModGenWaypoint("xaerominimap", GuiHandler::addXaeroWaypoint);
	}

	@OnlyIn(Dist.CLIENT)
	public static void addXaeroWaypoint(AttachmentWaypoint.Waypoint waypoint) {
		if (ModList.get().isLoaded("xaerominimap")) {
			Minecraft mc = Minecraft.getInstance();
			if (!Misc.hasEffect(mc.player, Effects.NO_WAYPOINTS) && !Misc.hasEffect(mc.player, Effects.NO_WAYPOINTS_HARMFUL)) {
				MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
				MinimapWorldRootContainer container = session.getWorldManager().getRootWorldContainer(session.getWorldState().getCurrentWorldPath().getRoot());
				for (MinimapWorldContainer subContainer : container.getSubContainers()) {
					for (MinimapWorld minimapWorld : subContainer.getWorlds()) {
						ResourceKey<Level>
								minimapDimID = minimapWorld.getDimId(),
								waypointDimID = waypoint.dimension;
						if (Objects.equals(minimapDimID, waypointDimID)) {
							int
									x = OptimizedMath.myFloor(waypoint.pos.getX()),
									y = OptimizedMath.myFloor(waypoint.pos.getY() + (double) 0.0625F),
									z = OptimizedMath.myFloor(waypoint.pos.getZ());

							if (HudMod.INSTANCE.getSettings().waypointsGUI(session)) {
								double waypointDestDimScale = session.getDimensionHelper().getDimCoordinateScale(minimapWorld);
								double dimDiv = container.getDimensionScale(waypoint.dimension) / waypointDestDimScale;
								x = OptimizedMath.myFloor((double) x * dimDiv);
								z = OptimizedMath.myFloor((double) z * dimDiv);
								Waypoint instant = new Waypoint(x, y, z, I18n.get(waypoint.name), waypoint.extraName != null ? waypoint.extraName : "X", WaypointColor.getRandom(), WaypointPurpose.NORMAL, false, true);
								minimapWorld.getCurrentWaypointSet().add(instant, !HudMod.INSTANCE.getSettings().waypointsBottom);
								return;
							}
						}
					}
				}
			}
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
