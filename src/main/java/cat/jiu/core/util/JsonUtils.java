package cat.jiu.core.util;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonUtils {
    public static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    public static JsonElement get(JsonObject data, String k, JsonElement failBack) {
        if (data!=null && data.has(k)) {
            return data.get(k);
        }
        return failBack;
    }
    public static byte get(JsonObject data, String k, byte failBack) {
        if (data!=null && data.has(k)) {
            return data.get(k).getAsByte();
        }
        return failBack;
    }
    public static short get(JsonObject data, String k, short failBack) {
        if (data!=null && data.has(k)) {
            return data.get(k).getAsShort();
        }
        return failBack;
    }
    public static int get(JsonObject data, String k, int failBack) {
        if (data!=null && data.has(k)) {
            return data.get(k).getAsInt();
        }
        return failBack;
    }
    public static long get(JsonObject data, String k, long failBack) {
        if (data!=null && data.has(k)) {
            return data.get(k).getAsLong();
        }
        return failBack;
    }
    public static String get(JsonObject data, String k, String failBack) {
        if (data!=null && data.has(k)) {
            return data.get(k).getAsString();
        }
        return failBack;
    }
    public static boolean get(JsonObject data, String k, boolean failBack) {
        if (data!=null && data.has(k)) {
            return data.get(k).getAsBoolean();
        }
        return failBack;
    }
    public static JsonObject get(JsonObject data, String k, JsonObject failBack) {
        if (data!=null && data.has(k)) {
            return data.getAsJsonObject(k);
        }
        return failBack;
    }
    public static JsonArray get(JsonObject data, String k, JsonArray failBack) {
        if (data!=null && data.has(k)) {
            return data.getAsJsonArray(k);
        }
        return failBack;
    }
    public static float get(JsonObject data, String k, float failBack) {
        if (data!=null && data.has(k)) {
            return data.get(k).getAsFloat();
        }
        return failBack;
    }
    public static double get(JsonObject data, String k, double failBack) {
        if (data!=null && data.has(k)) {
            return data.get(k).getAsDouble();
        }
        return failBack;
    }
    public static Number get(JsonObject data, String k, Number failBack) {
        if (data!=null && data.has(k)) {
            return data.get(k).getAsNumber();
        }
        return failBack;
    }
    public static BigInteger get(JsonObject data, String k, BigInteger failBack) {
        if (data!=null && data.has(k)) {
            return data.get(k).getAsBigInteger();
        }
        return failBack;
    }
    public static BigDecimal get(JsonObject data, String k, BigDecimal failBack) {
        if (data!=null && data.has(k)) {
            return data.get(k).getAsBigDecimal();
        }
        return failBack;
    }
    public static void pos(JsonObject data, BlockPos pos) {
        if (pos == null || BlockPos.ZERO.equals(pos)) return;
        JsonObject posTag = new JsonObject();
        posTag.addProperty("x", pos.getX());
        posTag.addProperty("y", pos.getY());
        posTag.addProperty("z", pos.getZ());
        data.add("pos", posTag);
    }
    public static BlockPos pos(JsonObject data) {
        JsonObject pos = get(data, "pos", (JsonObject) null);
        if (pos != null) {
            return new BlockPos(get(pos, "x", 0), get(pos, "y", 0), get(pos, "z", 0));
        }else {
            return new BlockPos(get(data, "x", 0), get(data, "y", 0), get(data, "z", 0));
        }
    }
}
