package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;

import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Inbox;
import cat.jiu.email.util.EmailUtils;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class CommandEmailDelete extends BaseCommand.Base {
    public CommandEmailDelete() {
        super("delete", 3);
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
        ctx.getSource().sendFeedback(new StringTextComponent(Arrays.toString(args)), false);

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
        EmailMain.log.error(sender);
        Inbox inbox = Inbox.get(id);
        if(!inbox.isEmptyInbox()) {
            if(inbox.hasEmail(email)){
                inbox.deleteEmail(email);
                EmailUtils.saveInboxToDisk(inbox);
                ctx.getSource().sendFeedback(new StringTextComponent(String.format(TextFormatting.GREEN + "已删除 %s 的邮箱中ID为 %s 的邮件.", player, email)), false);
            }else {
                ctx.getSource().sendFeedback(new StringTextComponent(String.format(TextFormatting.YELLOW + "在 %s 的邮箱中找不到ID为 %s 的邮件.", player, email)), false);
            }
        }else {
            ctx.getSource().sendFeedback(new StringTextComponent(TextFormatting.RED + String.format("无法找到 '%s' 的邮箱", id)), false);
        }
        return 1;
    }

    static class InboxArgumentType implements ArgumentType<String> {

        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();

            reader.setCursor(start);
            return null;
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return ArgumentType.super.listSuggestions(context, builder);
        }
    }

    static class EmailArgumentType implements ArgumentType<Long> {

        @Override
        public Long parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();

            reader.setCursor(start);
            return null;
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return ArgumentType.super.listSuggestions(context, builder);
        }
    }
}
