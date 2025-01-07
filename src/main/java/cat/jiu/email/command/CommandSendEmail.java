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
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ICommandSource;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;

import java.io.File;

class CommandSendEmail extends BaseCommand.Base {

    public CommandSendEmail() {
        super("send", 4);
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> apply(LiteralArgumentBuilder<CommandSource> node) {
        return node.then(Commands.argument("player", new GameProfileArgument())
                .then(Commands.argument("email", new EmailFileType()).executes(this)));
    }

    @Override
    public int execute(MinecraftServer server, ICommandSource sender, String[] args, CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        for (GameProfile addressee : GameProfileArgument.getGameProfiles(ctx, "player")) {
            String file = EmailAPI.getGlobalDataPath() + "emails/" + args[1];
            JsonElement emailJson = JsonParser.parse(ctx.getArgument("email", File.class));
            if (emailJson == null || !emailJson.isJsonObject()) {
                ctx.getSource().sendFeedback(new TranslationTextComponent("file are not a json object."), false);
                ctx.getSource().sendFeedback(new TranslationTextComponent(String.format("%s: %s", file, emailJson)), false);
                return 0;
            }
            if (EmailAPI.sendEmail(
                    sender instanceof PlayerEntity ? EmailSenderGroup.PLAYER : EmailSenderGroup.SYSTEM,
                    addressee.getName(),
                    new Email(emailJson.getAsJsonObject())
                            .setSender(new Text(sender instanceof PlayerEntity ? ((PlayerEntity)sender).getName().getString() : EmailMain.SYSTEM))
                            .setCreateTimeToNow()
            )){
                ctx.getSource().sendFeedback(new TranslationTextComponent("info.inbox.send.success", addressee.getName()), false);
            }else {
                ctx.getSource().sendErrorMessage(new TranslationTextComponent("info.inbox.send.fail"));
            }
        }
        return 1;
    }
}
