# Crocodile

A simple server for serving web calendars over HTTP with simple endpoints to modify them.
Crocodile only covers a small subset of the iCal standard in its web calendars.
The endpoints to alter calendars don't rely on iCal at all.

### How to use

You can start Crocodile with the options `--config /path/to/main/config/file`.
The main Crocodile configuration is a groovy file that looks like this:
```groovy
port 80
redirect 'https://mycoolsite'
database {
    host 'localhost'
    port 5432
    user 'dbuser'
    password 'dbpassword'
}
calendar 'cal1', {
    // calendar properties
}
virtual 'virt1', {
    // virtual calendar properties
}
```

`port` is the port to run on. It defaults to `80`. `redirect` sets a target to which the main page should redirect.

The `database` block configures how Crocodile should connect to its database backend.
Crocodile needs a [PostgreSQL](https://www.postgresql.org/) database.

`calendar` and `virtual` creates named calendars that will be served.
The difference between `calendar` and `virtual` is that `calendar` creates actual calendars that are stored in the database which can be queried and edited, while `virtual` creates read-only calendars that can combine multiple calendars into.
Calendars can be configured with additional options:

| Option | Default | Description |
| :--- | :--- | --- |
| `timezone` | `UTC` | The timezone to use when serving the calendar. |
| `loginRead` | `loginWrite` | The login method used for red access to the calendar. See below. |
| `loginWrite` | `never` | *Only for non-virtual calendars.* The login method used for write access to the calendar. See below. |
| `from` | - | *Only for virtual calendars.* Specifies an id of a calendar to include in this one. |

### Routes

Crocodile provides the following routes:

| Route | Description |
| :--- | --- |
| `GET` `/:calendar` | Gets a calendar as iCal file. |
| `GET` `/:calendar/:uid` | Gets a single event as iCal file. |
| `PUT` `/:calendar` | Adds a new event to a calendar. The event has to be provided as request body using the JSON syntax described below. |
| `PATCH` `/:calendar/:uid` | Modifies an existing event in a calendar. The entire event has to be provided as request body using the JSON syntax described below. |
| `DELETE` `/:calendar` | Deletes all events from a calendar. |
| `DELETE` `/:calendar/:uid` | Deletes a single event from a calendar. |

The `PUT`, `PATCH` and `DELETE` routes all return the iCal data of the created, patched or deleted event(s).
`PUT` will also st the HTTP header `X-EventID` to the uid allocated to the event.

Events are provided to Crocodile as a JSON object with the following properties:

| Key | Description |
| :--- | --- |
| `title` | *Required.* The event summary. |
| `description` | *Optional.* The event description. |
| `location` | *Optional.* The location at which the event takes place. |
| `url` | *Optional.* A URI that links to the event in some way. |
| `start` & `end` | *Forbidden if `startDay` and `endDay` are present. Required otherwise.* Start and end timestamps of the event. These have to be provided as ISO 8601 strings.  |
| `startDay` & `endDay` | *Forbidden if `start` and `end` are present. Required otherwise.* Start and end days of an all-day event. Both are inclusive. These have to be provided as ISO 8601 local date strings. |

### Login Methods

Calendars can be configured with login methods. Crocodile knows about two builtin login methods:

- `never` never succeeds and locks the calendar for everyone.
- `open` doesn't perform authentication and opens the calendar to everyone. This is not recommended for `loginWrite` as it would allow public write access.

Any other login method causes Crocodile to use [JAAS](https://de.wikipedia.org/wiki/Java_Authentication_and_Authorization_Service) for login.
Crocodile will attempt to authenticate the user with the corresponding JAAS entry and a callback handler that answers every name and password callback with the values provided through HTTP basic auth.

Crocodile also adds the `crocodile.login.TokenLoginModule` login module which can be configured with a fixed token (`token=abc`) and will check that the token is passed as `pw` query parameter (`?pw=abc`).

JAAS can be configured in the [JAAS Login Configuration File](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jgss/tutorials/LoginConfigFile.html).
This file can be loaded via a system property or the `--login` option.
