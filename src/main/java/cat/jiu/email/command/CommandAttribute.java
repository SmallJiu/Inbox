package cat.jiu.email.command;

import cat.jiu.core.api.ICommand;
import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.element.attachment.AttachmentAttribute;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.command.EnumArgument;

class CommandAttribute {
    public static BaseCommand.BaseTree register(){
        return new BaseCommand.BaseTree("attribute", 1)
                .addSubCommand(get())
                .addSubCommand(add())
                .addSubCommand(remove())
                .addSubCommand(set());
    }

    static BaseCommand.Base get(){
        return new BaseCommand.Builder("get")
                .level(1)
                .argument((cmd, event, node)->
                        node.executes(cmd).then(
                                Commands.argument("entity", EntityArgument.entities()).executes(cmd)
                        )
                )
                .run(ctx->{
                    AttachmentAttribute.AttributeState state = AttachmentAttribute.AttributeState.get(ctx.getSource().getServer());
                    try {
                        for (Entity entity : EntityArgument.getEntities(ctx, "entity")) {
                            if (entity instanceof LivingEntity) {
                                sendGetMessage(ctx.getSource(), state, (LivingEntity) entity);
                            }
                        }
                    }catch (Exception ignored){
                        sendGetMessage(ctx.getSource(), state, ctx.getSource().getPlayerOrException());
                    }
                    return ICommand.SINGLE_SUCCESS;
                })
                .build();
    }
    static void sendGetMessage(CommandSourceStack stack, AttachmentAttribute.AttributeState state, LivingEntity entity) {
        if (state.map.containsKey(entity.getStringUUID())) {
            stack.sendSystemMessage(Component.translatable("inbox.command.attribute.get", ((MutableComponent) entity.getDisplayName()).withStyle(ChatFormatting.YELLOW)));
            state.map.get(entity.getStringUUID()).forEach((k, v) -> {
                MutableComponent message = Component.literal("  - ")
                        .append(ComponentUtils.wrapInSquareBrackets(setHoverMessage(Component.translatable(k.getDescriptionId()), ChatFormatting.DARK_AQUA, Component.literal(String.valueOf(ForgeRegistries.ATTRIBUTES.getKey(k))))))
                        .append(": ");
                v.forEach((k1, v1) ->
                        message.append(setSetAndRemoveClickEvent(
                                Component.literal(AttachmentAttribute.getMethod(k1, v1)),
                                entity.getStringUUID(), k, k1)).append(", ")
                );
                stack.sendSystemMessage(message);
            });
        }
    }

