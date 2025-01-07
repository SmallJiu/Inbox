package cat.jiu.email.command;

import cat.jiu.email.element.EventEmail;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.ResourceLocation;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class EmailEventType implements ArgumentType<ResourceLocation> {
    @Override
    public ResourceLocation parse(StringReader reader) throws CommandSyntaxException {
//        String s = reader.readString();

        int i = reader.getCursor();
        while(reader.canRead() && reader.peek() != ' ') {
            reader.skip();
        }
        String s = reader.getString().substring(i, reader.getCursor());
        return new ResourceLocation(s);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return ISuggestionProvider.suggest(EventEmail.getRegistryEvents().stream().map(String::valueOf).collect(Collectors.toList()), builder);
    }
}