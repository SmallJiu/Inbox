package cat.jiu.email.element.attachment;

import cat.jiu.email.EmailMain;
import cat.jiu.email.api.IAttachment;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public abstract class AttachmentDatapack implements IAttachment {
    public static final ResourceLocation ID = new ResourceLocation(EmailMain.MODID, "attachment/datapack");

    public AttachmentDatapack() {}

    public AttachmentDatapack(CompoundTag tag) {
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
