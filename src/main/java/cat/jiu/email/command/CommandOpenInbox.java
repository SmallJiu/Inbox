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
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ICommandSource;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class CommandOpenInbox extends BaseCommand.Base {
    public CommandOpenInbox() {
        super("inbox", 0);
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> apply(LiteralArgumentBuilder<CommandSource> node) {
        return node.executes(this)
                .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(this));
    }

    @Override
    public int execute(MinecraftServer server, ICommandSource sender, String[] args, CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "player");
        if (!profiles.isEmpty()){
            if (ctx.getSource().hasPermissionLevel(4)) {
                for (GameProfile profile : profiles) {
                    ServerPlayerEntity player1 = server.getPlayerList().getPlayerByUUID(profile.getId());
                    if (player1 != null) {
                        GuiHandler.openGui(GuiHandler.EMAIL_MAIN, player1);
                    }else {
                        ctx.getSource().sendErrorMessage(new TranslationTextComponent(TextFormatting.RED + String.format("找不到在线玩家：%s", profile.getName())));
                    }
                }
            }else {
                ctx.getSource().sendErrorMessage(new TranslationTextComponent(TextFormatting.RED + "你没有所需权限。"));
            }

            return 1;
        }

        if (sender instanceof ServerPlayerEntity){
            GuiHandler.openGui(GuiHandler.EMAIL_MAIN, (ServerPlayerEntity)sender);
            return 1;
        }

        return 1;
    }
}
