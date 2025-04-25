package cat.jiu.email.element.attachment;

import cat.jiu.core.api.serializable.IJsonSerializable;
import cat.jiu.core.api.serializable.INBTSerializable;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.NBTUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.api.ParameterFunction;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.util.EmailUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class AttachmentCommand implements IAttachment {
    public static final ResourceLocation ID = new ResourceLocation(EmailMain.MODID, "attachment/command");
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
        this.readFrom(tag);
    }

    public AttachmentCommand(JsonObject json) {
        this.readFrom(json);
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
    public JsonObject write(JsonObject json) {
        if (!this.isEmpty()){
            JsonArray array = new JsonArray();
            for (Cmd cmd : this.commands) {
                array.add(cmd.write(new JsonObject()));
            }
            json.add("commands", array);
        }
        return json;
    }

    @Override
    public void read(JsonObject json) {
        boolean allServerCmd = JsonUtils.get(json, "serverCmd", false);
        if (json.has("commands")) {
            for (JsonElement element : json.getAsJsonArray("commands")) {
                if (element.isJsonObject()) {
                    JsonObject object = element.getAsJsonObject();
                    this.addCommand(object.get("cmd").getAsString(),
                            JsonUtils.get(object, "serverCmd", false) || allServerCmd,
                            JsonUtils.get(object, "hide", false));
                }else if (element.isJsonPrimitive()) {
                    this.addCommand(element.getAsString(), allServerCmd);
                }
            }
        }
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {
        if (!this.isEmpty()){
            ListTag array = new ListTag();
            for (Cmd cmd : this.commands) {
                array.add(cmd.write(new CompoundTag()));
            }
            nbt.put("commands", array);
        }
        return nbt;
    }

    @Override
    public void read(CompoundTag nbt) {
        if (nbt.contains("commands")) {
            for (Tag tag : nbt.getList("commands", 10)) {
                this.addCommand(Cmd.create((CompoundTag) tag));
            }
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
    public void render(AttachmentEvent.Render event) {
        if (!this.isEmpty()) {

            event.graphics.drawString(event.font, Component.nullToEmpty(null), event.x, event.getY(), Color.WHITE.getRGB());

            int cmd_x = event.x + event.font.width(event.renderSaveTo("info.inbox.commands")) + 2;
            event.graphics.renderFakeItem(COMMAND_BLOCK, cmd_x, event.getY());
            if (event.canSee() && EmailUtils.isInRange(event.mouseX, event.mouseY, cmd_x, event.getY(), 16, 16)) {
                event.disableScissor();

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
                event.graphics.renderComponentTooltip(event.font, cmdTooltip, event.mouseX, event.mouseY);

                event.enableScissor();
            }
            event.addY(16);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void getHeight(AttachmentEvent.GetHeight event) {
        if (!this.isEmpty()) {
            event.addHeight(16);
        }
    }

    public static class Cmd implements IJsonSerializable, INBTSerializable {
        protected String cmd;
        protected Performer performer;
        protected boolean hideInTooltip = false;

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
        public JsonObject write(JsonObject data) {
            data.addProperty("cmd", this.cmd());
            data.addProperty("serverCmd", this.performer().isServer());
            data.addProperty("hide", this.isHideInTooltip());
            return data;
        }

        @Override
        public void read(JsonObject data) {
            this.setCommand(JsonUtils.get(data, "cmd", ""));
            this.setPerformer(Performer.get(JsonUtils.get(data, "serverCmd", false)));
            this.setHideInTooltip(JsonUtils.get(data, "hide", false));
        }

        @Override
        public CompoundTag write(CompoundTag data) {
            data.putString("cmd", this.cmd());
            data.putBoolean("serverCmd", this.performer().isServer());
            data.putBoolean("hide", this.isHideInTooltip());
            return data;
        }

        @Override
        public void read(CompoundTag data) {
            this.setCommand(NBTUtils.get(data, "cmd", ""));
            this.setPerformer(Performer.get(NBTUtils.get(data, "serverCmd", false)));
            this.setHideInTooltip(NBTUtils.get(data, "hide", false));
        }

        public static Cmd create(CompoundTag data) {
            Cmd instance = new Cmd("", false);
            instance.read(data);
            return instance;
        }
        public static Cmd create(JsonObject data) {
            Cmd instance = new Cmd("", false);
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
