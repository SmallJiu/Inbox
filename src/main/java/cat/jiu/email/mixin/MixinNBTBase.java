package cat.jiu.email.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cat.jiu.email.util.NBTTagNull;

import net.minecraft.nbt.NBTBase;

@Mixin(value = NBTBase.class)
public class MixinNBTBase {
	private MixinNBTBase() {
		throw new RuntimeException();
	}
	
	@Mutable
	@Final
	@Shadow
	private static String[] NBT_TYPES;
	
	@Inject(
		at = {@At(value = "RETURN")},
		method = {"<clinit>()V"})
	private static void mixin_setNBTTypes(CallbackInfo ci) {
		String[] old = NBT_TYPES;
		NBT_TYPES = new String[NBT_TYPES.length+1];
		for(int i = 0; i < old.length; i++) {
			NBT_TYPES[i] = old[i];
		}
		NBT_TYPES[old.length] = "NULL";
	}
	
	@Inject(
		at = {@At(value = "HEAD")},
		method = {"createNewByType(B)Lnet/minecraft/nbt/NBTBase;"},
		cancellable = true
	)
	private static void mixin_createNewByType(byte id, CallbackInfoReturnable<NBTBase> cir) {
		if(id == -1) {
			cir.setReturnValue(new NBTTagNull());
		}
	}
	
	@Inject(
		at = {@At(value = "HEAD")},
		method = {"getTagTypeName(I)Ljava/lang/String;"},
		cancellable = true
	)
	private static void mixin_getTagTypeName(int id, CallbackInfoReturnable<String> cir) {
		if(id == -1) {
			cir.setReturnValue("TAG_NULL");
		}
	}
}
