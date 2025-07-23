package cat.jiu.email.element;

import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.registry.StaticRegistry;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.event.RegisterStorageTypeEvent;
import cat.jiu.email.util.DBParser;
import cat.jiu.email.configs.EmailConfigServer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.neoforged.neoforge.common.NeoForge;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.function.Supplier;

public class StorageType implements Supplier<String> {
    public static final StaticRegistry<String, StorageType> REGISTRY = new StaticRegistry<String, StorageType>(EmailMain.MODID, "storage_type")
            .register(registry->
                    NeoForge.EVENT_BUS.post(new RegisterStorageTypeEvent(registry))
            );

    public static StorageType getInstance() {
        return StorageType.REGISTRY.get(EmailConfigServer.Storage_Inbox_Types.get());
    }

    public final String name;
    public final Function_WithExceptions<String, JsonObject, Exception> read;
    public final Consumer_WithExceptions<Inbox, Exception> write;

    public StorageType(
            String name,
            Consumer_WithExceptions<Inbox, Exception> write,
            Function_WithExceptions<String, JsonObject, Exception> read
    ) {
        this.name = name;
        this.write = write;
        this.read = read;
    }

    @Override
    public String get(){
        return this.name;
    }
    public StorageType register(){
        REGISTRY.register(this);
        return this;
    }

    public static final StorageType JSON = new StorageType(
            "json",
            inbox -> StorageType.writeJson(inbox, false),
            owner->{
                File email = new File(EmailAPI.getSaveInboxPath() + owner + ".json");
                if(email.exists()) {
                    JsonElement file = JsonUtils.parseThrow(email);
                    if(file != null && file.isJsonObject()) {
                        return file.getAsJsonObject();
                    }
                    throw new JsonParseException("json not a object. " + file);
                }
                throw new FileNotFoundException(String.valueOf(email));
            }
    ).register();

    static void writeJson(Inbox inbox, boolean format) throws Exception {
        JsonUtils.toJsonFileThrow(EmailAPI.getSaveInboxPath() + inbox.getOwner() + ".json", inbox.write(new JsonObject()), format);
    }

    public static final StorageType JSON_FORMAT = new StorageType(
            "json_format",
            inbox->StorageType.writeJson(inbox, true), JSON.read
    ).register();

    public static final StorageType NBT = new StorageType(
            "nbt",
            inbox -> NbtIo.writeCompressed(inbox.write(new CompoundTag()), new File(EmailAPI.getSaveInboxPath() + inbox.getOwner() + ".dat").toPath()),
            owner->{
                Inbox inbox = Inbox.getEmpty(owner);
                File email = new File(EmailAPI.getSaveInboxPath() + owner + ".dat");
                if(email.exists()) {
                    inbox.read(NbtIo.readCompressed(email.toPath(), NbtAccounter.unlimitedHeap()));
                }
                return inbox.write(new JsonObject());
            }
    ).register();

    public static final StorageType SQL = new StorageType(
            "sql",
            inbox->DBParser.write(DBParser.getDBUrl(), inbox),
            owner->DBParser.read(DBParser.getDBUrl(), owner)
    ).register();



    @FunctionalInterface
    public interface Consumer_WithExceptions<T, E extends Exception> {
        void accept(T t) throws E;
    }

    @FunctionalInterface
    public interface Function_WithExceptions<T, R, E extends Exception> {
        R apply(T t) throws E;
    }
}
