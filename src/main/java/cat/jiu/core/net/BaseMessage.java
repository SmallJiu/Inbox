package cat.jiu.core.net;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public abstract class BaseMessage {
    public abstract void toBytes(PacketBuffer buf);
    public abstract void fromBytes(PacketBuffer buf);
    public abstract boolean handler(Supplier<NetworkEvent.Context> context);

    public static abstract class CallbackMessage<T extends BaseMessage> extends BaseMessage {
        protected final BiConsumer<Supplier<NetworkEvent.Context>, T> send;

        protected CallbackMessage(BiConsumer<Supplier<NetworkEvent.Context>, T> send) {
            this.send = send;
        }

        @Override
        public boolean handler(Supplier<NetworkEvent.Context> context) {
            T callback = this.callback(context);
            if (callback!=null) {
                this.send.accept(context, callback);
            }
            return true;
        }

        protected abstract T callback(Supplier<NetworkEvent.Context> context);
    }
}
