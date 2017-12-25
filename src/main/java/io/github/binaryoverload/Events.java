package io.github.binaryoverload;

import io.github.binaryoverload.commands.CommandType;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.DisconnectEvent;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.StatusChangeEvent;
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;
import org.slf4j.Logger;
import io.github.binaryoverload.commands.Command;
import io.github.binaryoverload.permissions.PerGuildPermissions;
import io.github.binaryoverload.util.GeneralUtils;
import io.github.binaryoverload.util.MessageUtils;
import io.github.binaryoverload.util.WebUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class Events extends ListenerAdapter {

    private static final ThreadGroup COMMAND_THREADS = new ThreadGroup("Command Threads");
    private static final ExecutorService CACHED_POOL = Executors.newCachedThreadPool(r ->
            new Thread(COMMAND_THREADS, r, "Command Pool-" + COMMAND_THREADS.activeCount()));

    private final Logger LOGGER = CookieBot.getLog(this.getClass());
    private final Pattern multiSpace = Pattern.compile(" {2,}");

    private CookieBot cookieBot;

    Events(CookieBot bot) {
        this.cookieBot = bot;
    }

    public void onReady(ReadyEvent event) {
        CookieBot.getInstance().run();
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        String message = multiSpace.matcher(event.getMessage().getContentRaw()).replaceAll(" ");
        if (message.startsWith(String.valueOf(CookieBot.getPrefixes().get(getGuildId(event))))) {
            List<Permission> perms = event.getChannel().getGuild().getSelfMember().getPermissions(event.getChannel());
            if (!perms.contains(Permission.ADMINISTRATOR)) {
                if (!perms.contains(Permission.MESSAGE_WRITE)) {
                    return;
                }
                if (!perms.contains(Permission.MESSAGE_EMBED_LINKS)) {
                    event.getChannel().sendMessage("Hey! I can't be used here." +
                            "\nI do not have the `Embed Links` permission! Please go to your permissions and give me Embed Links." +
                            "\nThanks :D").queue();
                    return;
                }
            }

            String command = message.substring(1);
            String[] args = new String[0];
            if (message.contains(" ")) {
                command = command.substring(0, message.indexOf(" ") - 1);
                args = message.substring(message.indexOf(" ") + 1).split(" ");
            }
            Command cmd = cookieBot.getCommand(command, event.getAuthor());
            if (cmd != null)
                handleCommand(event, cmd, args);
        } else {
        }
    }

    @Override
    public void onStatusChange(StatusChangeEvent event) {
        if (CookieBot.EXITING.get()) return;
        String statusHook = CookieBot.getStatusHook();
        if (statusHook == null) return;
        Request.Builder request = new Request.Builder().url(statusHook);
        RequestBody body = RequestBody.create(WebUtils.APPLICATION_JSON, new JSONObject()
                .put("content", String.format("onStatusChange: %s -> %s SHARD: %d",
                        event.getOldStatus(), event.getStatus(),
                        event.getJDA().getShardInfo() != null ? event.getJDA().getShardInfo().getShardId()
                                : null)).toString());
        WebUtils.postAsync(request.post(body));
    }

    @Override
    public void onDisconnect(DisconnectEvent event) {
        if (event.isClosedByServer())
            LOGGER.error(String.format("---- DISCONNECT [SERVER] CODE: [%d] %s%n", event.getServiceCloseFrame()
                    .getCloseCode(), event
                    .getCloseCode()));
        else
            LOGGER.error(String.format("---- DISCONNECT [CLIENT] CODE: [%d] %s%n", event.getClientCloseFrame()
                    .getCloseCode(), event
                    .getClientCloseFrame().getCloseReason()));
    }

    private void handleCommand(GuildMessageReceivedEvent event, Command cmd, String[] args) {
        if (cmd.getType() == CommandType.SECRET && !PerGuildPermissions.isCreator(event.getAuthor()) && !(cookieBot.isTestBot()
                && PerGuildPermissions.isContributor(event.getAuthor()))) {
            GeneralUtils.sendImage("https://flarebot.stream/img/trap.jpg", "trap.jpg", event.getAuthor());
            return;
        }
        CACHED_POOL.submit(() -> {
            LOGGER.info(
                    "Dispatching command '" + cmd.getCommand() + "' " + Arrays
                            .toString(args) + " in " + event.getChannel() + "! Sender: " +
                            event.getAuthor().getName() + '#' + event.getAuthor().getDiscriminator());
            try {
                cmd.onCommand(event.getAuthor(), event.getGuild(), event.getChannel(), event.getMessage(), args, event
                        .getMember());
            } catch (Exception ex) {
                MessageUtils
                        .sendException("**There was an internal error trying to execute your command**", ex, event
                                .getChannel());
                LOGGER.error("Exception in guild " + event.getGuild().getId() + "!\n" + '\'' + cmd.getCommand() + "' "
                        + Arrays.toString(args) + " in " + event.getChannel() + "! Sender: " +
                        event.getAuthor().getName() + '#' + event.getAuthor().getDiscriminator(), ex);
            }
            if (cmd.deleteMessage()) {
                delete(event.getMessage());
            }
        });
    }

    private void delete(Message message) {
        if (message.getTextChannel().getGuild().getSelfMember()
                .getPermissions(message.getTextChannel()).contains(Permission.MESSAGE_MANAGE))
            message.delete().queue();
    }

    private String getGuildId(GenericGuildMessageEvent e) {
        return e.getChannel().getGuild() != null ? e.getChannel().getGuild().getId() : null;
    }

}
