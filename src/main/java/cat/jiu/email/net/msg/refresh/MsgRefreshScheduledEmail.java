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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MsgRefreshScheduledEmail {
    public static final Refresh REFRESH_MAIN = new Refresh();
    public static final RefreshMap REFRESH_MAP = new RefreshMap();
    public static class Refresh extends BaseMessage {
        public static final Type<Refresh> TYPE = type(EmailMain.MODID);
        public Refresh() {
            super(TYPE);
        }

        @Override
        public void toBytes(FriendlyByteBuf buf) {}
        @Override
        public void fromBytes(FriendlyByteBuf buf) {}

        @Override
        public boolean handler(IPayloadContext ctx) {
            ctx.player();
            EmailAPI.sendScheduledEmailToClient((ServerPlayer) ctx.player());
            return true;
        }
    }

    public static class Send extends BaseMessage {
        public static final Type<Send> TYPE = type(EmailMain.MODID);
        private ScheduledEmail email;

        public Send() {
            super(TYPE);
        }

        public Send(ScheduledEmail email) {
            super(TYPE);
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
        public boolean handler(IPayloadContext context) {
            if(SideProxy.isClient()) {
                if(Minecraft.getInstance().screen instanceof GuiScheduledEmail gui){
                    gui.addEmail(this.email);
                    gui.refresh();
                }
            }
            return true;
        }
    }


    public static class RefreshMap extends BaseMessage {
        public static final Type<RefreshMap> TYPE = type(EmailMain.MODID);
        public RefreshMap() {
            super(TYPE);
        }

        @Override
        public void toBytes(FriendlyByteBuf buf) {}
        @Override
        public void fromBytes(FriendlyByteBuf buf) {}

        @Override
        public boolean handler(IPayloadContext ctx) {
            ctx.player();
            EmailAPI.refreshScheduledEmailMap((ServerPlayer) ctx.player());
            return true;
        }
    }

    public static class SendMap extends BaseMessage {
        public static final Type<SendMap> TYPE = type(EmailMain.MODID);
        private String path;
        private Email email;

        public SendMap() {
            super(TYPE);
        }

        public SendMap(String path, Email email) {
            super(TYPE);
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
        public boolean handler(IPayloadContext context) {
            if(SideProxy.isClient()) {
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
