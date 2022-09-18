package cat.jiu.email.command;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cat.jiu.email.Email;
import cat.jiu.email.util.EmailConfigs;
import cat.jiu.email.util.EmailSound;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.JsonToStackUtil;
import cat.jiu.email.util.JsonUtil;
import cat.jiu.email.util.EmailSenderSndSound.Time;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;

class CommandEmailSend extends CommandBase {
	public static final String Path = EmailUtils.getSaveEmailPath() + "type" + File.separator;
	public String getName() {return "send";}
	public int getRequiredPermissionLevel() {return 2;}
	public String getUsage(ICommandSender sender) {return "email.command.send";}

	@Override
	public void execute(MinecraftServer server, ICommandSender cmdSender, String[] args) throws CommandException {
		if(args.length < 1) {
			throw new CommandException(this.getUsage(cmdSender));
		}
		EmailFunction function = this.foundFunction(cmdSender, args[0]);
		if(function == null) {
			throw new CommandException("email.command.send.list.file_not_found", args[0]);
		}
		String addresser = function.addresser;
		if("@a".equals(addresser)) {
			for(String name : server.getOnlinePlayerNames()) {
				Email.sendCMDToPlayerEmail(function.sender, function.title, name, function.msgs, function.sound, function.items);
			}
			System.out.println("All players");
			return;
		}else if("@p".equals(addresser)) {
			if(cmdSender instanceof EntityPlayer) {
				addresser = cmdSender.getName();
			}
		}
		if(args.length >= 2) {
			if("@a".equals(addresser)) {
				for(String name : server.getOnlinePlayerNames()) {
					Email.sendCMDToPlayerEmail(function.sender, function.title, name, function.msgs, function.sound, function.items);
				}
				return;
			}else if("@p".equals(addresser)) {
				if(cmdSender instanceof EntityPlayer) {
					addresser = cmdSender.getName();
				}else {
					addresser = args[1];
				}
			}
		}
		cmdSender.sendMessage(EmailUtils.createTextComponent("info.email.send.success", TextFormatting.GREEN));
		Email.sendCMDToPlayerEmail(function.sender, function.title, addresser, function.msgs, null, function.items);
	}
	
