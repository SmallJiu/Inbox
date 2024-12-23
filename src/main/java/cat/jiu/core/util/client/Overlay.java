package cat.jiu.core.util.client;

import cat.jiu.core.api.element.IOverlay;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
public class Overlay {
    static final ArrayList<IOverlay> OVERLAYS = new ArrayList<>();
    public static <T extends IOverlay> T register(T overlayInstance) {
        OVERLAYS.add(overlayInstance);
        return overlayInstance;
    }

    @SubscribeEvent
    public static void onPreOverlayRender(RenderGuiEvent.Pre event) {
        int centerX = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        int centerY = Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2;
        if (!OVERLAYS.isEmpty()) {
            for (int i = 0; i < OVERLAYS.size(); i++) {
                IOverlay overlay = OVERLAYS.get(i);
                if (overlay.isEnable() && overlay.render(event.getGuiGraphics(), event, centerX, centerY, true)) {
                    if (overlay.canRemove(true)) {
                        OVERLAYS.remove(i);
                    }
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPostOverlayRender(RenderGuiEvent.Post  event) {
        int centerX = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        int centerY = Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2;
        if (!OVERLAYS.isEmpty()) {
            for (int i = 0; i < OVERLAYS.size(); i++) {
                IOverlay overlay = OVERLAYS.get(i);
                if (overlay.isEnable() && overlay.render(event.getGuiGraphics(), event, centerX, centerY, false)) {
                    if (overlay.canRemove(false)) {
                        OVERLAYS.remove(i);
                    }
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onKeyTyped(InputEvent.Key event) {
        for (IOverlay overlay : OVERLAYS) {
            if (overlay.isEnable() && overlay.keyTyped(event.getKey(), event.getScanCode(), event.getAction(), event.getModifiers())){
                break;
            }
        }
    }
}
