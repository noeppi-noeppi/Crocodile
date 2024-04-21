package crocodile.route;

import crocodile.Config;
import crocodile.db.Database;
import crocodile.model.ConfiguredCalendar;
import crocodile.model.Event;
import crocodile.route.base.CalendarRoute;
import spark.Request;
import spark.Response;
import spark.Service;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class WebcalRoute extends CalendarRoute {

    public WebcalRoute(Service spark, Config config, Database database) {
        super(spark, ConfiguredCalendar.Action.READ, config, database);
    }

    @Override
    protected String handle(Request request, Response response, ConfiguredCalendar calendar, @Nullable UUID uid) throws IOException, SQLException {
        List<Event> events;
        if (uid != null) {
            Event event = this.database.query(calendar, uid);
            if (event == null) throw new FileNotFoundException();
            events = List.of(event);
        } else {
            events = this.database.query(calendar);
        }
        response.header("Content-Type", "text/calendar; charset=utf-8");
        return calendar.toICal(events).toString();
    }
}
