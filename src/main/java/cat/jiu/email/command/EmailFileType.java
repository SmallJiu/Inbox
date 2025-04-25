package cat.jiu.email.command;

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
    public final File dir;

    public EmailFileType(String dir) {
        this.dir = new File(dir);
    }

    @Override
    public File parse(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();
        while(reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        String s = reader.getString().substring(i, reader.getCursor());
        return new File(this.dir, s);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        List<String> files = new ArrayList<>();
        File[] listFiles = this.dir.listFiles();
        if (listFiles!=null) {
            for (File file : listFiles) {
                files.add(file.getName());
            }
        }
        return SharedSuggestionProvider.suggest(files, builder);
    }
}