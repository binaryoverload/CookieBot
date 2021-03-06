package io.github.binaryoverload.commands.secret;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import io.github.binaryoverload.CookieBot;
import io.github.binaryoverload.commands.Command;
import io.github.binaryoverload.commands.CommandAuthority;
import io.github.binaryoverload.database.CassandraController;
import io.github.binaryoverload.util.MessageUtils;
import io.github.binaryoverload.util.errorhandling.Markers;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.apache.commons.codec.binary.StringUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EvalCommand implements Command {

    private static PreparedStatement insertSnippet;

    private ScriptEngineManager manager = new ScriptEngineManager();
    private static final ThreadGroup EVAL_POOL = new ThreadGroup("EvalCommand Thread Pool");
    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> new Thread(EVAL_POOL, r,
            EVAL_POOL.getName() + EVAL_POOL.activeCount()));
    private static final List<String> IMPORTS = Arrays.asList("stream.flarebot.flarebot",
            "stream.flarebot.flarebot.music",
            "stream.flarebot.flarebot.util",
            "stream.flarebot.flarebot.util.currency",
            "stream.flarebot.flarebot.util.objects",
            "stream.flarebot.flarebot.objects",
            "stream.flarebot.flarebot.web",
            "stream.flarebot.flarebot.mod",
            "stream.flarebot.flarebot.mod.modlog",
            "stream.flarebot.flarebot.mod.automod",
            "stream.flarebot.flarebot.mod.events",
            "stream.flarebot.flarebot.scheduler",
            "stream.flarebot.flarebot.database",
            "stream.flarebot.flarebot.permissions",
            "stream.flarebot.flarebot.commands",
            "stream.flarebot.flarebot.music.extractors",
            "net.dv8tion.jda.core",
            "net.dv8tion.jda.core.managers",
            "net.dv8tion.jda.core.entities.impl",
            "net.dv8tion.jda.core.entities",
            "net.dv8tion.jda.core.utils",
            "java.util.streams",
            "java.util",
            "java.lang",
            "java.text",
            "java.lang",
            "java.math",
            "java.time",
            "java.io",
            "java.nio",
            "java.nio.files",
            "java.util.stream");

    @Override
    public void onCommand(User sender, Guild guild, TextChannel channel, Message message, String[] args, Member member) {
        if (args.length == 0) {
            MessageUtils.sendErrorMessage("Eval something at least smh!", channel);
            return;
        }
        String imports =
                IMPORTS.stream().map(s -> "Packages." + s).collect(Collectors.joining(", ", "var imports = new JavaImporter(", ");\n"));
        ScriptEngine engine = manager.getEngineByName("nashorn");
        engine.put("channel", channel);
        engine.put("guild", guild);
        engine.put("message", message);
        engine.put("jda", sender.getJDA());
        engine.put("sender", sender);

        String msg = CookieBot.getMessage(args);
        final String[] code = {getCode(args)};

        boolean silent = hasOption(Options.SILENT, msg);
        if (hasOption(Options.SNIPPET, msg)) {
            String snippetName = MessageUtils.getNextArgument(msg, Options.SNIPPET.getAsArgument());
            if (snippetName == null) {
                MessageUtils.sendErrorMessage("Please specify the snippet you wish to run! Do `-snippet (name)`", channel);
                return;
            }

            CassandraController.runTask(session -> {
                ResultSet set = session.execute("SELECT encoded_code FROM flarebot.eval_snippets WHERE snippet_name = '"
                        + snippetName + "'");
                Row row = set.one();
                if (row != null) {
                    code[0] = StringUtils.newStringUtf8(Base64.getDecoder().decode(row.getString("encoded_code").getBytes()));
                } else {
                    MessageUtils.sendErrorMessage("That eval snippet does not exist!", channel);
                    code[0] = null;
                }
            });
        }

        if (hasOption(Options.SAVE, msg)) {
            String base64 = Base64.getEncoder().encodeToString(code[0].getBytes());
            CassandraController.runTask(session -> {
                String snippetName = MessageUtils.getNextArgument(msg, Options.SAVE.getAsArgument());
                if (snippetName == null) {
                    MessageUtils.sendErrorMessage("Please specify the name of the snippet to save! Do `-save (name)`", channel);
                    return;
                }
                if (insertSnippet == null)
                    insertSnippet = session.prepare("UPDATE flarebot.eval_snippets SET encoded_code = ? WHERE snippet_name = ?");

                session.execute(insertSnippet.bind().setString(0, base64).setString(1, snippetName));
                MessageUtils.sendSuccessMessage("Saved the snippet `" + snippetName + "`!", channel);
            });
            return;
        }

        if (hasOption(Options.LIST, msg)) {
            ResultSet set = CassandraController.execute("SELECT snippet_name FROM flarebot.eval_snippets");
            if (set == null) return;
            MessageUtils.sendInfoMessage("**Available eval snippets**\n" +
                            set.all().stream().map(row -> "`" + row.getString("snippet_name") + "`")
                                    .collect(Collectors.joining(", "))
                    , channel);
            return;
        }

        if (code[0] == null) return;
        final String finalCode = code[0];

        POOL.submit(() -> {
            try {
                String eResult = String.valueOf(engine.eval(imports + "with (imports) {\n" + finalCode + "\n}"));
                if (("```js\n" + eResult + "\n```").length() > 1048) {
                    eResult = String.format("[Result](%s)", MessageUtils.paste(eResult));
                } else eResult = "```js\n" + eResult + "\n```";
                if (!silent)
                    channel.sendMessage(MessageUtils.getEmbed(sender)
                            .addField("Code:", "```js\n" + finalCode + "```", false)
                            .addField("Result: ", eResult, false).build()).queue();
            } catch (Exception e) {
                CookieBot.LOGGER.error("Error occured in the evaluator thread pool!", e, Markers.NO_ANNOUNCE);
                channel.sendMessage(MessageUtils.getEmbed(sender)
                        .addField("Code:", "```js\n" + finalCode + "```", false)
                        .addField("Result: ", "```bf\n" + e.getMessage() + "```", false).build()).queue();
            }
        });
    }

    @Override
    public String getCommand() {
        return "eval";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getUsage() {
        return "";
    }

    @Override
    public CommandAuthority getAuthority() {
        return CommandAuthority.ADMIN;
    }

    enum Options {
        SILENT("s"),
        SNIPPET("snippet"),
        SAVE("save"),
        LIST("list");

        private String key;

        Options(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public String getAsArgument() {
            return "-" + key;
        }
    }

    private boolean hasOption(Options option, String message) {
        return Pattern.compile("(.^ *)*-\\b" + option.getKey() + "\\b( \\w+)?(.^ )*").matcher(message).find();
    }

    private Pattern getRegex(Options option) {
        return Pattern.compile("(.^ )*-\\b" + option.getKey() + "\\b( \\w+)?(.^ )*");
    }

    private String getCode(String[] args) {
        String code = CookieBot.getMessage(args);
        for (Options option : Options.values()) {
            if (hasOption(option, code)) {
                Matcher matcher = getRegex(option).matcher(code);
                code = matcher.replaceAll("");
            }
        }
        return code;
    }
}