    static MutableComponent setHoverMessage(MutableComponent component, Component hoverMessage) {
        return setHoverMessage(component, ChatFormatting.DARK_AQUA, hoverMessage);
    }
    static MutableComponent setHoverMessage(MutableComponent component, ChatFormatting color, Component hoverMessage) {
        return component.withStyle(style ->
                style.withColor(color).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverMessage))
        );
    }
    static MutableComponent setSetAndRemoveClickEvent(MutableComponent component, String name, Attribute attribute, AttributeModifier.Operation operation) {
        return component.append(Component.literal("[").withStyle(ChatFormatting.GRAY)
                        .append(Component.translatable("mco.configure.world.invites.remove.tooltip").withStyle(style ->
                            style.withColor(ChatFormatting.LIGHT_PURPLE).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/email attribute remove " + name + " " + ForgeRegistries.ATTRIBUTES.getKey(attribute) + " " + operation.name()))
                        ))
                        .append(",")
                        .append(Component.translatable("inbox.command.attribute.get.modifier").withStyle(style ->
                                style.withColor(ChatFormatting.AQUA).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/email attribute set " + name + " " + ForgeRegistries.ATTRIBUTES.getKey(attribute) + " " + operation.name() + " "))
                        ))
                .append(Component.literal("]").withStyle(ChatFormatting.GRAY)));
    }

    static BaseCommand.Base set(){
        return new BaseCommand.Builder("set")
                .level(1)
                .argument((cmd, event, node)->
                        node.then(
                                Commands.argument("entity", EntityArgument.entities())
                                        .then(Commands.argument("attribute", ResourceArgument.resource(event.getBuildContext(), ForgeRegistries.ATTRIBUTES.getRegistryKey()))
                                                .then(Commands.argument("operation", EnumArgument.enumArgument(AttributeModifier.Operation.class))
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .executes(cmd)
                                                        )
                                                )
                                        )
                        )
                )
                .run(ctx->{
                    AttachmentAttribute.AttributeState state = AttachmentAttribute.AttributeState.get(ctx.getSource().getServer());
                    Attribute attribute = ResourceArgument.getAttribute(ctx, "attribute").get();
                    AttributeModifier.Operation operation = ctx.getArgument("operation", AttributeModifier.Operation.class);
                    double value = DoubleArgumentType.getDouble(ctx, "value");

                    int success = 0;
                    for (Entity entity : EntityArgument.getEntities(ctx, "entity")) {
                        if (entity instanceof LivingEntity) {
                            if (state.map.containsKey(entity.getStringUUID())
                                    && state.map.get(entity.getStringUUID()).containsKey(attribute)) {
                                state.map.get(entity.getStringUUID()).get(attribute).put(operation, value);
                                AttributeMap map = ((LivingEntity) entity).getAttributes();
                                if (map.hasAttribute(attribute)) {
                                    AttributeInstance instance = map.getInstance(attribute);
                                    AttachmentAttribute.AttributeState.Id id = AttachmentAttribute.AttributeState.id(attribute, operation, false);
                                    AttributeModifier modifier = new AttributeModifier(id.uid, id.id, state.get(entity.getStringUUID(), attribute, operation), operation);
                                    instance.removeModifier(id.uid);
                                    instance.addPermanentModifier(modifier);
                                }
                                success++;
                            }
                        }
                    }
                    if (success > 0) {
                        state.setDirty();
                    }
                    ctx.getSource().sendSystemMessage(Component.translatable("inbox.command.attribute.set", success));
                    return success != 0 ? success : ICommand.SINGLE_SUCCESS;
                })
                .build();
    }

    static BaseCommand.Base add(){
        return new BaseCommand.Builder("add")
                .level(1)
                .argument((cmd, event, node)->
                        node.then(
                                Commands.argument("entity", EntityArgument.entities())
                                        .then(Commands.argument("attribute", ResourceArgument.resource(event.getBuildContext(), ForgeRegistries.ATTRIBUTES.getRegistryKey()))
                                                .then(Commands.argument("operation", EnumArgument.enumArgument(AttributeModifier.Operation.class))
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg()).executes(cmd)
                                                                .then(Commands.argument("temp", BoolArgumentType.bool()).executes(cmd))
                                                        )
                                                )
                                        )
                        )
                )
                .run(ctx->{
                    AttachmentAttribute.AttributeState state = AttachmentAttribute.AttributeState.get(ctx.getSource().getServer());
                    Attribute attribute = ResourceArgument.getAttribute(ctx, "attribute").get();
                    AttributeModifier.Operation operation = ctx.getArgument("operation", AttributeModifier.Operation.class);
                    boolean temp = true;
                    try {
                        temp = BoolArgumentType.getBool(ctx, "temp");
                    }catch (Exception ignored){}
                    double value = DoubleArgumentType.getDouble(ctx, "value");

                    int success = 0;
                    for (Entity entity : EntityArgument.getEntities(ctx, "entity")) {
                        if (entity instanceof LivingEntity) {
                            if (!temp) {
                                state.add(entity.getStringUUID(), attribute, operation, value);
                            }
                           AttributeMap map = ((LivingEntity) entity).getAttributes();
                            if (map.hasAttribute(attribute)) {
                                AttributeInstance instance = map.getInstance(attribute);
                                AttachmentAttribute.AttributeState.Id id = AttachmentAttribute.AttributeState.id(attribute, operation, temp);
                                AttributeModifier modifier = new AttributeModifier(id.uid, id.id,
                                        temp ? value + (instance.getModifier(id.uid) != null ? instance.getModifier(id.uid).getAmount() : 0)
                                        : state.get(entity.getStringUUID(), attribute, operation), operation);
                                instance.removeModifier(id.uid);
                                instance.addPermanentModifier(modifier);
                            }
                            success++;
                        }
                    }
                    ctx.getSource().sendSystemMessage(Component.translatable("inbox.command.attribute.add", success));
                    return success != 0 ? success : ICommand.SINGLE_SUCCESS;
                })
                .build();
    }
    static BaseCommand.Base remove(){
        return new BaseCommand.Builder("remove")
                .level(1)
                .argument((cmd, event, node)->
                        node.then(
                                Commands.argument("entity", EntityArgument.entities())
                                        .then(Commands.argument("attribute", ResourceArgument.resource(event.getBuildContext(), ForgeRegistries.ATTRIBUTES.getRegistryKey()))
                                                .then(Commands.argument("operation", EnumArgument.enumArgument(AttributeModifier.Operation.class))
                                                        .executes(cmd)
                                                )
                                        )
                        )
                )
                .run(ctx->{
                    AttachmentAttribute.AttributeState state = AttachmentAttribute.AttributeState.get(ctx.getSource().getServer());
                    Attribute attribute = ResourceArgument.getAttribute(ctx, "attribute").get();
                    AttributeModifier.Operation operation = ctx.getArgument("operation", AttributeModifier.Operation.class);

                    int success = 0;
                    for (Entity entity : EntityArgument.getEntities(ctx, "entity")) {
                        if (entity instanceof LivingEntity) {
                            if (state.map.containsKey(entity.getStringUUID())
                            && state.map.get(entity.getStringUUID()).containsKey(attribute)) {
                                state.map.get(entity.getStringUUID()).get(attribute).remove(operation);
                                success++;
                            }
                        }
                    }
                    if (success > 0) {
                        state.setDirty();
                    }
                    ctx.getSource().sendSystemMessage(Component.translatable("inbox.command.attribute.remove", success));
                    return success != 0 ? success : ICommand.SINGLE_SUCCESS;
                })
                .build();
    }
}
