package cat.jiu.email.net.msg;

import cat.jiu.email.net.BaseMessage;
import cat.jiu.email.ui.gui.GuiEmailSend;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.IOException;

public class MsgAddresseeHistory extends BaseMessage {
    private String player;
    public MsgAddresseeHistory() {}

    public MsgAddresseeHistory(String player) {
        this.player = player;
    }

    public void fromBytes(ByteBuf buf) {
        try {
            this.player = new PacketBuffer(buf).readCompoundTag().getString("player");
        }catch(IOException e) {
            e.printStackTrace();
        }
    }
    public void toBytes(ByteBuf buf) {
        NBTTagCompound nbt = new NBTTagCompound();

        nbt.setString("player", this.player);

        new PacketBuffer(buf).writeCompoundTag(nbt);
    }

    public IMessage handler(MessageContext ctx) {
        if(ctx.side.isClient()) {
            if (Minecraft.getMinecraft().currentScreen instanceof GuiEmailSend){
                ((GuiEmailSend)Minecraft.getMinecraft().currentScreen).addAddresseeHistory(this.player);
            }
        }
        return null;
    }
}
