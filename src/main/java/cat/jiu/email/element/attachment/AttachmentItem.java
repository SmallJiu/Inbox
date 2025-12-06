package cat.jiu.email.element.attachment;

import cat.jiu.core.api.IData;
import cat.jiu.core.api.Lambdas;
import cat.jiu.core.util.DataUtils;
import cat.jiu.core.util.Utils;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.AttachmentSendScreenWidget;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.ui.gui.GuiInbox;
import cat.jiu.email.util.EmailUtils;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.awt.Color;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class AttachmentItem implements IAttachment {
    public static final ResourceLocation ID = Utils.location(EmailMain.MODID, "attachment/item");

    protected List<ItemStack> items;
    protected Map<Integer, Integer> slots;

    public AttachmentItem() {}

    public AttachmentItem(CompoundTag tag) {
        this.read(tag);
    }
    public AttachmentItem(JsonObject json) {
        this.read(json);
    }
    public AttachmentItem(IData.IMapData<?> data) {
        this.read(data);
    }

    public AttachmentItem addStack(ItemStack... stacks) {
        if (this.items==null) {
            this.items = new ArrayList<>();
        }
        this.items.addAll(Arrays.asList(stacks));
        return this;
    }
    public AttachmentItem addStacks(Collection<ItemStack> stacks) {
        if (this.items==null) {
            this.items = new ArrayList<>();
        }
        this.items.addAll(stacks);
        return this;
    }

    public AttachmentItem addSlots(int slot, int count) {
        if (this.slots==null) {
            this.slots = new HashMap<>();
        }
        this.slots.put(slot, count);
        return this;
    }
    public AttachmentItem addSlots(Map<Integer, Integer> slots) {
        if (this.slots==null) {
            this.slots = new HashMap<>();
        }
        this.slots.putAll(slots);
        return this;
    }
    public AttachmentItem addStackCount(Map<Integer, ItemStack> slots) {
        for (Integer slot : slots.keySet()) {
            this.addSlots(slot, slots.get(slot).getCount());
        }
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

    public List<ItemStack> getItems() {
        return this.items;
    }
    public Map<Integer, Integer> getSlots() {
        return slots;
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentItem attachment && !attachment.isEmpty()) {
            this.addStacks(attachment.getItems());
            if (attachment.getSlots()!=null) {
                this.addSlots(attachment.getSlots());
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return this.items==null || this.items.isEmpty();
    }

    @Override
    public IData.IMapData<?> write(IData.IMapData<?> data) {
        if (!this.isEmpty()) {
            data.putData("items", DataUtils.toData(this.getItems(), data.newList()));
        }
        Map<Integer, Integer> slots = this.getSlots();
        if (slots !=null && !slots.isEmpty()) {
            IData.IMapData<?> slotDatas = data.newMap();
            for (Integer slot : slots.keySet()) {
                slotDatas.putData(String.valueOf(slot), slots.get(slot));
            }
            data.putData("slots", slotDatas);
        }
        return data;
    }

    @Override
    public void read(IData.IMapData<?> data) {
        this.items = DataUtils.toStack(data.getList("items", IData.IMapData.class));
        data.getMap("slots").foreach((k,v)->
            this.addSlots(Integer.parseInt(k), v.getAsPrimitive().getAsInt())
        );
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public String getName() {
        return "info.inbox.items";
    }

    @Override
    public void accept(Player player) {
        if (!this.isEmpty()) {
            EmailUtils.spawnAsEntity(player, this.getItems());
        }
    }

    @Override
    public boolean onPlayerSendCheck(Player player, Consumer<Component> msgHandler) {
        if (!this.isEmpty() && !player.isCreative()) {
            Map<Integer, Integer> slots = this.getSlots();
            if (slots != null && !slots.isEmpty()) {
                for (int slot : slots.keySet()) {
                    ItemStack stack = player.getInventory().getItem(slot);
                    if (stack.getCount() < slots.get(slot)) {
                        msgHandler.accept(Component.translatable("info.inbox.generate.attachment.item.send.fail", slot, slots.get(slot), stack.getCount()));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void onPlayerSendChecked(Player player) {
        if (!this.isEmpty() && !player.isCreative()) {
            Map<Integer, Integer> slots = this.getSlots();
            if (slots != null && !slots.isEmpty()) {
                for (int slot : slots.keySet()) {
                    player.getInventory().getItem(slot).shrink(slots.get(slot));
                }
            }
            player.inventoryMenu.broadcastChanges();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(AttachmentEvent.Render event) {
        if (!this.isEmpty()) {
            event.renderSaveTo("info.inbox.items");
            event.addY(event.font.lineHeight);

            int itemX = event.x;
            for (ItemStack item : this.getItems()) {
                if (itemX >= event.x + event.viewWidth - 30) {
                    event.addY(16);
                    itemX = event.x;
                }
                int y = event.getY() + 4;
                event.graphics.renderItem(item, itemX, y);
                event.graphics.renderItemDecorations(event.font, item, itemX, y);
                if (event.canSee() && EmailUtils.isInRange(event.mouseX, event.mouseY, itemX, y, 16, 16)) {
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

//            int maxWidth = event.guiWidth - 30;
//            int lineCount = maxWidth / (16 + 2) ;
//            float line1 = (float)this.getItems().size() / lineCount;
//            int line = (int)line1;
//            int less = this.getItems().size() - (line * lineCount);
//            if (less > 0) {
//                line += 1;
//            }
//            event.addHeight(16 * line);

            event.addHeight(event.font.lineHeight + 4);
            int itemX = 0;
            for (ItemStack item : this.getItems()) {
                if (itemX >= event.guiWidth - 30) {
                    event.addHeight(16);
                    itemX = 0;
                }
                itemX += 16 + 2;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class Widget extends AttachmentSendScreenWidget {
        public static final ResourceLocation TEXTURE = new ResourceLocation(EmailMain.MODID, "textures/gui/container/inbox_generate.png");
        public static final int SelectedColor = RenderUtils.rgb(255, 0, 0, 60);
        public static final AttachmentSendScreenWidget.WidgetEntry INSTANCE = new WidgetEntry(ID, true, Widget::new);

        public Map<Integer, ItemStack> selectedStacks = new HashMap<>();
        public final Button selectAll, cancelAll;
        public final Lambdas.Consumer2<Integer, ItemStack> handler;
        public Widget() {
            super(Component.translatable("info.inbox.items"));
            this.setWidth(172);
            this.setHeight(84);
            this.handler = (slot, stack)->{
                if (this.selectedStacks.containsKey(slot)) {
                    this.selectedStacks.remove(slot);
                }else if (!stack.isEmpty()) {
                    this.selectedStacks.put(slot, stack.copy());
                }
            };
            Component text = Component.translatable("info.inbox.generate.attachment.item.select_all");
            this.selectAll = GuiInbox.GuiButton.builder(text, b->{
                for (int i = 0; i < Minecraft.getInstance().player.getInventory().items.size(); i++) {
                    ItemStack stack = Minecraft.getInstance().player.getInventory().items.get(i);
                    if (!stack.isEmpty() && !this.selectedStacks.containsKey(i)) {
                        this.selectedStacks.put(i, stack.copy());
                    }
                }
            }).size(RenderUtils.width(text) + 6, RenderUtils.fontHeight() + 6).build();

            text = Component.translatable("info.inbox.generate.attachment.item.cancel_all");
            this.cancelAll = GuiInbox.GuiButton.builder(text, b->
                this.selectedStacks.clear()
            ).size(RenderUtils.width(text) + 6, RenderUtils.fontHeight() + 6).build();
        }

        @Override
        public IAttachment newAttachmentInstance() {
            IAttachment attachment = new AttachmentItem().addStacks(this.selectedStacks.values()).addStackCount(this.selectedStacks);
            this.selectedStacks = new HashMap<>();
            return attachment;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            RenderUtils.draw(graphics, TEXTURE, this.getX(), this.getY(), this.width, this.height, 0, 0);

            this.selectAll.setPosition(this.getX() + this.getWidth() + 2, this.getY() + 15);
            this.selectAll.render(graphics, mouseX, mouseY, partialTick);
            this.cancelAll.setPosition(this.selectAll.getX(), this.selectAll.getY() + this.selectAll.getHeight() + 2);
            this.cancelAll.render(graphics, mouseX, mouseY, partialTick);

            for (Integer slot : this.selectedStacks.keySet()) {
                ItemStack stack = this.selectedStacks.get(slot);
                ItemStack baseStack = Minecraft.getInstance().player.getInventory().getItem(slot);
                if (baseStack.isEmpty()) {
                    this.selectedStacks.remove(slot);
                }else if (stack.getCount() > baseStack.getCount()) {
                    stack.setCount(baseStack.getCount());
                }
            }

            int
                    x = this.getX() + 6,
                    y = this.getY() + 62;
            for (int slot = 0; slot < 9; slot++) {
                this.renderItem(graphics, slot, x, y, mouseX, mouseY);
                x += 16 + 2;
            }
            y = this.getY() + 6;
            for (int slotY = 0; slotY < 3; slotY++) {
                x = this.getX() + 6;
                for (int slotX = 0; slotX < 9; slotX++) {
                    this.renderItem(graphics, 9 + slotX + slotY * 9, x, y, mouseX, mouseY);
                    x += 2 + 16;
                }
                y += 2 + 16;
            }
        }

        public void renderItem(GuiGraphics graphics, int slot, int x, int y, int mouseX, int mouseY) {
            ItemStack stack = this.selectedStacks.containsKey(slot) ? this.selectedStacks.get(slot) : Minecraft.getInstance().player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, x, y);
                graphics.renderItemDecorations(RenderUtils.getFontRenderer(), stack, x, y);

                graphics.pose().pushPose();
                RenderSystem.enableDepthTest();
                graphics.pose().translate(0, 0, 5000);
                if (this.selectedStacks.containsKey(slot)) {
                    RenderUtils.fill(graphics, x, y, 16, 16, SelectedColor);
                }
                if (RenderUtils.inRange(mouseX, mouseY, x, y, 16, 16)) {
                    if (this.selectedStacks.containsKey(slot)) {
                        List<FormattedText> texts = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), stack));
                        texts.add(CommonComponents.EMPTY);
                        if (stack.getCount() > 1){
                            texts.add(Component.translatable("info.inbox.generate.attachment.item"));
                        }
                        texts.add(Component.translatable("info.inbox.generate.attachment.item.selected"));
                        graphics.renderComponentTooltip(RenderUtils.getFontRenderer(), texts, mouseX, mouseY, stack);
                    }else {
                        graphics.renderTooltip(RenderUtils.getFontRenderer(), stack, mouseX, mouseY);
                    }
                }
                graphics.pose().popPose();
            }

            RenderUtils.drawScaledString(graphics, String.valueOf(slot), x*2 + 1, y*2, Color.WHITE.getRGB(), true, 0.5f, 5000);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 5000);
            graphics.pose().scale(0.5f, 0.5f, 0);
            RenderUtils.drawString(graphics, String.valueOf(slot), x*2 + 1, y*2, Color.WHITE.getRGB(), true);
            graphics.pose().popPose();
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            return this.selectAll.mouseClicked(pMouseX, pMouseY, pButton) ||
                    this.cancelAll.mouseClicked(pMouseX, pMouseY, pButton) ||
                    super.mouseClicked(pMouseX, pMouseY, pButton);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            Inventory inventory = Minecraft.getInstance().player.getInventory();
            int
                    x = this.getX() + 6,
                    y = this.getY() + 62;
            for (int slot = 0; slot < 9; slot++) {
                if (RenderUtils.inRange(mouseX, mouseY, x, y, 16, 16)) {
                    this.handler.accept(slot, inventory.getItem(slot));
                    return;
                }
                x += 16 + 2;
            }
            y = this.getY() + 6;
            for (int slotY = 0; slotY < 3; slotY++) {
                x = this.getX() + 6;
                for (int slotX = 0; slotX < 9; slotX++) {
                    int slot = 9 + slotX + slotY * 9;
                    if (RenderUtils.inRange(mouseX, mouseY, x, y, 16, 16)) {
                        this.handler.accept(slot, inventory.getItem(slot));
                        return;
                    }
                    x += 2 + 16;
                }
                y += 2 + 16;
            }
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if (Screen.hasControlDown()) {
                int
                        x = this.getX() + 6,
                        y = this.getY() + 62;
                for (int slot = 0; slot < 9; slot++) {
                    if (RenderUtils.inRange(mouseX, mouseY, x, y, 16, 16) && this.updataSelectStackCount(slot, delta < 0)) {
                        return true;
                    }
                    x += 16 + 2;
                }
                y = this.getY() + 6;
                for (int slotY = 0; slotY < 3; slotY++) {
                    x = this.getX() + 6;
                    for (int slotX = 0; slotX < 9; slotX++) {
                        int slot = 9 + slotX + slotY * 9;
                        if (RenderUtils.inRange(mouseX, mouseY, x, y, 16, 16) && this.updataSelectStackCount(slot, delta < 0)) {
                            return true;
                        }
                        x += 2 + 16;
                    }
                    y += 2 + 16;
                }
            }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        public boolean updataSelectStackCount(int slot, boolean shrink){
            if (this.selectedStacks.containsKey(slot)) {
                ItemStack
                        stack = this.selectedStacks.get(slot),
                        baseStack = Minecraft.getInstance().player.getInventory().getItem(slot);
                if (baseStack.getCount() > 1 && stack.getCount() <= baseStack.getCount()) {
                    if (shrink) {
                        stack.shrink(1);
                    } else {
                        stack.grow(1);
                    }
                    if (stack.getCount() <= 0) {
                        stack.setCount(1);
                    } else if (stack.getCount() > baseStack.getCount()) {
                        stack.setCount(baseStack.getCount());
                    }
                }
                return true;
            }
            return false;
        }
    }
}
