package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.EmailAPI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MsgUndying extends BaseMessage {
    private long count;
    public MsgUndying() {}
    public MsgUndying(long count) {
        this.count = count;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeLong(count);
    }

    @Override
    public void fromBytes(FriendlyByteBuf buf) {
        this.count = buf.readLong();
    }

    @Override
    public boolean handler(Supplier<NetworkEvent.Context> context) {
        EmailAPI.setUndyingCount(this.count);
        return true;
    }
}
