package crocodile.route.base;

import crocodile.Config;
import crocodile.db.Database;
import crocodile.login.CrocodileCallbackHandler;
import crocodile.model.ConfiguredCalendar;
import spark.*;

import javax.annotation.Nullable;
import javax.security.auth.callback.CallbackHandler;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

public abstract class CalendarRoute implements Route {
    
    protected final Service spark;
    protected final ConfiguredCalendar.Action action;
    protected final Config config;
    protected final Database database;

    public CalendarRoute(Service spark, ConfiguredCalendar.Action action, Config config, Database database) {
        this.spark = spark;
        this.action = action;
        this.config = config;
        this.database = database;
    }

    @Override
    public Object handle(Request request, Response response) {
        try {
            @Nullable String calendarId = request.params(":calendar");
            @Nullable ConfiguredCalendar calendar = this.config.calendar(calendarId);
            
            if (calendar != null && calendar.login(this.action, getLoginData(request))) {
                return this.handle(request, response, calendar, this.getRequestedUid(request));
            } else {
                response.status(401);
                response.header("WWW-Authenticate", "Basic realm=login");
                return "Unauthorized";
            }
        } catch (HaltException e) {
            throw e;
        } catch (FileNotFoundException e) {
            throw this.spark.halt(404, "Not Found");
        } catch (ParseException e) {
            e.printStackTrace();
            throw this.spark.halt(400, "Bad Request");
        } catch (Exception e) {
            e.printStackTrace();
            throw this.spark.halt(500, "Server Error");
        }
    }
    
    private CallbackHandler getLoginData(Request request) {
        @Nullable String user = null;
        @Nullable String password = null;
        @Nullable String token = request.queryParams("pw");
        
        @Nullable String authHeader = request.headers("Authorization");
        if (authHeader != null && authHeader.toLowerCase(Locale.ROOT).contains("basic")) {
            String encodedLoginInformation = authHeader.substring(authHeader.toLowerCase(Locale.ROOT).indexOf("basic") + 5).strip();
            String loginInformation = new String(Base64.getDecoder().decode(encodedLoginInformation), StandardCharsets.UTF_8);
            if (loginInformation.indexOf(':') >= 0) {
                user = loginInformation.substring(0, loginInformation.indexOf(':'));
                password = loginInformation.substring(loginInformation.indexOf(':') + 1);
            }
        }
        
        return new CrocodileCallbackHandler(user, password, token);
    }
    
    @Nullable
    private UUID getRequestedUid(Request request) throws FileNotFoundException {
        String uidParam = request.params(":uid");
        if (uidParam != null) {
            UUID uid;
            try {
                uid = UUID.fromString(uidParam);
            } catch (IllegalArgumentException e) {
                throw new FileNotFoundException();
            }
            return uid;
        } else {
            return null;
        }
    }
    
    protected abstract String handle(Request request, Response response, ConfiguredCalendar calendar, @Nullable UUID eventUid) throws IOException, SQLException, ParseException;
}
