package cat.jiu.core.api.handler;

import com.google.gson.JsonObject;

public interface IJsonSerializable {
	JsonObject write(JsonObject json);
	void read(JsonObject json);
}
