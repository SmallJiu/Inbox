package cat.jiu.email.api;

import cat.jiu.core.util.Utils;
import cat.jiu.core.util.registry.StaticRegistry;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

public interface IEmailStyle extends Supplier<ResourceLocation> {
    static String NAME_ID = "style";
    static StaticRegistry<ResourceLocation, IEmailStyle> REGISTRY = new StaticRegistry<ResourceLocation, IEmailStyle>(EmailMain.MODID, "email/style")
            .setKeyGetter(
                    data-> Utils.location(data.getString(NAME_ID)),
                    data->Utils.location(data.get(NAME_ID).getAsString()),
                    data -> data.getLocation(NAME_ID)
            );

    void renderBack(GuiGraphics graphics, Email email, int x, int y, int width, int height, int mouseX, int mouseY, boolean isMouseOver, boolean canScroll, float partialTick);
    void renderContent(GuiGraphics graphics, Email email, int x, int y, int width, int height, int mouseX, int mouseY, boolean isMouseOver, boolean canScroll, float partialTick);
    void renderSelection(GuiGraphics graphics, Email email, int x, int y, int width, int height, int mouseX, int mouseY, boolean isMouseOver, boolean canScroll, float partialTick);

    @Override
    default ResourceLocation get(){return this.getID(); }
    ResourceLocation getID();
}
