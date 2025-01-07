package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgDisplayInbox extends BaseMessage {
    private boolean display;

    public MsgDisplayInbox() {
    }

    public MsgDisplayInbox(boolean display) {
        this.display = display;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.display);
    }

    @Override
    public void fromBytes(FriendlyByteBuf buf) {
        this.display = buf.readBoolean();
    }

    @Override
    public boolean handler(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(()->{
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                player.getPersistentData().putBoolean("displayInbox", this.display);
            }
        });
        return true;
    }
}
