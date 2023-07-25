package cat.jiu.email.command;

import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.EmailMain;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

public class EmailCommands extends BaseCommand.BaseTree {
    public EmailCommands() {
        super(EmailMain.MODID);
        this.addSubCommand(new CommandEmailDelete());
        this.addSubCommand(new CommandEmailExport());
    }
}
