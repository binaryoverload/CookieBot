package io.github.binaryoverload.commands.general;

import io.github.binaryoverload.CookieBot;
import io.github.binaryoverload.commands.Command;
import io.github.binaryoverload.commands.CommandType;
import io.github.binaryoverload.permissions.PerGuildPermissions;
import io.github.binaryoverload.util.MessageUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.objects.GuildWrapper;

public class CommandUsageCommand implements Command {

    @Override
    public void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member) {
        if (args.length == 0) {
            MessageUtils.sendUsage(this, channel, sender, args);
        } else {
            Command c = CookieBot.getInstance().getCommand(args[0], sender);
            if (c == null || (c.getType() == CommandType.SECRET && !PerGuildPermissions.isCreator(sender))) {
                MessageUtils.sendErrorMessage("That is not a command!", channel);
            } else {
                MessageUtils.sendUsage(c, channel, sender, new String[]{});
            }
        }
    }

    @Override
    public String getCommand() {
        return "usage";
    }

    @Override
    public String getDescription() {
        return "Allows you to view usages for other commands";
    }

    @Override
    public String getUsage() {
        return "`{%}usage <command_name>` - Displays the usage for another command.";
    }

    @Override
    public CommandType getType() {
        return CommandType.GENERAL;
    }
}
