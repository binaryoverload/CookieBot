package io.github.binaryoverload.commands.secret.internal;

import io.github.binaryoverload.commands.CommandType;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import io.github.binaryoverload.commands.Command;
import stream.flarebot.flarebot.objects.GuildWrapper;
import io.github.binaryoverload.permissions.PerGuildPermissions;
import stream.flarebot.flarebot.util.GitHubUtils;

public class ChangelogCommand implements Command {

    @Override
    public void onCommand(User sender, Guild guild, TextChannel channel, Message msg, String[] args, Member member) {
        if (PerGuildPermissions.isStaff(sender)) {
            if (args.length == 0) {
                channel.sendMessage("Specify a version or PR to post about!").queue();
                return;
            }
            if (args[0].startsWith("pr:")) {
                channel.sendMessage(new MessageBuilder().setEmbed(GitHubUtils.getEmbedForPR(args[0].substring(3))
                        .build()).build()).queue();
            } else {
                String message = msg.getContentRaw();
                message = message.substring(message.indexOf(" ") + 1);
                channel.sendMessage(message).queue();
            }
        }
    }

    @Override
    public String getCommand() {
        return "changelog";
    }

    @Override
    public String getDescription() {
        return "Get version changelogs";
    }

    @Override
    public String getUsage() {
        return "{%}changelog <version>\n`{%}changelog pr:<prNum>`";
    }

    @Override
    public CommandType getType() {
        return CommandType.INTERNAL;
    }
}
