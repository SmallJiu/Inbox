package cat.jiu.email.element;

import cat.jiu.core.api.IData;
import cat.jiu.core.api.serializable.IDataSerializable;
import cat.jiu.core.api.serializable.ISerializable;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.NBTUtils;
import cat.jiu.core.util.element.data.JsonData;
import cat.jiu.core.util.element.data.NBTData;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.SendDevEmail;
import cat.jiu.email.util.TimeMillis;
import cat.jiu.sql.SQLValues;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
public class ScheduledEmail implements IDataSerializable<IData.IMapData<?>> {
    protected long id = System.currentTimeMillis();
    protected String emailFile;
    protected String note;
    protected TimeMillis interval;
    protected long nextExecuteTime, lastExecuteTime;
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
        this.refreshNextExecuteTimeFromNewInterval();
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
        return this.custom_addressee;
    }

    public File getAsFile() {
        return new File(EmailAPI.getGlobalDataPath()+ "emails/" + this.getFilePath());
    }

    public Email getAsEmail() {
        if (this.email==null) {
            if ("default".equalsIgnoreCase(this.emailFile)) {
                this.email = SendDevEmail.getDevEmail();
            }else {
                JsonElement element = JsonUtils.parse(this.getAsFile(), EmailConfigServer.File_Charset.get());
                if (element != null) {
                    this.email = new Email(element.getAsJsonObject());
                }else {
                    return Email.EMPTY;
                }
            }
        }
        return this.email.setCreateTimeToNow();
    }

    public long getId() {
        return id;
    }

    public ScheduledEmail setId(long id) {
        this.id = id;
        return this;
    }

    public boolean canSend() {
        return System.currentTimeMillis() >= this.nextExecuteTime
                && this.getAsEmail() != null
                && this.getAsEmail() != Email.EMPTY;
    }

    public ScheduledEmail refreshNextExecuteTimeFromNewInterval() {
        this.nextExecuteTime = this.lastExecuteTime + this.getInterval().millis;
        return this;
    }
    public ScheduledEmail refreshNextExecuteTime() {
        long time = System.currentTimeMillis();
        this.lastExecuteTime = time;
        this.nextExecuteTime = time + this.getInterval().millis;
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
    public IData.IMapData<?> write(IData.IMapData<?> data) {
        data.putData("id", this.id);
        data.putData("path", this.getFilePath());
        data.putData("interval", this.getInterval().millis);
        data.putData("next", this.getNextExecuteTime());
        data.putData("last", this.lastExecuteTime);
        data.putData("note", this.getNote());
        data.putData("addressee", this.getAddressee().getName());
        if (this.getAddressee().isCustomPlayers()) {
            IData.IListData<?> list = data.newList();
            this.getCustomAddressee().forEach(list::putData);
            data.putData("custom_addressee", list);
        }
        return data;
    }

    @Override
    public void read(IData.IMapData<?> data) {
        this.id = data.getLong("id", System.currentTimeMillis());
        this.setFilePath(data.getString("path", ""));
        this.setInterval(new TimeMillis(data.getLong("interval", 0)));
        this.setNote(data.getString("note", ""));
        this.setAddressee(Addressee.get(data.getString("addressee", "online")));
        this.nextExecuteTime = data.getLong("next", -1);
        this.lastExecuteTime = data.getLong("last", -1);
        if (this.nextExecuteTime == -1) {
            this.refreshNextExecuteTime();
        }
        if (data.containsKey("custom_addressee") && this.getAddressee().isCustomPlayers()) {
            data.getList("custom_addressee", String.class).foreach((i,data1)->this.addCustomAddressee(data1.getAsPrimitive().getAsString()));
        }
    }

    public JsonObject write(JsonObject json) {
        this.write(JsonData.map(json));
        return json;
    }

    public void read(JsonObject data) {
        this.read(JsonData.map(data));
    }

    public CompoundTag write(CompoundTag nbt) {
        this.write(NBTData.map(nbt));
        return nbt;
    }

    public void read(CompoundTag nbt) {
        this.read(NBTData.map(nbt));
    }

    public SQLValues write(SQLValues value) {return value;}
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

    private static final Map<Long, ScheduledEmail>
            scheduled_emails = new HashMap<>(),
            unmodifiable = Collections.unmodifiableMap(scheduled_emails);

    public static void addScheduledEmail(ScheduledEmail email, boolean saveToDisk) {
        scheduled_emails.put(email.id, email);
        if (saveToDisk) {
            save();
        }
    }
    public static boolean removeScheduledEmail(long id) {
        boolean res = scheduled_emails.remove(id) != null;
        if (res) save();
        return res;
    }
    public static void removeAllScheduledEmail(){
        scheduled_emails.clear();
        save();
    }
    public static ScheduledEmail getScheduledEmail(long id) {
        return scheduled_emails.get(id);
    }
    public static boolean hasScheduledEmail(ScheduledEmail email) {
        return hasScheduledEmail(email.id);
    }
    public static boolean hasScheduledEmail(long id) {
        return scheduled_emails.containsKey(id);
    }

    public static Collection<ScheduledEmail> getScheduledEmails() {
        return unmodifiable.values();
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
                        email.read(JsonData.map(e.getAsJsonObject()));
                        email.getAsEmail();
//                        if (email.getAsEmail() != Email.EMPTY)
                        {
                            if (hasScheduledEmail(email)) {
                                getScheduledEmail(email.getId()).changeTo(email);
                            } else {
                                scheduled_emails.put(email.id, email.refreshNextExecuteTime());
                            }
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
        scheduled_emails.forEach((id, email) ->
            array.add(email.write(new JsonObject()))
        );
        JsonUtils.toJsonFile(EmailAPI.getGlobalDataPath() + "scheduled_emails.json", array, false, EmailConfigServer.File_Charset.get());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!scheduled_emails.isEmpty()) {
            for (Long id : scheduled_emails.keySet()) {
                ScheduledEmail email = scheduled_emails.get(id);
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
