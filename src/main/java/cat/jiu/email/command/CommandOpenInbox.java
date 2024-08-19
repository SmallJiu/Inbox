package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.ui.GuiHandler;
import cat.jiu.email.util.EmailUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class CommandOpenInbox extends BaseCommand.Base {
    public CommandOpenInbox() {
        super("inbox", 0);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> apply(LiteralArgumentBuilder<CommandSourceStack> node) {
        return node.executes(this)
                .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(this));
    }

    @Override
    public int execute(MinecraftServer server, CommandSource sender, String[] args, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
        if (!profiles.isEmpty()){
            if (ctx.getSource().hasPermission(4)) {
                for (GameProfile profile : profiles) {
                    ServerPlayer player1 = server.getPlayerList().getPlayer(profile.getId());
                    if (player1 != null) {
                        GuiHandler.openGui(GuiHandler.EMAIL_MAIN, player1);
                    }else {
                        ctx.getSource().sendFailure(Component.translatable(ChatFormatting.RED + String.format("找不到在线玩家：%s", profile.getName())));
                    }
                }
            }else {
                ctx.getSource().sendFailure(Component.translatable(ChatFormatting.RED + "你没有所需权限。"));
            }

            return 1;
        }

        if (sender instanceof ServerPlayer p){
            GuiHandler.openGui(GuiHandler.EMAIL_MAIN, p);
            return 1;
        }

        return 1;
    }
}
