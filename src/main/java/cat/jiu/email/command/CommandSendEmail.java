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

class CommandSendEmail extends BaseCommand.Base {

    public CommandSendEmail() {
        super("send", 4);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> apply(LiteralArgumentBuilder<CommandSourceStack> node) {
        return node
                .then(Commands.argument("player", new GameProfileArgument())
                .then(Commands.argument("email", new EmailFileType()).executes(this)));
    }

    @Override
    public int execute(MinecraftServer server, CommandSource sender, String[] args, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        for (GameProfile addressee : GameProfileArgument.getGameProfiles(ctx, "player")) {
            String file = EmailAPI.getGlobalDataPath()+ "emails/" + args[1];
            File f = ctx.getArgument("email", File.class);
            JsonElement emailJson = JsonParser.parse(f);
            if (emailJson == null || !emailJson.isJsonObject()) {
                ctx.getSource().sendFailure(Component.translatable("file are not a json object."));
                ctx.getSource().sendFailure(Component.translatable(String.format("%s: %s", file, emailJson)));
                return 0;
            }
            if (EmailAPI.sendEmail(
                    sender instanceof Player ? EmailSenderGroup.PLAYER : EmailSenderGroup.SYSTEM,
                    addressee.getName(),
                    new Email(emailJson.getAsJsonObject())
                            .setSender(new Text(sender instanceof Player ? ((Player)sender).getName().getString() : EmailMain.SYSTEM))
                            .setCreateTimeToNow()
            )){
                ctx.getSource().sendSystemMessage(Component.translatable("info.inbox.send.success", addressee.getName()));
            }else {
                ctx.getSource().sendFailure(Component.translatable("info.inbox.send.fail"));
            }
        }
        return 1;
    }
}
