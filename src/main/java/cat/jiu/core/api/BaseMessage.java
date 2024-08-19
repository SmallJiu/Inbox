package cat.jiu.core.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public abstract class BaseMessage {
    public abstract void toBytes(FriendlyByteBuf buf);
    public abstract void fromBytes(FriendlyByteBuf buf);
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
                return true;
            }
            return false;
        }

        protected abstract T callback(Supplier<NetworkEvent.Context> context);
    }
}
