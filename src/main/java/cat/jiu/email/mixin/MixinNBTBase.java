package cat.jiu.email.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cat.jiu.core.util.base.BaseNBT;
import cat.jiu.email.util.NBTTagNull;

import net.minecraft.nbt.NBTBase;

import net.minecraftforge.fml.common.Loader;

@Mixin(value = NBTBase.class)
public class MixinNBTBase {
	private MixinNBTBase() {
		throw new RuntimeException();
	}
	
	@Inject(
		at = {@At(value = "HEAD")},
		method = {"createNewByType(B)Lnet/minecraft/nbt/NBTBase;"},
		cancellable = true
	)
	private static void mixin_createNewByType(byte id, CallbackInfoReturnable<NBTBase> cir) {
		if(id == -1 && !(Loader.isModLoaded("jiucore") && BaseNBT.hasNBT(-1))) {
			cir.setReturnValue(new NBTTagNull());
		}
	}
	
	@Inject(
		at = {@At(value = "HEAD")},
		method = {"getTagTypeName(I)Ljava/lang/String;"},
		cancellable = true
	)
	private static void mixin_getTagTypeName(int id, CallbackInfoReturnable<String> cir) {
		if(id == -1 && !(Loader.isModLoaded("jiucore") && BaseNBT.hasNBT(-1))) {
			cir.setReturnValue("TAG_Null");
		}
	}
}
