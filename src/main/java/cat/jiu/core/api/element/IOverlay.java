package cat.jiu.core.api.element;

import cat.jiu.core.api.handler.IJsonSerializable;
import cat.jiu.core.api.handler.INBTSerializable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;

import java.util.function.Supplier;

public interface IOverlay extends INBTSerializable, IJsonSerializable, Supplier<ResourceLocation> {

    @OnlyIn(Dist.CLIENT)
    void render(GuiGraphics graphics);

    /**
     * @return true if canceled all overlay render.
     */
    @OnlyIn(Dist.CLIENT)
    boolean render(GuiGraphics graphics, RenderGuiEvent event, int windowCenterX, int windowCenterY, boolean preEvent);

    /**
     * @return true if canceled all overlay response keyTyped.
     */
    @OnlyIn(Dist.CLIENT)
    boolean keyTyped(int key, int scanCode, int action, int modifiers);

    /**
     * @return true if type or event is you need.
     */
    @OnlyIn(Dist.CLIENT)
    boolean isEffectType(boolean isPreEvent);

    /**
     * @return true if this overlay instance is enable on window render.
     */
    @OnlyIn(Dist.CLIENT)
    boolean isEnable();

    boolean canRemove(boolean preEvent);
}
