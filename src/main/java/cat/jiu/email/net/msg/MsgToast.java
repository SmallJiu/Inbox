package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.api.element.IText;
import cat.jiu.core.util.SideProxy;
import cat.jiu.core.util.element.Text;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.toasts.SystemToast;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

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
    public void toBytes(PacketBuffer buf) {
        buf.writeCompoundTag(this.title.write(new CompoundNBT()));
        if (this.message!=null) {
            buf.writeCompoundTag(this.message.write(new CompoundNBT()));
        }
    }

    @Override
    public void fromBytes(PacketBuffer buf) {
        this.title = new Text(buf.readCompoundTag());
        try {
            this.message = new Text(buf.readCompoundTag());
        }catch (Exception ignored){}
    }

    @Override
    public boolean handler(Supplier<NetworkEvent.Context> context) {
        if (SideProxy.isClient()) {
            Minecraft.getInstance().getToastGui().add(new SystemToast(
                    SystemToast.Type.PACK_COPY_FAILURE,
                    this.title.toTextComponent(),
                    this.message !=null ? this.message.toTextComponent() : null
            ));
        }
        return true;
    }
}
