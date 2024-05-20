package cat.jiu.core.api.handler;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.JsonObject;

import cat.jiu.sql.SQLValues;
import net.minecraft.nbt.CompoundTag;

public interface ISerializable extends IJsonSerializable, INBTSerializable, ISQLSerializable {
	@SuppressWarnings("unchecked")
	default <T> T writeTo(Class<T> type) {
		if(type == CompoundTag.class) {
			return (T) this.write(new CompoundTag());
		}else if(type == JsonObject.class) {
			return (T) this.write(new JsonObject());
		}else if(type == SQLValues.class) {
			return (T) this.write(new SQLValues());
		}
		return null;
	}
	
	default <T> void readFrom(T e) {
		if(e instanceof CompoundTag) {
			this.read((CompoundTag)e);
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
