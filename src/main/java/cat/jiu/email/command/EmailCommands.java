package cat.jiu.email.command;

import java.util.StringJoiner;
import java.util.Map.Entry;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;

import net.minecraftforge.server.command.CommandTreeBase;

public final class EmailCommands extends CommandTreeBase {
	public EmailCommands() {
		super.addSubcommand(new CommandEmailSend());
		super.addSubcommand(new CommandEmailSendBlackList());
		super.addSubcommand(new CommandEmailSendWhiteList());
		super.addSubcommand(new CommandEmailSendMsgs());
		super.addSubcommand(new CommandEmailExport());
		super.addSubcommand(new CommandEmailExecute());
		super.addSubcommand(new CommandEmailReloadCooling());
		super.addSubcommand(new CommandEmailDelete());
	}
	
	@Override
	public void addSubcommand(ICommand command) {
		if(!command.getClass().getPackage().equals(this.getClass().getPackage())) {
			throw new RuntimeException("Don't add sub-commands to /" + this.getName() + ", create your owner command !");
		}
		super.addSubcommand(command);
	}

	public String getName() {return "email";}
	public String getUsage(ICommandSender sender) {
		StringBuilder s = new StringBuilder("/email ");
		StringJoiner cmdName = new StringJoiner(", ", "<", ">");
		for(Entry<String, ICommand> cmds : super.getCommandMap().entrySet()) {
			cmdName.add(cmds.getKey());
		}
		s.append(cmdName);
		return s.toString();
	}
}
