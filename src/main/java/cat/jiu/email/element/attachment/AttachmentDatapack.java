package cat.jiu.email.element.attachment;

import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;

public abstract class AttachmentDatapack implements IAttachment {
    public static final ResourceLocation ID = new ResourceLocation(EmailMain.MODID, "attachment/datapack");

    public AttachmentDatapack() {}

    public AttachmentDatapack(CompoundNBT tag) {
        this.readFrom(tag);
    }

    public AttachmentDatapack(JsonObject json) {
        this.readFrom(json);
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }
}
