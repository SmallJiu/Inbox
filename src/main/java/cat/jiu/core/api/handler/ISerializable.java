package cat.jiu.core.api.handler;

import com.google.gson.JsonObject;

import net.minecraft.nbt.NBTTagCompound;

public interface ISerializable extends IJsonSerializable, INBTSerializable {
	@SuppressWarnings("unchecked")
	default <T> T writeTo(Class<T> type) {
		if(type == NBTTagCompound.class) {
			return (T) this.write(new NBTTagCompound());
		}else if(type == JsonObject.class) {
			return (T) this.write(new JsonObject());
		}
		return null;
	}
	default <T> void readFrom(T e) {
		if(e instanceof NBTTagCompound) {
			this.read((NBTTagCompound)e);
		}else if(e instanceof JsonObject) {
			this.read((JsonObject)e);
		}
	}
}
