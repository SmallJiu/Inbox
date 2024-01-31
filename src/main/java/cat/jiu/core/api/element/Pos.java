package cat.jiu.core.api.element;

import crafttweaker.annotations.ZenRegister;
import net.minecraft.util.math.BlockPos;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenProperty;

@ZenRegister
@ZenClass("core.Pos")
public class Pos {
	@ZenProperty
	public double x,y,z;
	public Pos(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Pos(BlockPos pos) {
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
	}

	public BlockPos toBlockPos() {
		return new BlockPos(x, y, z);
	}
	
	@ZenMethod
	public static Pos from(double x, double y, double z) {
		return new Pos(x,y,z);
	}
}
