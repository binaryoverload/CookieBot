package io.github.binaryoverload.commands.secret;

import io.github.binaryoverload.CookieBot;
import io.github.binaryoverload.commands.Command;
import io.github.binaryoverload.commands.CommandType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.objects.GuildWrapper;

public class QuitCommand implements Command {

    @Override
    public void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member) {
        CookieBot.getInstance().quit(false);
    }

    @Override
    public String getCommand() {
        return "quit";
    }

    @Override
    public String getDescription() {
        return "Dev only command";
    }

    @Override
    public String getUsage() {
        return "{%}quit";
    }

    @Override
    public CommandType getType() {
        return CommandType.SECRET;
    }

    @Override
    public boolean isDefaultPermission() {
        return false;
    }
}
