package cat.jiu.email.command;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonToStackUtil;
import cat.jiu.email.util.JsonUtil;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

class CommandEmailExport extends CommandBase {
	public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
	public String getName() {return "export";}
	public String getUsage(ICommandSender sender) {return "/email export [all]";}
	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if(!(sender instanceof EntityPlayer)) {
			throw new CommandException("email.command.export.only_player");
		}
		
		EntityPlayer player = (EntityPlayer) sender;
		if(args.length >= 1 && args[0].equals("all")) {
			if(player.inventory.getSizeInventory() < 1) {
				throw new CommandException("email.command.export.all.empty_item");
			}
			JsonArray stacks = new JsonArray();
			for(int i = 0; i < player.inventory.getSizeInventory(); i++) {
				ItemStack stack = player.inventory.getStackInSlot(i);
				if(!stack.isEmpty()) {
					stacks.add(JsonToStackUtil.toJson(stack));
				}
			}
			String path = EmailAPI.getExportPath() + "inventory"  + File.separator + dateFormat.format(new Date()) + ".json";
			JsonUtil.toJsonFile(path, stacks, false);
			try {
				player.sendMessage(EmailUtils.createTextComponent("email.command.export.all.success", TextFormatting.GREEN, new File(path).getCanonicalPath()));
			}catch(IOException e) {
				throw new CommandException(e.getLocalizedMessage());
			}
		}else {
			ItemStack stack = player.getHeldItemMainhand();
			if(stack.isEmpty()) {
				throw new CommandException("email.command.export.empty_item");
			}
			ResourceLocation name = stack.getItem().getRegistryName();
			String path = EmailAPI.getExportPath() + name.getResourceDomain() + "@" + name.getResourcePath() + File.separator + dateFormat.format(new Date()) + ".json";
			JsonUtil.toJsonFile(path, JsonToStackUtil.toJson(stack), false);
			try {
				player.sendMessage(EmailUtils.createTextComponent("email.command.export.success", TextFormatting.GREEN, name.toString(), new File(path).getCanonicalPath()));
			}catch(IOException e) {
				throw new CommandException(e.getLocalizedMessage());
			}
		}
	}
	
	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		return args.length == 1 ? Lists.newArrayList("all") : Collections.emptyList();
	}
}
