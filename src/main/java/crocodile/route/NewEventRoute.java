package crocodile.route;

import crocodile.Config;
import crocodile.db.Database;
import crocodile.model.ConfiguredCalendar;
import crocodile.model.Event;
import crocodile.model.EventJson;
import crocodile.model.StoredCalendar;
import crocodile.route.base.CalendarRoute;
import spark.Request;
import spark.Response;
import spark.Service;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

public class NewEventRoute extends CalendarRoute {
    
    private static final UUID NULL_UUID = new UUID(0, 0);

    public NewEventRoute(Service spark, Config config, Database database) {
        super(spark, ConfiguredCalendar.Action.WRITE, config, database);
    }

    @Override
    protected String handle(Request request, Response response, ConfiguredCalendar calendar, @Nullable UUID uid) throws SQLException, ParseException {
        if (calendar instanceof StoredCalendar stored) {
            if (request.body() == null || request.body().isEmpty()) throw this.spark.halt(400, "No content");
            Event event = EventJson.fromJson(NULL_UUID, request.body());
            Event addedEvent = this.database.insert(stored, event);
            response.header("Content-Type", "text/calendar; charset=utf-8");
            response.header("X-EventID", addedEvent.uid().toString());
            return calendar.toICal(List.of(addedEvent)).toString();
        } else {
            throw new ParseException("virtual calendar", 0);
        }
    }
}
