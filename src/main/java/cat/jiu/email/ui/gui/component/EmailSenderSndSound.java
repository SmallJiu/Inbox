package cat.jiu.email.ui.gui.component;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.ISound;
import cat.jiu.core.util.element.sound.SoundMC;
import cat.jiu.core.util.timer.MillisTimer;
import cat.jiu.email.event.InboxPlaySoundEvent;

import cat.jiu.email.ui.gui.GuiInbox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;

import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;

@OnlyIn(Dist.CLIENT)
public class EmailSenderSndSound extends AbstractTickableSoundInstance {
	protected final AbstractClientPlayer player;
	public final ITimer time;
	protected final ISound sound;
	private final long emailID;
	public EmailSenderSndSound(SoundMC sound, long emailID) {
		super(sound.getSoundEvent(), SoundSource.PLAYERS, Minecraft.getInstance().font.random);
		this.player = Minecraft.getInstance().player;
		this.sound = sound;
		this.time = new MillisTimer(sound.getDuration());
		this.pitch = sound.getSoundPitch();
		this.volume = sound.getSoundVolume();
		this.emailID = emailID;
	}
	
	@Override
	public void tick() {
		if(this.time.isDone()
		|| !(Minecraft.getInstance().screen instanceof GuiInbox gui && gui.getCurrentEmailID()==this.emailID)) {
			this.stop();
			NeoForge.EVENT_BUS.post(new InboxPlaySoundEvent.Stop(((GuiInbox)Minecraft.getInstance().screen).getInbox().getEmail(this.emailID)));
		}
		
		this.time.update();
		
		if(!this.isStopped()) {
			this.x = (float)this.player.getX();
			this.y = (float)this.player.getY();
			this.z = (float)this.player.getZ();
		}
	}
}
