# NOIRBOX — Cinema Ticket Booking (React + Java + MySQL)

### ▶ [Open the website](https://noirbox-cinema-g6br4p.freebuff.site)

**Live and public — it opens on any device, no setup needed:**
<https://noirbox-cinema-g6br4p.freebuff.site>

The hosted build (`hosted/`) serves the same React app and the same REST API from a
Cloudflare Worker, with SQLite (D1) standing in for MySQL. The original MySQL + JDBC
version below still runs locally exactly as before.

One project, two subjects:

| Part | Subject | Where |
|---|---|---|
| Booking website (React + Vite), now **live** against MySQL | design / prototype | `Project-1-main/` |
| MySQL database: schema, triggers, procedures, use cases | DBMS (24CSE0209) | `database/` |
| Java console application (JDBC) | Programming in Java (25CSE0202) | `java-app/` |
| Java HTTP API used by the website | Programming in Java + DBMS | `java-app/src/noirbox/api/` |
| Reports and viva answers | both | `docs/` |

The website and the console application talk to the **same MySQL database** through the
same Java DAO/service classes. There is no mock data path any more: movies, shows,
booked seats, bookings and reviews all come from `noirbox_db`.

---

## Run everything

### 1. Database (once)

```bash
mysql -u root -p < database/01_schema.sql
mysql -u root -p < database/02_triggers.sql
mysql -u root -p < database/03_sample_data.sql
mysql -u root -p < database/04_procedures_functions.sql
```

Order matters: the triggers must exist before the sample bookings are inserted.

### 2. Java API (keep this window open)

```powershell
cd D:\noirbox_proj\java-app
.\serve.bat
```
Listens on **http://localhost:8080/api** — check `http://localhost:8080/api/health`.

### 3. Website

```powershell
cd D:\noirbox_proj
npm run dev
```
Open **http://localhost:5173** (the port is pinned in `Project-1-main/vite.config.js`).

### Hosted version (no MySQL needed)

```powershell
cd D:\noirbox_proj\Project-1-main; npm run build
cd ..; rm -rf hosted/public; cp -r Project-1-main/dist hosted/public
cd hosted; npx wrangler dev        # http://localhost:8787
```
`hosted/worker.js` implements every endpoint of the Java API on D1 and seeds the
sample data on first request, so the public site needs no database server.

### Console version (for the Java viva)

```powershell
cd D:\noirbox_proj\java-app
.\run.bat
```

---

## What the website can do now

- Browse movies, shows and theatres — read from MySQL (`/api/movies`, `/api/shows`).
- Seat map shows the **real booked seats** of that show (`/api/shows/{id}/seats`).
- Booking goes through `BookingService`: one JDBC transaction, row lock
  (`SELECT ... FOR UPDATE`), price from the MySQL function `fn_seat_price`,
  and `UNIQUE(show_id, seat_no)` as the final guard.
- If two people pick the same seat, the second request gets **HTTP 409** and the site
  asks the user to choose other seats — nothing is half-saved.
- My Bookings lists the user's bookings from MySQL and can **cancel** them
  (seats are released by the `AFTER DELETE` trigger).
- Reviews are stored in `review` / `review_tag`; the trigger derives the verdict,
  and "Helpful" votes increment `review.likes`.

### API endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/health` | server + database check |
| GET | `/api/cities` | city list |
| GET | `/api/movies?status=now-showing` | catalogue with audience rating |
| GET | `/api/movies/{slug}` | one movie with genres and cast |
| GET | `/api/movies/{slug}/reviews` | reviews of a movie |
| GET | `/api/shows?movieId=&date=&city=` | showtimes |
| GET | `/api/shows/{id}` | one show |
| GET | `/api/shows/{id}/seats` | booked seats + prices |
| POST | `/api/bookings` | book seats (email, name, showId, seats, method) |
| GET | `/api/bookings?email=` | my bookings |
| GET | `/api/bookings/{id}` | one booking |
| POST | `/api/bookings/{id}/cancel` | cancel + release seats |
| POST | `/api/reviews` | write a review |
| POST | `/api/reviews/{id}/vote` | mark helpful |
| DELETE | `/api/reviews/{id}?email=` | delete own review |

Browser requests are form-encoded, so the Java side needs no JSON library
(`noirbox/api/Json.java` only writes JSON).

---

## Subject deliverables

### DBMS (24CSE0209)

- `database/01_schema.sql` — 12 tables in 3NF/BCNF, keys, CHECK/UNIQUE constraints,
  6 indexes, 3 views.
- `database/02_triggers.sql` — seat availability on insert/delete + audit log,
  review validation with `SIGNAL`.
- `database/03_sample_data.sql` — same cities, theatres, movies, cast and shows
  that the website displays.
- `database/04_procedures_functions.sql` — 4 functions, 4 procedures (transaction,
  cursor, `WHILE`, `EXIT HANDLER`).
- `database/05_use_cases.sql` — 17 use cases: joins, aggregates, sub-queries, UNION,
  views, DCL, manual transaction, `EXPLAIN`.

### Programming in Java (25CSE0202)

- `java-app/src/noirbox/model` — entities (encapsulation, constructors).
- `java-app/src/noirbox/dao` — JDBC with `PreparedStatement` and `ResultSet` mapping.
- `java-app/src/noirbox/db` — `DBConnection` + generic `JdbcHelper<T>`.
- `java-app/src/noirbox/service` — transactions, streams/collectors, CSV export,
  `ExecutorService`, `CallableStatement`.
- `java-app/src/noirbox/api` — HTTP server, routing, JSON writing, CORS, status codes.
- `java-app/src/noirbox/concurrent` — thread pool + `synchronized` seat reservation.
- `java-app/src/noirbox/util` / `exception` — validated input and custom exceptions.

### Documentation

- `docs/DBMS_Report.md` — ER diagram, schema, normalization, use cases, triggers,
  transactions, indexing, security.
- `docs/Java_Report.md` — problem statement, class structure, concept mapping,
  demo script (console + API).
- `docs/Viva_QA.md` — 49 questions with short answers.

---

## Notes

- Payments are simulated; no payment gateway is called.
- The API uses CORS `*` and no authentication beyond the demo email login — fine for a
  college demo, not for production.
- Seat rules match everywhere: rows A–B premium (×1.55), C–F executive (×1.20),
  G–J normal (×1.00), ₹28 convenience fee per ticket, 136 seats per show.
- Sample shows exist for today, tomorrow and the day after.
