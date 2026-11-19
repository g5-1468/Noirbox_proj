# NOIRBOX — Java Application Report

**Programming in Java (25CSE0202) — Continuous Evaluation 3: Project Milestone**
Semester III, Batch 2025, B.E. CSE

| | |
|---|---|
| **Project title** | NOIRBOX — Online Movie Ticket Booking System |
| **Student** | *(Name, Roll Number)* |
| **Supervised by** | *(Faculty Name)* |
| **Technology** | Java 17, JDBC, MySQL 8, (frontend: React + Vite) |
| **Folder** | `java-app/` |

---

## 1. Problem Statement

A cinema website lets a customer browse movies, choose a show, pick seats and pay. The
frontend of this project (`Project-1-main/`) already does that but stores everything in
the browser. A real booking system needs a **server-side program and a database**, so
that bookings survive a browser refresh, two customers cannot book the same seat, and
the manager can produce revenue reports.

This milestone implements that server side in **Java using JDBC and MySQL**. The Java
application performs the same steps as the website:

```
login (email) -> browse movies -> choose show by date -> see seat map and prices
-> book seats -> view my bookings -> cancel a booking -> write a review -> reports
```

## 2. Objectives and Scope

| # | Objective | Measured by |
|---|---|---|
| 1 | Store every booking in MySQL through JDBC | booking rows in `booking` / `booking_seat` |
| 2 | Never sell the same seat twice | `UNIQUE(show_id, seat_no)` + transaction rollback |
| 3 | Use object-oriented design | one class per entity, service and DAO layers |
| 4 | Handle all user input without crashing | `InputUtil` validation + custom `BookingException` |
| 5 | Produce reports quickly | Streams/Collectors + thread pool + CSV export |
| 6 | Keep the code explainable | small classes, no framework, plain JDBC |

**Out of scope:** a web server or REST API, Spring Boot, real payments, authentication
with passwords. The React frontend remains a self-contained demo; the Java application
is the database-backed implementation of the same use cases.

## 3. Class and Package Structure

```
java-app/
├── config/db.properties        JDBC url, user, password (not hard-coded)
├── lib/                        mysql-connector-j-8.x.jar goes here
├── run.bat / run.sh            compile + run scripts
└── src/noirbox/
    ├── Main.java               menu-driven console application
    ├── model/                  Movie, Show, Booking, Review, User, City
    ├── dao/                    MovieDAO, ShowDAO, BookingDAO, ReviewDAO, CityDAO
    ├── service/                BookingService, ReportService, StoredProcedureService
    ├── db/                     DBConnection, JdbcHelper, DataAccessException
    ├── api/                    ApiServer, ApiMain, Json (HTTP API for the website)
    ├── exception/              BookingException, SeatUnavailableException
    ├── concurrent/             SeatBookingSimulator
    └── util/                   InputUtil
```

| Class | Responsibility |
|---|---|
| `Main` | prints the menu, reads the choice, calls the right service, catches errors |
| `DBConnection` | reads `db.properties`, loads the driver, opens a `Connection` |
| `JdbcHelper` | generic `query(sql, RowMapper, params...)` and `update(...)` helpers |
| `MovieDAO` | SELECT on movie / movie_genre |
| `ShowDAO` | JOIN query for shows with theatre and city |
| `BookingDAO` | booking + seat inserts inside an open transaction; seat lookups |
| `ReviewDAO` | reviews, review tags, user login by email |
| `BookingService` | one JDBC transaction for booking and for cancellation |
| `ReportService` | Streams, Comparator, CSV export, ExecutorService reports |
| `StoredProcedureService` | `CallableStatement` calls to the MySQL procedures |
| `SeatBookingSimulator` | three threads reserving the same seat, `synchronized` method |
| `InputUtil` | validated console input (never throws to the user) |
| `ApiServer` | routes `/api/...` requests, parses form bodies, writes JSON, sets HTTP status codes |
| `Json` | small JSON writer (escaping, arrays, numbers) |
| `ApiMain` | starts the HTTP server on port 8080 |

### Data flow of a booking

```
Main.bookTicket()
   -> InputUtil.readSeats()                 validate "C1,C2"
   -> BookingService.bookTicket(user, show, seats, method)
        connection.setAutoCommit(false)
        SELECT seats_available ... FOR UPDATE      (lock the show row)
        SELECT fn_seat_price(show, seat)           (price from MySQL)
        INSERT INTO booking ...
        INSERT INTO booking_seat ...               (trigger: seats_available - 1)
        connection.commit()                        (or rollback on error)
   -> console prints booking id, seats, total
```

## 4. Mapping of Course Concepts

