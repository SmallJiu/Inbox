package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.element.ScheduledEmail;
import cat.jiu.email.util.TimeMillis;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.HashSet;
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
        public void toBytes(PacketBuffer buf) {
            CompoundNBT nbt = new CompoundNBT();

            nbt.putString("path", this.path);
            nbt.putString("addressee", this.addressee.getName());
            nbt.putLong("interval", this.interval.millis);
            nbt.putString("note", this.note);

            if (this.names!=null && !this.names.isEmpty()) {
                ListNBT names = new ListNBT();
                this.names.forEach(e->names.add(StringNBT.valueOf(e)));
                nbt.put("names", names);
            }

            buf.writeCompoundTag(nbt);
        }

        @Override
        public void fromBytes(PacketBuffer buf) {
            CompoundNBT nbt = buf.readCompoundTag();

            this.path = nbt.getString("path");
            this.addressee = ScheduledEmail.Addressee.get(nbt.getString("addressee"));
            this.interval = new TimeMillis(nbt.getLong("interval"));
            this.note = nbt.getString("note");

            if (nbt.contains("names")) {
                this.names = new HashSet<>();
                nbt.getList("names", 8).forEach(e->this.names.add(e.getString()));
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
            context.get().getSender().sendStatusMessage(ITextComponent.getTextComponentOrEmpty("Success!"), false);
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
        public void toBytes(PacketBuffer buf) {
            buf.writeLong(this.id);
        }

        @Override
        public void fromBytes(PacketBuffer buf) {
            this.id = buf.readLong();
        }

        @Override
        public boolean handler(Supplier<NetworkEvent.Context> context) {
            ScheduledEmail.removeScheduledEmail(this.id);
            return true;
        }
    }
}
