package cat.jiu.email.element.attachment;

import cat.jiu.core.api.IData;
import cat.jiu.core.util.Utils;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.AttachmentSendScreenWidget;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.ui.gui.GuiInbox;
import cat.jiu.email.ui.gui.component.GuiFilterTextField;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AttachmentWaypoint implements IAttachment {
    public static final ResourceLocation ID = Utils.location(EmailMain.MODID, "attachment/waypoint");
    public static final ItemStack MAP = new ItemStack(Items.FILLED_MAP);

    protected Map<ResourceKey<Level>, List<Waypoint>> waypoints;

    public AttachmentWaypoint() {
    }

    public AttachmentWaypoint(IData.IMapData<?> data) {
        this.read(data);
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    public Map<ResourceKey<Level>, List<Waypoint>> getDimensionWaypoints() {
        return waypoints;
    }
    public List<Waypoint> getWaypoints(){
        if (!this.isEmpty()) {
            List<Waypoint> waypoints = Lists.newArrayList();
            this.waypoints.forEach((key, value) -> waypoints.addAll(value));
            return waypoints;
        }
        return Collections.emptyList();
    }

    public AttachmentWaypoint setWaypoints(Map<ResourceKey<Level>, List<Waypoint>> waypoints) {
        this.waypoints = waypoints;
        return this;
    }
    public int getWaypointCount(){
        AtomicInteger result = new AtomicInteger();
        if (!this.isEmpty()) {
            this.waypoints.forEach((key, value) -> result.addAndGet(value.size()));
        }
        return result.get();
    }

    /**
     * @param dimension look like {@link Level#OVERWORLD}
     * @param name can be translation key
     */
    public AttachmentWaypoint addWaypoint(ResourceKey<Level> dimension, BlockPos pos, String name, int color) {
        return this.addWaypoint(new Waypoint(dimension, pos, name)
                .setColor(color)
        );
    }
    public AttachmentWaypoint addWaypoint(Waypoint waypoint) {
        if (this.getDimensionWaypoints() == null) {
            this.setWaypoints(new HashMap<>());
        }
        if (!this.getDimensionWaypoints().containsKey(waypoint.dimension)) {
            this.getDimensionWaypoints().put(waypoint.dimension, new ArrayList<>());
        }
        this.getDimensionWaypoints().get(waypoint.dimension).add(waypoint);
        return this;
    }

    @Override
    public boolean isEmpty() {
        return this.getDimensionWaypoints() == null || this.getDimensionWaypoints().isEmpty();
    }

    @Override
    public void accept(Player player) {
        if (player.level().isClientSide() && !this.isEmpty()) {
            this.accept();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void accept() {
        Minecraft.getInstance().setScreen(new ChoiceMapModScreen(Minecraft.getInstance().screen, this.getWaypoints(), Screen.hasShiftDown()));
//        this.getWaypoints().forEach(GuiHandler::addXaeroWaypoint);
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentWaypoint attachment && !attachment.isEmpty()) {
            attachment.getDimensionWaypoints().forEach((key, waypoints) -> waypoints.forEach(this::addWaypoint));
        }
    }

    @Override
    public void read(IData.IMapData<?> data) {
        data.getMap("waypoints").foreach((key, value) -> {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, Utils.location(key));
            if (value.isList()) {
                value.getAsList().foreach((index, waypoint_d) ->
                    this.addWaypoint(new Waypoint(
                            dimension,
                            new BlockPos(
                                    waypoint_d.getAsMap().getInt("x"),
                                    waypoint_d.getAsMap().getInt("y"),
                                    waypoint_d.getAsMap().getInt("z")
                            ),
                            waypoint_d.getAsMap().getString("name", "Point_" + this.getWaypointCount())
                        )
                            .setColor(waypoint_d.getAsMap().getInt("color"))
                            .setExtraName(waypoint_d.getAsMap().getString("extraName", null))
                    )
                );
            }else if (value.isMap()) {
                this.addWaypoint(new Waypoint(
                                dimension,
                                new BlockPos(
                                        value.getAsMap().getInt("x"),
                                        value.getAsMap().getInt("y"),
                                        value.getAsMap().getInt("z")
                                ),
                                value.getAsMap().getString("name", "Point_" + this.getWaypointCount())
                        )
                                .setColor(value.getAsMap().getInt("color"))
                                .setExtraName(value.getAsMap().getString("extraName", null))
                );
            }
        });
    }

    @Override
    public IData.IMapData<?> write(IData.IMapData<?> data) {
        if (!this.isEmpty()) {
            IData.IMapData<?> waypoints = data.newMap();
            this.getDimensionWaypoints().forEach((key, waypoints_d) -> {
                IData.IListData<?> dimension = data.newList();
                waypoints_d.forEach(waypoint ->{
                    IData.IMapData<?> waypoint_d = data.newMap();
                    waypoint_d.putData("x", waypoint.pos.getX());
                    waypoint_d.putData("y", waypoint.pos.getY());
                    waypoint_d.putData("z", waypoint.pos.getZ());
                    waypoint_d.putData("name", waypoint.name);
                    if (waypoint.extraName != null) {
                        waypoint_d.putData("extraName", waypoint.extraName);
                    }
                    waypoint_d.putData("color", waypoint.color);
                    dimension.putData(waypoint_d);
                });
                waypoints.putData(String.valueOf(key.location()), dimension);
            });
            data.putData("waypoints", waypoints);
        }
        return data;
    }

    @Override
    public String getName() {
        return "info.inbox.waypoints";
    }

    @Override
    public ItemStack getDisplayStack() {
        return MAP;
    }

    @Override
    public List<Component> getHoverMessage() {
        return Arrays.asList(Component.translatable("info.inbox.generate.attachment.waypoints.regenerate"));
    }

    private int iconX, iconY;
    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawIcon(AttachmentEvent.Render event, int x) {
        this.iconX = x;
        this.iconY = event.getY();
        IAttachment.super.drawIcon(event, x);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean onClicked(double mouseX, double mouseY, int button) {
        if (button == 0
                && RenderUtils.inRange(mouseX, mouseY, this.iconX, this.iconY, 16, 16)) {
            Minecraft.getInstance().setScreen(new ChoiceMapModScreen(Minecraft.getInstance().screen, this.getWaypoints(), Screen.hasShiftDown()));
            return true;
        }
        return false;
    }

    public static class Waypoint {
        public final ResourceKey<Level> dimension;
        public final BlockPos pos;
        public final String name;
        public String extraName;
        public int color;

        /**
         * @param dimension look like {@link Level#OVERWORLD}
         * @param name can be translation key
         */
        public Waypoint(ResourceKey<Level> dimension, BlockPos pos, String name) {
            this.dimension = dimension;
            this.pos = pos;
            this.name = name;
        }
        public Waypoint setColor(ChatFormatting color) {
            return this.setColor(color.getColor() == null ? Color.BLACK.getRGB() : color.getColor());
        }
        public Waypoint setColor(Color color) {
            return this.setColor(color.getRGB());
        }
        public Waypoint setColor(int color) {
            this.color = color;
            return this;
        }
        public Waypoint setExtraName(String extraName) {
            this.extraName = extraName;
            return this;
        }
    }
    @OnlyIn(Dist.CLIENT)
    public static class Widget extends AttachmentSendScreenWidget {
        public static final WidgetEntry INSTANCE = new WidgetEntry(ID, true, Widget::new);

        public final List<ResourceLocation> dimensions = new ArrayList<>();
        public final SubWidget waypoints;
        public final Button add;
        Widget() {
            super(Component.translatable("info.inbox.waypoints"));

            Minecraft.getInstance().level.registryAccess().registry(Registries.DIMENSION_TYPE).ifPresent(registry ->
                this.dimensions.addAll(registry.keySet())
            );
            this.dimensions.removeIf(rl-> rl.getPath().equals("overworld_caves"));

            this.initSubWiget(false);

            this.waypoints = this.addSubWidget(
                    new SubWidget(false),
                    3, 4, 0, 0
            ).cast();

            this.add = this.addSubWidget(Button.builder(Component.literal("++++++  ").append(Component.translatable("info.inbox.waypoints")).append("  ++++++"), b->{
                        UUID uuid = UUID.randomUUID();
                        this.waypoints.addWiget(new AttachmentWaypoint.Widget.WaypointWeiget(uuid, this.dimensions, ()->
                                this.waypoints.widgets.removeIf(wiget -> {
                                    if (wiget.widget instanceof AttachmentWaypoint.Widget.WaypointWeiget) {
                                        return ((AttachmentWaypoint.Widget.WaypointWeiget) wiget.widget).uuid.equals(uuid);
                                    }
                                    return false;
                                })
                        ),0, 5, 0, 0);
                    }).size(350, 15)
                    .build(), 2, 0, 0, 0).cast();
        }

        @Override
        public IAttachment newAttachmentInstance() {
            AttachmentWaypoint attachment = new AttachmentWaypoint();
            for (SubWidget.PositionWiget widget : this.waypoints.widgets) {
                if (widget.widget instanceof WaypointWeiget) {
                    WaypointWeiget waypoint = (WaypointWeiget) widget.widget;
                    attachment.addWaypoint(new Waypoint(
                            ResourceKey.create(Registries.DIMENSION, Utils.location(waypoint.dimension.getValue())),
                            new BlockPos(waypoint.x.getAsNumber().intValue(), waypoint.y.getAsNumber().intValue(), waypoint.z.getAsNumber().intValue()),
                            waypoint.name.getValue()
                    ).setExtraName(waypoint.extraName.getValue()));
                }
            }
            return attachment;
        }

        public static class WaypointWeiget extends SubWidget {
            public final UUID uuid;
            public final EditBox dimension, name, extraName;
            public ModifierWidget dimensionModifier;
            public GuiFilterTextField x, y, z;

            public WaypointWeiget(UUID uuid, List<ResourceLocation> dimensions, Runnable onRemove) {
                super(true);
                this.uuid = uuid;

                this.dimension = this.addWiget(new EditBox(
                        RenderUtils.getFontRenderer(), 0, 0, 120, RenderUtils.fontHeight()+4, CommonComponents.EMPTY
                )).cast();
                this.dimension.setValue(Minecraft.getInstance().player.level().dimension().location().toString());

                AtomicInteger dimensionIndex = new AtomicInteger(dimensions.indexOf(Minecraft.getInstance().player.level().dimension().location()));
                this.dimensionModifier = this.addWiget(new ModifierWidget(false, ()-> {
                    dimensionIndex.set(dimensionIndex.get() + 1);
                    if (dimensionIndex.get() >= dimensions.size()) {
                        dimensionIndex.set(0);
                    }
                    this.dimension.setValue(String.valueOf(dimensions.get(dimensionIndex.get())));
                    Tooltip dimensionName = Tooltip.create(Component.literal(this.dimension.getValue()));
                    this.dimension.setTooltip(dimensionName);
                    this.dimensionModifier.setTooltip(dimensionName);
                }, ()->{
                    dimensionIndex.set(dimensionIndex.get() - 1);
                    if (dimensionIndex.get() < 0) {
                        dimensionIndex.set(dimensions.size()-1);
                    }
                    this.dimension.setValue(String.valueOf(dimensions.get(dimensionIndex.get())));
                    Tooltip dimensionName = Tooltip.create(Component.literal(this.dimension.getValue()));
                    this.dimension.setTooltip(dimensionName);
                    this.dimensionModifier.setTooltip(dimensionName);
                }), 0, 0, 2, 2).cast();

                Tooltip dimensionName = Tooltip.create(Component.literal(this.dimension.getValue()));
                this.dimension.setTooltip(dimensionName);
                this.dimensionModifier.setTooltip(dimensionName);

                this.name = this.addWiget(new EditBox(
                        RenderUtils.getFontRenderer(), 0, 0, 50, RenderUtils.fontHeight()+4, CommonComponents.EMPTY
                ), 0, 0, 2, 0).cast();
                this.name.setValue("Waypoint");

                this.extraName = this.addWiget(new EditBox(
                        RenderUtils.getFontRenderer(), 0, 0, 20, RenderUtils.fontHeight()+4, CommonComponents.EMPTY
                ), 0, 0, 5, 2).cast();
                this.extraName.setValue("X");

                this.x = this.addWiget(new GuiFilterTextField(
                        String.valueOf((int)Minecraft.getInstance().player.getX()), false, 0, 0, 27, RenderUtils.fontHeight()+4
                ), 0, 0, 3, 0).setWigetRender(widget->{
                    RenderUtils.drawRightString(widget.graphics, "X:", widget.widget.getX()-1, widget.widget.getY() + 2, -1, true);
                    return RenderUtils.width("X: ") + 2;
                }, null).cast();
                this.y = this.addWiget(new GuiFilterTextField(
                        String.valueOf((int)Minecraft.getInstance().player.getY()), false, 0, 0, 27, RenderUtils.fontHeight()+4
                ), 0, 0, 3, 0).setWigetRender(widget->{
                    RenderUtils.drawRightString(widget.graphics, "Y:", widget.widget.getX()-1, widget.widget.getY()+2, -1, true);
                    return RenderUtils.width("Y: ") + 2;
                }, null).cast();
                this.z = this.addWiget(new GuiFilterTextField(
                        String.valueOf((int)Minecraft.getInstance().player.getZ()), false, 0, 0, 27, RenderUtils.fontHeight()+4
                ), 0, 0, 3, 0).setWigetRender(widget->{
                    RenderUtils.drawRightString(widget.graphics, "Z:", widget.widget.getX()-1, widget.widget.getY()+2, -1, true);
                    return RenderUtils.width("Z: ") + 2;
                }, null).cast();

                this.addWiget(Button.builder(Component.literal("X"), b->
                                onRemove.run()
                        ).size(RenderUtils.fontHeight(), RenderUtils.fontHeight()).build(), 0, 0, 10, 0)
                        .setConsumerEvent(false, true, false, false);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ChoiceMapModScreen extends Screen {
        static final Map<String, Consumer<Waypoint>> REGISTRY = new HashMap<>();

        /**
         * @return previous mod waypoint generator
         */
        public static Consumer<Waypoint> registerModGenWaypoint(String modId, Consumer<Waypoint> generator) {
           return REGISTRY.put(modId, generator);
        }

        public final Screen parent;
        public final ArrayList<Waypoint> waypoints = new ArrayList<>();
        public MapModList mapModList;
        public ChoiceMapModScreen(Screen parent, List<Waypoint> waypoints, boolean showMapModList) {
            super(CommonComponents.EMPTY);
            this.parent = parent;
            this.waypoints.addAll(waypoints);
            this.check = showMapModList;
        }

        @Override
        public void init() {
            Window window = Minecraft.getInstance().getWindow();

            this.mapModList = this.addRenderableWidget(new MapModList(this.waypoints::size,
                    window.getGuiScaledWidth() / 2 - 100,
                    75,
                    200, 100));

            for (String modId : REGISTRY.keySet()) {
                if (ModList.get().isLoaded(modId)) {
                    ModList.get().getModContainerById(modId).ifPresent(container ->
                        this.mapModList.add(Button.builder(Component.literal(container.getModInfo().getDisplayName()), b-> {
                            for (Waypoint waypoint : this.waypoints) {
                                REGISTRY.get(modId).accept(waypoint);
                            }
                            if (this.parent instanceof GuiInbox) {
                                ((GuiInbox)this.parent).setTip(Component.translatable("info.inbox.generate.attachment.waypoints.recived"));
                            }else {
                                Minecraft.getInstance().player.sendSystemMessage(Component.translatable("info.inbox.generate.attachment.waypoints.recived"));
                            }
                            Minecraft.getInstance().setScreen(this.parent);
                        }).build())
                    );
                }
            }
            if (false) {
                for (int i = 0; i < 10; i++) {
                    int index = this.mapModList.children().size();
                    this.mapModList.add(Button.builder(Component.literal("Test_" + (i + 1)), b ->
                        Minecraft.getInstance().player.sendSystemMessage(Component.literal(String.format("Clicked: %s", index)))
                    ).build());
                }
            }
        }

        private boolean check;
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            this.renderBackground(graphics);

            RenderUtils.drawCenteredComponent(graphics, Arrays.asList(
                    Component.translatable("info.inbox.generate.attachment.waypoints.create_wait.0", this.waypoints.size()),
                    Component.translatable("info.inbox.generate.attachment.waypoints.create_wait.1")
            ), this.mapModList.getLeft() + this.mapModList.getWidth() / 2, this.mapModList.getTop() - RenderUtils.fontHeight() * 2 - 4, Color.WHITE.getRGB(), true);

            super.render(graphics, mouseX, mouseY, partialTick);

            if (!this.check) {
                this.check = true;
                if (this.waypoints.isEmpty()) {
                    Minecraft.getInstance().setScreen(this.parent);
                    return;
                }
                if (this.mapModList.children().size() == 1) {
                    this.mapModList.children().get(0).btn.onPress();
                }
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }

        public static class MapModList extends ObjectSelectionList<MapModList.MapMod> {
            public final Supplier<Integer> waypointCount;
            public MapModList(Supplier<Integer> waypointCount, int x, int y, int width, int height) {
                super(Minecraft.getInstance(), width, height, y, y+height, RenderUtils.fontHeight() + 10);
                this.setLeftPos(x);
                this.setRenderBackground(false);
                this.setRenderTopAndBottom(false);
                this.setRenderSelection(false);
                this.waypointCount = waypointCount;
            }
            public void add(Button b) {
                this.addEntry(new MapMod(b));
            }

            @Override
            public int getRowWidth() {
                return super.getRowWidth() - 20;
            }

            @Override
            protected int getScrollbarPosition() {
                return this.x0 + this.width - 5;
            }

            public static class MapMod extends ObjectSelectionList.Entry<MapMod> {
                public final Button btn;
                public MapMod(Button btn) {
                    this.btn = btn;
                }

                @Override
                public @NotNull Component getNarration() {
                    return CommonComponents.EMPTY;
                }

                @Override
                public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
                    this.btn.setX(pLeft-2);
                    this.btn.setY(pTop-2);
                    this.btn.setWidth(pWidth - 6);
                    this.btn.setHeight(pHeight+4);
                    this.btn.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
                }

                @Override
                public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
                    this.btn.onPress();
                    return true;
                }
            }
        }
    }
}
