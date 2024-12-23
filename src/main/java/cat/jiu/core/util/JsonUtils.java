package cat.jiu.core.util;

import com.google.gson.*;

import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonUtils {
    public static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    public static JsonElement get(JsonObject data, String k, JsonElement failBack) {
        if (data.has(k)) {
            return data.get(k);
        }
        return failBack;
    }
    public static byte get(JsonObject data, String k, byte failBack) {
        if (data.has(k)) {
            return data.get(k).getAsByte();
        }
        return failBack;
    }
    public static short get(JsonObject data, String k, short failBack) {
        if (data.has(k)) {
            return data.get(k).getAsShort();
        }
        return failBack;
    }
    public static int get(JsonObject data, String k, int failBack) {
        if (data.has(k)) {
            return data.get(k).getAsInt();
        }
        return failBack;
    }
    public static long get(JsonObject data, String k, long failBack) {
        if (data.has(k)) {
            return data.get(k).getAsLong();
        }
        return failBack;
    }
    public static String get(JsonObject data, String k, String failBack) {
        if (data.has(k)) {
            return data.get(k).getAsString();
        }
        return failBack;
    }
    public static boolean get(JsonObject data, String k, boolean failBack) {
        if (data.has(k)) {
            return data.get(k).getAsBoolean();
        }
        return failBack;
    }
    public static JsonObject get(JsonObject data, String k, JsonObject failBack) {
        if (data.has(k)) {
            return data.getAsJsonObject(k);
        }
        return failBack;
    }
    public static JsonArray get(JsonObject data, String k, JsonArray failBack) {
        if (data.has(k)) {
            return data.getAsJsonArray(k);
        }
        return failBack;
    }
    public static float get(JsonObject data, String k, float failBack) {
        if (data.has(k)) {
            return data.get(k).getAsFloat();
        }
        return failBack;
    }
    public static double get(JsonObject data, String k, double failBack) {
        if (data.has(k)) {
            return data.get(k).getAsDouble();
        }
        return failBack;
    }
    public static Number get(JsonObject data, String k, Number failBack) {
        if (data.has(k)) {
            return data.get(k).getAsNumber();
        }
        return failBack;
    }
    public static BigInteger get(JsonObject data, String k, BigInteger failBack) {
        if (data.has(k)) {
            return data.get(k).getAsBigInteger();
        }
        return failBack;
    }
    public static BigDecimal get(JsonObject data, String k, BigDecimal failBack) {
        if (data.has(k)) {
            return data.get(k).getAsBigDecimal();
        }
        return failBack;
    }
}
