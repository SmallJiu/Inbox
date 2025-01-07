package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;

import cat.jiu.email.element.Inbox;
import cat.jiu.email.util.EmailUtils;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ICommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.UUID;

class CommandEmailDelete extends BaseCommand.Base {
    public CommandEmailDelete() {
        super("delete", 4);
    }

    @Override
    public LiteralArgumentBuilder<CommandSource> apply(LiteralArgumentBuilder<CommandSource> node) {
        return node.then(Commands.argument("player", StringArgumentType.word())
                .then(Commands.argument("email", LongArgumentType.longArg(0))
                        .executes(this))
        );
    }

    @Override
    public int execute(MinecraftServer server, ICommandSource sender, String[] args, CommandContext<CommandSource> ctx) throws CommandSyntaxException {
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
                ctx.getSource().sendFeedback(new TranslationTextComponent(String.format(TextFormatting.GREEN + "已删除 %s 的邮箱中ID为 %s 的邮件.", player, email)), false);
            }else {
                ctx.getSource().sendErrorMessage(new TranslationTextComponent(String.format(TextFormatting.YELLOW + "在 %s 的邮箱中找不到ID为 %s 的邮件.", player, email)));
            }
        }else {
            ctx.getSource().sendErrorMessage(new TranslationTextComponent(TextFormatting.RED + String.format("无法找到 '%s' 的邮箱", id)));
        }
        return 1;
    }
}
