package crocodile.model;

import java.time.DateTimeException;
import java.util.List;

public final class StoredCalendar extends ConfiguredCalendar {
    
    private final String id;
    private final List<String> storedCalendars;
    
    public StoredCalendar(String id, String tz, String loginModelRead, String loginModelWrite) throws DateTimeException {
        super(tz, loginModelRead, loginModelWrite);
        this.id = id;
        this.storedCalendars = List.of(id);
    }

    public String id() {
        return id;
    }

    @Override
    public List<String> getStoredCalendars() {
        return this.storedCalendars;
    }
}
