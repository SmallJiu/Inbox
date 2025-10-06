package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.ui.GuiHandler;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

class CommandOpenInbox {
    public static BaseCommand.Base register(){
        return new BaseCommand.Builder("inbox")
                .level(0)
                .argument((cmd, node)-> node.executes(cmd)
                        .then(Commands.argument("player", GameProfileArgument.gameProfile()).executes(cmd))
                )
                .execute((server, sender, args, ctx) -> {
                    try {
                        if (ctx.getSource().hasPermission(4)) {
                            for (GameProfile profile : GameProfileArgument.getGameProfiles(ctx, "player")) {
                                ServerPlayer player = server.getPlayerList().getPlayer(profile.getId());
                                if (player != null) {
                                    GuiHandler.openGui(GuiHandler.EMAIL_MAIN, player);
                                } else {
                                    ctx.getSource().sendFailure(Component.translatable(ChatFormatting.RED + String.format("找不到在线玩家：%s", profile.getName())));
                                }
                            }
                        } else {
                            ctx.getSource().sendFailure(Component.translatable(ChatFormatting.RED + "你没有所需权限。"));
                        }
                    } catch (Exception e) {
                        GuiHandler.openGui(GuiHandler.EMAIL_MAIN, ctx.getSource().getPlayer());
                    }
                    return 1;
                })
                .build();
    }
}
