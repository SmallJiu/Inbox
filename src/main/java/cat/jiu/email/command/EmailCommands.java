package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.EmailMain;

public class EmailCommands {
    public static BaseCommand.BaseTree register(){
        return new BaseCommand.BaseTree(EmailMain.MODID, 0)
                .addSubCommand(CommandEmailDelete.register())
                .addSubCommand(CommandEmailExport.register())
                .addSubCommand(CommandOpenInbox.register())
                .addSubCommand(CommandSendEmail.register())
                .addSubCommand(CommandScheduledEmail.register())
                .addSubCommand(CommandEventEmail.register())
                .addSubCommand(CommandUndying.register());
    }
}
