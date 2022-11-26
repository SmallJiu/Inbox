package cat.jiu.email.util;

import java.io.DataInput;
import java.io.DataOutput;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTSizeTracker;

public class NBTTagNull extends NBTBase {
	public void read(DataInput input, int depth, NBTSizeTracker tracker) {tracker.read(8L);}
	public void write(DataOutput output) {}
	public String toString() {return "NULL";}
	public byte getId() {return -1;}
	public NBTTagNull copy() {return new NBTTagNull();}
}
