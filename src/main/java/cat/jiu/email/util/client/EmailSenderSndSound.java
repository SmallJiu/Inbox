package cat.jiu.email.util.client;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.ISound;
import cat.jiu.email.event.InboxPlaySoundEvent;
import cat.jiu.email.ui.container.ContainerEmailMain;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.SoundCategory;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

@OnlyIn(Dist.CLIENT)
public class EmailSenderSndSound extends TickableSound {
	protected final ClientPlayerEntity player;
	public final ITimer time;
	protected final ISound sound;
	private final long emailID;
	public EmailSenderSndSound(ISound sound, long emailID) {
		super(sound.getSound(), SoundCategory.PLAYERS);
		this.player = Minecraft.getInstance().player;
		this.sound = sound;
		this.time = sound.getTime();
		this.pitch = sound.getSoundPitch();
		this.volume = sound.getSoundVolume();
		this.emailID = emailID;
	}
	
	@Override
	public void tick() {
		this.check();
		
		this.time.update();
		
		if(!this.isDonePlaying()) {
			this.x = (float)this.player.getPosX();
			this.y = (float)this.player.getPosY();
			this.z = (float)this.player.getPosZ();
		}
	}
	
	protected void check() {
		boolean lag = this.time.isDone() || !(this.player.openContainer instanceof ContainerEmailMain && ((ContainerEmailMain)this.player.openContainer).getCurrenEmail()==this.emailID);
		
		if(lag) {
			this.finishPlaying();
			MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Stop(this.emailID));
		}
	}
}
