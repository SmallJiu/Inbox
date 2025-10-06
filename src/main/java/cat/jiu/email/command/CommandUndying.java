package cat.jiu.email.command;

import cat.jiu.core.api.ICommand;
import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.attachment.AttachmentUndying;
import cat.jiu.email.net.msg.MsgUndying;
import cat.jiu.email.util.EmailUtils;
import cat.jiu.email.util.TotemLike;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

import java.util.Collection;

class CommandUndying  {
    public static BaseCommand.BaseTree register(){
        return new BaseCommand.BaseTree("undying", 1)
                .addSubCommand(new BaseCommand.Builder("totem")
                        .level(0)
                        .run(ctx->{
                            if (ctx.getSource().isPlayer()) {
                                ServerPlayer player = ctx.getSource().getPlayer();
                                int count = 0;

                                for(InteractionHand hand : InteractionHand.values()) {
                                    count += TotemLike.checkAndShrinkTotemLike(player.getItemInHand(hand)) ? 1 : 0;
                                }
                                for (int i = 0; i < player.getInventory().items.size(); i++) {
                                    count += TotemLike.checkAndShrinkTotemLike(player.getInventory().items.get(i)) ? 1 : 0;
                                }
                                if (count > 0) {
                                    player.sendSystemMessage(Component.translatable("commands.clear.test.single", count, player.getDisplayName()));
                                    AttachmentUndying.UndyingState state = AttachmentUndying.UndyingState.get(ctx.getSource().getServer());
                                    state.addCount(player.getUUID(), count);
                                    if (player instanceof ServerPlayer) {
                                        EmailMain.NETWORK.sendMessageToPlayer(new MsgUndying(state.getCount(player.getUUID())), player);
                                    }
                                }else {
                                    player.sendSystemMessage(Component.literal("Notfound totem."));
                                }
                            }else {
                                throw CommandSourceStack.ERROR_NOT_PLAYER.create();
                            }
                            return ICommand.SINGLE_SUCCESS;
                        })
                        .build())

                .addSubCommand(new BaseCommand.Builder("item")
                        .level(0)
                        .argument((cmd, node)->
                                node.executes(cmd)
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(cmd))
                                        .then(Commands.argument("entity", EntityArgument.entities()).executes(cmd)
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(cmd))
                                        ))
                        .run(ctx->{
                            if (!ctx.getSource().isPlayer()) {
                                throw EntityArgument.NO_PLAYERS_FOUND.create();
                            }
                            AttachmentUndying.UndyingState state = AttachmentUndying.UndyingState.get(ctx.getSource().getServer());
                            int count = 1;
                            try {
                                count = IntegerArgumentType.getInteger(ctx, "count");
                            }catch (Exception ignored){}

                            try {
                                Collection<? extends Entity> entities = EntityArgument.getEntities(ctx, "entity");
                                for (Entity entity : entities) {
                                    long finalCount = count;
                                    if (state.getCount(entity.getUUID()) - count < 0) {
                                        finalCount = count + (state.getCount(entity.getUUID()) - count);
                                    }
                                    if (entity instanceof ServerPlayer) {
                                        state.subCount((ServerPlayer) entity, finalCount);
                                    }else {
                                        state.subCount(entity.getUUID(), finalCount);
                                    }
                                    for (int i = 0; i < finalCount; i++) {
                                        EmailUtils.spawnAsEntity(entity.level(), entity.getEyePosition(), AttachmentUndying.TOTEM_UNDYING);
                                    }
                                }
                                count = entities.size();
                            }catch (Exception ignored){
                                long finalCount = count;
                                if (state.getCount(ctx.getSource().getPlayer().getUUID()) - count < 0) {
                                    finalCount = count + (state.getCount(ctx.getSource().getPlayer().getUUID()) - count);
                                }
                                state.subCount(ctx.getSource().getPlayer(), finalCount);
                                for (int i = 0; i < finalCount; i++) {
                                    EmailUtils.spawnAsEntity(ctx.getSource().getEntity().level(), ctx.getSource().getEntity().getEyePosition(), AttachmentUndying.TOTEM_UNDYING);
                                }

                            }
                            return count;
                        })
                        .build())

                .addSubCommand(new BaseCommand.Builder("set")
                        .argument((cmd, node) ->
                                node.then(Commands.argument("entity", EntityArgument.entity())
                                        .then(Commands.argument("count", LongArgumentType.longArg(0)).executes(cmd)
                                        ))
                        )
                        .run(ctx->{
                            long count = LongArgumentType.getLong(ctx, "count");
                            AttachmentUndying.UndyingState state = AttachmentUndying.UndyingState.get(ctx.getSource().getServer());
                            int success = 0;
                            for (Entity entity : EntityArgument.getEntities(ctx, "entity")) {
                                state.undying.put(entity.getStringUUID(), count);
                                if (entity instanceof ServerPlayer) {
                                    EmailMain.NETWORK.sendMessageToPlayer(new MsgUndying(state.getCount(entity.getUUID())), (ServerPlayer) entity);
                                    if (!(ctx.getSource() instanceof CommandSourceStack)) {
                                        entity.sendSystemMessage(Component.translatable("inbox.command.undying.set", count, ctx.getSource().getDisplayName()));
                                    }
                                }
                                success++;
                            }
                            if (success > 0) {
                                state.setDirty();
                            }
                            ctx.getSource().sendSystemMessage(Component.translatable("commands.scoreboard.players.set.success.multiple", Component.translatable("info.inbox.undying"), success, count));
                            return ICommand.SINGLE_SUCCESS;
                        })
                        .build())

                .addSubCommand(new BaseCommand.Builder("add")
                        .argument((cmd, node) ->
                                node.then(Commands.argument("entity", EntityArgument.entity())
                                        .then(Commands.argument("count", LongArgumentType.longArg(0)).executes(cmd)
                                        ))
                        )
                        .run(ctx->{
                            long count = LongArgumentType.getLong(ctx, "count");
                            AttachmentUndying.UndyingState state = AttachmentUndying.UndyingState.get(ctx.getSource().getServer());
                            int success = 0;
                            for (Entity entity : EntityArgument.getEntities(ctx, "entity")) {
                                state.addCount(entity.getUUID(), count);
                                if (entity instanceof ServerPlayer) {
                                    EmailMain.NETWORK.sendMessageToPlayer(new MsgUndying(state.getCount(entity.getUUID())), (ServerPlayer) entity);
                                    if (!(ctx.getSource() instanceof CommandSourceStack)) {
                                        entity.sendSystemMessage(Component.translatable("inbox.command.undying.add", count, ctx.getSource().getDisplayName()));
                                    }
                                }
                                success++;
                            }

                            ctx.getSource().sendSystemMessage(Component.translatable("commands.scoreboard.players.add.success.multiple", count, Component.translatable("info.inbox.undying"), success));
                            return ICommand.SINGLE_SUCCESS;
                        })
                        .build())
                ;
    }
}
