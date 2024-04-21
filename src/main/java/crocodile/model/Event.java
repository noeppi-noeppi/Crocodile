package crocodile.model;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record Event(UUID uid, String title, Instant modified, @Nullable String description, @Nullable String location, @Nullable String url, Times times) {
    
    public VEvent toICal(TimeZone tz) {
        VEvent event = new VEvent(false);
        event.getProperties().add(new Uid(this.uid().toString()));
        event.getProperties().add(new Summary(this.title()));
        if (this.description() != null) {
            event.getProperties().add(new Description(this.description()));
        }
        if (this.location() != null) {
            event.getProperties().add(new Location(this.location()));
        }
        if (this.url() != null) {
            try {
                event.getProperties().add(new Url(new URI(this.url())));
            } catch (URISyntaxException e) {
                //
            }
        }
        event.getProperties().add(new DtStamp(new DateTime(Date.from(this.modified()))));
        if (this.times() instanceof Timed timed) {
            event.getProperties().add(new DtStart(new DateTime(java.util.Date.from(timed.start()), tz)));
            event.getProperties().add(new DtEnd(new DateTime(java.util.Date.from(timed.end()), tz)));
        } else if (this.times() instanceof AllDay allDay) {
            try {
                event.getProperties().add(new DtStart(new Date(DateTimeFormatter.BASIC_ISO_DATE.format(allDay.start()))));
                event.getProperties().add(new DtEnd(new Date(DateTimeFormatter.BASIC_ISO_DATE.format(allDay.end().plusDays(1)))));
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IncompatibleClassChangeError();
        }
        return event;
    }
    
    public sealed interface Times {}
    public record Timed(Instant start, Instant end) implements Times {}
    public record AllDay(LocalDate start, LocalDate end) implements Times {}
}
