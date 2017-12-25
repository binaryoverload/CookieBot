package io.github.binaryoverload;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import org.slf4j.Logger;
import io.github.binaryoverload.database.CassandraController;

public class CookieBotManager {

    private final Logger LOGGER = CookieBot.getLog(getClass());

    private static CookieBotManager instance;

    private final String COOKIE_REP_TABLE;

    public CookieBotManager() {
        instance = this;
        COOKIE_REP_TABLE = (CookieBot.getInstance().isTestBot() ? "cookiebot.rep_test" : "cookiebot.rep");
    }

    public static CookieBotManager getInstance() {
        return instance;
    }

    public void executeCreations() {
        CassandraController.executeAsync("CREATE TABLE IF NOT EXISTS " + COOKIE_REP_TABLE + " (" +
                "user_id varchar, " +
                "rep int, " +
                "PRIMARY KEY(user_id))");
    }

    public synchronized int getUserRep(String id) {
        ResultSet set = CassandraController.execute("SELECT data FROM " + COOKIE_REP_TABLE + " WHERE user_id = '"
                + id + "'");
        Row row = set != null ? set.one() : null;
        try {
            if (row != null)
                return row.getInt(1);
            else
                return 0;
        } catch (Exception e) {
            LOGGER.error("Failed to load Rep!\n" +
                    "User ID: " + id + "\n" +
                    "Error: " + e.getMessage(), e);
            return 0;
        }
    }
}
