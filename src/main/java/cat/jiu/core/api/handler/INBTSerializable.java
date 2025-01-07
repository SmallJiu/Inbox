package cat.jiu.core.api.handler;

import net.minecraft.nbt.CompoundNBT;

public interface INBTSerializable {
	CompoundNBT write(CompoundNBT data);
	void read(CompoundNBT data);
}
