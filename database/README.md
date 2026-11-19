# Database — run and verify

MySQL 8.0 is required (the course handout lists MySQL 8.0.38).

## Run order

| Order | File | What it does |
|---|---|---|
| 1 | `01_schema.sql` | creates `noirbox_db`, 12 tables, constraints, indexes, 3 views |
| 2 | `02_triggers.sql` | 3 triggers (availability + audit, review validation) |
| 3 | `03_sample_data.sql` | cities, theatres, movies, shows, users, bookings, reviews |
| 4 | `04_procedures_functions.sql` | 4 functions + 4 procedures |
| 5 | `05_use_cases.sql` | the 17 use cases, for the report screenshots |

Run 2 **before** 3: the sample bookings must fire the triggers, otherwise
`seats_available` would not match the seat rows.

```bash
mysql -u root -p < database/01_schema.sql
mysql -u root -p < database/02_triggers.sql
mysql -u root -p < database/03_sample_data.sql
mysql -u root -p < database/04_procedures_functions.sql
mysql -u root -p < database/05_use_cases.sql
```

On Windows the client is usually at
`C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe`; you can also open each
file in MySQL Workbench and click the lightning icon, one file at a time.

## Quick checks after loading

```sql
USE noirbox_db;
SHOW TABLES;                                   -- 12 tables
SELECT COUNT(*) FROM movie_show;               -- generated shows
SELECT * FROM v_show_details LIMIT 5;
SELECT * FROM v_movie_rating ORDER BY avg_rating DESC;
SELECT * FROM audit_log ORDER BY log_id DESC LIMIT 5;
SHOW TRIGGERS;
SHOW PROCEDURE STATUS WHERE Db = 'noirbox_db';
SHOW FUNCTION STATUS  WHERE Db = 'noirbox_db';
```

## Demonstrate the important behaviour

```sql
-- 1. booking through the stored procedure (transaction + loop + handler)
CALL sp_book_ticket(3, 22, 'C1,C2', 'UPI', @id, @total);
SELECT @id, @total;

-- 2. seats_available went down by 2 (trigger evidence)
SELECT show_id, seats_available FROM movie_show WHERE show_id = 22;

-- 3. the same seat cannot be sold twice -> error 1062, booking rolled back
CALL sp_book_ticket(4, 22, 'C1', 'UPI', @id2, @total2);

-- 4. cancel and get the seats back
CALL sp_cancel_booking(@id, @msg);
SELECT @msg;

-- 5. cursor report
CALL sp_theatre_report('Mumbai');
```

## Tables

`city`, `theatre`, `movie`, `movie_cast`, `movie_genre`, `movie_show`, `app_user`,
`booking`, `booking_seat`, `review`, `review_tag`, `audit_log`.

`city.slug`, `theatre.slug` and `movie.slug` are the ids used by the website
(e.g. `mumbai`, `pvr-palladium`, `spiderman`), so the same rows serve the React UI
through the Java API and the console application.

Views: `v_show_details`, `v_movie_rating`, `v_booking_summary`.

Full explanation, ER diagram and normalization steps: `docs/DBMS_Report.md`.
