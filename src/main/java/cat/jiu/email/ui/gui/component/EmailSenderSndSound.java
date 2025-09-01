package cat.jiu.email.ui.gui.component;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.ISound;
import cat.jiu.core.util.client.FollowPosSoundInstance;
import cat.jiu.core.util.element.sound.SoundMC;
import cat.jiu.core.util.timer.MillisTimer;
import cat.jiu.email.event.InboxPlaySoundEvent;

import cat.jiu.email.ui.gui.GuiInbox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

@OnlyIn(Dist.CLIENT)
public class EmailSenderSndSound extends AbstractTickableSoundInstance {
	protected final AbstractClientPlayer player;
	public final ITimer time;
	protected final SoundMC sound;
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
			this.sound.stop();
			MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Stop(((GuiInbox)Minecraft.getInstance().screen).getInbox().getEmail(this.emailID)));
		}
	}

	@Override
	public boolean isStopped() {
		return this.sound.isStopped();
	}

	@Override
	public double getZ() {
		return this.sound.getSoundInstance().getZ();
	}

	@Override
	public double getY() {
		return this.sound.getSoundInstance().getY();
	}

	@Override
	public double getX() {
		return this.sound.getSoundInstance().getX();
	}

	@Override
	public boolean isLooping() {
		return this.sound.getSoundInstance().isLooping();
	}
}
