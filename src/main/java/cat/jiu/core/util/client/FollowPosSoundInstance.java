package cat.jiu.core.util.client;

import cat.jiu.core.util.element.sound.SoundMC;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FollowPosSoundInstance extends TickableSound {
    private boolean followEntity;
    private Entity entity;
    private BlockPos pos;
    public FollowPosSoundInstance(SoundMC sound) {
        super(sound.getSoundEvent(), sound.getSoundChannel());
        this.repeat = sound.isSoundLooping();
        this.volume = sound.getCurrentSoundVolume();
        this.pitch = sound.getSoundPitch();
    }

    public void setFollowEntity(Entity entity) {
        this.entity = entity;
        this.followEntity = true;
    }

    public void setFollowPos(BlockPos pos) {
        this.pos = pos;
        this.followEntity = false;
    }

    public void setFollowing(boolean followEntity, Entity entity, BlockPos pos) {
        this.followEntity = followEntity;
        this.entity = entity;
        this.pos = pos;
    }

    @Override
    public void tick() {
        if (this.followEntity) {
            this.x = this.entity.getPosX();
            this.y = this.entity.getPosY();
            this.z = this.entity.getPosZ();
        }else {
            this.x = this.pos.getX();
            this.y = this.pos.getY();
            this.z = this.pos.getZ();
        }
    }
}
