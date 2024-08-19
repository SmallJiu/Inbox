package cat.jiu.email.element.attachment;

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
                JsonObject object = new JsonObject();
                object.addProperty("cmd", cmd.cmd());
                object.addProperty("serverCmd", cmd.performer().isServer());
                object.addProperty("hide", cmd.isHideInTooltip());
                array.add(object);
            }
            json.add("commands", array);
        }
        return json;
    }

    @Override
    public void read(JsonObject json) {
        boolean allServerCmd = json.has("serverCmd") && json.get("serverCmd").getAsBoolean();
        if (json.has("commands")) {
            for (JsonElement element : json.getAsJsonArray("commands")) {
                if (element.isJsonObject()) {
                    JsonObject object = element.getAsJsonObject();
                    this.addCommand(object.get("cmd").getAsString(),
                            (object.has("serverCmd") && object.get("serverCmd").getAsBoolean()) || allServerCmd,
                            object.has("hide") && object.get("hide").getAsBoolean());
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
                CompoundTag tag = new CompoundTag();
                tag.putString("cmd", cmd.cmd());
                tag.putBoolean("serverCmd", cmd.performer().isServer());
                tag.putBoolean("hide", cmd.isHideInTooltip());
                array.add(tag);
            }
            nbt.put("commands", array);
        }
        return nbt;
    }

    @Override
    public void read(CompoundTag nbt) {
        if (nbt.contains("commands")) {
            for (Tag tag : nbt.getList("commands", 10)) {
                CompoundTag compound = (CompoundTag) tag;
                this.addCommand(compound.getString("cmd"), compound.getBoolean("serverCmd"), compound.getBoolean("hide"));
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
    @SubscribeEvent
    public static void onAttachmentRender(AttachmentEvent.Render.Pre event) {
        if (event.attachment instanceof AttachmentCommand attachment && !attachment.isEmpty()) {

            event.graphics.drawString(event.font, Component.nullToEmpty(null), event.x, event.getY(), Color.WHITE.getRGB());

            Component info = Component.translatable("info.email.command_save_to_email").append(" (").append(Component.translatable(event.email.isReceived() ? "info.email.filter.is_accept" : "info.email.filter.not_accept")).append(")").append(": ");
            event.graphics.drawString(event.font, info, event.x, event.getY()+4, Color.WHITE.getRGB());

            int cmd_x = event.x + event.font.width(info) + 2;
            event.graphics.renderFakeItem(COMMAND_BLOCK, cmd_x, event.getY());
            if (event.canSee() && EmailUtils.isInRange(event.mouseX, event.mouseY, cmd_x, event.getY(), 16, 16)) {
                event.disableScissor();

                List<Component> cmdTooltip = new ArrayList<>();
                int hideCount = 0;
                for (Cmd cmd : attachment.getCommands()) {
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
                    cmdTooltip.add(Component.translatable("info.email.command_save_to_email.hide", hideCount));
                }
                event.graphics.renderComponentTooltip(event.font, cmdTooltip, event.mouseX, event.mouseY);

                event.enableScissor();
            }
            event.addY(16);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onAttachmentGetHeight(AttachmentEvent.GetHeight event) {
        if (event.attachment instanceof AttachmentCommand attachment && !attachment.isEmpty()) {
            event.addHeight(16);
        }
    }

    public static class Cmd {
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
