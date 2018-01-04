package io.github.binaryoverload.util;

import com.arsenarsen.lavaplayerbridge.player.Player;
import com.arsenarsen.lavaplayerbridge.player.Track;
import com.google.gson.JsonElement;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import io.github.binaryoverload.CookieBot;
import io.github.binaryoverload.CookieBotManager;
import io.github.binaryoverload.JSONConfig;
import io.github.binaryoverload.commands.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.ErrorResponseException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import stream.flarebot.flarebot.database.RedisMessage;
import stream.flarebot.flarebot.objects.GuildWrapper;
import stream.flarebot.flarebot.objects.Report;
import stream.flarebot.flarebot.objects.ReportMessage;
import io.github.binaryoverload.util.errorhandling.Markers;
import stream.flarebot.flarebot.util.implementations.MultiSelectionContent;

import javax.net.ssl.HttpsURLConnection;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GeneralUtils {

    private static final DecimalFormat percentageFormat = new DecimalFormat("#.##");
    private static final Pattern userDiscrim = Pattern.compile(".+#[0-9]{4}");
    private static final Pattern timeRegex = Pattern.compile("^([0-9]*):?([0-9]*)?:?([0-9]*)?$");

    private static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("MMMM yyyy HH:mm:ss");

    private static final SimpleDateFormat preciseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS");


    public static String formatCommandPrefix(TextChannel channel, String usage) {
        String prefix = String.valueOf(Constants.COMMAND_CHAR);
        if (usage.contains("{%}"))
            return usage.replaceAll("\\{%}", prefix);
        return usage;
    }


    public static List<Role> getRole(String string, Guild guild) {
        return guild.getRolesByName(string, true);
    }

    public static User getUser(String s) {
        return getUser(s, null);
    }

    public static User getUser(String s, String guildId) {
        return getUser(s, guildId, false);
    }

    public static User getUser(String s, boolean forceGet) {
        return getUser(s, null, forceGet);
    }

    public static User getUser(String s, String guildId, boolean forceGet) {
        if (userDiscrim.matcher(s).find()) {
            if (guildId == null || guildId.isEmpty()) {
                return CookieBot.getInstance().getClient().getUsers().stream()
                        .filter(user -> (user.getName() + "#" + user.getDiscriminator()).equalsIgnoreCase(s))
                        .findFirst().orElse(null);
            } else {
                try {
                    return CookieBot.getInstance().getGuildById(guildId).getMembers().stream()
                            .map(Member::getUser)
                            .filter(user -> (user.getName() + "#" + user.getDiscriminator()).equalsIgnoreCase(s))
                            .findFirst().orElse(null);
                } catch (NullPointerException ignored) {
                }
            }
        } else {
            User tmp;
            if (guildId == null || guildId.isEmpty()) {
                tmp = CookieBot.getInstance().getClient().getUsers().stream().filter(user -> user.getName().equalsIgnoreCase(s))
                        .findFirst().orElse(null);
            } else {
                if (CookieBot.getInstance().getGuildById(guildId) != null) {
                    tmp = CookieBot.getInstance().getGuildById(guildId).getMembers().stream()
                            .map(Member::getUser)
                            .filter(user -> user.getName().equalsIgnoreCase(s))
                            .findFirst().orElse(null);
                } else
                    tmp = null;
            }
            if (tmp != null) return tmp;
            try {
                long l = Long.parseLong(s.replaceAll("[^0-9]", ""));
                if (guildId == null || guildId.isEmpty()) {
                    tmp = CookieBot.getInstance().getClient().getUserById(l);
                } else {
                    Member temMember = CookieBot.getInstance().getGuildById(guildId).getMemberById(l);
                    if (temMember != null) {
                        tmp = temMember.getUser();
                    }
                }
                if (tmp != null) {
                    return tmp;
                } else if (forceGet) {
                    return CookieBot.getInstance().getClient().retrieveUserById(l).complete();
                }
            } catch (NumberFormatException | NullPointerException ignored) {
            }
        }
        return null;
    }



    public static Emote getEmoteById(long l) {
        return CookieBot.getInstance().getClient().getGuilds().stream().map(g -> g.getEmoteById(l))
                .filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * This will download and cache the image if not found already!
     *
     * @param fileUrl  Url to download the image from.
     * @param fileName Name of the image file.
     * @param user     User to send the image to.
     */
    public static void sendImage(String fileUrl, String fileName, User user) {
        try {
            File dir = new File("imgs");
            if (!dir.exists() && !dir.mkdir())
                throw new IllegalStateException("Cannot create 'imgs' folder!");
            File image = new File("imgs" + File.separator + fileName);
            if (!image.exists() && image.createNewFile()) {
                URL url = new URL(fileUrl);
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 CookieBot");
                InputStream is = conn.getInputStream();
                OutputStream os = new FileOutputStream(image);
                byte[] b = new byte[2048];
                int length;
                while ((length = is.read(b)) != -1) {
                    os.write(b, 0, length);
                }
                is.close();
                os.close();
            }
            user.openPrivateChannel().complete().sendFile(image, fileName, null)
                    .queue();
        } catch (IOException | ErrorResponseException e) {
            CookieBot.LOGGER.error("Unable to send image '" + fileName + "'", e);
        }
    }

    public static String getStackTrace(Throwable e) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        printWriter.close();
        return writer.toString();
    }

    public static int getInt(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long getLong(String s, long defaultValue) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static void methodErrorHandler(Logger logger, String startMessage,
                                          String successMessage, String errorMessage,
                                          Runnable runnable) {
        Objects.requireNonNull(successMessage);
        Objects.requireNonNull(errorMessage);
        if (startMessage != null) logger.info(startMessage);
        try {
            runnable.run();
            logger.info(successMessage);
        } catch (Exception e) {
            logger.error(errorMessage, e);
        }
    }


    /**
     * Checks if paths exist in the given json
     * <p>
     * Key of the {@link Pair} is a list of the paths that exist in the JSON
     * Value of the {@link Pair} is a list of the paths that don't exist in the JSON
     *
     * @param json  The JSON to check <b>Mustn't be null</b>
     * @param paths The paths to check <b>Mustn't be null or empty</b>
     * @return
     */
    public static Pair<List<String>, List<String>> jsonContains(String json, String... paths) {
        Objects.requireNonNull(json);
        Objects.requireNonNull(paths);
        if (paths.length == 0)
            throw new IllegalArgumentException("Paths cannot be empty!");
        JsonElement jelem = CookieBot.GSON.fromJson(json, JsonElement.class);
        JSONConfig config = new JSONConfig(jelem.getAsJsonObject());
        List<String> contains = new ArrayList<>();
        List<String> notContains = new ArrayList<>();
        for (String path : paths) {
            if (path == null) continue;
            if (config.getElement(path).isPresent())
                contains.add(path);
            else
                notContains.add(path);
        }
        return new Pair<>(Collections.unmodifiableList(contains), Collections.unmodifiableList(notContains));
    }

}
