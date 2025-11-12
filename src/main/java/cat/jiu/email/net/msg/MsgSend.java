package cat.jiu.email.net.msg;

import cat.jiu.core.api.element.IText;
import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.SideProxy;
import cat.jiu.core.util.element.Text;
import cat.jiu.core.util.element.data.NBTData;
import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.EmailSenderGroup;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.event.EmailSendEvent;
import cat.jiu.email.ui.gui.GuiGenerateEmail;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.SizeReport;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class MsgSend extends BaseMessage {
    protected String addressed;
    protected EmailSenderGroup group;
    protected Email email;

    public MsgSend() {}
    public MsgSend(EmailSenderGroup group, String addressee, Email email) {
        this.group = group;
        this.addressed = addressee;
        this.email = email;
        this.email.setCreateTimeToNow();
    }

    @Override
    public void fromBytes(FriendlyByteBuf buf) {
        CompoundTag nbt = buf.readNbt();

        this.addressed = nbt.getString("addresser");
        this.group = EmailSenderGroup.getGroupByID(nbt.getInt("group"));
        this.email = new Email(nbt.getCompound("email"));
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        CompoundTag nbt = new CompoundTag();

        nbt.putString("addresser", this.addressed);
        nbt.putInt("group", EmailSenderGroup.getIDByGroup(this.group));
        nbt.put("email", (CompoundTag) this.email.write(NBTData.map()).getData());

        buf.writeNbt(nbt);
    }

    @Override
    public boolean handler(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            EmailUtils.initNameAndUUID(SideProxy.getServer());
            ServerPlayer sender = context.get().getSender();

            if(this.group.isPlayerSend() && this.addressed.equals(sender.getName().getString()) && !EmailConfigServer.Send.Enable_Send_To_Self.get()) {
                this.sendMessage(sender, "info.inbox.error.send_self");
                return;
            }

            if(!EmailUtils.hasName(this.addressed) && !("@a".equals(this.addressed) || "@p".equals(this.addressed))) {
                this.sendMessage(sender, "info.inbox.error.not_found_player", this.addressed);
                return;
            }
            if("@a".equals(this.addressed) || "@p".equals(this.addressed)) {
                if(sender!=null) {
                    if(EmailUtils.isOP(sender)) {
                        long time = System.currentTimeMillis();
                        int sended = 0;
                        if(this.group.isPlayerSend()) {
                            this.lockScreen(sender, true);
                        }
                        if("@a".equals(this.addressed)) {
                            for(UUID uid : EmailUtils.getAllUUID()) {
                                this.trySendEmail(sender, uid.toString(), false);
                                sended++;
                            }
                        }else if("@p".equals(this.addressed)) {
                            for(Player profile : sender.getServer().getPlayerList().getPlayers()) {
                                this.trySendEmail(sender, profile.getUUID().toString(), false);
                                sended++;
                            }
                        }
                        if(this.group.isPlayerSend()) {
                            this.lockScreen(sender, true);
                        }
                        this.sendMessage(sender, "info.inbox.send.abs", sended, System.currentTimeMillis() - time);
                    }else {
                        this.sendMessage(sender, "info.inbox.send.not_op.abs", this.addressed);
                    }
                }
            }else {
                this.trySendEmail(sender, this.addressed, true);
            }
        });
        return true;
    }

    private void trySendEmail(ServerPlayer sender, String addresses, boolean lock) {
        if(this.email.getExpirationTime()!=null && this.email.getExpirationTime().millis>0 && !EmailUtils.isOP(sender)) {
            this.sendMessage(sender, "info.inbox.send.not_op.expiration");
            return;
        }
        Inbox inbox = Inbox.get(addresses);
        if(this.group.isPlayerSend() && lock) {
            this.lockScreen(sender, true);
        }

        if(!EmailConfigServer.isInfiniteSize()) {
            SizeReport report = EmailUtils.checkEmailSize(this.email);
            if(!SizeReport.SUCCESS.equals(report)) {
                this.sendCheckSizeResult(sender, report, lock);
                if(this.group.isPlayerSend() && lock) {
                    this.lockScreen(sender, false);
                }
                return;
            }
        }
        if(this.group.isPlayerSend() && inbox.isSendCooling()){
            if(lock) this.lockScreen(sender, false);
            EmailMain.NETWORK.sendMessageToPlayer(new MsgSendRenderText(new Text("info.inbox.send.fail.cooling")), sender);
            return;
        }

        if(MinecraftForge.EVENT_BUS.post(new EmailSendEvent(TickEvent.Phase.START, this.group, addresses, this.email))) return;

        if(!inbox.isInSenderBlacklist(sender.getName().getString())) {
            if (this.email.hasAttachments()){
                BiConsumer<String, Object[]> messageHandler = (key, args)-> this.sendMessage(sender, key, args);
                for (IAttachment attachment : this.email.getAttachments()) {
                    if (!attachment.onPlayerSendCheck(sender, messageHandler)) {
                        if(this.group.isPlayerSend() && lock) {
                            this.lockScreen(sender, false);
                        }
                        return;
                    }
                }
                this.email.getAttachments().forEach(attachment -> attachment.onPlayerSendChecked(sender));
            }

            boolean result = inbox.addEmail(this.email, true);
            if (result) {
                this.sendSuccessMessage(inbox, addresses, sender, lock);
                this.sendLog(this.email.getSender().getText(), addresses, EmailUtils.getUUID(addresses));
            } else {
                this.sendFailMessage(sender, lock);
            }
        }else {
            this.sendIsBlackMessage(sender, lock);
        }

        MinecraftForge.EVENT_BUS.post(new EmailSendEvent(TickEvent.Phase.END, this.group, addresses, this.email));
        if(this.group.isPlayerSend() && lock) {
            this.lockScreen(sender, false);
        }
    }

    private void lockScreen(ServerPlayer player, boolean lock) {
        EmailMain.NETWORK.sendMessageToPlayer(new MsgLock(lock), player);
    }

    private void sendCheckSizeResult(ServerPlayer msgSender, SizeReport report, boolean lock) {
        if(msgSender!=null) {
            if(this.group.isPlayerSend()) {
                if(lock) this.lockScreen(msgSender, false);
                this.sendMessage(msgSender, "info.inbox.send.to_big", report.slot(), report.size());
            }else {
                IText.create(msgSender, ChatFormatting.RED, "info.inbox.error.send.to_big", report.slot(), report.size()).send(msgSender);
            }
        }else {
            EmailMain.log.warn("Email item is to big, please remove some item or nbt. Slot: {}, Size: {} / 2097152 Bytes", report.slot(), report.size());
        }
    }

    private void sendSuccessMessage(Inbox inbox, String addresser, ServerPlayer msgSender, boolean lock){
        if(this.group.isSystemSend()) return;
        try {
            addresser = EmailUtils.getName(UUID.fromString(addresser));
        }catch(Exception ignored) {}

        if(!this.group.isSystemSend() && msgSender!=null) {
            if(this.group.isPlayerSend()) {
                if(lock) this.lockScreen(msgSender, false);
                if(EmailConfigServer.Send.Enable_Send_Cooling.get()) {
                    inbox.setSendCoolingToNow();
                    EmailMain.NETWORK.sendMessageToPlayer(new MsgSendCooling(inbox.getSendCooling()), msgSender);
                    EmailUtils.saveInboxToDisk(inbox);
                }
            }
            this.sendMessage(msgSender, "info.inbox.send.success", addresser);
            Player player = getOnlinePlayer(addressed, msgSender.getServer());
            if(player != null) {
                IText.create(player, "info.inbox.from", this.email.getSender());
            }
        }
    }

    private void sendFailMessage(ServerPlayer msgSender, boolean lock) {
        if(!this.group.isSystemSend() && msgSender!=null) {
            if(this.group.isPlayerSend() && lock) {
                this.lockScreen(msgSender, false);
            }
            this.sendMessage(msgSender, "info.inbox.send.fail", lock);
        }else {
            EmailMain.log.info("Send email fail, check log find the reason.");
        }
    }

    private void sendIsBlackMessage(ServerPlayer msgSender, boolean lock) {
        if(msgSender!=null) {
            if(this.group.isPlayerSend()) {
                if(lock) this.lockScreen(msgSender, false);
                this.sendMessage(msgSender, "info.inbox.send.fail.blacklist");
            }else {
                IText.create(msgSender, ChatFormatting.RED, "info.inbox.send.fail.blacklist").send(msgSender);
            }
        }else {
            EmailMain.log.info("Send email fail, you have been block by the Addressee!");
        }
    }

    private void sendMessage(ServerPlayer sender, String msg, Object... arg) {
        if(this.group.isPlayerSend()) {
            EmailMain.NETWORK.sendMessageToPlayer(new MsgSendRenderText(new Text(msg, arg)), sender);
        } else if (sender != null) {
            IText.create(sender, ChatFormatting.RED, msg, arg).send(sender);
        } else {
            EmailMain.log.info(msg.replace("%s", "{}"), arg);
        }
    }

    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    private void sendLog(String sender, String name, UUID uid) {
        try {
            uid = UUID.fromString(name);
            name = EmailUtils.getName(uid);
        }catch(Exception ignored) {}

        File filepath = new File("./logs/email.log");
        JsonObject log;
        try {
            log = JsonUtils.parseThrow(filepath);
        }catch (Exception ignored) {
            log = new JsonObject();
        }

        StringBuilder msg = new StringBuilder()
                .append(sender)
                .append(" send a email to Player, name: ")
                .append(name)
                .append(", uuid: ")
                .append(uid==null ? addressed : uid.toString());

        log.addProperty(String.format("[%s]", dateFormat.format(new Date())), msg.toString());

        if(!this.group.isSystemSend()) EmailMain.log.info(msg);
        JsonUtils.toJsonFile(filepath, log, true);
    }

    /**
     * @param s name or uuid
     */
    public static Player getOnlinePlayer(String s, MinecraftServer server) {
        Player player = null;
        if(server!=null && s!=null & !s.isEmpty()) {
            PlayerList playerList = server.getPlayerList();
            try {
                player = playerList.getPlayer(UUID.fromString(s));
            }catch(Exception e) {
                player = playerList.getPlayerByName(s);
            }
        }
        return player;
    }

    public static class MsgLock extends BaseMessage {
        protected boolean locked;

        public MsgLock() {}
        public MsgLock(boolean locked) {
            this.locked = locked;
        }

        @Override
        public void toBytes(FriendlyByteBuf buf) {
            buf.writeBoolean(this.locked);
        }

        @Override
        public void fromBytes(FriendlyByteBuf buf) {
            this.locked = buf.readBoolean();
        }

        @Override
        public boolean handler(Supplier<NetworkEvent.Context> context) {
            if (Minecraft.getInstance().screen instanceof GuiGenerateEmail) {
                ((GuiGenerateEmail) Minecraft.getInstance().screen).setLocked(this.locked);
            }
            return false;
        }
    }
}
