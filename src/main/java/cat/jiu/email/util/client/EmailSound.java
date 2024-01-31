package cat.jiu.email.util.client;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.ISound;
import cat.jiu.email.event.InboxPlaySoundEvent;
import cat.jiu.email.ui.container.ContainerEmailMain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.SoundCategory;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EmailSound extends MovingSound {
	protected final EntityPlayerSP player;
	public final ITimer time;
	protected final ISound sound;
	private final long emailID;
	public EmailSound(ISound sound, long emailID) {
		super(sound.getSound(), SoundCategory.PLAYERS);
		this.player = Minecraft.getMinecraft().player;
		this.sound = sound;
		this.time = sound.getTime();
		this.pitch = sound.getSoundPitch();
		this.volume = sound.getSoundVolume();
		this.emailID = emailID;
	}
	
	@Override
	public void update() {
		this.check();
		this.time.update();
		this.xPosF = (float)this.player.posX;
		this.yPosF = (float)this.player.posY;
		this.zPosF = (float)this.player.posZ;
	}
	
	protected void check() {
		if(this.time.isDone() || !(this.player.openContainer instanceof ContainerEmailMain && ((ContainerEmailMain)this.player.openContainer).getCurrenEmail()==this.emailID)) {
			this.donePlaying = true;
			MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Stop(this.emailID));
		}
	}
}
