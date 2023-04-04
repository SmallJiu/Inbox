package cat.jiu.email.proxy;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.world.World;

import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

public class ClientProxy extends ServerProxy {
	public static final KeyBinding REFESH_INBOX = new KeyBinding("email.key.refresh_inbox", KeyConflictContext.GUI, KeyModifier.NONE, Keyboard.KEY_F5, "email.keys");

	public boolean isClient() {
		return true;
	}
	@Override
	public World getClientWorld() {
		return Minecraft.getMinecraft().world;
	}
	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
		ClientRegistry.registerKeyBinding(REFESH_INBOX);
	}
}
