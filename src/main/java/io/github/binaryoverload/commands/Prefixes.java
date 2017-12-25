package io.github.binaryoverload.commands;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.binaryoverload.CookieBot;
import io.github.binaryoverload.database.CassandraController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Prefixes {

    private Map<String, Character> prefixes = new ConcurrentHashMap<>();

    public Prefixes() {
        CassandraController.runTask(session -> {
            session.execute("CREATE TABLE IF NOT EXISTS flarebot.prefixes (" +
                    "guild_id varchar PRIMARY KEY, " +
                    "prefix varchar" +
                    ")");
            ResultSet set = session.execute("SELECT * FROM flarebot.prefixes;");
            List<Row> rows = set.all();
            for (Row row : rows) {
                prefixes.put(row.getString("guild_id"), row.getString("prefix").charAt(0));
            }
        });
    }

    public char get(String guildId) {
        if (guildId == null)
            return CookieBot.COMMAND_CHAR;
        return prefixes.getOrDefault(guildId, CookieBot.COMMAND_CHAR);
    }

    public void set(String guildId, char character) {
        if (character == CookieBot.COMMAND_CHAR) {
            prefixes.remove(guildId);
            CassandraController.execute("DELETE FROM flarebot.prefixes WHERE guild_id = '" + guildId + "'");
            update(guildId, character);
            return;
        }
        prefixes.put(guildId, character);
        CassandraController.runTask(session -> session.execute(session.prepare("UPDATE flarebot.prefixes SET prefix = ? WHERE guild_id = ?").bind()
                .setString(0, String.valueOf(character)).setString(1, guildId)));
        update(guildId, character);
    }

    public void update(String guildId, char prefix) {
        JsonArray array = new JsonArray();
        JsonObject guildObj = new JsonObject();
        guildObj.addProperty("guildId", guildId);
        guildObj.addProperty("prefix", prefix);
        array.add(guildObj);

        //TODO: Move to new API
        //CookieBot.getInstance().postToApi("updatePrefixes", "prefixes", array);
    }

    public Map<String, Character> getPrefixes() {
        return this.prefixes;
    }
}
