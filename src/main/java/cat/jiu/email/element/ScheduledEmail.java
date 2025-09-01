package cat.jiu.email.element;

import cat.jiu.core.api.serializable.ISerializable;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.NBTUtils;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.TimeMillis;
import cat.jiu.sql.SQLValues;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Mod.EventBusSubscriber
public class ScheduledEmail implements ISerializable {
    protected long id = System.currentTimeMillis();
    protected String emailFile;
    protected String note;
    protected TimeMillis interval;
    protected long nextExecuteTime;
    protected Email email;
    protected Addressee addressee;
    protected HashSet<String> custom_addressee;

    public String getFilePath() {
        return emailFile;
    }

    public ScheduledEmail setFilePath(String emailFile) {
        this.emailFile = emailFile;
        return this;
    }

    public TimeMillis getInterval() {
        return interval;
    }

    public ScheduledEmail setInterval(TimeMillis interval) {
        this.interval = interval;
        this.refreshNextExecuteTime();
        return this;
    }

    public String getNote() {
        return note;
    }

    public ScheduledEmail setNote(String note) {
        this.note = note;
        return this;
    }

    public Addressee getAddressee() {
        return addressee;
    }

    public ScheduledEmail setAddressee(Addressee addressee) {
        this.addressee = addressee;
        return this;
    }
    public ScheduledEmail addCustomAddressee(String name) {
        if (this.custom_addressee ==null) this.custom_addressee = new HashSet<>();
        this.custom_addressee.add(name);
        return this;
    }
    public Set<String> getCustomAddressee(){
        if (this.custom_addressee ==null) this.custom_addressee = new HashSet<>();
        return Collections.unmodifiableSet(this.custom_addressee);
    }

    public File getAsFile() {
        return new File(EmailAPI.getGlobalDataPath()+ "emails/" + this.getFilePath());
    }

    public Email getAsEmail() {
        if (this.email==null) {
            this.email = new Email(JsonUtils.parse(this.getAsFile(), EmailConfigServer.File_Charset.get()).getAsJsonObject());
        }
        return this.email.setCreateTimeToNow();
    }

    public long getId() {
        return id;
    }

    public boolean canSend() {
        return System.currentTimeMillis() >= this.nextExecuteTime && this.getAsEmail() != null;
    }

    public ScheduledEmail refreshNextExecuteTime() {
        this.nextExecuteTime = System.currentTimeMillis() + this.getInterval().millis;
        return this;
    }

    public long getNextExecuteTime() {
        return nextExecuteTime;
    }

    public ScheduledEmail changeTo(ScheduledEmail other) {
        this
                .setFilePath(other.getFilePath())
                .setInterval(other.getInterval()).refreshNextExecuteTime()
                .setAddressee(other.getAddressee())
                .setNote(other.getNote());

        this.custom_addressee = null;
        other.getCustomAddressee().forEach(this::addCustomAddressee);
        this.email = null;

        return this;
    }

    public void send(String addressee) {
        EmailAPI.sendEmail(EmailSenderGroup.SYSTEM, addressee, this.getAsEmail());
    }

    @Override
    public JsonObject write(JsonObject json) {
        json.addProperty("id", this.id);
        json.addProperty("path", this.getFilePath());
        json.addProperty("interval", this.getInterval().millis);
        json.addProperty("next", this.getNextExecuteTime());
        json.addProperty("note", this.getNote());
        json.addProperty("addressee", this.getAddressee().getName());
        if (this.getAddressee().isCustomPlayers()) {
            JsonArray array = new JsonArray();
            this.getCustomAddressee().forEach(array::add);
            json.add("custom_addressee", array);
        }
        return json;
    }

