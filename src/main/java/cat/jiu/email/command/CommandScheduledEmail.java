package cat.jiu.email.command;

import cat.jiu.core.api.ITimer;
import cat.jiu.core.util.base.BaseCommand;
import cat.jiu.email.element.ScheduledEmail;
import cat.jiu.email.util.TimeMillis;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.server.command.EnumArgument;

public class CommandScheduledEmail {
    public static BaseCommand.BaseTree register(){
        return new BaseCommand.BaseTree("scheduled", 1)
                .addSubCommand(new BaseCommand.Builder("reload")
                        .level(1)
                        .alias("load")
                        .execute((server, sender, args, ctx) -> {
                            ScheduledEmail.init();
                            sender.sendSystemMessage(Component.literal("Success!"));
                            return Command.SINGLE_SUCCESS;
                        })
                        .build()
                )
                .addSubCommand(new BaseCommand.Builder("add")
                        .level(1)
                        .argument((command, node) -> node
                                .then(Commands.argument("email", new EmailFileType())
                                        .then(Commands.argument("id", LongArgumentType.longArg())
                                                .then(Commands.argument("day", LongArgumentType.longArg())
                                                        .then(Commands.argument("hour", LongArgumentType.longArg())
                                                                .then(Commands.argument("minutes", LongArgumentType.longArg())
                                                                        .then(Commands.argument("type", EnumArgument.enumArgument(ScheduledEmail.Addressee.class))
                                                                                .executes(command)
                                                                                .then(Commands.argument("note",  StringArgumentType.string())
                                                                                        .executes(command)
                                                                                        .then(Commands.argument("addressee",  StringArgumentType.string())
                                                                                                .executes(command)
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                        .run(ctx->{
                            long id = LongArgumentType.getLong(ctx, "id");
                            if (ScheduledEmail.hasScheduledEmail(id)) {
                                throw new SimpleCommandExceptionType(Component.literal("Scheduled email has exists, id: " + id)).create();
                            }

                            String file = EmailFileType.fileString(ctx, "email").replace('\\', '/');
                            file = file.substring(file.lastIndexOf("inbox/data/emails/") + "inbox/data/emails/".length());
                            long day = LongArgumentType.getLong(ctx, "day");
                            long hour = LongArgumentType.getLong(ctx, "hour");
                            long minutes = LongArgumentType.getLong(ctx, "minutes");
                            ScheduledEmail.Addressee addresseeType = ctx.getArgument("type", ScheduledEmail.Addressee.class);
                            String note = "";
                            try {
                                note = StringArgumentType.getString(ctx, "note");
                            }catch (Exception ignored){}
                            String addressee = null;
                            try {
                                addressee = StringArgumentType.getString(ctx, "addressee");
                            }catch (Exception ignored){}

                            ScheduledEmail email = new ScheduledEmail()
                                    .setFilePath(file)
                                    .setId(id)
                                    .setInterval(new TimeMillis(day, hour, minutes, 0, 0))
                                    .setAddressee(addresseeType)
                                    .setNote(note);
                            if(addressee != null) {
                                if (!addresseeType.isCustomPlayers()) {
                                    throw new SimpleCommandExceptionType(Component.literal("Scheduled email address type must be CUSTOM")).create();
                                }
                                if (addressee.contains(",")) {
                                    for (String s : addressee.split(",")) {
                                        email.addCustomAddressee(s);
                                    }
                                }else {
                                    email.addCustomAddressee(addressee);
                                }
                            }
                            ScheduledEmail.addScheduledEmail(email, true);

                            ctx.getSource().sendSystemMessage(Component.literal("Success! A new scheduled email has be added."));
                            ctx.getSource().sendSystemMessage(Component.literal("  - ID: " + id));
                            ctx.getSource().sendSystemMessage(Component.literal("  - File: " + file));
                            ctx.getSource().sendSystemMessage(Component.literal("  - Interval: " + ITimer.formatTimestamp(email.getInterval().millis, false, true, true,true, true)));
                            if (!note.isEmpty()){
                                ctx.getSource().sendSystemMessage(Component.literal("  - Note: " + note));
                            }
                            ctx.getSource().sendSystemMessage(Component.literal("  - AddresseeType: " + addresseeType));
                            if (addresseeType.isCustomPlayers() && addressee != null){
                                ctx.getSource().sendSystemMessage(Component.literal("  - Addressee: " + email.getCustomAddressee()));
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                        .build()
                )
                .addSubCommand(new BaseCommand.BaseTree("modify")
                        .addSubCommand(new BaseCommand.Builder("id")
                                .level(1)
                                .argument((command, event, node) -> node
                                        .then(Commands.argument("oldID", LongArgumentType.longArg())
                                                .then(Commands.argument("newID", LongArgumentType.longArg())
                                                        .executes(command)
                                                )
                                        )
                                )
                                .run(ctx->{
                                    long oldID = LongArgumentType.getLong(ctx, "oldID");
                                    ScheduledEmail email = ScheduledEmail.getScheduledEmail(oldID);
                                    if (email == null) {
                                        throw new SimpleCommandExceptionType(Component.literal("Not found scheduled email, id: " + oldID)).create();
                                    }
                                    long newID = LongArgumentType.getLong(ctx, "newID");
                                    if (ScheduledEmail.hasScheduledEmail(newID)) {
                                        throw new SimpleCommandExceptionType(Component.literal("Scheduled email has exists, id: " + newID)).create();
                                    }
                                    ScheduledEmail.removeScheduledEmail(oldID);
                                    ScheduledEmail.addScheduledEmail(email.setId(newID), true);

                                    ctx.getSource().sendSystemMessage(Component.literal("Success! Scheduled email id has modify to: " + newID));
                                    return Command.SINGLE_SUCCESS;
                                })
                                .build()
                        )
                        .addSubCommand(new BaseCommand.Builder("file")
                                .level(1)
                                .argument((command, event, node) -> node
                                        .then(Commands.argument("id", LongArgumentType.longArg())
                                                .then(Commands.argument("file", new EmailFileType())
                                                        .executes(command)
                                                )
                                        )
                                )
                                .run(ctx->{
                                    long id = LongArgumentType.getLong(ctx, "id");
                                    ScheduledEmail email = ScheduledEmail.getScheduledEmail(id);
                                    if (email == null) {
                                        throw new SimpleCommandExceptionType(Component.literal("Not found scheduled email, id: " + id)).create();
                                    }
                                    String filePath = EmailFileType.fileString(ctx, "file").replace('\\', '/');
                                    if (!"default".equalsIgnoreCase(filePath)) {
                                        email.setFilePath(filePath.substring(filePath.lastIndexOf("inbox/data/emails/") + "inbox/data/emails/".length()));
                                    }else {
                                        email.setFilePath("default");
                                    }

                                    ScheduledEmail.save();

                                    ctx.getSource().sendSystemMessage(Component.literal("Success! Scheduled email note has modify to: " + email.getNote()));
                                    return Command.SINGLE_SUCCESS;
                                })
                                .build()
                        )
                        .addSubCommand(new BaseCommand.BaseTree("addressee")
                                .addSubCommand(new BaseCommand.Builder("set")
                                        .level(1)
                                        .argument((command, node) -> node
                                                .then(Commands.argument("id", LongArgumentType.longArg())
                                                        .then(Commands.argument("address", StringArgumentType.string()).executes(command))
                                                )
                                        )
                                        .run(ctx->{
                                            long id = LongArgumentType.getLong(ctx, "id");
                                            String addressee = StringArgumentType.getString(ctx, "address");
                                            ScheduledEmail email = ScheduledEmail.getScheduledEmail(id);
                                            if (email == null) {
                                                throw new SimpleCommandExceptionType(Component.literal("Not found scheduled email, id: " + id)).create();
                                            }
                                            if (!email.getAddressee().isCustomPlayers()) {
                                                throw new SimpleCommandExceptionType(Component.literal("Scheduled email address type must be CUSTOM")).create();
                                            }
                                            email.getCustomAddressee().clear();
                                            if (addressee.contains(",")) {
                                                for (String s : addressee.split(",")) {
                                                    email.addCustomAddressee(s);
                                                }
                                            }else {
                                                email.addCustomAddressee(addressee);
                                            }
                                            ScheduledEmail.save();
                                            ctx.getSource().sendSystemMessage(Component.literal("Success! Scheduled email address has modify to: " + email.getCustomAddressee()));
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .build()
                                )
                                .addSubCommand(new BaseCommand.Builder("add")
                                        .level(1)
                                        .argument((command, node) -> node
                                                .then(Commands.argument("id", LongArgumentType.longArg())
                                                        .then(Commands.argument("address", StringArgumentType.string()).executes(command))
                                                )
                                        )
                                        .run(ctx->{
                                            long id = LongArgumentType.getLong(ctx, "id");
                                            String addressee = StringArgumentType.getString(ctx, "address");
                                            ScheduledEmail email = ScheduledEmail.getScheduledEmail(id);
                                            if (email == null) {
                                                throw new SimpleCommandExceptionType(Component.literal("Not found scheduled email, id: " + id)).create();
                                            }
                                            if (!email.getAddressee().isCustomPlayers()) {
                                                throw new SimpleCommandExceptionType(Component.literal("Scheduled email address type must be CUSTOM")).create();
                                            }
                                            if (addressee.contains(",")) {
                                                for (String s : addressee.split(",")) {
                                                    email.addCustomAddressee(s);
                                                }
                                            }else {
                                                email.addCustomAddressee(addressee);
                                            }
                                            ScheduledEmail.save();
                                            ctx.getSource().sendSystemMessage(Component.literal("Success! address has add to scheduled email: " + email.getId()));
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .build()
                                )
                                .addSubCommand(new BaseCommand.Builder("remove")
                                        .level(1)
                                        .argument((command, node) -> node
                                                .then(Commands.argument("id", LongArgumentType.longArg())
                                                        .then(Commands.argument("address", StringArgumentType.string()).executes(command))
                                                )
                                        )
                                        .run(ctx->{
                                            long id = LongArgumentType.getLong(ctx, "id");
                                            String addressee = StringArgumentType.getString(ctx, "address");
                                            ScheduledEmail email = ScheduledEmail.getScheduledEmail(id);
                                            if (email == null) {
                                                throw new SimpleCommandExceptionType(Component.literal("Not found scheduled email, id: " + id)).create();
                                            }
                                            if (!email.getAddressee().isCustomPlayers()) {
                                                throw new SimpleCommandExceptionType(Component.literal("Scheduled email address type must be CUSTOM")).create();
                                            }
                                            email.getCustomAddressee().removeIf(addressee::contains);
                                            ScheduledEmail.save();
                                            ctx.getSource().sendSystemMessage(Component.literal("Success! address has remove from scheduled email: " + email.getId()));
                                            return Command.SINGLE_SUCCESS;
                                        })
                                        .build()
                                )
                        )
                        .addSubCommand(new BaseCommand.Builder("interval")
                                .level(1)
                                .argument((command, event, node) -> node
                                        .then(Commands.argument("id", LongArgumentType.longArg())
                                                .then(Commands.argument("days", LongArgumentType.longArg()).executes(command)
                                                        .then(Commands.argument("hours", LongArgumentType.longArg()).executes(command)
                                                                .then(Commands.argument("minutes",  LongArgumentType.longArg()).executes(command))
                                                        )
                                                )
                                        )
                                )
                                .run(ctx->{
                                    long id = LongArgumentType.getLong(ctx, "id");
                                    ScheduledEmail email = ScheduledEmail.getScheduledEmail(id);
                                    if (email == null) {
                                        throw new SimpleCommandExceptionType(Component.literal("Not found scheduled email, id: " + id)).create();
                                    }
                                    long minutes = LongArgumentType.getLong(ctx, "minutes");
                                    long hours = 0;
                                    try {
                                        hours = LongArgumentType.getLong(ctx, "hours");
                                    }catch (Exception ignored){}
                                    long days = 0;
                                    try {
                                        days = LongArgumentType.getLong(ctx, "days");
                                    }catch (Exception ignored){}

                                    email.setInterval(new TimeMillis(days, hours, minutes, 0, 0));
                                    ScheduledEmail.save();

                                    ctx.getSource().sendSystemMessage(Component.literal(String.format("Success! Scheduled email interval has modify to: Day: %s, Hour: %s, Minute: %s.", days, hours, minutes)));
                                    return Command.SINGLE_SUCCESS;
                                })
                                .build()
                        )
                        .addSubCommand(new BaseCommand.Builder("type")
                                .level(1)
                                .argument((command, event, node) -> node
                                        .then(Commands.argument("id", LongArgumentType.longArg())
                                                .then(Commands.argument("type", EnumArgument.enumArgument(ScheduledEmail.Addressee.class))
                                                        .executes(command)
                                                )
                                        )
                                )
                                .run(ctx->{
                                    long id = LongArgumentType.getLong(ctx, "id");
                                    ScheduledEmail email = ScheduledEmail.getScheduledEmail(id);
                                    if (email == null) {
                                        throw new SimpleCommandExceptionType(Component.literal("Not found scheduled email, id: " + id)).create();
                                    }
                                    email.setAddressee(ctx.getArgument("type", ScheduledEmail.Addressee.class));
                                    ScheduledEmail.save();

                                    ctx.getSource().sendSystemMessage(Component.literal("Success! Scheduled email address type has modify to: " + email.getAddressee()));
                                    return Command.SINGLE_SUCCESS;
                                })
                                .build()
                        )
                        .addSubCommand(new BaseCommand.Builder("note")
                                .level(1)
                                .argument((command, event, node) -> node
                                        .then(Commands.argument("id", LongArgumentType.longArg())
                                                .then(Commands.argument("note", StringArgumentType.string())
                                                        .executes(command)
                                                )
                                        )
                                )
                                .run(ctx->{
                                    long id = LongArgumentType.getLong(ctx, "id");
                                    ScheduledEmail email = ScheduledEmail.getScheduledEmail(id);
                                    if (email == null) {
                                        throw new SimpleCommandExceptionType(Component.literal("Not found scheduled email, id: " + id)).create();
                                    }
                                    email.setNote(StringArgumentType.getString(ctx, "note"));
                                    ScheduledEmail.save();

                                    ctx.getSource().sendSystemMessage(Component.literal("Success! Scheduled email note has modify to: " + email.getNote()));
                                    return Command.SINGLE_SUCCESS;
                                })
                                .build()
                        )
                );
    }
}
