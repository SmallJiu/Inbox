package cat.jiu.core.api;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public interface ICommand extends Command<CommandSource>, Function<LiteralArgumentBuilder<CommandSource>, LiteralArgumentBuilder<CommandSource>> {
    String getName();
    List<String> getAliases();
    boolean checkPermission(CommandSource source);

    @Override
    default int run(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        String[] a = ctx.getInput().split(" ");
        int index = 0;
        for (int i = 0; i < a.length; i++) {
            if(this.getName().equals(a[i])){
                index = i;
                break;
            }
        }
        return this.execute(ctx.getSource().getServer(), ctx.getSource().source, Arrays.copyOfRange(a, index+1, a.length), ctx);
    }

    int execute(MinecraftServer server, ICommandSource sender, String[] args, CommandContext<CommandSource> ctx) throws CommandSyntaxException;

    default LiteralCommandNode<CommandSource> register(CommandDispatcher<CommandSource> dispatcher) {
        LiteralCommandNode<CommandSource> node = this.apply(Commands.literal(this.getName()).requires(this::checkPermission)).build();
        List<String> alias = this.getAliases();
        if(alias!=null && !alias.isEmpty()){
            alias.forEach(alia->dispatcher.register(Commands.literal(alia).redirect(node)));
        }
        return node;
    }

    @Override
    default LiteralArgumentBuilder<CommandSource> apply(LiteralArgumentBuilder<CommandSource> node){
        return node;
    }
}
