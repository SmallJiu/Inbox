package cat.jiu.email.command;

import cat.jiu.core.api.ICommand;
import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.EmailMain;
import cat.jiu.email.element.attachment.AttachmentUndying;
import cat.jiu.email.net.msg.MsgUndying;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
                                    ItemStack stack = player.getItemInHand(hand);
                                    if (stack.is(Items.TOTEM_OF_UNDYING)) {
                                        count += stack.getCount();
                                        stack.setCount(0);
                                    }
                                }
                                for (int i = 0; i < player.getInventory().items.size(); i++) {
                                    ItemStack stack = player.getInventory().items.get(i);
                                    if (stack.is(Items.TOTEM_OF_UNDYING)) {
                                        count += stack.getCount();
                                        stack.setCount(0);
                                    }
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

                .addSubCommand(new BaseCommand.Builder("set")
                        .argument((cmd, node) ->
                                node.then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .then(Commands.argument("count", LongArgumentType.longArg(0)).executes(cmd)
                                        ))
                        )
                        .run(ctx->{
                            long count = LongArgumentType.getLong(ctx, "count");
                            AttachmentUndying.UndyingState state = AttachmentUndying.UndyingState.get(ctx.getSource().getServer());
                            int success = 0;
                            for (GameProfile playerProfile : GameProfileArgument.getGameProfiles(ctx, "player")) {
                                ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayer(playerProfile.getId());
                                state.undying.put(player.getStringUUID(), count);
                                EmailMain.NETWORK.sendMessageToPlayer(new MsgUndying(state.getCount(player.getUUID())), player);
                                player.sendSystemMessage(Component.translatable("inbox.command.undying.set", count, ctx.getSource().getDisplayName()));
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
                                node.then(Commands.argument("player", GameProfileArgument.gameProfile())
                                        .then(Commands.argument("count", LongArgumentType.longArg(0)).executes(cmd)
                                        ))
                        )
                        .run(ctx->{
                            long count = LongArgumentType.getLong(ctx, "count");
                            AttachmentUndying.UndyingState state = AttachmentUndying.UndyingState.get(ctx.getSource().getServer());
                            int success = 0;
                            for (GameProfile playerProfile : GameProfileArgument.getGameProfiles(ctx, "player")) {
                                ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayer(playerProfile.getId());
                                if (player != null) {
                                    state.addCount(playerProfile.getId(), count);
                                    EmailMain.NETWORK.sendMessageToPlayer(new MsgUndying(state.getCount(playerProfile.getId())), player);
                                    player.sendSystemMessage(Component.translatable("inbox.command.undying.add", count, ctx.getSource().getDisplayName()));
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
