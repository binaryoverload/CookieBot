package io.github.binaryoverload.commands;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public interface Command {

    void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member);

    String getCommand();

    String getDescription();

    String getUsage();

    default CommandAuthority getAuthority() {
        return CommandAuthority.EVERYONE;
    }

    default String getExtraInfo() {
        return null;
    }

    default String[] getAliases() {
        return new String[]{};
    }

    default boolean deleteMessage() {
        return true;
    }
}
