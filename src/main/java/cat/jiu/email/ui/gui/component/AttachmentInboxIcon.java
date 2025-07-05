package cat.jiu.email.ui.gui.component;

import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.element.attachment.AttachmentAttribute;
import cat.jiu.email.ui.InboxButton;
import dev.shadowsoffire.attributeslib.client.ModifierSource;
import dev.shadowsoffire.attributeslib.client.ModifierSourceType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

public class AttachmentInboxIcon extends ModifierSource<ItemStack> {
    static ModifierSourceType<ItemStack> TYPE = null;

    public static ModifierSourceType<ItemStack> getIconType() {
        if (TYPE == null) {
            TYPE = ModifierSourceType.register(new ModifierSourceType<ItemStack>() {
                @Override
                public void extract(LivingEntity entity, BiConsumer<AttributeModifier, ModifierSource<?>> consumer) {
                    String prefix = AttachmentAttribute.ID.toString();
                    for (AttributeInstance value : entity.getAttributes().attributes.values()) {
                        for (AttributeModifier modifier : value.getModifiers()) {
                            String name = modifier.getName();
                            if(name.startsWith(prefix)) {
                                consumer.accept(modifier, new AttachmentInboxIcon(name.endsWith(".temp")));
                            }
                        }
                    }
                }

                @Override
                public int getPriority() {
                    return 0;
                }
            });
        }
        return TYPE;
    }

    boolean temp;
    public AttachmentInboxIcon(boolean data) {
        super(getIconType(), (o1,o2)->0, ItemStack.EMPTY);
        this.temp = data;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int x, int y) {
        RenderUtils.draw(graphics, this.temp ? InboxButton.inbox_hover : InboxButton.inbox, x+1, y+2, 7, 4, 0, 0, 23, 15, 23, 15);
    }
}
