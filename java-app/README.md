## Fresh clone setup

On a fresh clone, two things are usually missing and must be set up before the app runs:

1. **MySQL JDBC driver** — `lib/mysql-connector-j-8.4.0.jar` is not committed to the repo. Download the same 8.x version and put it in `lib/`:

   - https://dev.mysql.com/downloads/connector/j/
   - choose **Platform Independent** → ZIP → copy the `.jar` into `java-app/lib/`

   The app will not start without it. The placeholder file `lib/PUT_MYSQL_CONNECTOR_JAR_HERE.txt` stays in the repo so this is easy to spot.

2. **Database password** — `config/db.properties` is in the repo, but only as a template:

   ```
   db.url=jdbc:mysql://localhost:3306/noirbox_db
   db.user=root
   db.password=YOUR_PASSWORD_HERE
   ```

   Set `db.password` to your MySQL password before running anything. On this machine that is currently `root`.

Then run `run.bat` or `serve.bat` as normal.

---

# java-app — NOIRBOX Java + JDBC application

Two entry points over the same code:

| Entry point | Script | Used for |
|---|---|---|
| `noirbox.Main` | `run.bat` | menu-driven console application (Java viva) |
| `noirbox.api.ApiMain` | `serve.bat` | HTTP API on port 8080 that the React website calls |

Both use the same DAO and service classes, so the website and the console share the
same business rules and the same MySQL data.

## Requirements

- JDK 17 or newer (`javac -version`)
- MySQL 8 with `noirbox_db` loaded (see `../database/README.md`)
- `mysql-connector-j-8.x.jar` inside `lib/` — **already included**
  (`lib/mysql-connector-j-8.4.0.jar`, downloaded from Maven Central). Replace it with the
  same version from <https://dev.mysql.com/downloads/connector/j/> if you prefer the
  official download.

## Setup

1. The JDBC driver is already in `lib/`. If it is missing, copy it back:

   ```
   java-app/lib/mysql-connector-j-8.4.0.jar
   ```

2. Edit `config/db.properties`:

   ```
   db.url=jdbc:mysql://localhost:3306/noirbox_db
   db.user=root
   db.password=your_mysql_password
   ```

3. Run:

   ```
   run.bat            # Windows (double-click works too)
   ./run.sh           # Linux / macOS / Git Bash
   ```

   Or manually:

   ```
   javac -d out @sources.txt      (after: find src -name "*.java" > sources.txt)
   java -cp "out;lib/*" noirbox.Main
   ```

## Menu

| Option | What it shows |
|---|---|
| 1 | movies — all / now showing / coming soon / search |
| 2 | shows of a movie on a date |
| 3 | seat map, booked seats, price per category |
| 4 | book seats — either a JDBC transaction or the stored procedure |
| 5 | my bookings |
| 6 | cancel — JDBC transaction or stored procedure |
| 7 | write a review (the trigger sets the verdict) |
| 8 | revenue, bookings per movie, audience ratings, CSV export |
| 9 | three threads booking the same seat (synchronization demo) |
| 10 | cursor reports from MySQL |

Sample user: `aarav.sharma@example.com`.

## Packages

| Package | Contents |
|---|---|
| `noirbox` | `Main` — menu loop and error handling |
| `noirbox.api` | `ApiServer`, `ApiMain`, `Json` — REST API for the website |
| `noirbox.model` | `Movie`, `Show`, `Booking`, `Review`, `User`, `City` |
| `noirbox.dao` | `MovieDAO`, `ShowDAO`, `BookingDAO`, `ReviewDAO`, `CityDAO` |
| `noirbox.service` | `BookingService`, `ReportService`, `StoredProcedureService` |
| `noirbox.db` | `DBConnection`, `JdbcHelper`, `DataAccessException` |
| `noirbox.concurrent` | `SeatBookingSimulator` |
| `noirbox.util` | `InputUtil` (validated console input) |
| `noirbox.exception` | `BookingException`, `SeatUnavailableException` |

## Web API

```
serve.bat            (or ./serve.sh)
http://localhost:8080/api/health
```

| Endpoint | What it does |
|---|---|
| `GET /api/movies`, `GET /api/movies/{slug}` | catalogue + one movie (genres, cast, audience rating) |
| `GET /api/shows?movieId=&date=&city=` | showtimes from `movie_show` |
| `GET /api/shows/{id}/seats` | booked seats (`booking_seat`) + price per category |
| `POST /api/bookings` | books seats in one JDBC transaction; **409** if a seat is taken |
| `GET /api/bookings?email=` | a user's bookings |
| `POST /api/bookings/{id}/cancel` | cancels and releases the seats |
| `GET/POST /api/movies/{slug}/reviews`, `POST /api/reviews` | reviews |
| `POST /api/reviews/{id}/vote`, `DELETE /api/reviews/{id}` | helpful votes, delete own review |

The server uses `com.sun.net.httpserver.HttpServer` from the JDK, so no framework and
no extra jar is required. Bodies are form-encoded; responses are JSON.

Design notes for the viva: `docs/Java_Report.md`; questions: `docs/Viva_QA.md`.
