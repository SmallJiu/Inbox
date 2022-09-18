package cat.jiu.email.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;

public class ClientProxy extends ServerProxy {
	public boolean isClient() {
		return true;
	}
	@Override
	public World getClientWorld() {
		return Minecraft.getMinecraft().world;
	}
}
