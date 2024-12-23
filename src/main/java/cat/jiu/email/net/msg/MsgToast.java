package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailMain;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class MsgToast extends BaseMessage {
    private IText title, message;

    public MsgToast() {
    }

    public MsgToast(IText title) {
        this(title, null);
    }
    public MsgToast(IText title, @Nullable IText message) {
        this.title = title;
        this.message = message;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(this.title.write(new CompoundTag()));
        if (this.message!=null) {
            buf.writeNbt(this.message.write(new CompoundTag()));
        }
    }

    @Override
    public void fromBytes(FriendlyByteBuf buf) {
        this.title = new Text(buf.readNbt());
        try {
            this.message = new Text(buf.readNbt());
        }catch (Exception ignored){}
    }

    @Override
    public boolean handler(Supplier<NetworkEvent.Context> context) {
        if (EmailMain.proxy.isClient()) {
            Minecraft.getInstance().getToasts().addToast(new net.minecraft.client.gui.components.toasts.SystemToast(
                    net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds.PACK_COPY_FAILURE,
                    this.title.toTextComponent(),
                    this.message !=null ? this.message.toTextComponent() : null
            ));
        }
        return true;
    }
}
