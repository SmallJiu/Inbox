package cat.jiu.email.command;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.Text;
import cat.jiu.email.util.EmailConfigs;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

class CommandEmailSendMsgs extends CommandBase {
	public String getName() {return "sendmsg";}
	public String getUsage(ICommandSender sender) {return "email.command.send.msg";}
	public int getRequiredPermissionLevel() {return 0;}
	public void execute(MinecraftServer server, ICommandSender cmdSender, String[] args) throws CommandException {
		if(args.length < 4) {
			throw new CommandException(this.getUsage(cmdSender));
		}
		Text sender = new Text(args[0]);
		String addresser = args[1];
		Text title = new Text(args[2]);
		List<Text> msgs = Lists.newArrayList();
		for(int i = 3; i < args.length; i++) {
			msgs.add(new Text(args[i]));
		}
		EmailAPI.sendCommandEmail(addresser, new Email(title, sender, null, null, msgs));
	}
	
	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		if(args.length == 1) {
			return Lists.newArrayList(sender.getName());
		}else if(args.length == 2) {
			if(EmailConfigs.Send.Enable_Send_To_Self) {
				return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
			}
			return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()).stream().
					filter(str -> !str.equals(sender.getName()))
					.collect(Collectors.toList());
		}
		return Collections.emptyList();
	}
}
