package cat.jiu.email.util.client;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.api.element.ISound;
import cat.jiu.core.util.element.sound.SoundMC;
import cat.jiu.core.util.timer.MillisTimer;
import cat.jiu.email.event.InboxPlaySoundEvent;

import cat.jiu.email.ui.gui.GuiInbox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.TickableSound;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

@OnlyIn(Dist.CLIENT)
public class EmailSenderSndSound extends TickableSound {
	protected final PlayerEntity player;
	public final ITimer time;
	protected final ISound sound;
	private final long emailID;
	public EmailSenderSndSound(SoundMC sound, long emailID) {
		super(sound.getSoundEvent(), SoundCategory.PLAYERS);
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
		|| Minecraft.getInstance().currentScreen instanceof GuiInbox) {
			GuiInbox gui = (GuiInbox) Minecraft.getInstance().currentScreen;
			if (gui.getCurrentEmailID()==this.emailID) {
				this.finishPlaying();
				MinecraftForge.EVENT_BUS.post(new InboxPlaySoundEvent.Stop(((GuiInbox)Minecraft.getInstance().currentScreen).getInbox().getEmail(this.emailID)));
			}
		}

		this.time.update();
		
		if(!this.isDonePlaying()) {
			this.x = (float)this.player.getPosX();
			this.y = (float)this.player.getPosY();
			this.z = (float)this.player.getPosZ();
		}
	}
}
