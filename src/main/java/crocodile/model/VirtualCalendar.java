package crocodile.model;

import javax.security.auth.callback.CallbackHandler;
import java.time.DateTimeException;
import java.util.List;

public final class VirtualCalendar extends ConfiguredCalendar {
    
    private final List<String> storedCalendars;
    
    public VirtualCalendar(List<String> calendars, String tz, String loginModelRead) throws DateTimeException {
        super(tz, loginModelRead, "never");
        this.storedCalendars = List.copyOf(calendars);
    }

    @Override
    public List<String> getStoredCalendars() {
        return this.storedCalendars;
    }

    @Override
    public boolean login(Action action, CallbackHandler handler) {
        if (action == Action.WRITE) return false;
        return super.login(action, handler);
    }
}
