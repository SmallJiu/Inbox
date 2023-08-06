package cat.jiu.core.util.base;

import cat.jiu.core.api.ICommand;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;

import java.util.*;
import java.util.function.Consumer;

public class BaseCommand {
    public static abstract class BaseTree implements ICommand {
        protected final Map<String, ICommand> commandMap = new HashMap<>();
        protected final Map<String, ICommand> aliasMap = new HashMap<>();
        protected final String name;
        protected List<String> alias;
        public BaseTree(String name) {
            this.name = name;
        }
        public BaseTree(CommandDispatcher<CommandSourceStack> dispatcher, String name, Consumer<CommandDispatcher<CommandSourceStack>> init) {
            this.name = name;
            init.accept(dispatcher);
            this.register(dispatcher);
        }

        @Override
        public LiteralCommandNode<CommandSourceStack> register(CommandDispatcher<CommandSourceStack> dispatcher) {
            LiteralArgumentBuilder<CommandSourceStack> s;
            if(!this.commandMap.isEmpty() || !this.aliasMap.isEmpty()){
                LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(name).requires(this::checkPermission);
                // TODO 注册子命令
                this.commandMap.forEach((k,v) -> builder.then(v.register(dispatcher)).requires(v::checkPermission).executes(v));
                this.aliasMap.forEach((k,v) -> builder.then(v.register(dispatcher)).requires(v::checkPermission).executes(v));
                s = builder;
            }else {
                // TODO 注册空命令树
                s = Commands.literal(name)
                        .requires(this::checkPermission)
                        .executes(this);
            }

            LiteralCommandNode<CommandSourceStack> cmd = dispatcher.register(s);

            // TODO 注册别名
            if(this.alias!=null && !this.alias.isEmpty()){
                for (String alias : this.alias) {
                    dispatcher.register(Commands.literal(alias).redirect(cmd));
                }
            }
            return cmd;
        }

        public boolean hasSubCommand(String name){
            return this.commandMap.containsKey(name) || this.aliasMap.containsKey(name);
        }

        public ICommand getSubCommand(String name){
            if(this.commandMap.containsKey(name)){
                return this.commandMap.get(name);
            }
            if(this.aliasMap.containsKey(name)){
                return this.aliasMap.get(name);
            }
            return null;
        }
        public <T extends ICommand> T addSubCommand(T cmd){
            if(this.canAddSubCommand(cmd)){
                this.commandMap.put(cmd.getName(), cmd);
                List<String> alias = cmd.getAliases();
                if(alias!=null && !alias.isEmpty()){
                    alias.forEach(alia-> {
                        if(!this.hasSubCommand(alia)){
                            this.aliasMap.put(alia, cmd);
                        }
                    });
                }
            }
            return cmd;
        }
        protected <T extends ICommand> boolean canAddSubCommand(T cmd){
            return true;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public List<String> getAliases() {
            return this.alias;
        }
        protected BaseTree addAliases(String name){
            if(this.alias==null) this.alias = new ArrayList<>();
            this.alias.add(name);
            return this;
        }

        @Override
        public boolean checkPermission(CommandSourceStack source) {
            return source.hasPermission(this.getRequiredPermissionLevel());
        }

        public int getRequiredPermissionLevel() {
            return 4;
        }

        @Override
        public int execute(MinecraftServer server, CommandSource sender, String[] args, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
            return 0;
        }
    }

    public static abstract class Base implements ICommand {
        protected final String name;
        protected final int executeLevel;
        protected List<String> alias;
        public Base(String name, int executeLevel) {
            this.name = name;
            this.executeLevel = executeLevel;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public List<String> getAliases() {
            return this.alias;
        }
        public Base addAliases(String name){
            if(this.alias==null) this.alias = new ArrayList<>();
            this.alias.add(name);
            return this;
        }

        @Override
        public boolean checkPermission(CommandSourceStack source) {
            return source.hasPermission(this.executeLevel);
        }
    }
}
