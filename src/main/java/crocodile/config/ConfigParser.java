package crocodile.config;

import crocodile.Config;
import crocodile.db.DatabaseConfig;
import crocodile.model.ConfiguredCalendar;
import crocodile.model.StoredCalendar;
import crocodile.model.VirtualCalendar;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.GroovyShell;
import groovy.util.DelegatingScript;
import org.codehaus.groovy.control.CompilerConfiguration;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class ConfigParser {
    
    public static Config readConfig(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new FileNotFoundException("System configuration not found: " + path.toAbsolutePath().normalize());
        }

        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.setScriptBaseClass(DelegatingScript.class.getName());
        GroovyShell shell = new GroovyShell(new Binding(), compilerConfig);
        DelegatingScript script;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            script = (DelegatingScript) shell.parse(reader);
        }
        ConfigAdapter dsl = new ConfigAdapter();
        script.setDelegate(dsl);
        script.run();
        
        return build(dsl);
    }
    
    private static Config build(ConfigAdapter adapter) {
        int port = adapter.port;
        @Nullable String redirect = adapter.redirect;
        if (adapter.database == null) throw new IllegalStateException("Database not configured");
        if (adapter.database.host == null) throw new IllegalStateException("Database host not configured");
        DatabaseConfig database = new DatabaseConfig(adapter.database.host, adapter.database.port, adapter.database.user, adapter.database.password);
        Map<String, ConfiguredCalendar> calendars = buildCalendars(adapter);
        return new Config(port, redirect, database, calendars);
    }

    private static Map<String, ConfiguredCalendar> buildCalendars(ConfigAdapter adapter) {
        Map<String, ConfiguredCalendar> calendars = Map.copyOf(adapter.calendars);
        for (ConfiguredCalendar calendar : calendars.values()) {
            for (String ref : calendar.getStoredCalendars()) {
                if (!calendars.containsKey(ref)) {
                    throw new IllegalStateException("Invalid calendar reference in config file: " + ref);
                } else if (!(calendars.get(ref) instanceof StoredCalendar)) {
                    throw new IllegalStateException("Reference to virtual calendar in config file: " + ref);
                }
            }
        }
        return calendars;
    }

    private static class ConfigAdapter {
    
        protected int port = 80;
        protected String redirect = null;
        protected DatabaseAdapter database = null;
        protected final Map<String, ConfiguredCalendar> calendars = new HashMap<>();

        public void port(int port) {
            this.port = port;
        }
        
        public void redirect(String redirect) {
            this.redirect = redirect;
        }

        public void database(@DelegatesTo(value = DatabaseAdapter.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            if (this.database != null) throw new IllegalStateException("Multiple database blocks.");
            DatabaseAdapter adapter = new DatabaseAdapter();
            closure.setDelegate(adapter);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.call();
            this.database = adapter;
        }
    
        public void calendar(String id, @DelegatesTo(value = StoredCalendarAdapter.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            if (calendars.containsKey(id)) throw new IllegalStateException("Duplicate calendar: " + id);
            StoredCalendarAdapter adapter = new StoredCalendarAdapter();
            closure.setDelegate(adapter);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.call();
            this.calendars.put(id, new StoredCalendar(id, adapter.timezone, adapter.loginRead == null ? adapter.loginWrite : adapter.loginRead, adapter.loginWrite));
        }
    
        public void virtual(String id, @DelegatesTo(value = VirtualCalendarAdapter.class, strategy = Closure.DELEGATE_FIRST) Closure<?> closure) {
            if (calendars.containsKey(id)) throw new IllegalStateException("Duplicate calendar: " + id);
            VirtualCalendarAdapter adapter = new VirtualCalendarAdapter();
            closure.setDelegate(adapter);
            closure.setResolveStrategy(Closure.DELEGATE_FIRST);
            closure.call();
            this.calendars.put(id, new VirtualCalendar(adapter.sources.stream().sorted().toList(), adapter.timezone, adapter.loginRead == null ? "never" : adapter.loginRead));
        }
    }

    private static class DatabaseAdapter {
        
        protected String host = null;
        protected int port = 5432;
        protected String user = null;
        protected String password = null;

        public void host(String host) {
            this.host = host;
        }

        public void port(int port) {
            this.port = port;
        }

        public void user(String user) {
            this.user = user;
        }

        public void password(String password) {
            this.password = password;
        }
    }
    
    private static class CalendarAdapter {
        
        protected String timezone = "UTC";
        protected String loginRead = null;

        public void timezone(String timezone) {
            this.timezone = timezone;
        }

        public void loginRead(String loginRead) {
            this.loginRead = loginRead;
        }
    }
    
    private static class StoredCalendarAdapter extends CalendarAdapter {

        protected String loginWrite = "never";

        public void loginWrite(String loginWrite) {
            this.loginWrite = loginWrite;
        }
    }
    
    private static class VirtualCalendarAdapter extends CalendarAdapter {

        protected final Set<String> sources = new HashSet<>();

        public void from(String calendar) {
            this.sources.add(calendar);
        }
    }
}
