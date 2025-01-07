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
import net.minecraft.command.ISuggestionProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EmailFileType implements ArgumentType<File> {
    @Override
    public File parse(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();
        while(reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        String s = reader.getString().substring(i, reader.getCursor());
        return new File(EmailAPI.getGlobalDataPath()+"emails/", s);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        List<String> files = new ArrayList<>();
        File[] listFiles = new File(EmailAPI.getGlobalDataPath()+"emails/").listFiles();
        if (listFiles!=null) {
            for (File file : listFiles) {
                files.add(file.getName());
            }
        }
        return ISuggestionProvider.suggest(files, builder);
    }
}