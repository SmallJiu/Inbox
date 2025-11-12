package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.EmailAPI;
import cat.jiu.email.element.EventEmail;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.File;

public class CommandEventEmail {
    public static BaseCommand.BaseTree register(){
        return new BaseCommand.BaseTree("event", 1)
                .addSubCommand(new BaseCommand.Builder("load")
                        .level(1)
                        .execute((server, sender, args, ctx) -> {
                            EventEmail.load();
                            sender.sendSystemMessage(Component.literal("Success!"));
                            return 1;
                        })
                        .build()
                ).addSubCommand(new BaseCommand.Builder("add")
                        .level(1)
                        .argument((cmd, node)->node
                                .then(Commands.argument("event", new EmailEventType())
                                        .then(Commands.argument("email", new EmailFileType())
                                                .executes(cmd)
                                        ))
                        )
                        .execute((server, sender, args, ctx) -> {
                            ResourceLocation e = ctx.getArgument("event", ResourceLocation.class);
                            String f = ctx.getArgument("email", File.class).getName();
                            EventEmail.register(e,f);
                            ctx.getSource().sendSystemMessage(
                                    Component.literal(ChatFormatting.GREEN + "success")
                                            .append(". event: ")
                                            .append(ChatFormatting.GREEN + String.valueOf(e))
                                            .append(", file: ")
                                            .append(ChatFormatting.GREEN + f)
                            );
                            return 1;
                        })
                        .build()
                );
    }
}
