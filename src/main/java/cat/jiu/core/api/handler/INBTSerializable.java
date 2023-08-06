package cat.jiu.core.api.handler;

import net.minecraft.nbt.CompoundTag;

public interface INBTSerializable {
	CompoundTag write(CompoundTag nbt);
	void read(CompoundTag nbt);
}
