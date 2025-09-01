package cat.jiu.email.command;

import cat.jiu.core.util.JsonUtils;
import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.configs.EmailConfigServer;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.EmailSenderGroup;
import cat.jiu.email.util.SendDevEmail;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.util.Collection;

class CommandSendEmail {
    public static BaseCommand.Base register() {
        return new BaseCommand.Builder("send")
                .argument((cmd, node)->node
                        .then(Commands.argument("player", new GameProfileArgument())
                                .then(Commands.argument("email", new EmailFileType())
                                        .executes(cmd)
                                )
                        ))
                .execute((server, sender, args, ctx) -> {
                    Email email;
                    try {
                        File file = ctx.getArgument("email", File.class);

                        if (EmailFileType.DEFAULT_EMAIL.equals(file)) {
                            email = SendDevEmail.getDevEmail();
                        }else {
                            JsonElement emailJson = JsonUtils.parseThrow(file, EmailConfigServer.File_Charset.get());
                            email = new Email(emailJson.getAsJsonObject())
                                    .setSender(new Text(sender instanceof Player ? ((Player)sender).getName().getString() : EmailMain.SYSTEM));
                        }
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
                })
                .build();
    }
}
