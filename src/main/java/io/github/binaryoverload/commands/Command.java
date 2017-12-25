package io.github.binaryoverload.commands;

import io.github.binaryoverload.CookieBotManager;
import io.github.binaryoverload.permissions.PerGuildPermissions;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import io.github.binaryoverload.CookieBot;
import stream.flarebot.flarebot.objects.GuildWrapper;

import java.util.EnumSet;

public interface Command {

    void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member);

    String getCommand();

    String getDescription();

    String getUsage();

    CommandType getType();

    default String getExtraInfo() {
        return null;
    }

    default String getPermission() {
        return getType() == CommandType.SECRET ? null : "flarebot." + getCommand();
    }

    default EnumSet<Permission> getDiscordPermission() {
        return EnumSet.noneOf(Permission.class);
    }

    default String[] getAliases() {
        return new String[]{};
    }

    default PerGuildPermissions getPermissions(TextChannel chan) {
        return CookieBotManager.getInstance().getGuild(chan.getGuild().getId()).getPermissions();
    }

    default boolean isDefaultPermission() {
        return (getPermission() != null && getType() != CommandType.SECRET && getType() != CommandType.INTERNAL 
                && getType() != CommandType.MODERATION);
    }

    default boolean deleteMessage() {
        return true;
    }

    default boolean isBetaTesterCommand() {
        return false;
    }

    default char getPrefix(Guild guild) {
        return CookieBot.getPrefix(guild.getId());
    }
}
