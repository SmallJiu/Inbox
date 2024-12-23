package cat.jiu.core.util;

import net.minecraft.nbt.*;

import java.math.BigDecimal;
import java.math.BigInteger;

public class NBTUtils {
    public static Tag get(CompoundTag data, String k, Tag failBack) {
        if (data.contains(k)) {
            return data.get(k);
        }
        return failBack;
    }
    public static byte get(CompoundTag data, String k, byte failBack) {
        if (data.contains(k)) {
            return data.getByte(k);
        }
        return failBack;
    }
    public static short get(CompoundTag data, String k, short failBack) {
        if (data.contains(k)) {
            return data.getShort(k);
        }
        return failBack;
    }
    public static int get(CompoundTag data, String k, int failBack) {
        if (data.contains(k)) {
            return data.getInt(k);
        }
        return failBack;
    }
    public static long get(CompoundTag data, String k, long failBack) {
        if (data.contains(k)) {
            return data.getLong(k);
        }
        return failBack;
    }
    public static String get(CompoundTag data, String k, String failBack) {
        if (data.contains(k)) {
            return data.getString(k);
        }
        return failBack;
    }
    public static boolean get(CompoundTag data, String k, boolean failBack) {
        if (data.contains(k)) {
            return data.getBoolean(k);
        }
        return failBack;
    }
    public static CompoundTag get(CompoundTag data, String k, CompoundTag failBack) {
        if (data.contains(k)) {
            return data.getCompound(k);
        }
        return failBack;
    }
    public static ListTag get(CompoundTag data, String k, int type, ListTag failBack) {
        if (data.contains(k)) {
            return data.getList(k, type);
        }
        return failBack;
    }
    public static float get(CompoundTag data, String k, float failBack) {
        if (data.contains(k)) {
            return data.getFloat(k);
        }
        return failBack;
    }
    public static double get(CompoundTag data, String k, double failBack) {
        if (data.contains(k)) {
            return data.getDouble(k);
        }
        return failBack;
    }
    public static BigInteger get(CompoundTag data, String k, BigInteger failBack) {
        String string = get(data, k, (String) null);
        if (string != null) {
            return new BigInteger(string);
        }
        return failBack;
    }
    public static BigDecimal get(CompoundTag data, String k, BigDecimal failBack) {
        String string = get(data, k, (String) null);
        if (string != null) {
            return new BigDecimal(string);
        }
        return failBack;
    }
}
