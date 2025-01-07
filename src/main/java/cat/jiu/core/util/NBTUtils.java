package cat.jiu.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.util.StringUtil;

import java.math.BigDecimal;
import java.math.BigInteger;

public class NBTUtils {
    public static Tag get(CompoundTag data, String k, Tag failBack) {
        if (data!=null && data.contains(k)) {
            return data.get(k);
        }
        return failBack;
    }
    public static byte get(CompoundTag data, String k, byte failBack) {
        if (data!=null && data.contains(k)) {
            return data.getByte(k);
        }
        return failBack;
    }
    public static short get(CompoundTag data, String k, short failBack) {
        if (data!=null && data.contains(k)) {
            return data.getShort(k);
        }
        return failBack;
    }
    public static int get(CompoundTag data, String k, int failBack) {
        if (data!=null && data.contains(k)) {
            return data.getInt(k);
        }
        return failBack;
    }
    public static long get(CompoundTag data, String k, long failBack) {
        if (data!=null && data.contains(k)) {
            return data.getLong(k);
        }
        return failBack;
    }
    public static String get(CompoundTag data, String k, String failBack) {
        if (data!=null && data.contains(k)) {
            return data.getString(k);
        }
        return failBack;
    }
    public static boolean get(CompoundTag data, String k, boolean failBack) {
        if (data!=null && data.contains(k)) {
            return data.getBoolean(k);
        }
        return failBack;
    }
    public static CompoundTag get(CompoundTag data, String k, CompoundTag failBack) {
        if (data!=null && data.contains(k)) {
            return data.getCompound(k);
        }
        return failBack;
    }
    public static ListTag get(CompoundTag data, String k, int type, ListTag failBack) {
        if (data!=null && data.contains(k)) {
            return data.getList(k, type);
        }
        return failBack;
    }
    public static float get(CompoundTag data, String k, float failBack) {
        if (data!=null && data.contains(k)) {
            return data.getFloat(k);
        }
        return failBack;
    }
    public static double get(CompoundTag data, String k, double failBack) {
        if (data!=null && data.contains(k)) {
            return data.getDouble(k);
        }
        return failBack;
    }
    public static BigInteger get(CompoundTag data, String k, BigInteger failBack) {
        String string = get(data, k, (String) null);
        if (!isEmpty(string)) {
            return new BigInteger(string);
        }
        return failBack;
    }
    public static BigDecimal get(CompoundTag data, String k, BigDecimal failBack) {
        String string = get(data, k, (String) null);
        if (!isEmpty(string)) {
            return new BigDecimal(string);
        }
        return failBack;
    }
    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static void pos(CompoundTag data, BlockPos pos) {
        if (pos == null || BlockPos.ZERO.equals(pos)) return;
        CompoundTag posTag = new CompoundTag();
        posTag.putInt("x", pos.getX());
        posTag.putInt("y", pos.getY());
        posTag.putInt("z", pos.getZ());
        data.put("pos", posTag);
    }
    public static BlockPos pos(CompoundTag data) {
        CompoundTag pos = NBTUtils.get(data, "pos", (CompoundTag) null);
        if (pos != null) {
            return new BlockPos(get(pos, "x", 0), get(pos, "y", 0), get(pos, "z", 0));
        }else {
            return new BlockPos(get(data, "x", 0), get(data, "y", 0), get(data, "z", 0));
        }
    }
}
