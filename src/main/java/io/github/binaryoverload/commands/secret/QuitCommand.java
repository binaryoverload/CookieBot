package io.github.binaryoverload.commands.secret;

import io.github.binaryoverload.CookieBot;
import io.github.binaryoverload.commands.Command;
import io.github.binaryoverload.commands.CommandAuthority;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class QuitCommand implements Command {

    @Override
    public void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member) {
        CookieBot.getInstance().quit();
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
    public CommandAuthority getAuthority() {
        return CommandAuthority.ADMIN;
    }

}
