# Viva Preparation — DBMS + Java

Short, honest answers you can give in 20–30 seconds each. Every answer points to the
file where the examiner can see the evidence.

---

## Part A — DBMS

**1. What is your project about?**
NOIRBOX is a cinema ticket booking system. A customer picks a city, a movie, a show, seats
and pays; the booking, the seats, the price and the review are stored in MySQL. The
database is in `database/`, the Java client in `java-app/`.

**2. Why did you normalize the database?**
To remove repeating data. For example a theatre's city was stored with every booking, so
changing a city meant editing hundreds of rows. After normalization the city is stored once
in `city` and `theatre` only keeps `city_id`.

**3. Show me 1NF.**
Every column has a single value. Genres (`Action, Adventure`), seats and review tags were
multi-valued, so they became separate tables: `movie_genre`, `booking_seat`, `review_tag`.

**4. Show me 2NF.**
No partial dependency. In `booking_seat(booking_id, seat_no, price)` the price depends on
the full key (which seat of which booking), not on a part of it. Every other table has a
single-attribute key.

**5. Show me 3NF.**
No transitive dependency. `theatre.theatre_id → city_id → city_name`, so `city_name`
depends on the city, not directly on the theatre; it was moved to `city`.

**6. Is your schema in BCNF?**
Yes. In every table the determinant is a candidate key — for example
`booking_id → user_id, show_id, total_amount`, and `(booking_id, seat_no) → price`.

**7. What is the primary key of `booking_seat` and why?**
`(booking_id, seat_no)`. A seat number alone repeats across bookings of different shows, so
the booking id is needed to identify the row. There is also a unique key on
`(show_id, seat_no)` so one seat of one show exists only once.

**8. How do you prevent double booking?**
Three levels: the `UNIQUE(show_id, seat_no)` constraint, the transaction in
`BookingService` / `sp_book_ticket` that locks the show row with `SELECT ... FOR UPDATE`,
and a pre-check in Java that shows a friendly error before inserting.

**9. What is a foreign key? Give an example.**
It enforces referential integrity: `booking.show_id` references `movie_show.show_id`, so a
booking cannot point to a show that does not exist.

**10. What does `seats_available` do and who updates it?**
It is the number of free seats of a show. It is updated only by triggers
(`trg_booking_seat_after_insert` / `..._after_delete`), so it always matches the seat rows.

**11. Which triggers did you write?**
Two on `booking_seat` (decrease / increase availability and write `audit_log`) and one
`BEFORE INSERT` on `review` that rejects a rating outside 1–10 with `SIGNAL` and derives the
verdict.

**12. Why a trigger instead of doing it in Java?**
The rule belongs to the data: even if someone inserts a seat row directly in MySQL, the
availability stays correct. It is enforced once, for every client.

**13. What is a stored procedure? Which one do you use?**
A named block of SQL stored in the database. `sp_book_ticket` does the whole booking —
lock the show, insert the booking, insert each seat in a `WHILE` loop, update the totals.
The Java app calls it with `CallableStatement`.

**14. What is a cursor?**
A pointer to the rows of a result set so they can be processed one at a time. In
`sp_theatre_report` a cursor walks over the theatres of a city and prints bookings and
revenue for each.

**15. Difference between a function and a procedure here?**
A function returns one value and is used inside a `SELECT` (`fn_seat_price`), a procedure
performs actions and can return several result sets (`sp_book_ticket`).

**16. What is `SIGNAL SQLSTATE '45000'`?**
It raises a user-defined error from SQL. We use it when there are not enough seats, so the
transaction is rolled back with a clear message.

**17. Explain your transaction and its ACID properties.**
`START TRANSACTION` → insert booking → insert seats → `COMMIT`. Atomicity: all or nothing.
Consistency: constraints and triggers. Isolation: `FOR UPDATE` lock. Durability: InnoDB
writes committed data to the redo log.

**18. What happens if two users book the same seat?**
The first transaction locks the show row and commits. The second one either waits and then
fails on the unique constraint, or fails immediately; in both cases the whole booking is
rolled back and no seat row is left behind.

**19. Which indexes did you create and why?**
`idx_show_movie_date` for "shows of a movie on a date", `idx_booking_user` for "my
bookings", `idx_review_movie` for average rating, and `idx_movie_status` for the now
showing list. `EXPLAIN` shows the index being used.

**20. What is a view? Which views do you have?**
A saved query used like a table: `v_show_details` (show + movie + theatre + city),
`v_movie_rating` (review count, average, recommendation %) and `v_booking_summary`.

**21. Difference between DELETE and TRUNCATE?**
`DELETE` removes selected rows, fires triggers and can be rolled back. `TRUNCATE` empties
the whole table, does not fire row triggers and resets the auto-increment.

**22. What is the difference between WHERE and HAVING?**
`WHERE` filters rows before grouping, `HAVING` filters groups after `GROUP BY` — used in UC7
with `HAVING revenue > 0`.

**23. Write a query to get revenue per theatre.**
See UC7 in `database/05_use_cases.sql`: join `theatre`, `movie_show`, `booking`, group by
theatre, `SUM(total_amount)`.

**24. Why use `PreparedStatement` instead of string concatenation?**
It binds values separately from the SQL text, so a value like `' OR 1=1 --` cannot change the
query. It also reuses the parsed statement.

