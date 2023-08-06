package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.util.JsonParser;
import cat.jiu.email.util.JsonToStackUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

class CommandEmailExport extends BaseCommand.Base {
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    public CommandEmailExport() {
        super("export", 3);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> apply(LiteralArgumentBuilder<CommandSourceStack> node) {
        return node.executes(this)
                .then(Commands.literal("inventory").executes(this));
    }

    @Override
    public int execute(MinecraftServer server, CommandSource sender, String[] args, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (sender instanceof Player) {
            if (args.length == 0) {
                long time = System.currentTimeMillis();
                JsonObject stacks = new JsonObject();
                stacks.add("mainhand", JsonToStackUtil.toJson(((Player) sender).getMainHandItem()));
                stacks.add("offhand", JsonToStackUtil.toJson(((Player) sender).getOffhandItem()));
                String path = EmailAPI.getExportPath() + File.separator + dateFormat.format(new Date()) + ".json";
                JsonParser.toJsonFile(path, stacks, false);
                ctx.getSource().sendSuccess(()->Component.translatable(String.format("已导出主副手物品至 %s, 耗时 %s毫秒", path, System.currentTimeMillis() - time)), false);
            } else {
                long time = System.currentTimeMillis();
                JsonArray stacks = new JsonArray();
                ((Player) sender).inventoryMenu.getItems().forEach(stack -> {
                    if (!stack.isEmpty()) {
                        stacks.add(JsonToStackUtil.toJson(stack));
                    }
                });
                String path = EmailAPI.getExportPath() + "inventory" + File.separator + dateFormat.format(new Date()) + ".json";
                JsonParser.toJsonFile(path, stacks, false);
                ctx.getSource().sendSuccess(()->Component.translatable(String.format("已导出背包物品至 %s, 耗时 %s 毫秒", path, System.currentTimeMillis() -time)), false);
            }
        }
        return 1;
    }
}
