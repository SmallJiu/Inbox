package cat.jiu.core.api;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public abstract class BaseMessage {
    public abstract void toBytes(FriendlyByteBuf buffer);
    public abstract void fromBytes(FriendlyByteBuf buf);
    public abstract boolean handler(Supplier<NetworkEvent.Context> context);
}
