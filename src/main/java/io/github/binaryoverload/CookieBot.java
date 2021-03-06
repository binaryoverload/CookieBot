package io.github.binaryoverload;

import ch.qos.logback.classic.Level;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.binaryoverload.commands.Command;
import io.github.binaryoverload.commands.CommandAuthority;
import io.github.binaryoverload.commands.secret.EvalCommand;
import io.github.binaryoverload.commands.secret.LogsCommand;
import io.github.binaryoverload.commands.secret.QuitCommand;
import io.github.binaryoverload.database.CassandraController;
import io.github.binaryoverload.scheduler.Scheduler;
import io.github.binaryoverload.util.Constants;
import io.github.binaryoverload.util.GeneralUtils;
import io.github.binaryoverload.util.MessageUtils;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CookieBot {

    private static final Map<String, Logger> LOGGERS;
    public static final Logger LOGGER;
    private static boolean testBot;

    static {
        handleLogArchive();
        LOGGERS = new ConcurrentHashMap<>();
        LOGGER = getLog(CookieBot.class.getName());
    }

    private static CookieBot instance;

    private static JSONConfig config;
    private CookieBotManager manager;

    public static final Gson GSON = new GsonBuilder().create();

    public static final AtomicBoolean EXITING = new AtomicBoolean(false);

    private Events events;
    private String version = null;
    private JDA client;

    private Set<Command> commands = Collections.newSetFromMap(new ConcurrentHashMap<Command, Boolean>());
    private long startTime;

    public static void main(String[] args) {
        try {
            File file = new File("config.json");
            if (!file.exists() && !file.createNewFile())
                throw new IllegalStateException("Can't create config file!");
            try {
                config = new JSONConfig("config.json");
            } catch (NullPointerException e) {
                LOGGER.error("Invalid JSON!", e);
                System.exit(1);
            }
        } catch (IOException e) {
            LOGGER.error("Unable to create config.json!", e);
            System.exit(1);
        }

        List<String> required = new ArrayList<>();
        required.add("bot.token");
        required.add("cassandra.username");
        required.add("cassandra.password");

        boolean good = true;
        for (String req : required) {
            if (config.getString(req) != null) {
                if (!config.getString(req).isPresent()) {
                    good = false;
                    LOGGER.error("Missing required json " + req);
                }
            } else {
                good = false;
                LOGGER.error("Missing required json " + req);
            }
        }

        if (!good) {
            LOGGER.error("One or more of the required JSON objects where missing. Exiting to prevent problems");
            System.exit(1);
        }

        new CassandraController(config);

        if (config.getArray("options").isPresent()) {
            for (JsonElement em : config.getArray("options").get()) {
                if (em.getAsString() != null) {
                    if (em.getAsString().equals("tb")) {
                        CookieBot.testBot = true;
                    }
                    if (em.getAsString().equals("debug")) {
                        ((ch.qos.logback.classic.Logger) LoggerFactory
                                .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME))
                                .setLevel(Level.DEBUG);
                    }
                }
            }
        }

        Thread.setDefaultUncaughtExceptionHandler(((t, e) -> LOGGER.error("Uncaught exception in thread " + t, e)));
        Thread.currentThread()
                .setUncaughtExceptionHandler(((t, e) -> LOGGER.error("Uncaught exception in thread " + t, e)));
        try {
            (instance = new CookieBot()).init();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isTestBot() {
        return testBot;
    }

    public Events getEvents() {
        return events;
    }

    public void init() throws InterruptedException {
        LOGGER.info("Starting init!");
        manager = new CookieBotManager();
        RestAction.DEFAULT_FAILURE = t -> {
        };
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        events = new Events(this);
        LOGGER.info("Starting builders");

        try {
            client = new JDABuilder(AccountType.BOT)
                    .addEventListener(events)
                    .setToken(config.getString("bot.token").get())
                    .buildBlocking();
        } catch (Exception e) {
            LOGGER.error("Could not log in!", e);
            Thread.sleep(500);
            System.exit(1);
            return;
        }
        System.setErr(new PrintStream(new OutputStream() {
            // Nothing really so all good.
            @Override
            public void write(int b) {
            }
        })); // No operation STDERR. Will not do much of anything, except to filter out some Jsoup spam

        manager = new CookieBotManager();
        manager.executeCreations();
    }

    protected void run() {

        registerCommand(new InfoCommand());
        registerCommand(new QuitCommand());
        registerCommand(new LogsCommand());
        registerCommand(new EvalCommand());
        registerCommand(new StatsCommand());

        LOGGER.info("Loaded " + commands.size() + " commands!");

        GeneralUtils.methodErrorHandler(LOGGER, null,
                "Executed creations!", "Failed to execute creations!",
                () -> manager.executeCreations());

        startTime = System.currentTimeMillis();
        LOGGER.info("CookieBot v" + getVersion() + " booted!");
    }

    /**
     * This will always return the main shard or just the client itself.
     * For reference the main shard will always be shard 0 - the shard responsible for DMs
     *
     * @return The main shard or actual client in the case of only 1 shard.
     */
    public JDA getClient() {
        return client;
    }

    public SelfUser getSelfUser() {
        return getClient().getSelfUser();
    }

    private Runtime runtime = Runtime.getRuntime();

    public void quit() {
        LOGGER.info("Exiting.");
        stop();
        System.exit(0);
    }

    // TODO: Saving
    private void stop() {
        if (EXITING.get()) return;
        LOGGER.info("Saving data.");
        EXITING.set(true);
        for (ScheduledFuture<?> scheduledFuture : Scheduler.getTasks().values())
            scheduledFuture.cancel(false); // No tasks in theory should block this or cause issues. We'll see
        LOGGER.info("Finished saving!");
        client.shutdown();
    }

    private void registerCommand(Command command) {
        this.commands.add(command);
    }

    // https://bots.are-pretty.sexy/214501.png
    // New way to process commands, this way has been proven to be quicker overall.
    public Command getCommand(String s, User user) {
        if (CommandAuthority.ADMIN.hasPerm(user)) {
            for (Command cmd : getCommandsByAuthority(CommandAuthority.ADMIN)) {
                if (cmd.getCommand().equalsIgnoreCase(s))
                    return cmd;
                for (String alias : cmd.getAliases())
                    if (alias.equalsIgnoreCase(s)) return cmd;
            }
        }
        for (Command cmd : getCommands()) {
            if (cmd.getAuthority() == CommandAuthority.ADMIN) continue;
            if (cmd.getCommand().equalsIgnoreCase(s))
                return cmd;
            for (String alias : cmd.getAliases())
                if (alias.equalsIgnoreCase(s)) return cmd;
        }
        return null;
    }

    public Set<Command> getCommands() {
        return this.commands;
    }

    public Set<Command> getCommandsByAuthority(CommandAuthority authority) {
        return commands.stream().filter(command -> command.getAuthority() == authority).collect(Collectors.toSet());
    }

    public static CookieBot getInstance() {
        return instance;
    }

    public String getUptime() {
        long totalSeconds = (System.currentTimeMillis() - startTime) / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = (totalSeconds / 3600);
        return (hours < 10 ? "0" + hours : hours) + "h " + (minutes < 10 ? "0" + minutes : minutes) + "m " + (seconds < 10 ? "0" + seconds : seconds) + "s";
    }

    public String getVersion() {
        if (version == null) {
            Properties p = new Properties();
            try {
                p.load(getClass().getClassLoader().getResourceAsStream("version.properties"));
            } catch (IOException e) {
                LOGGER.error("There was an error trying to load the version!", e);
                return null;
            }
            version = (String) p.get("version");
        }
        return version;
    }

    public static String getMessage(String[] args) {
        StringBuilder msg = new StringBuilder();
        for (String arg : args) {
            msg.append(arg).append(" ");
        }
        return msg.toString().trim();
    }

    public static String getMessage(String[] args, int min) {
        return Arrays.stream(args).skip(min).collect(Collectors.joining(" ")).trim();
    }

    public static String getMessage(String[] args, int min, int max) {
        StringBuilder message = new StringBuilder();
        for (int index = min; index < max; index++) {
            message.append(args[index]).append(" ");
        }
        return message.toString().trim();
    }

    // Disabled for now.
    // TODO: Make sure the API has a way to handle this and also update that page.
    public static void reportError(TextChannel channel, String s, Exception e) {
        JsonObject message = new JsonObject();
        message.addProperty("message", s);
        message.addProperty("exception", GeneralUtils.getStackTrace(e));
        MessageUtils.sendErrorMessage(s, channel);
        //String id = instance.postToApi("postReport", "error", message);
        //MessageUtils.sendErrorMessage(s + "\nThe error has been reported! You can follow the report on the website, https://flarebot.stream/report?id=" + id, channel);
    }

    public static String getStatusHook() {
        return config.getString("bot.statusHook").isPresent() ? config.getString("bot.statusHook").get() : null;
    }

    public String formatTime(long duration, TimeUnit durUnit, boolean fullUnits, boolean append0) {
        long totalSeconds = 0;
        switch (durUnit) {
            case MILLISECONDS:
                totalSeconds = duration / 1000;
                break;
            case SECONDS:
                totalSeconds = duration;
                break;
            case MINUTES:
                totalSeconds = duration * 60;
                break;
            case HOURS:
                totalSeconds = (duration * 60) * 60;
                break;
            case DAYS:
                totalSeconds = ((duration * 60) * 60) * 24;
                break;
        }
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = (totalSeconds / 3600) % 24;
        long days = (totalSeconds / 86400);
        return (days > 0 ? (append0 && days < 10 ? "0" + days : days) + (fullUnits ? " days " : "d ") : "")
                + (hours > 0 ? (append0 && hours < 10 ? "0" + hours : hours) + (fullUnits ? " hours " : "h ") : "")
                + (minutes > 0 ? (append0 && minutes < 10 ? "0" + minutes : minutes) + (fullUnits ? " minutes" : "m ") : "")
                + (seconds > 0 ? (append0 && seconds < 10 ? "0" + seconds : seconds) + (fullUnits ? " seconds" : "s") : "")
                .trim();
    }

    public TextChannel getErrorLogChannel() {
        return (testBot ? getChannelById(Constants.FLARE_TEST_BOT_CHANNEL) : getChannelById("226786557862871040"));
    }

    public TextChannel getImportantLogChannel() {
        return (testBot ? getChannelById(Constants.FLARE_TEST_BOT_CHANNEL) : getChannelById("358978253966278657"));
    }

    private TextChannel getChannelById(String channelId) {
        return client.getTextChannelById(channelId);
    }

    public Guild getGuildById(String guildId) {
        return client.getGuildById(guildId);
    }

    public CookieBotManager getManager() {
        return this.manager;
    }

    private static Logger getLog(String name) {
        return LOGGERS.computeIfAbsent(name, LoggerFactory::getLogger);
    }

    public static Logger getLog(Class<?> clazz) {
        return getLog(clazz.getName());
    }

    public String getPasteKey() {
        return config.getString("bot.pasteAccessKey").isPresent() ? config.getString("bot.pasteAccessKey").get() : null;
    }

    private WebhookClient importantHook;

    private WebhookClient getImportantWebhook() {
        if (!config.getString("bot.importantHook").isPresent())
            return null;
        if (importantHook == null)
            importantHook = new WebhookClientBuilder(config.getString("bot.importantHook").get()).build();
        return importantHook;
    }

    private static void handleLogArchive() {
        try {
            byte[] buffer = new byte[1024];
            String time = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss").format(new Date());

            File dir = new File("logs");
            if (!dir.exists() && !dir.mkdir())
                LOGGER.error("Failed to create directory for latest log!");
            File f = new File(dir, "latest.log " + time + ".zip");
            File latestLog = new File("latest.log");

            FileOutputStream fos = new FileOutputStream(f);
            ZipOutputStream zos = new ZipOutputStream(fos);
            ZipEntry entry = new ZipEntry(latestLog.getName());
            zos.putNextEntry(entry);
            FileInputStream in = new FileInputStream(latestLog);

            int len;
            while ((len = in.read(buffer)) > 0)
                zos.write(buffer, 0, len);

            in.close();
            zos.closeEntry();
            zos.close();
            fos.close();

            if (!latestLog.delete()) {
                throw new IllegalStateException("Failed to delete the old log file!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
