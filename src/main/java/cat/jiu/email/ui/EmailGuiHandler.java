package cat.jiu.email.ui;

import cat.jiu.email.EmailMain;
import cat.jiu.email.ui.container.ContainerEmailMain;
import cat.jiu.email.ui.container.ContainerEmailSend;
import cat.jiu.email.ui.gui.GuiEmailMain;
import cat.jiu.email.ui.gui.GuiEmailSend;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class EmailGuiHandler implements IGuiHandler {
	public static final int EMAIL_MAIN = 0;
	public static final int EMAIL_SEND = 1;

	public EmailGuiHandler() {
		NetworkRegistry.INSTANCE.registerGuiHandler(EmailMain.MODID, this);
	}

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		switch(ID) {
			case EMAIL_MAIN: return new ContainerEmailMain();
			case EMAIL_SEND: return new ContainerEmailSend(world, player);
		}
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		switch(ID) {
			case EMAIL_MAIN: return new GuiEmailMain();
			case EMAIL_SEND: return new GuiEmailSend(world, player);
		}
		return null;
	}
}
