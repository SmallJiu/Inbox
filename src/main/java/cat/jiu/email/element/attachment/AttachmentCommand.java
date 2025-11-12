package cat.jiu.email.element.attachment;

import cat.jiu.core.api.IData;
import cat.jiu.core.api.serializable.IDataSerializable;
import cat.jiu.core.util.Utils;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.core.util.element.data.JsonData;
import cat.jiu.core.util.element.data.NBTData;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.AttachmentSendScreenWidget;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.api.ParameterFunction;
import cat.jiu.email.ui.gui.component.GuiCheckbox;
import cat.jiu.email.ui.gui.component.GuiFilterTextField;
import cat.jiu.email.util.EmailUtils;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class AttachmentCommand implements IAttachment {
    public static final ResourceLocation ID = Utils.location(EmailMain.MODID, "attachment/command");
    protected static final ItemStack COMMAND_BLOCK = new ItemStack(Items.COMMAND_BLOCK);
    public static final String PARAMETER_START = "{*", PARAMETER_END = "*}";
    private static final Map<String, ParameterFunction> PARAMETERS_PARSER = new ConcurrentHashMap<>();
    public static void registerParameterParser(String key, boolean addStartEnd, ParameterFunction parser) {
        if (addStartEnd) {
            key = PARAMETER_START + key + PARAMETER_END;
        }
        if (!PARAMETERS_PARSER.containsKey(key)) {
            PARAMETERS_PARSER.put(key, parser);
        }
    }

    protected List<Cmd> commands;

    public AttachmentCommand() {}

    public AttachmentCommand(CompoundTag tag) {
        this.read(tag);
    }

    public AttachmentCommand(JsonObject json) {
        this.read(json);
    }
    public AttachmentCommand(IData.IMapData<?> data) {
        this.read(data);
    }

    public AttachmentCommand addCommand(String cmd) {
        return this.addCommand(cmd, Performer.SERVER);
    }
    public AttachmentCommand addCommand(String cmd, boolean isServerCommand) {
        return this.addCommand(cmd, isServerCommand, false);
    }
    public AttachmentCommand addCommand(String cmd, boolean isServerCommand, boolean hideInTooltip) {
        return this.addCommand(cmd, Performer.get(isServerCommand), hideInTooltip);
    }
    public AttachmentCommand addCommand(String cmd, Performer performer) {
        return this.addCommand(cmd, performer, false);
    }
    public AttachmentCommand addCommand(String cmd, Performer performer, boolean hideInTooltip) {
        if (this.commands ==null) {
            this.commands =new ArrayList<>();
        }
        this.commands.add(new Cmd(cmd, performer).setHideInTooltip(hideInTooltip));
        return this;
    }
    public AttachmentCommand addCommand(Cmd cmd) {
        if (this.commands ==null) {
            this.commands =new ArrayList<>();
        }
        this.commands.add(cmd);
        return this;
    }
    public AttachmentCommand.Cmd setCommand(int index, Cmd cmd) {
        if (this.isEmpty()) {
            this.addCommand(cmd);
            return null;
        }else {
            return this.commands.set(index, cmd);
        }
    }
    public Cmd removeCommand(int slot) {
        return this.isEmpty() ? null : this.commands.remove(slot);
    }
    public AttachmentCommand removeAllCommand(){
        if (!this.isEmpty()) {
            this.commands.clear();
        }
        return this;
    }

    public Cmd getCommand(int index) {
        return this.commands != null ? this.getCommands().get(index) : null;
    }

    public List<Cmd> getCommands() {
        return this.isEmpty() ? Collections.emptyList() : this.commands;
    }

    @Override
    public boolean isEmpty() {
        return this.commands ==null || this.commands.isEmpty();
    }

    @Override
    public IData.IMapData<?> write(IData.IMapData<?> data) {
        if (!this.isEmpty()){
            IData.IListData<?> array = data.newList();
            for (Cmd cmd : this.commands) {
                array.putData(cmd.write(data.newMap()));
            }
            data.putData("commands", array);
        }
        return data;
    }

    @Override
    public void read(IData.IMapData<?> data) {
        boolean allServerCmd = data.getBoolean("serverCmd", false);
        if (data.containsKey("commands")) {
            data.getList("commands", IData.IMapData.class).foreach((index, value)->{
                if (value.isMap()) {
                    IData.IMapData<?> object = value.getAsMap();
                    this.addCommand(object.getString("cmd"),
                            object.getBoolean("serverCmd", false) || allServerCmd,
                            object.getBoolean("hide", false));
                }else if (value.isPrimitive()) {
                    this.addCommand(value.getAsPrimitive().getAsString(), allServerCmd);
                }
            });
        }
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentCommand attachment && !attachment.isEmpty()) {
            attachment.getCommands().forEach(this::addCommand);
        }
    }
    @Override
    public void accept(Player player) {
        if (!this.isEmpty() && player.getServer()!=null) {
            for (Cmd cmd : this.commands) {
                cmd.execute(player, player.getServer());
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public List<Component> getHoverMessage() {
        if (this.isEmpty()) return Collections.emptyList();

        List<Component> cmdTooltip = new ArrayList<>();
        int hideCount = 0;
        for (Cmd cmd : this.getCommands()) {
            if (cmd.isHideInTooltip()) {
                hideCount++;
                continue;
            }
            String c = cmd.cmd();
            for (Map.Entry<String, ParameterFunction> entry : PARAMETERS_PARSER.entrySet()) {
                c = entry.getValue().parser(entry.getKey(), c, Minecraft.getInstance().player);
            }
            cmdTooltip.add(Component.literal((cmd.performer().isServer() ? ChatFormatting.RED : ChatFormatting.GREEN) + c));
        }
        if (hideCount > 0) {
            cmdTooltip.add(Component.translatable("info.inbox.command_save_to_email.hide", hideCount));
            if (EmailUtils.isOP(Minecraft.getInstance().player)) {
                for (Cmd cmd : this.getCommands()) {
                    if (cmd.isHideInTooltip()) {
                        String c = cmd.cmd();
                        for (Map.Entry<String, ParameterFunction> entry : PARAMETERS_PARSER.entrySet()) {
                            c = entry.getValue().parser(entry.getKey(), c, Minecraft.getInstance().player);
                        }
                        cmdTooltip.add(Component.literal((cmd.performer().isServer() ? ChatFormatting.DARK_RED : ChatFormatting.DARK_GREEN) + c));
                    }
                }
            }
        }
        return cmdTooltip;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("info.inbox.commands");
    }

    @Override
    public String getName() {
        return "info.inbox.commands";
    }

    @Override
    public ItemStack getDisplayStack() {
        return COMMAND_BLOCK;
    }

    public static class Cmd implements IDataSerializable<IData.IMapData<?>> {
        protected String cmd;
        protected Performer performer;
        protected boolean hideInTooltip = false;

        public Cmd() {
        }
        public Cmd(String cmd, boolean isServer) {
            this(cmd, Performer.get(isServer));
        }
        public Cmd(String cmd, Performer performer) {
            this.cmd = cmd;
            this.performer = performer;
        }

        public boolean isHideInTooltip() {
            return hideInTooltip;
        }

        public Cmd setHideInTooltip(boolean hideInTooltip) {
            this.hideInTooltip = hideInTooltip;
            return this;
        }

        public String cmd() {
            return cmd;
        }

        public Cmd setCommand(String cmd) {
            this.cmd = cmd;
            return this;
        }

        public Performer performer() {
            return performer;
        }

        public Cmd setPerformer(Performer performer) {
            this.performer = performer;
            return this;
        }

        public int execute(Player player, MinecraftServer server) {
            String c = this.cmd();
            for (Map.Entry<String, ParameterFunction> entry : PARAMETERS_PARSER.entrySet()) {
                c = entry.getValue().parser(entry.getKey(), c, player);
            }
            return server.getCommands().performPrefixedCommand(this.performer().isServer() ? server.createCommandSourceStack() : player.createCommandSourceStack(), c);
        }

        @Override
        public IData.IMapData<?> write(IData.IMapData<?> data) {
            data.putData("cmd", this.cmd());
            data.putData("serverCmd", this.performer().isServer());
            data.putData("hide", this.isHideInTooltip());
            return data;
        }

        @Override
        public void read(IData.IMapData<?> data) {
            this.setCommand(data.getString("cmd", ""));
            this.setPerformer(Performer.get(data.getBoolean("serverCmd", false)));
            this.setHideInTooltip(data.getBoolean("hide", false));
        }

        public static Cmd create(CompoundTag data) {
            Cmd instance = new Cmd();
            instance.read(NBTData.map(data));
            return instance;
        }
        public static Cmd create(JsonObject data) {
            Cmd instance = new Cmd();
            instance.read(JsonData.map(data));
            return instance;
        }
        public static Cmd create(IData.IMapData<?> data) {
            Cmd instance = new Cmd();
            instance.read(data);
            return instance;
        }
    }
    public enum Performer {
        SERVER, PLAYER;
        public boolean isServer(){
            return this == SERVER;
        }
        public static Performer get(boolean isServer) {
            return isServer ? SERVER : PLAYER;
        }
    }
    @OnlyIn(Dist.CLIENT)
    public static class Widget extends AttachmentSendScreenWidget {
        public static final WidgetEntry INSTANCE = new WidgetEntry(ID, false, Widget::new);

        public final SubWidget commands;
        public final Button add;
        private Widget() {
            super(Component.translatable("info.inbox.commands"));
            this.initSubWiget(false);
            this.commands = this.addSubWidget(
                    new SubWidget(false),
                    1, 4, 0, 0
            ).cast();

            this.add = this.addSubWidget(Button.builder(Component.literal("++++++  ").append(Component.translatable("info.inbox.commands")).append("  ++++++"), b->{
                                UUID uuid = UUID.randomUUID();
                                this.commands.addWiget(new CommandBox(uuid, ()->
                                    this.commands.widgets.removeIf(wiget -> {
                                        if (wiget.widget instanceof CommandBox) {
                                            return ((CommandBox) wiget.widget).uuid.equals(uuid);
                                        }
                                        return false;
                                    })
                                ),0, 3, 0, 0);
                    })
                            .size(350, 15)
                    .build(),0, 0, 0, 0)
                    .setConsumerEvent(false, true, false, false).cast();
        }

        @Override
        public IAttachment newAttachmentInstance() {
            AttachmentCommand attachment = new AttachmentCommand();
            for (SubWidget.PositionWiget widget : this.commands.widgets) {
                if (widget.widget instanceof CommandBox) {
                    CommandBox box = (CommandBox) widget.widget;
                    attachment.addCommand(box.command.getValue(), box.performer.selected(), box.hide.selected());
                }
            }
            return attachment;
        }

        public static class CommandBox extends SubWidget {
            public final UUID uuid;
            public final EditBox command;
            public final Checkbox performer, hide;
            public CommandBox(UUID uid, Runnable delete) {
                super(true, CommonComponents.EMPTY);
                this.uuid = uid;
                this.command = this.addWiget(
                        new GuiFilterTextField("/", 0, 0, 250, RenderUtils.fontHeight() + 4),
                        0, 0, 0, 2
                ).cast();

                this.performer = this.addWiget(
                        new GuiCheckbox(0, 0, RenderUtils.fontHeight(), RenderUtils.fontHeight(), Component.translatable("info.inbox.generate.attachment.commands.op_permission"), false, null).messageToTooltip(),
                        1, 0, 0, 2
                ).setConsumerEvent(false, true, false, false).cast();

                this.hide = this.addWiget(
                        new GuiCheckbox(0, 0, RenderUtils.fontHeight(), RenderUtils.fontHeight(), Component.translatable("info.inbox.generate.attachment.commands.hide_on_tooltip"), false, null).messageToTooltip(),
                        1, 0, 0, 2
                ).setConsumerEvent(false, true, false, false).cast();

                this.addWiget(Button.builder(Component.literal("X"), b0->
                                delete.run()
                        )
                                .size(RenderUtils.fontHeight(), RenderUtils.fontHeight())
                                .build(), 0, 0, 10, 0
                        ).setConsumerEvent(false, true, false, false);
            }

            @Override
            public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
                if (this.command.isFocused()) {
                    super.keyPressed(pKeyCode, pScanCode, pModifiers);
                    return true;
                }
                return super.keyPressed(pKeyCode, pScanCode, pModifiers);
            }
        }
    }
}
