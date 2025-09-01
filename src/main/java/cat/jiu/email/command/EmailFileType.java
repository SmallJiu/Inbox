package cat.jiu.email.command;

import cat.jiu.email.EmailAPI;
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
    public static final File DEFAULT_EMAIL = new File("default");

    public final File dir;

    public EmailFileType() {
        this(EmailAPI.getGlobalDataPath() + "emails/");
    }
    public EmailFileType(String dir) {
        this.dir = new File(dir);
        this.dir.mkdirs();
    }

    @Override
    public File parse(StringReader reader) throws CommandSyntaxException {
        int i = reader.getCursor();
        while(reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        String s = reader.getString().substring(i, reader.getCursor());
        if ("default".equals(s)) {
            return DEFAULT_EMAIL;
        }
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
        files.add("default");
        return SharedSuggestionProvider.suggest(files, builder);
    }
}