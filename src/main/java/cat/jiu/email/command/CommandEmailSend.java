package cat.jiu.email.command;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.EmailFunction;
import cat.jiu.email.element.Text;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonUtil;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

class CommandEmailSend extends CommandBase {
	public String getName() {return "send";}
	public String getUsage(ICommandSender sender) {return "email.command.send";}

	@Override
	public void execute(MinecraftServer server, ICommandSender cmdSender, String[] args) throws CommandException {
		if(args.length < 1) {
			throw new CommandException(this.getUsage(cmdSender));
		}
		EmailFunction function = EmailUtils.findFunction(args[0]);
		if(function == null) {
			try {
				throw new CommandException("email.command.send.list.file_not_found", new File("./emails/export/" + args[0]).getCanonicalPath());
			}catch(IOException e) {
				throw new CommandException(e.getLocalizedMessage());
			}
		}
		if(cmdSender instanceof EntityPlayer) {
			function.sender = new Text(cmdSender.getName());
		}
		String addresser = function.addresser;
		if("@a".equals(addresser)) {
			cmdSender.sendMessage(EmailUtils.createTextComponent("email.command.send.ing", TextFormatting.YELLOW));
			new Thread(()->{
				for(String name : EmailUtils.getAllName()) {
					replaceAbstract(name, function.msgs);
					EmailAPI.sendCommandEmail(name, function.toEmail());
				}
				cmdSender.sendMessage(EmailUtils.createTextComponent("email.command.send.success.all", TextFormatting.GREEN));
			}).start();
			return;
		}else if("@a-online".equals(addresser)) {
			cmdSender.sendMessage(EmailUtils.createTextComponent("email.command.send.ing", TextFormatting.YELLOW));
			new Thread(()->{
				for(String name : server.getOnlinePlayerNames()) {
					replaceAbstract(name, function.msgs);
					EmailAPI.sendCommandEmail(name, function.toEmail());
				}
				cmdSender.sendMessage(EmailUtils.createTextComponent("email.command.send.success.all.online", TextFormatting.GREEN));
			}).start();
			return;
		}else if("@p".equals(addresser)) {
			if(args!=null && args.length >= 2) {
				String argAddrsser = args[1];
				if(EmailUtils.hasNameOrUUID(argAddrsser)) {
					addresser = argAddrsser;
				}
			}else {
				if(cmdSender instanceof EntityPlayer) {
					addresser = cmdSender.getName();
				}else {
					throw new CommandException("email.command.send.error.abstract_name");
				}
			}
		}
		cmdSender.sendMessage(EmailUtils.createTextComponent("info.email.send.success", TextFormatting.GREEN));
		EmailAPI.sendCommandEmail(addresser, function.toEmail());
	}
	
	private void replaceAbstract(String name, List<Text> msgs) {
		for(Text msg : msgs) {
			Object[] args = msg.getArgs();
			for(int i = 0; i < args.length; i++) {
				Object arg = args[i];
				System.out.println(msg.getKey() + " | " + arg);
				if("@a".equals(arg) || "@a-online".equals(arg)) {
					args[i] = name;
				}
			}
		}
	}
	
	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		if(args.length == 1) {
			File originDir = new File(EmailUtils.typePath);
			if(originDir.exists()) {
				List<String> functions = Lists.newArrayList();
				this.getAllFunction(functions, originDir);
				return functions;
			}
		}else if(args.length == 2) {
			List<String> players = getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
			players.add("@p");
			players.add("@a");
			players.add("@a-online");
			return players;
		}
		return Collections.emptyList();
	}
	
	private void getAllFunction(List<String> names, File file) {
		if(file.isDirectory()) {
			if(!"export".equals(file.getName()) && !"event".equals(file.getName())) {
				for(File subFile : file.listFiles()) {
					this.getAllFunction(names, subFile);
				}
			}
		}else {
			JsonElement e = JsonUtil.parse(file);
			if(e != null && e.isJsonObject()) {
				JsonObject o = e.getAsJsonObject();
				if(o.has("sender")
				&& o.has("addresser")
				&& o.has("title")
				&& (o.has("items") || o.has("msgs"))) {
					names.add(file.getName());
				}
			}
		}
	}
	
	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		boolean lag = true;
		if(sender instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) sender;
			if(EmailConfigs.Send.Enable_Send_BlackList) {
				lag = !EmailAPI.isInBlackList(player);
			}else if(EmailConfigs.Send.Enable_Send_WhiteList) {
				lag = EmailAPI.isInWhiteList(player);
			}
		}
		return super.checkPermission(server, sender) && lag;
	}
}
