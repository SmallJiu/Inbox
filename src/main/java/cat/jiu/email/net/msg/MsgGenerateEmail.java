package cat.jiu.email.net.msg;

import cat.jiu.core.net.BaseMessage;
import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.SideProxy;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.api.IAttachment;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.email.element.Email;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.io.File;
import java.util.function.Supplier;

public class MsgGenerateEmail extends BaseMessage {
    protected String fileName;
    protected Email email;

    public MsgGenerateEmail() {
    }

    public MsgGenerateEmail( String fileName, Email email) {
        this.email = email;
        this.fileName = fileName;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.fileName);
        buf.writeNbt(this.email.write(new CompoundTag()));
    }

    @Override
    public void fromBytes(FriendlyByteBuf buf) {
        this.fileName = buf.readUtf();
        this.email = new Email(buf.readNbt());
    }

    @Override
    public boolean handler(Supplier<NetworkEvent.Context> context) {
        if (!SideProxy.isClient()){
            JsonObject object = this.email.write(new JsonObject());
            object.remove("time");
            if (!this.email.hasAttachments()) {
                JsonArray array = new JsonArray();
                for (ResourceLocation resourceLocation : IAttachment.REGISTRY.getIDs()) {
                    array.add(String.valueOf(resourceLocation));
                }
                object.add("attachments", new JsonArray());
                object.add("AllAttachmentID", array);
            }
            try {
                JsonUtils.toJsonFileThrow(new File(EmailAPI.getGlobalDataPath(), "emails/" + this.fileName), object, true, EmailConfigServer.File_Charset.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
