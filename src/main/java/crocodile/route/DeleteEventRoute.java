package crocodile.route;

import crocodile.Config;
import crocodile.db.Database;
import crocodile.model.ConfiguredCalendar;
import crocodile.model.Event;
import crocodile.model.StoredCalendar;
import crocodile.route.base.CalendarRoute;
import spark.Request;
import spark.Response;
import spark.Service;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

public class DeleteEventRoute extends CalendarRoute {

    public DeleteEventRoute(Service spark, Config config, Database database) {
        super(spark, ConfiguredCalendar.Action.WRITE, config, database);
    }

    @Override
    protected String handle(Request request, Response response, ConfiguredCalendar calendar, @Nullable UUID uid) throws IOException, SQLException, ParseException {
        if (calendar instanceof StoredCalendar stored) {
            List<Event> events;
            if (uid != null) {
                Event event = this.database.query(stored, uid);
                if (event == null) throw new FileNotFoundException();
                this.database.delete(stored, uid);
                events = List.of(event);
            } else {
                events = this.database.query(stored);
                this.database.clear(stored);
            }
            response.header("Content-Type", "text/calendar; charset=utf-8");
            return calendar.toICal(events).toString();
        } else {
            throw new ParseException("virtual calendar", 0);
        }
    }
}
