package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.util.JsonParser;
import cat.jiu.email.util.JsonToStackUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

class CommandEmailExport extends BaseCommand.Base {
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    public CommandEmailExport() {
        super("export", 2);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> apply(LiteralArgumentBuilder<CommandSourceStack> node) {
        return node.then(Commands.argument("inventory", BoolArgumentType.bool()).executes(this));
    }

    @Override
    public int execute(MinecraftServer server, CommandSource sender, String[] args, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (sender instanceof Player) {
            String name = dateFormat.format(new Date()) + ".json";
            if (!BoolArgumentType.getBool(ctx, "inventory")) {
                long time = System.currentTimeMillis();
                JsonObject stacks = new JsonObject();
                stacks.add("mainhand", JsonToStackUtil.toJson(((Player) sender).getMainHandItem()));
                stacks.add("offhand", JsonToStackUtil.toJson(((Player) sender).getOffhandItem()));

                if (stacks.get("mainhand").isJsonNull() && stacks.get("offhand").isJsonNull()) {
                    throw new SimpleCommandExceptionType(Component.translatable("clear.failed.single", ((Player) sender).getScoreboardName())).create();
                }
                String path = EmailAPI.getGlobalDataPath() + "export";
                String file = path + File.separator + name;
                JsonParser.toJsonFile(file, stacks, false);
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
                    throw new SimpleCommandExceptionType(Component.translatable("clear.failed.single", ((Player) sender).getScoreboardName())).create();
                }
                String path = EmailAPI.getGlobalDataPath() + "export" + File.separator + "inventory";
                String file = path + File.separator + name;
                JsonParser.toJsonFile(file, stacks, false);
                ctx.getSource().sendSuccess(()->
                        openOnClickedText(Component.translatable(String.format("已导出背包物品至文件夹, 耗时 %s 毫秒，文件名：%s", System.currentTimeMillis() - time, name)), path),
                        false);
            }
        }else {
            throw new SimpleCommandExceptionType(Component.translatable("permissions.requires.player")).create();
        }
        return 1;
    }

    public static MutableComponent openOnClickedText(Component component, String path) {
        String pText = component.getString();
        return ComponentUtils.wrapInSquareBrackets(Component.literal(pText).withStyle((style) ->
                style.withColor(ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, path)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Open on folder."))).withInsertion(pText)
        ));
    }
}
