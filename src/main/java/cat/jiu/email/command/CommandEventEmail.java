package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.element.EventEmail;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.io.File;

public class CommandEventEmail extends BaseCommand.BaseTree {

    public CommandEventEmail() {
        super("event");
        this.addSubCommand(new Load());
        this.addSubCommand(new Add());
    }

    static class Load extends BaseCommand.Base {
        public Load() {
            super("load", 4);
        }

        @Override
        public LiteralArgumentBuilder<CommandSource> apply(LiteralArgumentBuilder<CommandSource> node) {
            return node.executes(this);
        }

        @Override
        public int execute(MinecraftServer server, ICommandSource sender, String[] args, CommandContext<CommandSource> ctx) throws CommandSyntaxException {
            if (ctx.getSource().hasPermissionLevel(4)) {
                EventEmail.load();
                ctx.getSource().sendFeedback(ITextComponent.getTextComponentOrEmpty("Success!"), false);
            }
            return 1;
        }
    }

    static class Add extends BaseCommand.Base {
        public Add() {
            super("add", 4);
        }

        @Override
        public LiteralArgumentBuilder<CommandSource> apply(LiteralArgumentBuilder<CommandSource> node) {
            return node
                    .then(Commands.argument("event", new EmailEventType())
                    .then(Commands.argument("email", new EmailFileType()).executes(this)));
        }

        @Override
        public int execute(MinecraftServer server, ICommandSource sender, String[] args, CommandContext<CommandSource> ctx) throws CommandSyntaxException {
            if (ctx.getSource().hasPermissionLevel(4)) {
                ResourceLocation e = ctx.getArgument("event", ResourceLocation.class);
                String f = ctx.getArgument("email", File.class).getName();
                EventEmail.register(e,f);
                ctx.getSource().sendFeedback(
                        new StringTextComponent(TextFormatting.GREEN + "success")
                                .appendString(". event: ")
                                .appendString(TextFormatting.GREEN + String.valueOf(e))
                                .appendString(", file: ")
                                .appendString(TextFormatting.GREEN + f),
                        false);
            }else {
                ctx.getSource().sendErrorMessage(new StringTextComponent(TextFormatting.RED + "you are not op."));
            }
            return 1;
        }
    }
}
