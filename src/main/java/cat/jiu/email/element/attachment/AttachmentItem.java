package cat.jiu.email.element.attachment;

import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonToStackUtil;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber
public class AttachmentItem implements IAttachment {
    public static final ResourceLocation ID = new ResourceLocation(EmailMain.MODID, "attachment/item");

    protected List<ItemStack> items, unmodifiable;

    public AttachmentItem() {}

    public AttachmentItem(CompoundTag tag) {
        this.readFrom(tag);
    }
    public AttachmentItem(JsonObject json) {
        this.readFrom(json);
    }

    public AttachmentItem addStack(ItemStack... stacks) {
        if (this.items==null) {
            this.items = new ArrayList<>();
        }
        this.items.addAll(Arrays.asList(stacks));
        return this;
    }
    public AttachmentItem addStacks(List<ItemStack> stacks) {
        if (this.items==null) {
            this.items = new ArrayList<>();
        }
        this.items.addAll(stacks);
        return this;
    }

    public ItemStack setStack(int slot, ItemStack newStack) {
        if (this.items!=null) {
            return this.items.set(slot, newStack);
        }
        return ItemStack.EMPTY;
    }

    public ItemStack remove(int slot) {
        if (this.items!=null) {
            return this.items.remove(slot);
        }
        return ItemStack.EMPTY;
    }

    public void removeAll(){
        if (this.items!=null) {
            this.items.clear();
        }
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentItem attachment && !attachment.isEmpty()) {
            this.addStacks(attachment.getItems());
        }
    }

    public List<ItemStack> getItems() {
        if (this.unmodifiable==null) {
            this.unmodifiable = Collections.unmodifiableList(this.items);
        }
        return this.unmodifiable;
    }

    @Override
    public boolean isEmpty() {
        return this.items==null || this.items.isEmpty();
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {
        if (!this.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack item : this.items) {
                list.add(item.save(new CompoundTag()));
            }
            nbt.put("items", list);
        }
        return nbt;
    }

    @Override
    public void read(CompoundTag nbt) {
        if (nbt.contains("items")) {
            ListTag list = nbt.getList("items", 10);
            for (int i = 0; i < list.size(); i++) {
                this.addStack(ItemStack.of(list.getCompound(i)));
            }
        }
    }

    @Override
    public JsonObject write(JsonObject json) {
        if (!this.isEmpty()) {
            json.add("items", JsonToStackUtil.toJsonArray(this.items, false));
        }
        return json;
    }

    @Override
    public void read(JsonObject json) {
        if (json.has("items")) {
            this.items = JsonToStackUtil.toStacks(json.get("items"));
        }
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void accept(Player player) {
        if (!this.isEmpty()) {
            EmailUtils.spawnAsEntity(player, this.items);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(AttachmentEvent.Render event) {
        if (!this.isEmpty()) {
            event.graphics.drawString(event.font, Component.translatable("info.email.item_save_to_email").append(" (").append(Component.translatable(event.email.isReceived() ? "info.email.filter.is_accept" : "info.email.filter.not_accept")).append(")"), event.x, event.getY(), Color.WHITE.getRGB());
            event.addY(event.font.lineHeight + 2);

            int itemX = event.x;
            for (ItemStack item : this.getItems()) {
                if (itemX >= event.x + event.viewWidth - 36) {
                    event.addY(16 + 2);
                    itemX = event.x;
                }
                event.graphics.renderItem(item, itemX, event.getY());
                event.graphics.renderItemDecorations(event.font, item, itemX, event.getY());
                if (event.canSee() && EmailUtils.isInRange(event.mouseX, event.mouseY, itemX, event.getY(), 16, 16)) {
                    event.disableScissor();
                    event.graphics.renderTooltip(event.font, item, event.mouseX, event.mouseY);
                    event.enableScissor();
                }
                itemX += 16 + 2;
            }
            event.addY(16 + 2);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void getHeight(AttachmentEvent.GetHeight event) {
        if (!this.isEmpty()) {
            event.addHeight(event.font.lineHeight + 2);

//            int maxWidth = event.guiWidth - 36;
//            int lineCount = maxWidth / (16 + 2) ;
//            float line1 = (float)attachment.getItems().size() / lineCount;
//            int line = (int)line1;
//            int less = attachment.getItems().size() - (line * lineCount);
//            if (less > 0) {
//                line += 1;
//            }
//            event.addHeight((16 + 2) * line);

            event.addHeight(16 + 2);
            int itemX = 0;
            for (ItemStack item : this.getItems()) {
                if (itemX >= event.guiWidth - 36) {
                    event.addHeight(16 + 2);
                    itemX = 0;
                }
                itemX += 16 + 2;
            }
        }
    }
}
