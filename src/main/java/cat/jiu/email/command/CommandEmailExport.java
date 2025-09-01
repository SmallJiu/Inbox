package cat.jiu.email.command;

import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.core.util.JsonToStackUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

class CommandEmailExport  {
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    public static BaseCommand.Base register(){
        return new BaseCommand.Builder("export")
                .argument((cmd, node) -> node.then(Commands.argument("inventory", BoolArgumentType.bool()).executes(cmd)))
                .execute((server, sender, args, ctx) -> {
                    if (sender instanceof Player) {
                        String name = dateFormat.format(new Date()) + ".json";
                        if (!BoolArgumentType.getBool(ctx, "inventory")) {
                            long time = System.currentTimeMillis();
                            JsonObject stacks = new JsonObject();
                            stacks.add("mainhand", JsonToStackUtil.toJson(((Player) sender).getMainHandItem()));
                            stacks.add("offhand", JsonToStackUtil.toJson(((Player) sender).getOffhandItem()));

                            if (stacks.get("mainhand").isJsonNull() && stacks.get("offhand").isJsonNull()) {
                                throw new SimpleCommandExceptionType(Component.translatable("clear.failed.single", ((Player) sender).getName())).create();
                            }
                            String path = EmailAPI.getGlobalDataPath() + "export";
                            String file = path + File.separator + name;
                            JsonUtils.toJsonFile(file, stacks, false, EmailConfigServer.File_Charset.get());
                            ctx.getSource().sendSuccess(()->
                                            openOnClickedText(Component.translatable(String.format("已导出主副手物品至文件夹, 耗时 %s 毫秒，文件名：%s", System.currentTimeMillis() - time, name)), path),
                                    false);
                        } else {
                            long time = System.currentTimeMillis();
                            JsonArray stacks = new JsonArray();
                            ((Player) sender).inventoryMenu.getItems().forEach(stack -> {
                                if (!stack.isEmpty()) {
                                    stacks.add(JsonToStackUtil.toJson(stack));
                                }
                            });
                            if (stacks.isEmpty()) {
                                throw new SimpleCommandExceptionType(Component.translatable("clear.failed.single", ((Player) sender).getName())).create();
                            }
                            String path = EmailAPI.getGlobalDataPath() + "export" + File.separator + "inventory";
                            String file = path + File.separator + name;
                            JsonUtils.toJsonFile(file, stacks, false, EmailConfigServer.File_Charset.get());
                            ctx.getSource().sendSuccess(()->
                                            openOnClickedText(Component.translatable(String.format("已导出背包物品至文件夹, 耗时 %s 毫秒，文件名：%s", System.currentTimeMillis() - time, name)), path),
                                    false);
                        }
                        return 1;
                    }
                    throw CommandSourceStack.ERROR_NOT_PLAYER.create();
                })
                .build();
    }

    public static MutableComponent openOnClickedText(Component component, String path) {
        String pText = component.getString();
        return ComponentUtils.wrapInSquareBrackets(Component.literal(pText).withStyle((style) ->
                style.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, path)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Open on folder."))).withInsertion(pText)
        ));
    }
}
