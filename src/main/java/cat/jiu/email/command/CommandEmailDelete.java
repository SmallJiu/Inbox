package cat.jiu.email.command;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import cat.jiu.email.element.Inbox;
import cat.jiu.email.util.EmailUtils;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

class CommandEmailDelete extends CommandBase {
	public String getName() {return "delete";}
	public String getUsage(ICommandSender sender) {return "email.command.delete";}
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if(args.length < 2) throw new CommandException(this.getUsage(sender));
		Inbox inbox = Inbox.get(getPlayer(server, sender, args[0]));
		int id = parseInt(args[1]);
		if(inbox.hasEmail(id)) {
			inbox.deleteEmail(id);
			EmailUtils.saveInboxToDisk(inbox);
			sender.sendMessage(EmailUtils.createTextComponent(TextFormatting.GREEN, "email.command.delete.success", (Object[])args));
		}else {
			throw new CommandException("email.command.delete.email_not_found", id);
		}
	}
	
	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		if(args.length == 1) {
			return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
		}else if(args.length == 2) {
			try {
				Inbox inbox = Inbox.get(getPlayer(server, sender, args[0]));
				List<String> list = Lists.newLinkedList();
				for(long id : inbox.getEmailIDs()) {
					list.add(String.valueOf(id));
				}
				return list;
			}catch(CommandException e) {}
		}
		return Collections.emptyList();
	}
}
