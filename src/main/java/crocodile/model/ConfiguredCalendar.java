package crocodile.model;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.time.DateTimeException;
import java.util.List;
import java.util.Objects;

public sealed abstract class ConfiguredCalendar permits StoredCalendar, VirtualCalendar {
    
    private final TimeZone timezone;
    private final String loginModelRead;
    private final String loginModelWrite;

    public ConfiguredCalendar(String tz, String loginModelRead, String loginModelWrite) throws DateTimeException {
        this.timezone = TimeZoneRegistryFactory.getInstance().createRegistry().getTimeZone(tz);
        if (this.timezone == null) throw new DateTimeException("timezone not available for iCal use: " + tz);
        this.loginModelRead = loginModelRead;
        this.loginModelWrite = loginModelWrite;
    }
    
    public TimeZone timezone() {
        return timezone;
    }
    
    public abstract List<String> getStoredCalendars();

    public boolean login(Action action, CallbackHandler handler) {
        try {
            String loginModel = Objects.requireNonNull(action) == Action.WRITE ? this.loginModelWrite : this.loginModelRead;
            if ("never".equalsIgnoreCase(loginModel)) throw new LoginException("calendar does not allow login");
            if ("open".equalsIgnoreCase(loginModel)) return true;
            LoginContext context = new LoginContext(loginModel, handler);
            context.login();
            return true;
        } catch (LoginException e) {
            return false;
        }
    }
    
    public Calendar toICal(List<Event> events) {
        Calendar calendar = new Calendar();
        calendar.getComponents().add(this.timezone().getVTimeZone());
        for (Event event : events) {
            calendar.getComponents().add(event.toICal(this.timezone()));
        }
        return calendar;
    }

    public enum Action {
        READ, WRITE
    }
}
