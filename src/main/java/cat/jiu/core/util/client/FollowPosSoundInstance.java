package cat.jiu.core.util.client;

import cat.jiu.core.util.element.sound.SoundMC;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FollowPosSoundInstance extends AbstractTickableSoundInstance {
    private boolean followEntity;
    private Entity entity;
    private BlockPos pos;
    public FollowPosSoundInstance(SoundMC sound) {
        super(sound.getSoundEvent(), sound.getSoundChannel(), SoundInstance.createUnseededRandom());
        this.looping = sound.isSoundLooping();
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
            this.x = this.entity.getX();
            this.y = this.entity.getY();
            this.z = this.entity.getZ();
        }else {
            this.x = this.pos.getX();
            this.y = this.pos.getY();
            this.z = this.pos.getZ();
        }
    }
}
