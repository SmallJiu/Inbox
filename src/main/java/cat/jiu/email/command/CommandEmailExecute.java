package cat.jiu.email.command;

import cat.jiu.email.util.EmailExecuteEvent;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

class CommandEmailExecute extends CommandBase {
	public String getName() {return "event_reload";}
	public String getUsage(ICommandSender sender) {return "email.command.send.event";}
	public void execute(MinecraftServer server, ICommandSender cmdSender, String[] args) throws CommandException {
		EmailExecuteEvent.init();
	}
}
