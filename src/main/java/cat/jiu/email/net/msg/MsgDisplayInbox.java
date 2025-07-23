package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.email.EmailMain;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class MsgDisplayInbox extends BaseMessage {
    public static final Type<MsgDisplayInbox> TYPE = type(EmailMain.MODID);
    private boolean display;

    public MsgDisplayInbox() {
        super(TYPE);
    }

    public MsgDisplayInbox(boolean display) {
        super(TYPE);
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
    public boolean handler(IPayloadContext context) {
        context.enqueueWork(()->
            context.player().getPersistentData().putBoolean("displayInbox", this.display)
        );
        return true;
    }
}
