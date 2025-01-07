package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgDisplayInbox extends BaseMessage {
    private boolean display;

    public MsgDisplayInbox() {
    }

    public MsgDisplayInbox(boolean display) {
        this.display = display;
    }

    @Override
    public void toBytes(PacketBuffer buf) {
        buf.writeBoolean(this.display);
    }

    @Override
    public void fromBytes(PacketBuffer buf) {
        this.display = buf.readBoolean();
    }

    @Override
    public boolean handler(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(()->{
            ServerPlayerEntity player = context.get().getSender();
            if (player != null) {
                player.getPersistentData().putBoolean("displayInbox", this.display);
            }
        });
        return true;
    }
}
