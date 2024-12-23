package cat.jiu.core.api.handler;

import net.minecraft.nbt.CompoundTag;

public interface INBTSerializable {
	CompoundTag write(CompoundTag data);
	void read(CompoundTag data);
}
