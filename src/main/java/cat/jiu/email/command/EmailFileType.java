package cat.jiu.email.command;

import cat.jiu.core.util.SideProxy;
import cat.jiu.core.util.element.data.NBTData;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EmailFileType implements ArgumentType<File> {
    public static final File DEFAULT_EMAIL = new File("default");

    public final Map<String, File> files;
    public final File[] fileArray;

    public EmailFileType() {
        this(EmailAPI.getGlobalDataPath() + "emails/");
    }
    public EmailFileType(String dir) {
        this(new File(dir));
    }
    public EmailFileType(File dir) {
        dir.mkdirs();
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            this.files = new HashMap<>();
            for (File file : files) {
                this.files.put(file.getName(), file);
            }
            this.fileArray = files;
        }else {
            this.files = Collections.emptyMap();
            this.fileArray = new File[0];
        }
    }
    public EmailFileType(File... files) {
        if (files != null && files.length > 0) {
            this.files = new HashMap<>();
            for (File file : files) {
                this.files.put(file.getName(), file);
            }
            this.fileArray = files;
        }else {
            this.files = Collections.emptyMap();
            this.fileArray = new File[0];
        }
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
        if (this.files.containsKey(s)) {
            return this.files.get(s);
        }
        for (String file : this.files.keySet()) {
            if (file.contains(s)) {
                return this.files.get(file);
            }
        }
        throw new SimpleCommandExceptionType(Component.literal("Not found file.")).create();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(this.files.keySet(), builder);
    }

    public static class Info implements ArgumentTypeInfo<EmailFileType, Info.Template> {
        boolean test(){
            return false;
        }
        @Override
        public void serializeToNetwork(Template template, FriendlyByteBuf buf) {
            if (this.test()){
                CompoundTag tag = new CompoundTag();
                ListTag list = new ListTag();
                for (File file : template.files) {
                    list.add(StringTag.valueOf(file.getPath()));
                }
                tag.put("files", list);
                buf.writeNbt(tag);
            }
        }

        @Override
        public Template deserializeFromNetwork(FriendlyByteBuf buf) {
            if (this.test()){
                List<File> files = new ArrayList<>();
                for (Tag tag : buf.readNbt().getList("files", NBTData.getType(String.class))) {
                    files.add(new File(tag.getAsString()));
                }
                return new Template(files.toArray(new File[0]));
            }
            return new Template(new EmailFileType().fileArray);
        }

        @Override
        public void serializeToJson(Template template, JsonObject json) {
        }

        @Override
        public Template unpack(EmailFileType arg) {
            return new Template(arg.fileArray);
        }

        public class Template implements ArgumentTypeInfo.Template<EmailFileType> {
            final File[] files;
            Template(File... files) {
                this.files = files;
            }

            @Override
            public EmailFileType instantiate(CommandBuildContext pContext) {
                return new EmailFileType(this.files);
            }

            @Override
            public ArgumentTypeInfo<EmailFileType, ?> type() {
                return Info.this;
            }
        }
    }
}
