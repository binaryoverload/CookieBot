package io.github.binaryoverload.commands.useful;

import io.github.binaryoverload.commands.Command;
import io.github.binaryoverload.commands.CommandType;
import io.github.binaryoverload.scheduler.Scheduler;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.joda.time.Period;
import io.github.binaryoverload.CookieBot;
import stream.flarebot.flarebot.objects.GuildWrapper;
import io.github.binaryoverload.scheduler.FutureAction;
import io.github.binaryoverload.util.GeneralUtils;
import io.github.binaryoverload.util.MessageUtils;

public class RemindCommand implements Command {

    @Override
    public void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member) {
        if (args.length < 2) {
            MessageUtils.sendUsage(this, channel, sender, args);
        } else {
            Period period;
            if ((period = GeneralUtils.getTimeFromInput(args[0], channel)) == null) return;
            String reminder = CookieBot.getMessage(args, 1);
            channel.sendMessage("\uD83D\uDC4D I will remind you in " + GeneralUtils.formatJodaTime(period).toLowerCase() + " (at "
                    + GeneralUtils.formatPrecisely(period) + ") to `" + reminder + "`").queue();

            Scheduler.queueFutureAction(guild.getGuildIdLong(), channel.getIdLong(), sender.getIdLong(), reminder.substring(0,
                    Math.min(reminder.length(), 1000)), period, FutureAction.Action.REMINDER);
        }
    }

    @Override
    public String getCommand() {
        return "remind";
    }

    @Override
    public String getDescription() {
        return "Get reminders about things easily!";
    }

    @Override
    public String getUsage() {
        return "`{%}remind <duration> <reminder>` - Reminds a user about something after a duration.";
    }

    @Override
    public CommandType getType() {
        return CommandType.USEFUL;
    }

    @Override
    public String[] getAliases() {
        return new String[]{"r", "reminder"};
    }

    @Override
    public boolean isBetaTesterCommand() {
        return true;
    }
}
