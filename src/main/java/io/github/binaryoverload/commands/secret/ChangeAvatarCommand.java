package io.github.binaryoverload.commands.secret;

import io.github.binaryoverload.commands.Command;
import io.github.binaryoverload.commands.CommandAuthority;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.io.IOException;
import java.net.URL;

public class ChangeAvatarCommand implements Command {

    @Override
    public void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member) {
        if (args.length == 0) {
            if (!message.getAttachments().isEmpty()) {
                Message.Attachment attachment = message.getAttachments().get(0);
                try {
                    sender.getJDA().getSelfUser().getManager().setAvatar(Icon.from(
                            new URL(attachment.getUrl()).openStream()
                    )).complete();
                } catch (IOException e) {
                    channel.sendMessage("Failed to update avatar!! " + e).queue();
                }
                channel.sendMessage("Success!").queue();
            } else {
                channel.sendMessage("You must either attach an image or link to one!").queue();
            }
        } else {
            try {
                sender.getJDA().getSelfUser().getManager().setAvatar(Icon.from(
                        new URL(args[0]).openStream()
                )).complete();
            } catch (IOException e) {
                channel.sendMessage("Failed to update avatar!! " + e).queue();
            }
            channel.sendMessage("Success!").queue();
        }
    }

    @Override
    public String getCommand() {
        return "avatar";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getUsage() {
        return "{%}avatar [user]";
    }

    @Override
    public CommandAuthority getAuthority() {
        return CommandAuthority.ADMIN;
    }

}
