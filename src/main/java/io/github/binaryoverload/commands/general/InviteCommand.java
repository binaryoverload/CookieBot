package io.github.binaryoverload.commands.general;

import io.github.binaryoverload.commands.Command;
import io.github.binaryoverload.commands.CommandType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import io.github.binaryoverload.CookieBot;
import stream.flarebot.flarebot.objects.GuildWrapper;
import io.github.binaryoverload.util.MessageUtils;

public class InviteCommand implements Command {

    @Override
    public void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member) {
        MessageUtils.sendPM(channel, sender, "You can invite me to your server using the link below!\n"
                + CookieBot.getInstance().getInvite());
    }

    @Override
    public String getCommand() {
        return "invite";
    }

    @Override
    public String getDescription() {
        return "Get my invite link!";
    }

    @Override
    public String getUsage() {
        return "`{%}invite` - Gets CookieBot's invite link.";
    }

    @Override
    public CommandType getType() {
        return CommandType.GENERAL;
    }
}
