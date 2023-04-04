package cat.jiu.email.proxy;

import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

public class ServerProxy {
	public Side getSide() {
		return this.isClient() ? Side.CLIENT : Side.SERVER;
	}
	
	public ClientProxy getAsClientProxy() {
		return (ClientProxy) this;
	}
	public boolean isClient() {return false;}
	public World getClientWorld() {return null;}
	public void init(FMLInitializationEvent event) {}
}
