package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.element.ScheduledEmail;
import net.minecraft.network.chat.Component;

public class CommandScheduledEmail {
    public static BaseCommand.BaseTree register(){
        return new BaseCommand.BaseTree("scheduled", 1)
                .addSubCommand(new BaseCommand.Builder("load")
                        .level(1)
                        .execute((server, sender, args, ctx) -> {
                            if (ctx.getSource().hasPermission(4)) {
                                ScheduledEmail.updata();
                                sender.sendSystemMessage(Component.literal("Success!"));
                            }
                            return 1;
                        })
                        .build()
                ).addSubCommand(new BaseCommand.Builder("reload")
                        .level(1)
                        .execute((server, sender, args, ctx) -> {
                            if (ctx.getSource().hasPermission(4)) {
                                ScheduledEmail.init();
                                sender.sendSystemMessage(Component.literal("Success!"));
                            }
                            return 1;
                        })
                        .build()
                );
    }
}
