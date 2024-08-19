package cat.jiu.email.command;

import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.Email;
import cat.jiu.email.util.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EmailFileType implements ArgumentType<File> {
    @Override
    public File parse(StringReader reader) throws CommandSyntaxException {
        String[] a = reader.readString().split(" ");
        return new File(EmailAPI.getTypePath(), a[a.length-1]);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        List<String> files = new ArrayList<>();
        File[] listFiles = new File(EmailAPI.getTypePath()).listFiles();
        if (listFiles!=null) {
            for (File file : listFiles) {
                try {
                    new Email(JsonParser.parse(file).getAsJsonObject());
                    files.add(file.getName());
                }catch (Throwable ignored){}
            }
        }
        return SharedSuggestionProvider.suggest(files, builder);
    }
}