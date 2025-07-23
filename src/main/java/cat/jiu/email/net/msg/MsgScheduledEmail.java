package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.ScheduledEmail;
import cat.jiu.email.util.TimeMillis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

public class MsgScheduledEmail {
    public static class Add extends BaseMessage {
        public static final Type<Add> TYPE = type(EmailMain.MODID);
        private String path;
        private ScheduledEmail.Addressee addressee = ScheduledEmail.Addressee.ONLINE;
        private TimeMillis interval;
        private String note = "";
        private Set<String> names;

        public Add() {
            super(TYPE);
        }

        public Add(ScheduledEmail email) {
            this(email.getFilePath(), email.getAddressee(), email.getInterval(), email.getNote(), email.getCustomAddressee());
        }

        public Add(String path, ScheduledEmail.Addressee addressee, TimeMillis interval, String note, Set<String> names) {
            super(TYPE);
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
        public boolean handler(IPayloadContext context) {
            ScheduledEmail email = new ScheduledEmail()
                    .setFilePath(this.path)
                    .setAddressee(this.addressee)
                    .setInterval(this.interval)
                    .setNote(this.note);
            if (this.names!=null) {
                this.names.forEach(email::addCustomAddressee);
            }
            ScheduledEmail.addScheduledEmail(email, true);
            context.player().sendSystemMessage(Component.nullToEmpty("Success!"));
            return true;
        }
    }

    public static class Remove extends BaseMessage {
        public static final Type<Remove> TYPE = type(EmailMain.MODID);
        private long id;

        public Remove() {
            super(TYPE);
        }

        public Remove(long id) {
            super(TYPE);
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
        public boolean handler(IPayloadContext context) {
            ScheduledEmail.removeScheduledEmail(this.id);
            return true;
        }
    }
}
