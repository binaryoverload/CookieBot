package io.github.binaryoverload.commands.secret;

import io.github.binaryoverload.CookieBot;
import io.github.binaryoverload.CookieBotManager;
import io.github.binaryoverload.commands.Command;
import io.github.binaryoverload.commands.CommandType;
import io.github.binaryoverload.permissions.PerGuildPermissions;
import io.github.binaryoverload.util.MessageUtils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import stream.flarebot.flarebot.objects.GuildWrapper;
import io.github.binaryoverload.util.GeneralUtils;

import java.awt.Color;
import java.util.Map;

public class DisableCommandCommand implements Command {

    @Override
    public void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member) {
        if (PerGuildPermissions.isCreator(sender) || PerGuildPermissions.isContributor(sender)) {
            if (args.length == 0) {
                channel.sendMessage("Can't really disable commands if you don't supply any ¯\\_(ツ)_/¯").queue();
                return;
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
                StringBuilder sb = new StringBuilder();
                Map<String, String> disabledCmds = CookieBotManager.getInstance().getDisabledCommands();
                for (String cmd : disabledCmds.keySet()) {
                    sb.append("`").append(cmd).append("` - ").append(disabledCmds.get(cmd));
                }
                channel.sendMessage(MessageUtils.getEmbed(sender).setColor(Color.orange).addField("Disabled Commands",
                        sb.toString(), false).build()).queue();
                return;
            }
            String msg = CookieBot.getMessage(args, 0);
            String reason = "This command is currently disabled! Please check our support server for more info! " +
                    "https://flarebot.stream/support-server";
            if (msg.contains("-"))
                reason = msg.substring(msg.indexOf("-") + 1).trim();
            String[] cmds = msg.substring(0, msg.contains("-") ? msg.indexOf("-") : msg.length()).trim().split("\\|");
            StringBuilder sb = new StringBuilder();
            for (String command : cmds) {
                Command cmd = CookieBot.getInstance().getCommand(command.trim(), sender);
                if (cmd == null || cmd.getType() == CommandType.SECRET) continue;

                // If it's already disabled and there's only 1 (with a reason supplied) just update the reason.
                if (cmds.length == 1 && msg.contains("-") && CookieBotManager.getInstance().isCommandDisabled(cmd.getCommand())) {
                    CookieBotManager.getInstance().getDisabledCommands().put(cmd.getCommand(), reason);
                    sb.append("`").append(cmd.getCommand()).append("` - ").append(getEmote(false))
                            .append(" New reason: ").append(reason);
                } else
                    sb.append("`").append(cmd.getCommand()).append("` - ").append(getEmote(CookieBotManager.getInstance()
                            .toggleCommand(cmd.getCommand(), reason))).append("\n");
            }
            if (sb.toString().isEmpty()) return;
            channel.sendMessage(MessageUtils.getEmbed(sender).setColor(Color.orange).setDescription(sb.toString())
                    .build()).queue();
        }
    }

    @Override
    public String getCommand() {
        return "disablecommand";
    }

    @Override
    public String getDescription() {
        return "Disable or enable commands.";
    }

    @Override
    public String getUsage() {
        return "{%}disablecommand <command | ...> (- reason)";
    }

    @Override
    public CommandType getType() {
        return CommandType.SECRET;
    }

    @Override
    public String[] getAliases() {
        return new String[]{"fuckthat", "kill", "disablecmd"};
    }

    private String getEmote(boolean b) {
        // tick - enabled (true)
        return (b ? GeneralUtils.getEmoteById(355776056092917761L).getAsMention()
                // cross - disabled (false)
                : GeneralUtils.getEmoteById(355776081384570881L).getAsMention());
    }
}
