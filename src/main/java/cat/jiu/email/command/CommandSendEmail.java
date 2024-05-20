package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.core.util.element.Text;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.Email;
import cat.jiu.email.element.EmailSenderGroup;
import cat.jiu.email.util.JsonParser;
import com.google.gson.JsonElement;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("all")
class CommandSendEmail extends BaseCommand.Base {
    public static final File TYPE_PATH = new File(EmailAPI.getTypePath());

    public CommandSendEmail() {
        super("send", 3);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> apply(LiteralArgumentBuilder<CommandSourceStack> node) {
        return node.then(Commands.argument("player", GameProfileArgument.gameProfile())
                .then(Commands.argument("file", StringArgumentType.word()).executes(this)));
    }

    @Override
    public int execute(MinecraftServer server, CommandSource sender, String[] args, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (args.length < 2) {
            ctx.getSource().sendFailure(new TranslatableComponent("/email send <addressee> <email file>"));
        }
        String addressee = args[0];
        String file = EmailAPI.getTypePath() + args[1];
        JsonElement emailJson = JsonParser.parse(file);
        if (emailJson == null || !emailJson.isJsonObject()) {
            ctx.getSource().sendFailure(new TranslatableComponent("file are not a json object."));
            return 0;
        }
        Email email = new Email(emailJson.getAsJsonObject());

        email.setSender(new Text(sender instanceof Player ? ((Player)sender).getName().getString() : EmailMain.SYSTEM));

        if (EmailAPI.sendEmail(
                sender instanceof Player ? EmailSenderGroup.PLAYER : EmailSenderGroup.SYSTEM,
                addressee, email
        )){
            sender.sendMessage(new TranslatableComponent("info.email.send.success", addressee), ((Player)sender).getUUID());
        }else {
            ctx.getSource().sendFailure(new TranslatableComponent("info.email.send.fail"));
        }
        return 1;
    }

//    @Override
//    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
//        if (args.length == 1) {
//            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
//        }else if (args.length == 2) {
//            if (!TYPE_PATH.exists()) {
//                TYPE_PATH.mkdirs();
//            }
//            List<String> files = new ArrayList<>();
//            for (File file : TYPE_PATH.listFiles()) {
//                files.add(file.getName());
//            }
//            return files;
//        }
//        return Collections.emptyList();
//    }

    public static class EmailFileType implements ArgumentType<String> {
        public static final EmailFileType INSTANCE = new EmailFileType();
        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            return reader.getString();
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
            return ArgumentType.super.listSuggestions(context, builder);
        }
    }
}
