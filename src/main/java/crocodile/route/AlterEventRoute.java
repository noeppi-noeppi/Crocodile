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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

public class AlterEventRoute extends CalendarRoute {
    
    public AlterEventRoute(Service spark, Config config, Database database) {
        super(spark, ConfiguredCalendar.Action.WRITE, config, database);
    }

    @Override
    protected String handle(Request request, Response response, ConfiguredCalendar calendar, @Nullable UUID uid) throws IOException, SQLException, ParseException {
        if (calendar instanceof StoredCalendar stored) {
            if (uid == null) throw new ParseException("no event to alter", 0);
            if (request.body() == null || request.body().isEmpty()) throw this.spark.halt(400, "No content");
            Event event = EventJson.fromJson(uid, request.body());
            if (this.database.query(stored, uid) == null) throw new FileNotFoundException();
            Event alteredEvent = this.database.update(stored, event);
            response.header("Content-Type", "text/calendar; charset=utf-8");
            return calendar.toICal(List.of(alteredEvent)).toString();
        } else {
            throw new ParseException("virtual calendar", 0);
        }
    }
}
