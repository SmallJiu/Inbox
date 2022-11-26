package cat.jiu.email.mixin;

import java.util.Map;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

public class Configuration implements IFMLLoadingPlugin {
	public Configuration() {
		try {
			MixinBootstrap.init();
			Mixins.addConfiguration("email.mixin.json");
		}catch(Exception e) {}
	}
	public String[] getASMTransformerClass() {return null;}
	public String getModContainerClass() {return null;}
	public String getSetupClass() {return null;}
	public void injectData(Map<String, Object> data) {}
	public String getAccessTransformerClass() {return null;}
}
