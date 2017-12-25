package io.github.binaryoverload.commands.general;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import io.github.binaryoverload.CookieBot;
import io.github.binaryoverload.CookieBotManager;
import io.github.binaryoverload.commands.Command;
import io.github.binaryoverload.commands.CommandType;
import stream.flarebot.flarebot.music.VideoThread;
import stream.flarebot.flarebot.objects.GuildWrapper;
import io.github.binaryoverload.util.GeneralUtils;
import io.github.binaryoverload.util.MessageUtils;
import stream.flarebot.flarebot.util.implementations.MultiSelectionContent;

import java.awt.Color;
import java.util.function.Supplier;

public class StatsCommand implements Command {

    @Override
    public void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member) {
        if (args.length == 0) {
            EmbedBuilder bld = MessageUtils.getEmbed(sender).setColor(Color.CYAN)
                    .setThumbnail(MessageUtils.getAvatar(channel.getJDA().getSelfUser()));
            bld.setDescription("CookieBot v" + CookieBot.getInstance().getVersion() + " stats");
            for (MultiSelectionContent<String, String, Boolean> content : Content.values) {
                bld.addField(content.getName(), content.getReturn(), content.isAlign());
            }
            channel.sendMessage(bld.build()).queue();
        } else
            GeneralUtils.handleMultiSelectionCommand(sender, channel, args, Content.values);
    }

    private static String getMb(long bytes) {
        return (bytes / 1024 / 1024) + "MB";
    }

    @Override
    public String getCommand() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "Displays stats about the bot.";
    }

    @Override
    public String getUsage() {
        return "`{%}stats [section]` - Sends stats about the bot.";
    }

    @Override
    public CommandType getType() {
        return CommandType.GENERAL;
    }

    public enum Content implements MultiSelectionContent<String, String, Boolean> {

        SERVERS("Servers", () -> CookieBot.getInstance().getGuilds().size()),
        TOTAL_USERS("Total Users", () -> CookieBot.getInstance().getUsers().size()),
        VOICE_CONNECTIONS("Voice Connections", () -> CookieBot.getInstance().getConnectedVoiceChannels()),
        ACTIVE_CHANNELS("Channels Playing Music", () -> CookieBot.getInstance().getActiveVoiceChannels()),
        TEXT_CHANNELS("Text Channels", () -> CookieBot.getInstance().getChannels().size()),
        LOADED_GUILDS("Loaded Guilds", () -> CookieBotManager.getInstance().getGuilds().size()),
        COMMANDS_EXECUTED("Commands Executed", () -> CookieBot.getInstance().getEvents().getCommandCount()),
        UPTIME("Uptime", () -> CookieBot.getInstance().getUptime()),
        MEM_USAGE("Memory Usage", () -> getMb(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())),
        MEM_FREE("Memory Free", () -> getMb(Runtime.getRuntime().freeMemory())),
        VIDEO_THREADS("Video Threads", VideoThread.VIDEO_THREADS::activeCount),
        TOTAL_THREADS("Total Threads", () -> Thread.getAllStackTraces().size());

        private String name;
        private Supplier<Object> returns;
        private boolean align = true;

        public static Content[] values = values();

        Content(String name, Supplier<Object> returns) {
            this.name = name;
            this.returns = returns;
        }

        public String getName() {
            return name;
        }

        public String getReturn() {
            return String.valueOf(returns.get());
        }

        public Boolean isAlign() {
            return this.align;
        }
    }
}
