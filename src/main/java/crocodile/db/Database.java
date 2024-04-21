package crocodile.db;

import crocodile.model.ConfiguredCalendar;
import crocodile.model.Event;
import crocodile.model.StoredCalendar;

import javax.annotation.Nullable;
import java.sql.Date;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class Database implements AutoCloseable  {

    public static Database connect(DatabaseConfig config) throws SQLException {
        String url = "jdbc:postgresql://" + config.host() + ":" + config.port() + "/";
        Properties properties = new Properties();
        if (config.user() != null) properties.put("user", config.user());
        if (config.password() != null) properties.put("password", config.password());
        Connection connection = DriverManager.getConnection(url, properties);
        connection.setAutoCommit(false);
        setupDatabase(connection);
        return new Database(connection);
    }

    private static void setupDatabase(Connection connection) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();
        try (ResultSet tables = meta.getTables(null, null, "events", null)) {
            if (!tables.next()) {
                createEventsTable(connection);
            }
        }
    }
    
    private static void createEventsTable(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE events (
                      uid UUID PRIMARY KEY,
                      cal TEXT NOT NULL,
                      title TEXT NOT NULL,
                      modified TIMESTAMP NOT NULL,
                      description TEXT,
                      location TEXT,
                      url TEXT,
                      start_date DATE NOT NULL,
                      start_time TIME,
                      end_date DATE NOT NULL,
                      end_time TIME
                    );
                    """);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }
    
    private final Connection connection;
    
    private Database(Connection connection) {
        this.connection = connection;
    }
    
    public synchronized List<Event> query(ConfiguredCalendar calendar) throws SQLException {
        List<String> storedCalendars = calendar.getStoredCalendars();
        String stmtTemplate = String.format("SELECT * FROM events WHERE cal IN (%s) ORDER BY uid ASC;", storedCalendars.stream().map(v -> "?").collect(Collectors.joining(", ")));
        try (PreparedStatement stmt = this.connection.prepareStatement(stmtTemplate)) {
            for (int i = 0; i < storedCalendars.size(); i++) {
                stmt.setString(i + 1, storedCalendars.get(i));
            }
            if (stmt.execute()) {
                List<Event> events = new LinkedList<>();
                ResultSet resultSet = stmt.getResultSet();
                while (resultSet.next()) {
                    events.add(createEvent(resultSet));
                }
                return Collections.unmodifiableList(events);
            } else {
                return List.of();
            }
        }
    }

    @Nullable
    public synchronized Event query(ConfiguredCalendar calendar, UUID uid) throws SQLException {
        List<String> storedCalendars = calendar.getStoredCalendars();
        String stmtTemplate = String.format("SELECT * FROM events WHERE uid = ? AND cal IN (%s);", storedCalendars.stream().map(v -> "?").collect(Collectors.joining(", ")));
        try (PreparedStatement stmt = this.connection.prepareStatement(stmtTemplate)) {
            stmt.setObject(1, uid);
            for (int i = 0; i < storedCalendars.size(); i++) {
                stmt.setString(i + 2, storedCalendars.get(i));
            }
            if (stmt.execute()) {
                ResultSet resultSet = stmt.getResultSet();
                if (resultSet.next()) {
                    return createEvent(resultSet);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }
    
    public synchronized Event insert(StoredCalendar calendar, Event event) throws SQLException {
        UUID assignedUid = UUID.randomUUID();
        Instant modified = Instant.now();
        try (PreparedStatement stmt = this.connection.prepareStatement("""
                INSERT INTO events (uid, cal, title, modified, description, location, url, start_date, start_time, end_date, end_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """)) {
            stmt.setObject(1, assignedUid);
            stmt.setString(2, calendar.id());
            fillInPreparedStatement(modified, event, 3, stmt);
            stmt.executeUpdate();
            connection.commit();
            return new Event(assignedUid, event.title(), modified, event.description(), event.location(), event.url(), event.times());
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }
    
    public synchronized Event update(StoredCalendar calendar, Event event) throws SQLException {
        Instant modified = Instant.now();
        try (PreparedStatement stmt = this.connection.prepareStatement("""
                UPDATE events SET
                  title = ?,
                  modified = ?,
                  description = ?,
                  location = ?,
                  url = ?,
                  start_date = ?,
                  start_time = ?,
                  end_date = ?,
                  end_time = ?
                WHERE cal = ? AND uid = ?;
                """)) {
            fillInPreparedStatement(modified, event, 1, stmt);
            stmt.setString(10, calendar.id());
            stmt.setObject(11, event.uid());
            stmt.executeUpdate();
            connection.commit();
            return new Event(event.uid(), event.title(), modified, event.description(), event.location(), event.url(), event.times());
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }
    
    public synchronized void delete(StoredCalendar calendar, UUID uid) throws SQLException {
        try (PreparedStatement stmt = this.connection.prepareStatement("DELETE FROM events WHERE cal = ? AND uid = ?;")) {
            stmt.setString(1, calendar.id());
            stmt.setObject(2, uid);
            stmt.execute();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }
    
    public synchronized void clear(StoredCalendar calendar) throws SQLException {
        try (PreparedStatement stmt = this.connection.prepareStatement("DELETE FROM events WHERE cal = ?;")) {
            stmt.setString(1, calendar.id());
            stmt.execute();
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }
    
    private static Event createEvent(ResultSet resultSet) throws SQLException {
        UUID uid = resultSet.getObject("uid", UUID.class);
        String title = resultSet.getString("title");
        Instant modified = resultSet.getTimestamp("modified").toInstant();
        @Nullable String description = resultSet.getString("description");
        @Nullable String location = resultSet.getString("location");
        @Nullable String url = resultSet.getString("url");
        LocalDate startDate = resultSet.getDate("start_date").toLocalDate();
        LocalDate endDate = resultSet.getDate("end_date").toLocalDate();
        @Nullable Time sqlStartTime = resultSet.getTime("start_time");
        @Nullable Time sqlEndTime = resultSet.getTime("end_time");
        @Nullable LocalTime startTime = sqlStartTime == null ? null : sqlStartTime.toLocalTime();
        @Nullable LocalTime endTime = sqlEndTime == null ? null : sqlEndTime.toLocalTime();
        Event.Times times;
        if (startTime != null && endTime != null) {
            times = new Event.Timed(
                    ZonedDateTime.of(startDate, startTime, ZoneOffset.UTC).toInstant(),
                    ZonedDateTime.of(endDate, endTime, ZoneOffset.UTC).toInstant()
            );
        } else {
            times = new Event.AllDay(startDate, endDate);
        }
        return new Event(uid, title, modified, description, location, url, times);
    }
    
    private static void fillInPreparedStatement(Instant modified, Event event, int offset, PreparedStatement stmt) throws SQLException {
        stmt.setString(offset, event.title());
        stmt.setTimestamp(offset + 1, Timestamp.from(modified));
        stmt.setString(offset + 2, event.description());
        stmt.setString(offset + 3, event.location());
        stmt.setString(offset + 4, event.url());
        if (event.times() instanceof Event.Timed timed) {
            ZonedDateTime start = ZonedDateTime.ofInstant(timed.start(), ZoneOffset.UTC);
            ZonedDateTime end = ZonedDateTime.ofInstant(timed.end(), ZoneOffset.UTC);
            stmt.setDate(offset + 5, Date.valueOf(start.toLocalDate()));
            stmt.setTime(offset + 6, Time.valueOf(start.toLocalTime()));
            stmt.setDate(offset + 7, Date.valueOf(end.toLocalDate()));
            stmt.setTime(offset + 8, Time.valueOf(end.toLocalTime()));
        } else if (event.times() instanceof Event.AllDay allDay) {
            stmt.setDate(offset + 5, Date.valueOf(allDay.start()));
            stmt.setTime(offset + 6, null);
            stmt.setDate(offset + 7, Date.valueOf(allDay.end()));
            stmt.setTime(offset + 8, null);
        } else {
            throw new IncompatibleClassChangeError();
        }
    }

    @Override
    public void close() throws Exception {
        this.connection.close();
    }
}
