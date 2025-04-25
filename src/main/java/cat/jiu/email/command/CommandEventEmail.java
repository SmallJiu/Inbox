package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.EventEmail;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.io.File;

public class CommandEventEmail extends BaseCommand.BaseTree {

    public CommandEventEmail() {
        super("event");
        this.addSubCommand(new Load());
        this.addSubCommand(new Add());
    }

    static class Load extends BaseCommand.Base {
        public Load() {
            super("load", 2);
        }

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> apply(LiteralArgumentBuilder<CommandSourceStack> node) {
            return node.executes(this);
        }

        @Override
        public int execute(MinecraftServer server, CommandSource sender, String[] args, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
            if (ctx.getSource().hasPermission(4)) {
                EventEmail.load();
                sender.sendSystemMessage(Component.translatable("Success!"));
            }
            return 1;
        }
    }

    static class Add extends BaseCommand.Base {
        public Add() {
            super("add", 2);
        }

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> apply(LiteralArgumentBuilder<CommandSourceStack> node) {
            return node
                    .then(Commands.argument("event", new EmailEventType())
                    .then(Commands.argument("email", new EmailFileType(EmailAPI.getGlobalDataPath() + "emails/")).executes(this)));
        }

        @Override
        public int execute(MinecraftServer server, CommandSource sender, String[] args, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
            if (ctx.getSource().hasPermission(4)) {
                ResourceLocation e = ctx.getArgument("event", ResourceLocation.class);
                String f = ctx.getArgument("email", File.class).getName();
                EventEmail.register(e,f);
                ctx.getSource().sendSystemMessage(
                        Component.literal(ChatFormatting.GREEN + "success")
                                .append(". event: ")
                                .append(ChatFormatting.GREEN + String.valueOf(e))
                                .append(", file: ")
                                .append(ChatFormatting.GREEN + f)
                );
            }else {
                ctx.getSource().sendFailure(Component.literal(ChatFormatting.RED + "you are not op."));
            }
            return 1;
        }
    }
}
