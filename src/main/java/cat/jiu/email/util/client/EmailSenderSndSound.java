package cat.jiu.email.util.client;

import cat.jiu.email.event.InboxPlaySoundEvent;
import cat.jiu.email.iface.IInboxSound;
import cat.jiu.email.iface.IInboxTime;
import cat.jiu.email.ui.container.ContainerEmailMain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EmailSenderSndSound extends MovingSound {
	protected final EntityPlayerSP player;
	public final IInboxTime time;
	protected final IInboxSound sound;
	private long id;
	public EmailSenderSndSound(IInboxSound sound, long msgID) {
		super(sound.getSound(), SoundCategory.PLAYERS);
		this.player = Minecraft.getMinecraft().player;
		this.sound = sound;
		this.time = sound.getTime();
		this.pitch = sound.getSoundPitch();
		this.volume = sound.getSoundVolume();
		this.id = msgID;
	}
	
	@Override
	public void update() {
		MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Tick(this.sound, this.id));
		if(!this.time.isDone()
		&& this.player.openContainer instanceof ContainerEmailMain
		&& ((ContainerEmailMain)this.player.openContainer).getCurrenEmail()==this.id) {
			this.xPosF = (float)this.player.posX;
			this.yPosF = (float)this.player.posY;
			this.zPosF = (float)this.player.posZ;
			this.time.update();
		}else {
			this.donePlaying = true;
			MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Stop(this.id));
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
