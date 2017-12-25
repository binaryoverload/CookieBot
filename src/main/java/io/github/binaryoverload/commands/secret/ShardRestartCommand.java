package io.github.binaryoverload.commands.secret;

import io.github.binaryoverload.CookieBot;
import io.github.binaryoverload.commands.Command;
import io.github.binaryoverload.commands.CommandType;
import io.github.binaryoverload.util.MessageUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.objects.GuildWrapper;

public class ShardRestartCommand implements Command {

    @Override
    public void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member) {
        if (getPermissions(channel).isCreator(sender)) {
            int shard = Integer.parseInt(args[0]);
            if (shard >= 0 && shard < CookieBot.getInstance().getShards().size()) {
                CookieBot.getInstance().getShardManager().restart(shard);
                MessageUtils.sendSuccessMessage("Restarting shard " + shard, channel);
            } else
                MessageUtils.sendErrorMessage("Invalid shard ID!", channel);
        }
    }

    @Override
    public String getCommand() {
        return "restart";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getUsage() {
        return "{%}restart <shard>";
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