| Java concept (CHO) | Where it is used |
| HTTP handling / REST | `ApiServer` (routing, query/form parsing, JSON, 404/409/500) |
|---|---|
| Classes, objects, constructors, encapsulation | `model/` classes with private fields and getters/setters |
| Inheritance | `DataAccessException extends RuntimeException`, `BookingException extends Exception` |
| Interfaces | `JdbcHelper.RowMapper<T>` (functional interface) |
| Method overloading | `DataAccessException` constructors, `InputUtil.readInt/readLine/readDate` |
| String handling | `String.format`, `String.join`, `StringBuilder` in the seat map |
| Wrapper classes / parsing | `Integer.parseInt`, `Double.parseDouble` in `InputUtil` |
| Arrays | `String[] rows`, `String[] users` in the simulator |
| Collections: List / Map / Set | `List<Show>`, `Map<String,Long>`, `Set<String>` booked seats |
| Comparator / Comparable | `Comparator.comparingDouble(Booking::getTotalAmount).reversed()` |
| Iterators / enhanced for | reported lists, `for (Future<String> future : futures)` |
| Exception handling (checked / unchecked) | `BookingException`, `DataAccessException`, try-with-resources everywhere |
| Custom exception | `BookingException` |
| Multithreading: Thread / Runnable / ExecutorService | `SeatBookingSimulator` with `Executors.newFixedThreadPool(3)` |
| Synchronization | `synchronized boolean reserve(String seat)` |
| `sleep()`, `join()`-style waiting | `awaitTermination`, `Thread.sleep` in report tasks |
| JDBC: DriverManager, Connection | `DBConnection.getConnection()` |
| Statement / PreparedStatement | all DAO queries use `?` placeholders |
| ResultSet, typed getters | `getInt`, `getString`, `getDouble`, `getDate` in the mappers |
| Transactions | `setAutoCommit(false)`, `commit()`, `rollback()` in `BookingService` |
| Lambda expressions | `resultSet -> resultSet.getString("seat_no")`, `this::mapMovie` |
| Stream API | `filter`, `map`, `sorted`, `limit`, `mapToDouble`, `collect(groupingBy)` |
| Generics | `JdbcHelper.query(...)` returns `List<T>` using `RowMapper<T>` |
| File handling | `BufferedWriter` + `Files.newBufferedWriter` CSV export in `ReportService` |

## 5. Setup and Run

**Step 1 — database.** Create the schema and sample data (see `database/README.md`):

```
mysql -u root -p < database/01_schema.sql
mysql -u root -p < database/02_triggers.sql
mysql -u root -p < database/03_sample_data.sql
mysql -u root -p < database/04_procedures_functions.sql
```

**Step 2 — driver.** `java-app/lib/mysql-connector-j-8.4.0.jar` is already included.
If it is missing, download `mysql-connector-j-8.x.jar` from
<https://dev.mysql.com/downloads/connector/j/> and copy it into `java-app/lib/`.

**Step 3 — password.** Edit `java-app/config/db.properties`:

```
db.url=jdbc:mysql://localhost:3306/noirbox_db
db.user=root
db.password=your_mysql_password
```

**Step 4 — run.** From the `java-app` folder:

```
run.bat          console application (noirbox.Main)
serve.bat        HTTP API on port 8080 (noirbox.api.ApiMain)
./run.sh, ./serve.sh   same on Linux / macOS / Git Bash
```

The scripts compile every file in `src` into `out` and start the chosen entry point.
Start `serve.bat` before opening the React website.

## 6. Demonstration Script for the Viva (5 minutes)

1. Start the program — it prints the banner and `Connected to database: noirbox_db`.
2. Log in with `aarav.sharma@example.com` (already in the sample data).
3. Option **1 → 2**: list now-showing movies.
4. Option **2**: choose movie id 1 and today's date — shows with theatre, format, price.
5. Option **3**: enter a show id — the seat map marks booked seats, prices per category.
6. Option **4**: book `H5,H6`, pay by UPI — booking confirmed with total.
7. Option **5**: the new booking is listed. In MySQL, `SELECT seats_available ...` is 2 less.
8. Option **6**: cancel that booking — seats return, `audit_log` has the entries.
9. Option **8**: revenue, bookings per movie, top audience ratings, CSV file path.
10. Option **9**: three threads try seat A1 — only one succeeds.
11. Option **10**: run `sp_theatre_report('Mumbai')` to show cursor output from MySQL.
12. Optional: start `serve.bat`, open the website, and book seats there — the same
    `BookingService` transaction writes the rows you then show in MySQL Workbench.

## 7. Current Status and Evidence

| Item | Status |
|---|---|
| Database schema, triggers, procedures, use-case queries | complete (`database/`) |
| Java model, DAO, service, util, concurrency classes | complete, compiles with `javac -Xlint:all` |
| Console prototype with input validation | complete |
| Reports + CSV export | complete |
| Documentation and viva answers | `docs/` |
| Pending for submission | paste your own MySQL outputs/screenshots into the report, add name and roll number |

Progress evidence: the folders `database/`, `java-app/src/`, `docs/` and the README
files show the work done for this milestone; each class has a header comment explaining
its purpose so the code can be walked through during the viva.
