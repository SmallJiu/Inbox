package cat.jiu.core.util.mc;

import net.minecraft.nbt.*;

public class SimpleNBTTagList extends NBTTagList {
	public void append(String data) {
		super.appendTag(new NBTTagString(data));
	}

	public void append(int data) {
		super.appendTag(new NBTTagInt(data));
	}

	public void append(byte data) {
		super.appendTag(new NBTTagByte(data));
	}

	public void append(boolean data) {
		super.appendTag(new NBTTagByte((byte) (data ? 1 : 0)));
	}

	public void append(short data) {
		super.appendTag(new NBTTagShort(data));
	}

	public void append(long data) {
		super.appendTag(new NBTTagLong(data));
	}

	public void append(float data) {
		super.appendTag(new NBTTagFloat(data));
	}

	public void append(double data) {
		super.appendTag(new NBTTagDouble(data));
	}

	public void append(int[] data) {
		super.appendTag(new NBTTagIntArray(data));
	}

	public void append(byte[] data) {
		super.appendTag(new NBTTagByteArray(data));
	}

	public void append(long[] data) {
		super.appendTag(new NBTTagLongArray(data));
	}

	public void append(NBTTagList data) {
		super.appendTag(data);
	}

	public void append(NBTTagCompound data) {
		super.appendTag(data);
	}

	public static int getType(Class<? extends NBTBase> clazz) {
		if(clazz == NBTTagEnd.class) return 0;
		if(clazz == NBTTagByte.class) return 1;
		if(clazz == NBTTagShort.class) return 2;
		if(clazz == NBTTagInt.class) return 3;
		if(clazz == NBTTagLong.class) return 4;
		if(clazz == NBTTagFloat.class) return 5;
		if(clazz == NBTTagDouble.class) return 6;
		if(clazz == NBTTagByteArray.class) return 7;
		if(clazz == NBTTagString.class) return 8;
		if(clazz == NBTTagList.class) return 9;
		if(clazz == NBTTagCompound.class) return 10;
		if(clazz == NBTTagIntArray.class) return 11;
		if(clazz == NBTTagLongArray.class) return 12;
		return -1;
	}
}
