package cat.jiu.core.api.handler;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.JsonObject;

import cat.jiu.sql.SQLValues;
import net.minecraft.nbt.NBTTagCompound;

public interface ISerializable extends IJsonSerializable, INBTSerializable, ISQLSerializable {
	@SuppressWarnings("unchecked")
	default <T> T writeTo(Class<T> type) {
		if(type == NBTTagCompound.class) {
			return (T) this.write(new NBTTagCompound());
		}else if(type == JsonObject.class) {
			return (T) this.write(new JsonObject());
		}else if(type == SQLValues.class) {
			return (T) this.write(new SQLValues());
		}
		return null;
	}
	
	default <T> void readFrom(T e) {
		if(e instanceof NBTTagCompound) {
			this.read((NBTTagCompound)e);
		}else if(e instanceof JsonObject) {
			this.read((JsonObject)e);
		}else if(e instanceof ResultSet) {
			try {
				this.read((ResultSet)e);
			}catch(SQLException e1) {
				e1.printStackTrace();
			}
		}
	}
}