**25. How is your database secure?**
A limited MySQL user with only the needed privileges (UC15), `PreparedStatement` in Java,
and no passwords stored in plain text in the database.

---

## Part B — Java

**26. Explain your package structure.**
`model` (data classes), `dao` (SQL), `service` (business logic), `db` (connection + helper),
`exception`, `util`, `concurrent`, and `Main` as the entry point. Each layer only talks to the
one below it, so the code is easy to explain and to change.

**27. Where is inheritance used?**
`BookingException extends Exception` (checked, so the caller must handle it) and
`DataAccessException extends RuntimeException` (unchecked, wraps `SQLException`).

**28. Where is an interface used?**
`JdbcHelper.RowMapper<T>` with one method `map(ResultSet)`. Each DAO passes a method
reference such as `this::mapMovie`, which is also an example of a lambda/method reference.

**29. Where are generics used?**
`public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params)` — the
same method returns `List<Movie>`, `List<Show>`, `List<String>` without casts.

**30. Which collections did you use and why?**
`ArrayList` for query results (ordered, fast iteration), `HashSet`/`Collections.synchronizedSet`
for booked seats (uniqueness), `LinkedHashMap` for report order (insertion order preserved),
`Map` for counting bookings per movie.

**31. Where is multithreading used?**
`SeatBookingSimulator` starts three tasks in a fixed thread pool; each one tries to reserve
seat A1. The `synchronized reserve()` method makes check-and-book atomic, so only one thread
succeeds. `ReportService` also runs two report tasks in a pool.

**32. What is a race condition and how did you fix it?**
Two threads read the same "seat free" value and both book it. The fix is to make the
check-and-update one atomic operation — `synchronized` in Java, `SELECT ... FOR UPDATE` and
the unique constraint in MySQL.

**33. What is the difference between `Thread` and `Runnable`?**
`Thread` is a class you extend; `Runnable` is an interface with `run()` that you implement and
pass to a `Thread` or an executor. We use `Runnable` lambdas with `ExecutorService`.

**34. Why `ExecutorService` instead of `new Thread()`?**
It reuses a fixed number of threads, returns `Future` results and is shut down cleanly with
`shutdown()` and `awaitTermination()`.

**35. Explain your JDBC steps.**
Load the driver, `DriverManager.getConnection(url, user, password)`, create a
`PreparedStatement`, bind parameters, `executeQuery()` / `executeUpdate()`, read the
`ResultSet` with typed getters, close everything with try-with-resources.

**36. What is `ResultSet`?**
A cursor over the rows returned by a query. `next()` moves to the next row, `getInt`,
`getString`, `getDouble`, `getDate` read columns by name or index.

**37. How do you manage resources?**
try-with-resources on `Connection`, `PreparedStatement` and `ResultSet`; Java closes them
automatically even when an exception is thrown.

**38. Where do you use Streams?**
`ReportService`: `groupingBy` + `counting` for bookings per movie, `mapToDouble` + `sum` for
revenue, `filter`, `sorted` with a `Comparator`, `limit` for the top five.

**39. Difference between `map` and `filter` in a Stream?**
`filter` keeps elements that match a condition, `map` transforms each element into another
value.

**40. Where do you do file handling?**
`ReportService.exportBookingsCsv` writes `reports/bookings_user_<id>.csv` with
`BufferedWriter` inside try-with-resources; the folder is created with
`Files.createDirectories`.

**41. How do you stop bad input from crashing the program?**
`InputUtil` re-asks until the value is valid (`NumberFormatException`,
`DateTimeParseException` are caught), seat numbers are checked against the real layout, and
`Main` catches `BookingException`, `DataAccessException` and `SQLException`.

**42. What is a checked and an unchecked exception? Give your examples.**
Checked exceptions must be handled or declared (`BookingException`, `SQLException`);
unchecked exceptions do not (`DataAccessException`, `NullPointerException`).

**43. Why a custom exception?**
`BookingException` carries a message meant for the user — "these seats are already booked" —
instead of a raw SQL error, so `Main` can print it directly.

**44. How does your Java app relate to the React frontend?**
The frontend has the same screens and the same data model (cities, movies, shows, seats,
bookings, reviews). The Java application is the database-backed version of the same flow, so
the UI can be shown as the design and the Java + MySQL program as the implementation.

**45. How does the website talk to the database?**
React calls the Java API (`java-app/src/noirbox/api/ApiServer.java`) with `fetch`. The API uses
the same DAO and `BookingService` classes as the console app, so one set of rules serves
both interfaces.

**46. Why is there no JSON library in the project?**
The browser sends form-encoded bodies and the server only writes JSON, so a ~60 line
`Json` helper is enough. That keeps the project dependency-free and easy to explain.

**47. What happens when two users click the same seat?**
The first request takes the row lock and commits. The second gets HTTP **409** with a
message; `SeatUnavailableException` is mapped to that status and the site shows "pick
different seats". No partial booking is saved.

**48. How does the seat map know which seats are taken?**
`GET /api/shows/{id}/seats` selects `seat_no` from `booking_seat` for that show. The React
seat map disables exactly those seats, so the picture always matches the database.

**49. What would you add next?**
Password-based login, an admin panel for adding shows, payment gateway integration, and
moving the API behind HTTPS with a proper deployment.
