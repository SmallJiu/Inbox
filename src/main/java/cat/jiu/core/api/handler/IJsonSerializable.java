package cat.jiu.core.api.handler;

import com.google.gson.JsonObject;

public interface IJsonSerializable {
	JsonObject write(JsonObject data);
	void read(JsonObject data);
}
