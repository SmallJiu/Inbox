package cat.jiu.email.net.msg.refresh;

import cat.jiu.core.api.BaseMessage;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.ScheduledEmail;
import cat.jiu.email.ui.gui.GuiGenerateScheduledEmail;
import cat.jiu.email.ui.gui.GuiScheduledEmail;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgRefreshScheduledEmail {
    public static final Refresh REFRESH_MAIN = new Refresh();
    public static final RefreshMap REFRESH_MAP = new RefreshMap();
    public static class Refresh extends BaseMessage {
        @Override
        public void toBytes(FriendlyByteBuf buf) {}
        @Override
        public void fromBytes(FriendlyByteBuf buf) {}

        @Override
        public boolean handler(Supplier<NetworkEvent.Context> ctx) {
            if(ctx.get().getSender() != null) {
                EmailAPI.sendScheduledEmailToClient(ctx.get().getSender());
                return true;
            }
            return false;
        }
    }

    public static class Send extends BaseMessage {
        private ScheduledEmail email;

        public Send() {
        }

        public Send(ScheduledEmail email) {
            this.email = email;
        }

        @Override
        public void toBytes(FriendlyByteBuf buf) {
            buf.writeNbt(this.email.write(new CompoundTag()));
        }

        @Override
        public void fromBytes(FriendlyByteBuf buf) {
            this.email = new ScheduledEmail();
            this.email.readFrom(buf.readNbt());
        }

        @Override
        public boolean handler(Supplier<NetworkEvent.Context> context) {
            if(EmailMain.proxy.isClient()) {
                if(Minecraft.getInstance().screen instanceof GuiScheduledEmail gui){
                    gui.addEmail(this.email);
                    gui.refresh();
                }
            }
            return true;
        }
    }


    public static class RefreshMap extends BaseMessage {
        @Override
        public void toBytes(FriendlyByteBuf buf) {}
        @Override
        public void fromBytes(FriendlyByteBuf buf) {}

        @Override
        public boolean handler(Supplier<NetworkEvent.Context> ctx) {
            if(ctx.get().getSender() != null) {
                EmailAPI.refreshScheduledEmailMap(ctx.get().getSender());
                return true;
            }
            return false;
        }
    }

    public static class SendMap extends BaseMessage {
        private String path;
        private Email email;

        public SendMap() {
        }

        public SendMap(String path, Email email) {
            this.path = path;
            this.email = email;
        }

        @Override
        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(this.path);
            buf.writeNbt(this.email.write(new CompoundTag()));
        }

        @Override
        public void fromBytes(FriendlyByteBuf buf) {
            this.path = buf.readUtf();
            this.email = new Email(buf.readNbt());
        }

        @Override
        public boolean handler(Supplier<NetworkEvent.Context> context) {
            if(EmailMain.proxy.isClient()) {
                if(Minecraft.getInstance().screen instanceof GuiGenerateScheduledEmail gui){
                    gui.addPath(this.path, this.email);
                    gui.refresh();
                    return true;
                }
            }
            return true;
        }
    }
}
