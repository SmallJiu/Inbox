package cat.jiu.email.command;

import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import cat.jiu.email.Email;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;

import java.util.Map.Entry;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import net.minecraftforge.server.command.CommandTreeBase;

class CommandEmailSendWhiteList extends CommandTreeBase {
	static final TextComponentTranslation white = new TextComponentTranslation("email.command.send.list.white");
	CommandEmailSendWhiteList() {
		super.addSubcommand(new Add());
		super.addSubcommand(new Remove());
	}
	
	public String getName() {return "whitelist";}
	public String getUsage(ICommandSender sender) {
		StringBuilder s = new StringBuilder("/email whitelist ");
		StringJoiner cmdName = new StringJoiner(", ", "<", ">");
		for(Entry<String, ICommand> cmds : super.getCommandMap().entrySet()) {
			cmdName.add(cmds.getKey());
		}
		s.append(cmdName);
		return s.toString();
	}
	
	class Add extends CommandBase {
		public String getName() {return "add";}
		public String getUsage(ICommandSender sender) {return "email.command.send.white.add";}
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if(!EmailConfigs.Send.Enable_Send_WhiteList) {
				throw new CommandException("email.command.send.list.unenable", white);
			}
			if(args.length < 1) throw new CommandException(this.getUsage(sender));
			
			String argName = args[0];
			String name = null;
			UUID uid = null;
			EntityPlayerMP player = server.getPlayerList().getPlayerByUsername(argName);
			if(player != null) {
				name = player.getName();
				uid = player.getUniqueID();
			}else {
				EmailUtils.initNameAndUUID(server);
				UUID id = EmailUtils.getUUID(argName);
				if(id != null) {
					uid = id;
					name = EmailUtils.getName(uid);
				}
			}
			if(name != null && uid != null) {
				if(Email.isInWhiteList(name) || Email.isInWhiteList(uid)) {
					throw new CommandException("email.command.send.white.already_exist", argName);
				}else {
					boolean lag = Email.addToWhiteList(name, uid);
					if(lag) {
						sender.sendMessage(new TextComponentTranslation("email.command.send.list.add.success", argName, white));
					}else {
						sender.sendMessage(new TextComponentTranslation("email.command.send.error"));
					}
				}
			}else {
				throw new CommandException("email.command.send.white.not_found_player", argName);
			}
		}
		
		@Override
		public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
			return args.length == 1 ? getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()) : Collections.emptyList();
		}
	}
	
	class Remove extends CommandBase {
		public String getName() {return "remove";}
		public String getUsage(ICommandSender sender) {return "email.command.send.white.remove";}
		public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
			if(!EmailConfigs.Send.Enable_Send_WhiteList) {
				throw new CommandException("email.command.send.white.unenable");
			}
			if(args.length < 1) throw new CommandException(this.getUsage(sender));
			
			String argName = args[0];
			if(Email.isInWhiteList(argName)) {
				boolean lag = Email.removeInWhiteList(argName);
				if(lag) {
					sender.sendMessage(new TextComponentTranslation("email.command.send.list.remove.success", argName, white));
				}else {
					sender.sendMessage(new TextComponentTranslation("email.command.send.error"));
				}
			}else {
				throw new CommandException("email.command.send.list.not_found_player", argName, white);
			}
		}
		
		@Override
		public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
			return args.length == 1 ? getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()) : Collections.emptyList();
		}
	}
}
