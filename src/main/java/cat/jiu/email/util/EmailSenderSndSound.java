package cat.jiu.email.util;

import cat.jiu.email.element.EmailSound;
import cat.jiu.email.ui.container.ContainerEmailMain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.SoundCategory;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EmailSenderSndSound extends MovingSound {
	protected final EntityPlayerSP player;
	protected long time;
	private int id;
	public EmailSenderSndSound(EmailSound sound, int msgID) {
		super(sound.getSound(), SoundCategory.PLAYERS);
		this.player = Minecraft.getMinecraft().player;
		this.time = sound.getTime().tick;
		this.pitch = sound.getSoundPitch();
		this.volume = sound.getSoundVolume();
		this.id = msgID;
	}
	
	@Override
	public void update() {
		if(this.time > 0
		&& this.player.openContainer instanceof ContainerEmailMain
		&& ((ContainerEmailMain)this.player.openContainer).getCurrenMsg()==this.id) {
			this.xPosF = (float)this.player.posX;
			this.yPosF = (float)this.player.posY;
			this.zPosF = (float)this.player.posZ;
			this.time--;
		}else {
			this.donePlaying = true;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) return true;
		if(obj instanceof EmailSenderSndSound) {
			EmailSenderSndSound other = (EmailSenderSndSound) obj;
			return other.getSoundLocation().equals(this.getSoundLocation());
		}
		return false;
	}
}
