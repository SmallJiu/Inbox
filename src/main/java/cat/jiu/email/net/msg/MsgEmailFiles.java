package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.SideProxy;
import cat.jiu.email.command.EmailFileType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.util.Collection;
import java.util.function.Supplier;

public class MsgEmailFiles extends BaseMessage {
    protected Collection<File> files;
    public MsgEmailFiles() {}
    public MsgEmailFiles(Collection<File> files) {
        this.files = files;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(this.files, (b,file)->b.writeUtf(file.getPath().replace('\\', '/')));
    }

    @Override
    public void fromBytes(FriendlyByteBuf buf) {
        this.files = buf.readList(b -> new File(b.readUtf()));
    }

    @Override
    public boolean handler(Supplier<NetworkEvent.Context> context) {
//        EmailFileType.setFiles(this.files);
        return true;
    }
}
