package cat.jiu.email.api;

import cat.jiu.core.util.Utils;
import cat.jiu.core.util.registry.StaticRegistry;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public interface IEmailStyle extends Supplier<ResourceLocation> {
    String ID_NAME = "style";
    static StaticRegistry<ResourceLocation, IEmailStyle> REGISTRY = new StaticRegistry<ResourceLocation, IEmailStyle>(EmailMain.MODID, "email/style")
            .setKeyGetter(ID_NAME, Utils::location)
    ;

    void renderBack(GuiGraphics graphics, Email email, int x, int y, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick);
    void renderContent(GuiGraphics graphics, Email email, int x, int y, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick);
    void renderSelection(GuiGraphics graphics, Email email, int x, int y, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick);

    @Override
    default ResourceLocation get(){return this.getID(); }
    ResourceLocation getID();
}
