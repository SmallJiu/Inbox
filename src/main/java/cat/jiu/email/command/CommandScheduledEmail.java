package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.element.ScheduledEmail;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

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
        public LiteralArgumentBuilder<CommandSourceStack> apply(LiteralArgumentBuilder<CommandSourceStack> node) {
            return node.executes(this);
        }

        @Override
        public int execute(MinecraftServer server, CommandSource sender, String[] args, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
            if (ctx.getSource().hasPermission(4)) {
                ScheduledEmail.updataScheduledEmail();
                sender.sendSystemMessage(Component.translatable("Success!"));
            }
            return 1;
        }
    }

    static class Reload extends BaseCommand.Base {
        public Reload() {
            super("reload", 4);
        }

        @Override
        public LiteralArgumentBuilder<CommandSourceStack> apply(LiteralArgumentBuilder<CommandSourceStack> node) {
            return node.executes(this);
        }

        @Override
        public int execute(MinecraftServer server, CommandSource sender, String[] args, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
            if (ctx.getSource().hasPermission(4)) {
                ScheduledEmail.initScheduledEmail();
                sender.sendSystemMessage(Component.translatable("Success!"));
            }
            return 1;
        }
    }
}
