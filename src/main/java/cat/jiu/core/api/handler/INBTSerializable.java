package cat.jiu.core.api.handler;

import net.minecraft.nbt.NBTTagCompound;

public interface INBTSerializable {
	NBTTagCompound write(NBTTagCompound nbt);
	void read(NBTTagCompound nbt);
}