	private EmailFunction foundFunction(ICommandSender sender, String file) throws CommandException {
		File functionFile = this.foundFile(new File(Path), file);
		if(functionFile != null) {
			JsonElement e = JsonUtil.parse(functionFile);
			if(e != null && e.isJsonObject()) {
				JsonObject function = e.getAsJsonObject();
				List<String> msgs = null;
				if(function.has("msgs")) {
					msgs = Lists.newArrayList();
					JsonElement msgE = function.get("msgs");
					if(msgE.isJsonPrimitive()) {
						msgs.add(msgE.getAsString());
					}else if(msgE.isJsonArray()) {
						JsonArray msgsArray = msgE.getAsJsonArray();
						for(int i = 0; i < msgsArray.size(); i++) {
							msgs.add(msgsArray.get(i).getAsString());
						}
					}else if(msgE.isJsonObject()) {
						for(Entry<String, JsonElement> msg : msgE.getAsJsonObject().entrySet()) {
							msgs.add(msg.getValue().getAsString());
						}
					}
				}
				
				List<ItemStack> items = null;
				if(function.has("items")) {
					JsonElement itemE = function.get("items");
					if(itemE.isJsonPrimitive() && itemE.getAsJsonPrimitive().isString()) {
						ItemStack stack = JsonToStackUtil.toStack(function.get("items"));
						if(stack!=null) {
							items = Lists.newArrayList(stack);
						}else {
							String path = function.get("items").getAsString();
							JsonElement itemsE = JsonUtil.parse(CommandEmailExport.Path + path);
							if(itemsE!=null) {
								items = JsonToStackUtil.toStacks(itemsE);
							}else {
								throw new CommandException("email.command.send.file_not_found", path);
							}
						}
					}else if(itemE.isJsonObject()) {
						items = Lists.newArrayList();
						for(Entry<String, JsonElement> item : itemE.getAsJsonObject().entrySet()) {
							if(item.getValue().isJsonPrimitive()) {
								ItemStack stack = JsonToStackUtil.toStack(item.getValue());
								if(stack!=null) {
									items.add(stack);
								}else {
									JsonElement itemsE = JsonUtil.parse(CommandEmailExport.Path + item.getValue().getAsJsonPrimitive().getAsString());
									if(itemsE!=null) {
										for(ItemStack stack0 : JsonToStackUtil.toStacks(itemsE)) {
											if(stack0!=null) {
												items.add(stack0);
											}
										}
									}
								}
							}else {
								items.add(JsonToStackUtil.toStack(item.getValue()));
							}
						}
					}else if(itemE.isJsonArray()) {
						items = Lists.newArrayList();
						for(int i = 0; i < itemE.getAsJsonArray().size(); i++) {
							JsonElement item = itemE.getAsJsonArray().get(i);
							if(item.isJsonPrimitive()) {
								ItemStack stack = JsonToStackUtil.toStack(item);
								if(stack!=null) {
									items.add(stack);
								}else {
									JsonElement itemsE = JsonUtil.parse(CommandEmailExport.Path + item.getAsJsonPrimitive().getAsString());
									if(itemsE!=null) {
										for(ItemStack stack0 : JsonToStackUtil.toStacks(itemsE)) {
											if(stack0!=null) {
												items.add(stack0);
											}
										}
									}
								}
							}else {
								ItemStack stack = JsonToStackUtil.toStack(item);
								if(stack!=null) {
									items.add(stack);
								}
							}
						}
					}
				}
				
				EmailSound sound = null;
				if(function.has("sound")) {
					sound = new EmailSound(new Time(function.get("time").getAsLong()), new SoundEvent(new ResourceLocation(function.get("name").getAsString())), function.get("volume").getAsInt(), function.get("pitch").getAsInt());
				}
				
				return new EmailFunction(
						function.get("sender").getAsString(),
						function.get("addresser").getAsString(),
						function.get("title").getAsString(),
						items, msgs, sound);
			}
		}
		
		return null;
	}
	
	private File foundFile(File file, String name) {
		if(file==null || !file.exists()) return null;
		for(File subFile : file.listFiles()) {
			if(subFile.isDirectory() && !"export".equals(subFile.getName())) {
				File f = this.foundFile(subFile, name);
				if(f != null) return f;
			}else if(subFile.getName().equals(name)) {
				return subFile;
			}
		}
		return null;
	}
	
	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		if(args.length == 1) {
			File originDir = new File(Path);
			if(originDir.exists()) {
				List<String> functions = Lists.newArrayList();
				this.getAllFunction(functions, originDir);
				return functions;
			}
		}else if(args.length == 2) {
			List<String> players = getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
			players.add("@p");
			players.add("@a");
			return players;
		}
		return Collections.emptyList();
	}
	
	private void getAllFunction(List<String> names, File file) {
		if(file.isDirectory()) {
			if(!"export".equals(file.getName())) {
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
				lag = !Email.isInBlackList(player);
			}else if(EmailConfigs.Send.Enable_Send_WhiteList) {
				lag = Email.isInWhiteList(player);
			}
		}
		return super.checkPermission(server, sender) && lag;
	}
	
	private static class EmailFunction {
		private String sender;
		private String addresser;
		private String title;
		private final List<ItemStack> items;
		private final EmailSound sound;
		private final List<String> msgs;
		private EmailFunction(String sender, String addresser, String title, List<ItemStack> items, List<String> msgs, @Nullable EmailSound sound) {
			this.sender = sender;
			this.addresser = addresser;
			this.title = title;
			this.items = items;
			this.msgs = msgs;
			this.sound = sound;
		}
	}
}
