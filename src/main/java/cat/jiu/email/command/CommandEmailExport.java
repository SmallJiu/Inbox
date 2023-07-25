package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.util.JsonParser;
import cat.jiu.email.util.JsonToStackUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;

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
    public LiteralArgumentBuilder<CommandSource> apply(LiteralArgumentBuilder<CommandSource> node) {
        return node.executes(this)
                .then(Commands.literal("inventory").executes(this));
    }

    @Override
    public int execute(MinecraftServer server, ICommandSource sender, String[] args, CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        if (sender instanceof PlayerEntity) {
            if (args.length == 0) {
                long time = System.currentTimeMillis();
                JsonObject stacks = new JsonObject();
                stacks.add("mainhand", JsonToStackUtil.toJson(((PlayerEntity) sender).getHeldItemMainhand()));
                stacks.add("offhand", JsonToStackUtil.toJson(((PlayerEntity) sender).getHeldItemOffhand()));
                String path = EmailAPI.getExportPath() + File.separator + dateFormat.format(new Date()) + ".json";
                JsonParser.toJsonFile(path, stacks, false);
                ctx.getSource().sendFeedback(new StringTextComponent(String.format("已导出主副手物品至 %s, 耗时 %s毫秒", path, System.currentTimeMillis() - time)), false);
            } else {
                long time = System.currentTimeMillis();
                JsonArray stacks = new JsonArray();
                for (int i = 0; i < ((PlayerEntity) sender).inventory.getSizeInventory(); i++) {
                    ItemStack stack = ((PlayerEntity) sender).inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        stacks.add(JsonToStackUtil.toJson(stack));
                    }
                }
                String path = EmailAPI.getExportPath() + "inventory" + File.separator + dateFormat.format(new Date()) + ".json";
                JsonParser.toJsonFile(path, stacks, false);
                ctx.getSource().sendFeedback(new StringTextComponent(String.format("已导出背包物品至 %s, 耗时 %s 毫秒", path, System.currentTimeMillis() -time)), false);
            }
        }
        return 1;
    }
}
