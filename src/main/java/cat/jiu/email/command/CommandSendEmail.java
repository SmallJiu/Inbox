package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.EmailSenderGroup;
import cat.jiu.email.util.JsonParser;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.util.Collection;

class CommandSendEmail extends BaseCommand.Base {

    public CommandSendEmail() {
        super("send", 2);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> apply(LiteralArgumentBuilder<CommandSourceStack> node) {
        return node
                .then(Commands.argument("player", new GameProfileArgument())
                .then(Commands.argument("email", new EmailFileType(EmailAPI.getGlobalDataPath() + "emails/")).executes(this)));
    }

    @Override
    public int execute(MinecraftServer server, CommandSource sender, String[] args, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Email email;
        try {
            File file = ctx.getArgument("email", File.class);
            JsonElement emailJson = JsonParser.parseThrow(file);
            email = new Email(emailJson.getAsJsonObject())
                    .setSender(new Text(sender instanceof Player ? ((Player)sender).getName().getString() : EmailMain.SYSTEM));
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }

        Collection<GameProfile> players = GameProfileArgument.getGameProfiles(ctx, "player");
        new Thread(()->{
            int result = 0;
            GameProfile currentPlayer = null;
            for (GameProfile addressee : players) {
                currentPlayer = addressee;
                if (EmailAPI.sendEmail(
                        sender instanceof Player ? EmailSenderGroup.PLAYER : EmailSenderGroup.SYSTEM,
                        addressee.getName(),
                        email.setCreateTimeToNow()
                )){
                    result++;
                }else {
                    ctx.getSource().sendFailure(Component.translatable("info.inbox.send.fail"));
                }
            }
            if (players.size() == 1) {
                ctx.getSource().sendSystemMessage(Component.translatable("info.inbox.send.success", currentPlayer.getName()));
            }else {
                ctx.getSource().sendSystemMessage(Component.translatable("inbox.command.send.success", result));
            }
        }).start();

        return 1;
    }
}
