package cat.jiu.core.api.handler;

import net.minecraft.nbt.CompoundNBT;

public interface INBTSerializable {
	CompoundNBT write(CompoundNBT nbt);
	void read(CompoundNBT nbt);
}
