package io.github.binaryoverload.commands.secret;

import io.github.binaryoverload.commands.Command;
import io.github.binaryoverload.commands.CommandAuthority;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.io.File;

public class LogsCommand implements Command {

    @Override
    public void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member) {
        channel.sendFile(new File("latest.log"), new MessageBuilder().append('\u200B').build()).queue();
    }

    @Override
    public String getCommand() {
        return "logs";
    }

    @Override
    public String getDescription() {
        return "Gets the logs";
    }

    @Override
    public String getUsage() {
        return "{%}logs";
    }

    @Override
    public CommandAuthority getAuthority() {
        return CommandAuthority.ADMIN;
    }
}
