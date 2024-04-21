package crocodile.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.UUID;

public class EventJson {
    
    private static final Gson GSON;
    
    static {
        GsonBuilder builder = new GsonBuilder();
        builder.disableHtmlEscaping();
        GSON = builder.create();
    }
    
    public static Event fromJson(UUID uid, String json) throws ParseException {
        JsonElement jsonElement;
        try {
            jsonElement = GSON.fromJson(json, JsonElement.class);
        } catch (Exception e) {
            ParseException ex = new ParseException("invalid json", 0);
            ex.initCause(e);
            throw ex;
        }
        return fromJson(uid, jsonElement);
    }
    
    public static Event fromJson(UUID uid, JsonElement jsonElement) throws ParseException {
        try {
            JsonObject json = jsonElement.getAsJsonObject();
            String title = json.get("title").getAsString();
            Instant modified = json.has("modified") ? Instant.parse(json.get("modified").getAsString()) : Instant.now();
            String description = json.has("description") ? json.get("description").getAsString() : null;
            String location = json.has("location") ? json.get("location").getAsString() : null;
            String url = json.has("url") ? json.get("url").getAsString() : null;
            Event.Times times;
            if (json.has("start") && json.has("end") && json.has("startDay") && json.has("endDay")) {
                throw new ParseException("ambiguous event", 0);
            } else if (json.has("start") && json.has("end")) {
                times = new Event.Timed(
                        Instant.parse(json.get("start").getAsString()),
                        Instant.parse(json.get("end").getAsString())
                );
            } else if (json.has("startDay") && json.has("endDay")) {
                times = new Event.AllDay(
                        LocalDate.parse(json.get("startDay").getAsString()),
                        LocalDate.parse(json.get("endDay").getAsString())
                );
            } else {
                throw new ParseException("invalid event", 0);
            }
            return new Event(uid, title, modified, description, location, url, times);
        } catch (ClassCastException | NullPointerException | NoSuchElementException | DateTimeParseException e) {
            ParseException ex = new ParseException("invalid event", 0);
            ex.initCause(e);
            throw ex;
        }
    }
}
