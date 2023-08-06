package cat.jiu.email.util.client;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.ISound;
import cat.jiu.email.event.InboxPlaySoundEvent;
import cat.jiu.email.ui.container.ContainerEmailMain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;

import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

@OnlyIn(Dist.CLIENT)
public class EmailSenderSndSound extends AbstractTickableSoundInstance {
	protected final AbstractClientPlayer player;
	public final ITimer time;
	protected final ISound sound;
	private final long emailID;
	public EmailSenderSndSound(ISound sound, long emailID) {
		super(sound.getSound(), SoundSource.PLAYERS, Minecraft.getInstance().level.random);
		this.player = Minecraft.getInstance().player;
		this.sound = sound;
		this.time = sound.getTime();
		this.pitch = sound.getSoundPitch();
		this.volume = sound.getSoundVolume();
		this.emailID = emailID;
	}
	
	@Override
	public void tick() {
		if(this.time.isDone()
		|| !(this.player.containerMenu instanceof ContainerEmailMain && ((ContainerEmailMain)this.player.containerMenu).getCurrenEmail()==this.emailID)) {
			this.stop();
			MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Stop(this.emailID));
		}
		
		this.time.update();
		
		if(!this.isStopped()) {
			this.x = (float)this.player.getX();
			this.y = (float)this.player.getY();
			this.z = (float)this.player.getZ();
		}
	}
}
