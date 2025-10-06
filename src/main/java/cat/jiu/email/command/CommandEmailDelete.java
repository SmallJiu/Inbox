package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;

import cat.jiu.email.element.Inbox;
import cat.jiu.email.util.EmailUtils;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.UUID;

class CommandEmailDelete {
    public static BaseCommand.Base register(){
        return new BaseCommand.Builder("delete")
                .argument((cmd, node) -> node
                        .then(Commands.argument("player", StringArgumentType.word())
                                .then(Commands.argument("email", LongArgumentType.longArg(0))
                                        .executes(cmd)
                        ))
                )
                .execute(((server, sender, args, ctx) -> {
                    final String player = ctx.getArgument("player", String.class);
                    final long email = ctx.getArgument("email", Long.class);

                    String id = player;
                    try{
                        UUID uuid = UUID.fromString(id);
                        if(EmailUtils.hasUUID(uuid)){
                            id = uuid.toString();
                        }
                    }catch (Exception e){
                        if(EmailUtils.hasName(id)){
                            id = EmailUtils.getUUID(id).toString();
                        }
                    }
                    Inbox inbox = Inbox.get(id);
                    if(!inbox.isEmptyInbox()) {
                        if(inbox.hasEmail(email)){
                            inbox.deleteEmail(email);
                            EmailUtils.saveInboxToDisk(inbox);
                            ctx.getSource().sendSuccess(()->Component.translatable(String.format(ChatFormatting.GREEN + "已删除 %s 的邮箱中ID为 %s 的邮件.", player, email)), false);
                        }else {
                            ctx.getSource().sendFailure(Component.translatable(String.format(ChatFormatting.YELLOW + "在 %s 的邮箱中找不到ID为 %s 的邮件.", player, email)));
                        }
                    }else {
                        ctx.getSource().sendFailure(Component.translatable(ChatFormatting.RED + String.format("无法找到 '%s' 的邮箱", id)));
                    }
                    return 1;
                }))
                .build();
    }
}
