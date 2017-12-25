package io.github.binaryoverload.commands.secret;

import io.github.binaryoverload.commands.CommandType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import io.github.binaryoverload.commands.Command;
import stream.flarebot.flarebot.objects.GuildWrapper;

public class TestCommand implements Command {

    @Override
    public void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member) {
        // Served it's purpose once again :) Now it's free to be reused... no memes pls
    }

    @Override
    public String getCommand() {
        return "test";
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getUsage() {
        return "{%}test";
    }

    @Override
    public CommandType getType() {
        return CommandType.SECRET;
    }
}
