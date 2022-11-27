package cat.jiu.email.command;

import cat.jiu.email.element.Cooling;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class CommandEmailReloadCooling extends CommandBase {
	public String getName() {return "cooling_reload";}
	public String getUsage(ICommandSender sender) {return "email.command.send.cooling";}
	public void execute(MinecraftServer server, ICommandSender cmdSender, String[] args) throws CommandException {
		Cooling.load();
	}
}
