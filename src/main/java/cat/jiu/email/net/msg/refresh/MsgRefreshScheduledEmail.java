package cat.jiu.email.net.msg.refresh;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.SideProxy;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.ScheduledEmail;
import cat.jiu.email.ui.gui.GuiGenerateScheduledEmail;
import cat.jiu.email.ui.gui.GuiScheduledEmail;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgRefreshScheduledEmail {
    public static final Refresh REFRESH_MAIN = new Refresh();
    public static final RefreshMap REFRESH_MAP = new RefreshMap();
    public static class Refresh extends BaseMessage {
        @Override
        public void toBytes(PacketBuffer buf) {}
        @Override
        public void fromBytes(PacketBuffer buf) {}

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
        public void toBytes(PacketBuffer buf) {
            buf.writeCompoundTag(this.email.write(new CompoundNBT()));
        }

        @Override
        public void fromBytes(PacketBuffer buf) {
            this.email = new ScheduledEmail();
            this.email.readFrom(buf.readCompoundTag());
        }

        @Override
        public boolean handler(Supplier<NetworkEvent.Context> context) {
            if(SideProxy.isClient()) {
                if(Minecraft.getInstance().currentScreen instanceof GuiScheduledEmail){
                    ((GuiScheduledEmail) Minecraft.getInstance().currentScreen).addEmail(this.email);
                    ((GuiScheduledEmail) Minecraft.getInstance().currentScreen).refresh();
                    return true;
                }
            }
            return true;
        }
    }


    public static class RefreshMap extends BaseMessage {
        @Override
        public void toBytes(PacketBuffer buf) {}
        @Override
        public void fromBytes(PacketBuffer buf) {}

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
        public void toBytes(PacketBuffer buf) {
            buf.writeString(this.path);
            buf.writeCompoundTag(this.email.write(new CompoundNBT()));
        }

        @Override
        public void fromBytes(PacketBuffer buf) {
            this.path = buf.readString();
            this.email = new Email(buf.readCompoundTag());
        }

        @Override
        public boolean handler(Supplier<NetworkEvent.Context> context) {
            if(SideProxy.isClient()) {
                if(Minecraft.getInstance().currentScreen instanceof GuiGenerateScheduledEmail){
                    ((GuiGenerateScheduledEmail) Minecraft.getInstance().currentScreen).addPath(this.path, this.email);
                    ((GuiGenerateScheduledEmail) Minecraft.getInstance().currentScreen).refresh();
                    return true;
                }
            }
            return true;
        }
    }
}
