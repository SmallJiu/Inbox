package cat.jiu.email.command;

import cat.jiu.email.EmailAPI;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EmailFileType implements ArgumentType<File> {
    public static final File DEFAULT_EMAIL = new File("default");
    public static File directory(){
        return new File(EmailAPI.getGlobalDataPath() + "emails/");
    }

    public final Map<String, File> files = new HashMap<>();

    public EmailFileType() {
        this(directory());
    }
    public EmailFileType(File dir) {
        dir.mkdirs();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file == null) continue;
                this.files.put(file.getName(), file);
            }
        }
        this.files.put("default", DEFAULT_EMAIL);
    }
    public EmailFileType(File... files) {
        if (files != null) {
            for (File file : files) {
                if (file == null) continue;
                this.files.put(file.getName(), file);
            }
        }
        this.files.put("default", DEFAULT_EMAIL);
    }

    @Override
    public File parse(StringReader reader) throws CommandSyntaxException {
        String s = reader.readUnquotedString();
        if ("default".equals(s)) {
            return DEFAULT_EMAIL;
        }
        if (this.files.containsKey(s)) {
            return this.files.get(s);
        }
        for (String file : this.files.keySet()) {
            if (file.contains(s)) {
                return this.files.get(file);
            }
        }
        for (File file : directory().listFiles()) {
            if (file.getName().contains(s)) {
                return file;
            }
        }
        throw new SimpleCommandExceptionType(Component.literal("Not found file.")).create();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(this.files.keySet(), builder);
    }

    public static class Info implements ArgumentTypeInfo<EmailFileType, Info.Template> {
        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf buf) {
            buf.writeCollection(template.files, (b, file) -> b.writeUtf(file.getName().replace('\\', '/')));
        }
        @Override
        public Template deserializeFromNetwork(FriendlyByteBuf buf) {
            return new Template(buf.readList(b -> new File(b.readUtf().replace('\\', '/'))));
        }
        @Override
        public void serializeToJson(Template template, JsonObject json) {
            JsonArray array = new JsonArray();
            for (File file : template.files) {
                array.add(file.getPath().replace('\\', '/'));
            }
            json.add("files", array);
        }
        @Override
        public Template unpack(EmailFileType arg) {
            return new Template(arg.files.values());
        }
        public class Template implements ArgumentTypeInfo.Template<EmailFileType> {
            final Collection<File> files;
            Template(Collection<File> files) {
                this.files = files;
            }
            @Override
            public EmailFileType instantiate(CommandBuildContext pContext) {
                return new EmailFileType(this.files.toArray(new File[0]));
            }
            @Override
            public ArgumentTypeInfo<EmailFileType, ?> type() {
                return Info.this;
            }
        }
    }
}
