package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.element.ScheduledEmail;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;

public class CommandScheduledEmail extends BaseCommand.BaseTree {

    public CommandScheduledEmail() {
        super("scheduled");
        this.addSubCommand(new Load());
        this.addSubCommand(new Reload());
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
                ScheduledEmail.updataScheduledEmail();
                ctx.getSource().sendFeedback(new TranslationTextComponent("Success!"), false);
            }
            return 1;
        }
    }

    static class Reload extends BaseCommand.Base {
        public Reload() {
            super("reload", 4);
        }

        @Override
        public LiteralArgumentBuilder<CommandSource> apply(LiteralArgumentBuilder<CommandSource> node) {
            return node.executes(this);
        }

        @Override
        public int execute(MinecraftServer server, ICommandSource sender, String[] args, CommandContext<CommandSource> ctx) throws CommandSyntaxException {
            if (ctx.getSource().hasPermissionLevel(4)) {
                ScheduledEmail.initScheduledEmail();
                ctx.getSource().sendFeedback(new TranslationTextComponent("Success!"), false);
            }
            return 1;
        }
    }
}
