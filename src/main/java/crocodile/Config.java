package crocodile;

import crocodile.db.DatabaseConfig;
import crocodile.model.ConfiguredCalendar;

import javax.annotation.Nullable;
import java.util.Map;

public class Config {
    
    private final int port;
    @Nullable private final String redirect;
    private final DatabaseConfig database;
    private final Map<String, ConfiguredCalendar> calendars;

    public Config(int port, @Nullable String redirect, DatabaseConfig database, Map<String, ConfiguredCalendar> calendars) {
        this.port = port;
        this.redirect = redirect;
        this.database = database;
        this.calendars = Map.copyOf(calendars);
    }

    public int port() {
        return this.port;
    }

    @Nullable
    public String redirect() {
        return this.redirect;
    }

    public DatabaseConfig database() {
        return this.database;
    }

    @Nullable
    public ConfiguredCalendar calendar(String id) {
        return this.calendars.getOrDefault(id, null);
    }
}
