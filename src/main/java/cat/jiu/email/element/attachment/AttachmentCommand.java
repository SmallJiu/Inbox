package cat.jiu.email.element.attachment;

import cat.jiu.core.api.handler.IJsonSerializable;
import cat.jiu.core.api.handler.INBTSerializable;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.NBTUtils;
import cat.jiu.core.util.client.RenderUtils;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.api.ParameterFunction;
import cat.jiu.email.event.AttachmentEvent;
import cat.jiu.email.util.EmailUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
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

    public AttachmentCommand(CompoundNBT tag) {
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
        if (other instanceof AttachmentCommand && !other.isEmpty()) {
            ((AttachmentCommand) other).getCommands().forEach(this::addCommand);
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
    public CompoundNBT write(CompoundNBT nbt) {
        if (!this.isEmpty()){
            ListNBT array = new ListNBT();
            for (Cmd cmd : this.commands) {
                array.add(cmd.write(new CompoundNBT()));
            }
            nbt.put("commands", array);
        }
        return nbt;
    }

    @Override
    public void read(CompoundNBT nbt) {
        if (nbt.contains("commands")) {
            for (INBT tag : nbt.getList("commands", 10)) {
                this.addCommand(Cmd.create((CompoundNBT) tag));
            }
        }
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public void accept(PlayerEntity player) {
        if (!this.isEmpty() && player.getServer()!=null) {
            for (Cmd cmd : this.commands) {
                String c = cmd.cmd();
                for (Map.Entry<String, ParameterFunction> entry : PARAMETERS_PARSER.entrySet()) {
                    c = entry.getValue().parser(entry.getKey(), c, player);
                }
                player.getServer().getCommandManager().handleCommand(cmd.performer().isServer() ? player.getServer().getCommandSource() : player.getCommandSource(), c);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(AttachmentEvent.Render event) {
        if (!this.isEmpty()) {

            RenderUtils.drawString(event.stack, "", event.x, event.getY(), Color.WHITE.getRGB(), true);

            int cmd_x = event.x + RenderUtils.width(event.renderSaveTo("info.inbox.commands")) + 2;
            event.renderItem(COMMAND_BLOCK, cmd_x, event.getY());
            if (event.canSee() && EmailUtils.isInRange(event.mouseX, event.mouseY, cmd_x, event.getY(), 16, 16)) {
                List<ITextComponent> cmdTooltip = new ArrayList<>();
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
                    cmdTooltip.add(new StringTextComponent((cmd.performer().isServer() ? TextFormatting.RED : TextFormatting.GREEN) + c));
                }
                if (hideCount > 0) {
                    cmdTooltip.add(new TranslationTextComponent("info.inbox.command_save_to_email.hide", hideCount));
                    if (EmailUtils.isOP(Minecraft.getInstance().player)) {
                        for (Cmd cmd : this.getCommands()) {
                            if (cmd.isHideInTooltip()) {
                                String c = cmd.cmd();
                                for (Map.Entry<String, ParameterFunction> entry : PARAMETERS_PARSER.entrySet()) {
                                    c = entry.getValue().parser(entry.getKey(), c, Minecraft.getInstance().player);
                                }
                                cmdTooltip.add(new StringTextComponent((cmd.performer().isServer() ? TextFormatting.DARK_RED : TextFormatting.DARK_GREEN) + c));
                            }
                        }
                    }
                }

                event.disableScissor();
                RenderUtils.drawComponentTooltip(event.stack, cmdTooltip, event.mouseX, event.mouseY);
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
        public CompoundNBT write(CompoundNBT data) {
            data.putString("cmd", this.cmd());
            data.putBoolean("serverCmd", this.performer().isServer());
            data.putBoolean("hide", this.isHideInTooltip());
            return data;
        }

        @Override
        public void read(CompoundNBT data) {
            this.setCommand(NBTUtils.get(data, "cmd", ""));
            this.setPerformer(Performer.get(NBTUtils.get(data, "serverCmd", false)));
            this.setHideInTooltip(NBTUtils.get(data, "hide", false));
        }

        public static Cmd create(CompoundNBT data) {
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
