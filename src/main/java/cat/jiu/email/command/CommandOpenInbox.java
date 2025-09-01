package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.ui.GuiHandler;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

class CommandOpenInbox {
    public static BaseCommand.Base register(){
        return new BaseCommand.Builder("inbox")
                .level(0)
                .argument((cmd, node)-> node.executes(cmd)
                        .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(cmd))
                )
                .execute((server, sender, args, ctx) -> {
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
                })
                .build();
    }
}
