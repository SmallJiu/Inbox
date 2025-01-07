package cat.jiu.email.element;

import cat.jiu.core.api.handler.IJsonSerializable;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.util.JsonParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

import java.util.*;
import java.util.function.Supplier;

public class EventEmail implements IJsonSerializable, Supplier<String> {
    private static final Set<ResourceLocation> REGISTRY_EVENT = new HashSet<>();
    static final HashMap<ResourceLocation, ArrayList<EventEmail>> REGISTRY = new HashMap<>();
    public static final ResourceLocation
            Logged = registerEvent(EmailMain.MODID, "email/event/mc/logged")
    ;





    public static ResourceLocation registerEvent(String id, String name) {
        return registerEvent(new ResourceLocation(id, name));
    }
    public static ResourceLocation registerEvent(ResourceLocation event) {
        REGISTRY_EVENT.add(event);
        return event;
    }
    public static Set<ResourceLocation> getRegistryEvents() {
        return REGISTRY_EVENT;
    }
    public static void register(ResourceLocation event, String email) {
        register(event, new EventEmail(email));
    }
    public static void register(ResourceLocation event, EventEmail email) {
        if (!REGISTRY.containsKey(event)) {
            REGISTRY.put(event, new ArrayList<>());
        }
        REGISTRY.get(event).add(email);
        save();
    }
    public static List<EventEmail> getEventEmails(ResourceLocation event) {
        if (REGISTRY.containsKey(event)) {
            return REGISTRY.get(event);
        }
        return Collections.emptyList();
    }
    public static boolean hasEventEmails(ResourceLocation event) {
        return REGISTRY.containsKey(event) && REGISTRY.get(event)!=null && !REGISTRY.get(event).isEmpty();
    }
    public static void sendEventEmails(ResourceLocation event, String player) {
        if (hasEventEmails(event)) {
            for (EventEmail email : getEventEmails(event)) {
                email.send(player);
            }
        }
    }
    public static void load() {
        REGISTRY.clear();
        JsonObject emails = JsonParser.parse(EmailAPI.getGlobalDataPath()+"event_emails.json");
        if (emails != null) {
            for (Map.Entry<String, JsonElement> s : emails.entrySet()) {
                ResourceLocation event = new ResourceLocation(s.getKey());
                for (JsonElement element : s.getValue().getAsJsonArray()) {
                    register(event, new EventEmail(element.getAsString()));
                }
            }
        }
    }
    public static void save() {
        JsonObject object = new JsonObject();
        if (!REGISTRY.isEmpty()) {
            REGISTRY.forEach((k, v)->{
                String event = String.valueOf(k);
                JsonArray array = new JsonArray();
                for (EventEmail email : v) {
                    array.add(email.getEmailPath());
                }
                object.add(event, array);
            });
        }else {
            for (ResourceLocation event : REGISTRY_EVENT) {
                object.add(String.valueOf(event), new JsonArray());
            }
        }
        JsonParser.toJsonFile(EmailAPI.getGlobalDataPath()+"event_emails.json", object, true);
    }

    protected String emailPath;
    protected Email email;

    public EventEmail() {
    }

    public EventEmail(String emailPath) {
        this.emailPath = emailPath;
    }

    public String getEmailPath() {
        return emailPath;
    }

    public EventEmail setEmailPath(String emailPath) {
        this.emailPath = emailPath;
        this.email = null;
        return this;
    }

    public Email getAsEmail() {
        if (this.email==null) {
            this.email = new Email(JsonParser.parse(EmailAPI.getGlobalDataPath() + "emails/" + this.get()));
        }
        return this.email.setCreateTimeToNow();
    }

    public void send(String player) {
        EmailAPI.sendEmail(EmailSenderGroup.SYSTEM, player, this.getAsEmail());
    }

    @Override
    public JsonObject write(JsonObject data) {
        data.addProperty("path", this.getEmailPath());
        return data;
    }

    @Override
    public void read(JsonObject data) {
        this.setEmailPath(JsonUtils.get(data, "path", ""));
    }

    @Override
    public String get() {
        return this.emailPath;
    }
}