    @Override
    public void read(JsonObject data) {
        this.setFilePath(JsonUtils.get(data, "path", ""));
        this.setInterval(new TimeMillis(JsonUtils.get(data, "interval", 0)));
        this.id = JsonUtils.get(data, "id", 0);
        this.setNote(JsonUtils.get(data, "note", ""));
        this.setAddressee(Addressee.get(data.has("addressee") ? data.get("addressee").getAsString() : "online"));
        this.nextExecuteTime = JsonUtils.get(data, "next", -1);
        if (this.nextExecuteTime == -1) {
            this.refreshNextExecuteTime();
        }
        if (data.has("custom_addressee") && this.getAddressee().isCustomPlayers()) {
            data.getAsJsonArray("custom_addressee").forEach(e->this.addCustomAddressee(e.getAsString()));
        }
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {
        nbt.putLong("id", this.id);
        nbt.putString("path", this.getFilePath());
        nbt.putLong("interval", this.getInterval().millis);
        nbt.putLong("next", this.getNextExecuteTime());
        nbt.putString("note", this.getNote());
        nbt.put("email", this.getAsEmail().write(new CompoundTag()));
        nbt.putString("addressee", this.getAddressee().getName());
        if (this.getAddressee().isCustomPlayers()) {
            ListTag list = new ListTag();
            this.getCustomAddressee().forEach(e->list.add(StringTag.valueOf(e)));
            nbt.put("custom_addressee", list);
        }
        return nbt;
    }

    @Override
    public void read(CompoundTag nbt) {
        this.id = nbt.getLong("id");
        this.setFilePath(nbt.getString("path"));
        this.setInterval(new TimeMillis(nbt.getLong("interval")));
        this.setNote(nbt.getString("note"));
        this.nextExecuteTime = NBTUtils.get(nbt, "next", -1);
        if (this.nextExecuteTime == -1) {
            this.refreshNextExecuteTime();
        }
        this.email = new Email(nbt.getCompound("email"));
        this.setAddressee(Addressee.get(nbt.getString("addressee")));
        if (nbt.contains("custom_addressee") && this.getAddressee().isCustomPlayers()) {
            nbt.getList("custom_addressee", 8).forEach(e->this.addCustomAddressee(e.getAsString()));
        }
    }

    @Override
    public SQLValues write(SQLValues value) {return value;}
    @Override
    public void read(ResultSet result) throws SQLException {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduledEmail email = (ScheduledEmail) o;
        return this.id == email.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(emailFile, this.id);
    }

    private static final List<ScheduledEmail>
            scheduled_emails = new ArrayList<>(),
            unmodifiable = Collections.unmodifiableList(scheduled_emails);

    public static void addScheduledEmail(ScheduledEmail email, boolean saveToDisk) {
        scheduled_emails.add(email);
        if (saveToDisk) {
            save();
        }
    }
    public static boolean removeScheduledEmail(long id) {
        boolean res = scheduled_emails.removeIf(e->e.getId() == id);
        if (res) save();
        return res;
    }
    public static void removeAllScheduledEmail(){
        scheduled_emails.clear();
        save();
    }
    public static ScheduledEmail getScheduledEmail(long id) {
        for (ScheduledEmail email : scheduled_emails) {
            if (email.getId() == id) {
                return email;
            }
        }
        return null;
    }
    public static boolean hasScheduledEmail(ScheduledEmail email) {
        return scheduled_emails.contains(email);
    }

    public static List<ScheduledEmail> getScheduledEmails() {
        return unmodifiable;
    }

    public static void init() {
        scheduled_emails.clear();
        updata();
    }
    public static void updata() {
        try {
            File file = new File(EmailAPI.getGlobalDataPath() + "scheduled_emails.json");
            if (file.exists()) {
                JsonUtils.parse(file, EmailConfigServer.File_Charset.get()).getAsJsonArray().forEach(e->{
                    try {
                        ScheduledEmail email = new ScheduledEmail();
                        email.readFrom(e);
                        email.getAsEmail();
                        if (hasScheduledEmail(email)) {
                            getScheduledEmail(email.getId()).changeTo(email);
                        }else {
                            scheduled_emails.add(email.refreshNextExecuteTime());
                        }
                    }catch (Throwable t) {
                        t.printStackTrace();
                    }
                });
                save();
            }else {
                EmailMain.log.info("Not found file ' scheduled_emails.json ', path: {}", file);
            }
        }catch (Throwable ignored){

        }
    }

    public static void save() {
        JsonArray array = new JsonArray();
        for (ScheduledEmail email : scheduled_emails) {
            array.add(email.write(new JsonObject()));
        }
        JsonUtils.toJsonFile(EmailAPI.getGlobalDataPath() + "scheduled_emails.json", array, false, EmailConfigServer.File_Charset.get());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!scheduled_emails.isEmpty()) {
            for (int i = 0; i < scheduled_emails.size(); i++) {
                ScheduledEmail email = scheduled_emails.get(i);
                if (email.getAsEmail()==null) {
                    removeScheduledEmail(email.getId());
                    continue;
                }

                if (email.canSend()) {
                    email.refreshNextExecuteTime();

                    switch (email.getAddressee()) {
                        case CUSTOM -> {
                            for (String name : email.getCustomAddressee()) {
                                Player player = event.getServer().getPlayerList().getPlayerByName(name);
                                if (player==null) {
                                    player = event.getServer().getPlayerList().getPlayer(EmailUtils.getTrueUUID(name));
                                }
                                if (player != null) {
                                    email.send(player.getStringUUID());
                                }
                            }
                        }
                        case ONLINE -> {
                            for (String name : event.getServer().getPlayerList().getPlayerNamesArray()) {
                                Player player = event.getServer().getPlayerList().getPlayerByName(name);
                                if (player != null) {
                                    email.send(player.getStringUUID());
                                }
                            }
                        }
                        case ARCHIVE -> {
                            EmailUtils.initNameAndUUID(event.getServer());
                            for (UUID uuid : EmailUtils.getAllUUID()) {
                                email.send(String.valueOf(uuid));
                            }
                        }
                    }
                }
            }
        }
    }

    public static enum Addressee {
        ONLINE, ARCHIVE, CUSTOM;
        public boolean isInArchivePlayers(){
            return this == ARCHIVE;
        }
        public boolean isAllOnlinePlayers() {
            return this == ONLINE;
        }
        public boolean isCustomPlayers() {
            return this == CUSTOM;
        }
        public String getName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
        public static Addressee get(String s) {
            if ("archive".equalsIgnoreCase(s)) {
                return ARCHIVE;
            }
            if ("custom".equalsIgnoreCase(s)) {
                return CUSTOM;
            }
            return ONLINE;
        }
        public static Addressee get(int index) {
            if (index==0) return ONLINE;
            if (index==1) return ARCHIVE;
            return CUSTOM;
        }
    }
}
