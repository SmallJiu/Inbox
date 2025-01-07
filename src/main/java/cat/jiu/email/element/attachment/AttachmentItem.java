package cat.jiu.email.element.attachment;

import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.core.util.JsonToStackUtil;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber
public class AttachmentItem implements IAttachment {
    public static final ResourceLocation ID = new ResourceLocation(EmailMain.MODID, "attachment/item");

    protected List<ItemStack> items, unmodifiable;

    public AttachmentItem() {}

    public AttachmentItem(CompoundNBT tag) {
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
        if (other instanceof AttachmentItem && !other.isEmpty()) {
            this.addStacks(((AttachmentItem) other).getItems());
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
    public CompoundNBT write(CompoundNBT nbt) {
        if (!this.isEmpty()) {
            ListNBT list = new ListNBT();
            for (ItemStack item : this.items) {
                list.add(item.write(new CompoundNBT()));
            }
            nbt.put("items", list);
        }
        return nbt;
    }

    @Override
    public void read(CompoundNBT nbt) {
        if (nbt.contains("items")) {
            ListNBT list = nbt.getList("items", 10);
            for (int i = 0; i < list.size(); i++) {
                this.addStack(ItemStack.read(list.getCompound(i)));
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
    public void accept(PlayerEntity player) {
        if (!this.isEmpty()) {
            EmailUtils.spawnAsEntity(player, this.items);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(AttachmentEvent.Render event) {
        if (!this.isEmpty()) {
            event.renderSaveTo("info.inbox.items");
            event.addY(event.font.FONT_HEIGHT + 4);

            int itemX = event.x;
            for (ItemStack item : this.getItems()) {
                if (itemX >= event.x + event.viewWidth - 36) {
                    event.addY(16);
                    itemX = event.x;
                }
                event.renderItem(item, itemX, event.getY());
                if (event.canSee() && EmailUtils.isInRange(event.mouseX, event.mouseY, itemX, event.getY(), 16, 16)) {
                    event.disableScissor();
                    RenderUtils.drawItemTooltip(event.stack, item, event.mouseX, event.mouseY);
                    event.enableScissor();
                }
                itemX += 16;
            }
            event.addY(16);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void getHeight(AttachmentEvent.GetHeight event) {
        if (!this.isEmpty()) {
            event.addHeight(event.font.FONT_HEIGHT + 2);

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
