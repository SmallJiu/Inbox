package cat.jiu.email.net.msg;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.element.ScheduledEmail;
import cat.jiu.email.util.TimeMillis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class MsgScheduledEmail {
    public static class Add extends BaseMessage {
        private String path;
        private ScheduledEmail.Addressee addressee = ScheduledEmail.Addressee.ONLINE;
        private TimeMillis interval;
        private String note = "";
        private Set<String> names;

        public Add() {
        }

        public Add(ScheduledEmail email) {
            this(email.getFilePath(), email.getAddressee(), email.getInterval(), email.getNote(), email.getCustomAddressee());
        }

        public Add(String path, ScheduledEmail.Addressee addressee, TimeMillis interval, String note, Set<String> names) {
            this.path = path;
            this.addressee = addressee;
            this.interval = interval;
            this.note = note;
            this.names = names;
        }

        @Override
        public void toBytes(FriendlyByteBuf buf) {
            CompoundTag nbt = new CompoundTag();

            nbt.putString("path", this.path);
            nbt.putString("addressee", this.addressee.getName());
            nbt.putLong("interval", this.interval.millis);
            nbt.putString("note", this.note);

            if (this.names!=null && !this.names.isEmpty()) {
                ListTag names = new ListTag();
                this.names.forEach(e->names.add(StringTag.valueOf(e)));
                nbt.put("names", names);
            }

            buf.writeNbt(nbt);
        }

        @Override
        public void fromBytes(FriendlyByteBuf buf) {
            CompoundTag nbt = buf.readNbt();

            this.path = nbt.getString("path");
            this.addressee = ScheduledEmail.Addressee.get(nbt.getString("addressee"));
            this.interval = new TimeMillis(nbt.getLong("interval"));
            this.note = nbt.getString("note");

            if (nbt.contains("names")) {
                this.names = new HashSet<>();
                nbt.getList("names", 8).forEach(e->this.names.add(e.getAsString()));
            }
        }

        @Override
        public boolean handler(Supplier<NetworkEvent.Context> context) {
            ScheduledEmail email = new ScheduledEmail()
                    .setFilePath(this.path)
                    .setAddressee(this.addressee)
                    .setInterval(this.interval)
                    .setNote(this.note);
            if (this.names!=null) {
                this.names.forEach(email::addCustomAddressee);
            }
            ScheduledEmail.addScheduledEmail(email, true);
            context.get().getSender().sendSystemMessage(Component.nullToEmpty("Success!"));
            return true;
        }
    }

    public static class Remove extends BaseMessage {
        private long id;

        public Remove() {
        }

        public Remove(long id) {
            this.id = id;
        }

        @Override
        public void toBytes(FriendlyByteBuf buf) {
            buf.writeLong(this.id);
        }

        @Override
        public void fromBytes(FriendlyByteBuf buf) {
            this.id = buf.readLong();
        }

        @Override
        public boolean handler(Supplier<NetworkEvent.Context> context) {
            ScheduledEmail.removeScheduledEmail(this.id);
            return true;
        }
    }
}
