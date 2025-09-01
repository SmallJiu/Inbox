package cat.jiu.email.element.attachment;

import cat.jiu.core.api.IData;
import cat.jiu.core.api.serializable.IDataSerializable;
import cat.jiu.core.util.Utils;
import cat.jiu.core.util.element.data.JsonData;
import cat.jiu.core.util.element.data.NBTData;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.api.ParameterFunction;
import cat.jiu.email.util.EmailUtils;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    protected List<Cmd> commands, unmodifiable;

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
        if (this.unmodifiable==null) {
            this.unmodifiable = Collections.unmodifiableList(this.commands);
        }
        return this.unmodifiable;
    }

    @Override
    public void merge(IAttachment other) {
        if (other instanceof AttachmentCommand attachment && !attachment.isEmpty()) {
            attachment.getCommands().forEach(this::addCommand);
        }
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
    public void accept(Player player) {
        if (!this.isEmpty() && player.getServer()!=null) {
            for (Cmd cmd : this.commands) {
                String c = cmd.cmd();
                for (Map.Entry<String, ParameterFunction> entry : PARAMETERS_PARSER.entrySet()) {
                    c = entry.getValue().parser(entry.getKey(), c, player);
                }
                player.getServer().getServerResources().managers().getCommands().performPrefixedCommand(cmd.performer().isServer() ? player.getServer().createCommandSourceStack() : player.createCommandSourceStack(), c);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public List<Component> getHoverMessage() {
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
}
