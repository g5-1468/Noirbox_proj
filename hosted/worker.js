/**
 * NOIRBOX - hosted build (Cloudflare Worker + D1).
 *
 * Same REST contract as java-app (ApiServer.java), so the React frontend does
 * not change: it simply calls /api on the same origin instead of
 * http://localhost:8080. MySQL is replaced by D1 (SQLite), and the MySQL
 * triggers become real SQLite triggers in the schema below.
 */

// ---------------------------------------------------------------------------
// Schema (SQLite / D1 flavour of database/01_schema.sql + 02_triggers.sql)
// ---------------------------------------------------------------------------
const SCHEMA = [
  `CREATE TABLE IF NOT EXISTS meta (
     key   TEXT PRIMARY KEY,
     value TEXT NOT NULL
   )`,
  `CREATE TABLE IF NOT EXISTS city (
     city_id    INTEGER PRIMARY KEY AUTOINCREMENT,
     city_name  TEXT NOT NULL UNIQUE,
     state_name TEXT NOT NULL,
     slug       TEXT NOT NULL UNIQUE
   )`,
  `CREATE TABLE IF NOT EXISTS theatre (
     theatre_id   INTEGER PRIMARY KEY AUTOINCREMENT,
     theatre_name TEXT NOT NULL,
     chain        TEXT NOT NULL,
     city_id      INTEGER NOT NULL REFERENCES city(city_id),
     locality     TEXT NOT NULL,
     amenities    TEXT,
     slug         TEXT NOT NULL UNIQUE
   )`,
  `CREATE TABLE IF NOT EXISTS movie (
     movie_id      INTEGER PRIMARY KEY AUTOINCREMENT,
     slug          TEXT NOT NULL UNIQUE,
     title         TEXT NOT NULL,
     language      TEXT NOT NULL DEFAULT 'English',
     certification TEXT,
     duration_min  INTEGER,
     release_date  TEXT,
     movie_status  TEXT NOT NULL DEFAULT 'now-showing',
     rating        REAL NOT NULL DEFAULT 0,
     popularity    INTEGER NOT NULL DEFAULT 0,
     director      TEXT,
     tagline       TEXT,
     description   TEXT,
     poster_url    TEXT,
     backdrop_url  TEXT,
     trailer_url   TEXT,
     featured      INTEGER NOT NULL DEFAULT 0
   )`,
  `CREATE TABLE IF NOT EXISTS movie_cast (
     movie_id   INTEGER NOT NULL REFERENCES movie(movie_id) ON DELETE CASCADE,
     actor_name TEXT NOT NULL,
     position   INTEGER NOT NULL,
     PRIMARY KEY (movie_id, actor_name)
   )`,
  `CREATE TABLE IF NOT EXISTS movie_genre (
     movie_id INTEGER NOT NULL REFERENCES movie(movie_id) ON DELETE CASCADE,
     genre    TEXT NOT NULL,
     PRIMARY KEY (movie_id, genre)
   )`,
  `CREATE TABLE IF NOT EXISTS movie_show (
     show_id         INTEGER PRIMARY KEY AUTOINCREMENT,
     movie_id        INTEGER NOT NULL REFERENCES movie(movie_id),
     theatre_id      INTEGER NOT NULL REFERENCES theatre(theatre_id),
     show_date       TEXT NOT NULL,
     show_time       TEXT NOT NULL,
     format          TEXT NOT NULL DEFAULT '2D',
     price           REAL NOT NULL,
     seats_available INTEGER NOT NULL DEFAULT 136,
     UNIQUE (movie_id, theatre_id, show_date, show_time, format)
   )`,
  `CREATE TABLE IF NOT EXISTS app_user (
     user_id   INTEGER PRIMARY KEY AUTOINCREMENT,
     full_name TEXT NOT NULL,
     email     TEXT NOT NULL UNIQUE,
     phone     TEXT
   )`,
  `CREATE TABLE IF NOT EXISTS booking (
     booking_id      INTEGER PRIMARY KEY AUTOINCREMENT,
     user_id         INTEGER NOT NULL REFERENCES app_user(user_id),
     show_id         INTEGER NOT NULL REFERENCES movie_show(show_id),
     booking_time    TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
     seat_count      INTEGER NOT NULL,
     subtotal        REAL NOT NULL DEFAULT 0,
     convenience_fee REAL NOT NULL DEFAULT 0,
     total_amount    REAL NOT NULL DEFAULT 0,
     payment_method  TEXT NOT NULL,
     booking_status  TEXT NOT NULL DEFAULT 'CONFIRMED'
   )`,
  `CREATE TABLE IF NOT EXISTS booking_seat (
     booking_id INTEGER NOT NULL REFERENCES booking(booking_id) ON DELETE CASCADE,
     show_id    INTEGER NOT NULL REFERENCES movie_show(show_id),
     seat_no    TEXT NOT NULL,
     price      REAL NOT NULL,
     PRIMARY KEY (booking_id, seat_no),
     UNIQUE (show_id, seat_no)
   )`,
  `CREATE TABLE IF NOT EXISTS review (
     review_id    INTEGER PRIMARY KEY AUTOINCREMENT,
     movie_id     INTEGER NOT NULL REFERENCES movie(movie_id) ON DELETE CASCADE,
     user_id      INTEGER NOT NULL REFERENCES app_user(user_id),
     rating       INTEGER NOT NULL,
     verdict      TEXT NOT NULL DEFAULT 'worth-it',
     headline     TEXT NOT NULL,
     review_text  TEXT,
     has_spoilers INTEGER NOT NULL DEFAULT 0,
     likes        INTEGER NOT NULL DEFAULT 0,
     review_date  TEXT NOT NULL DEFAULT (date('now'))
   )`,
  `CREATE TABLE IF NOT EXISTS review_tag (
     review_id INTEGER NOT NULL REFERENCES review(review_id) ON DELETE CASCADE,
     tag       TEXT NOT NULL,
     PRIMARY KEY (review_id, tag)
   )`,
  `CREATE TABLE IF NOT EXISTS audit_log (
     log_id      INTEGER PRIMARY KEY AUTOINCREMENT,
     table_name  TEXT NOT NULL,
     action_name TEXT NOT NULL,
     detail      TEXT,
     logged_at   TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
   )`,
  `CREATE INDEX IF NOT EXISTS idx_show_movie_date ON movie_show (movie_id, show_date)`,
  `CREATE INDEX IF NOT EXISTS idx_booking_user ON booking (user_id)`,
  `CREATE INDEX IF NOT EXISTS idx_review_movie ON review (movie_id)`,
  // MySQL trigger 1: booking a seat reduces seats_available.
  `CREATE TRIGGER IF NOT EXISTS trg_booking_seat_after_insert
   AFTER INSERT ON booking_seat
   BEGIN
     UPDATE movie_show SET seats_available = seats_available - 1
      WHERE show_id = NEW.show_id;
   END`,
  // MySQL trigger 2: releasing a seat gives it back.
  `CREATE TRIGGER IF NOT EXISTS trg_booking_seat_after_delete
   AFTER DELETE ON booking_seat
   BEGIN
     UPDATE movie_show SET seats_available = seats_available + 1
      WHERE show_id = OLD.show_id;
   END`,
];

// ---------------------------------------------------------------------------
// Seed data (database/03_sample_data.sql, dates relative to today)
// ---------------------------------------------------------------------------
const SEED_HEAD = [
  `INSERT INTO city (city_name, state_name, slug) VALUES
     ('Mumbai','Maharashtra','mumbai'),
     ('Delhi NCR','Delhi','delhi'),
     ('Bengaluru','Karnataka','bengaluru'),
     ('Hyderabad','Telangana','hyderabad'),
     ('Pune','Maharashtra','pune'),
     ('Chennai','Tamil Nadu','chennai')`,
  `INSERT INTO theatre (theatre_name, chain, city_id, locality, amenities, slug) VALUES
     ('PVR Luxe Palladium','PVR',1,'Lower Parel','Dolby Atmos, Recliner, M-Ticket','pvr-palladium'),
     ('INOX R-City','INOX',1,'Ghatkopar','4K Laser, Food Court, M-Ticket','inox-rcity'),
     ('IMAX Wadala','IMAX',1,'Wadala','IMAX, Parking, M-Ticket','imax-wadala'),
     ('PVR Select Citywalk','PVR',2,'Saket','Dolby Atmos, Recliner, Valet','pvr-select-delhi'),
     ('Cinepolis DLF Avenue','Cinepolis',2,'Saket','4DX, M-Ticket, Cafe','cinepolis-dlf'),
     ('PVR Orion Mall','PVR',3,'Rajajinagar','IMAX, Recliner, M-Ticket','pvr-orion'),
     ('INOX Mantri Square','INOX',3,'Malleswaram','Dolby 7.1, Food Court','inox-mantri'),
     ('AMB Cinemas Gachibowli','AMB',4,'Gachibowli','Gold Class, IMAX, Valet','amb-hyderabad'),
     ('PVR ICON Pavilion','PVR',5,'Shivajinagar','Atmos, Recliner','pvr-icon-pune'),
     ('SPI Palazzo Nexus','SPI',6,'Vadapalani','Dolby Atmos, M-Ticket','spi-chennai')`,
];

const MOVIES = [
  ['spiderman', 'Spider-Man: Brand New Day', 'English', 'UA 13+', 130, '2026-07-31', 'now-showing', 8.4, 96, 'Destin Daniel Cretton', 'A brand new day.',
    'Peter Parker tries to rebuild a normal life in New York after the multiverse crisis. A new threat rises with the city, and Spider-Man has to choose who he is when the world no longer remembers the hero in the mask.',
    '/posters/brand-new-day.jpg', 'https://images.unsplash.com/photo-1635805737707-575885ab0820?auto=format&fit=crop&w=1600&q=80', 'https://www.youtube.com/watch?v=62bIsvRcPv0', 1],
  ['the-odyssey', 'The Odyssey', 'English', 'UA 16+', 173, '2026-07-17', 'now-showing', 8.1, 91, 'Christopher Nolan', 'Defy the gods.',
    'After the Trojan War, Odysseus, king of Ithaca, begins a long voyage home. Across sea, myth and memory he fights to reunite with Penelope and to outlast the will of the gods.',
    '/posters/odyssey.jpg', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1600&q=80', 'https://www.youtube.com/watch?v=Mzw2ttJD2qQ', 0],
  ['last-monsoon', 'Barbie', 'English', 'PG-13', 114, '2023-07-21', 'now-showing', 7.9, 88, 'Greta Gerwig', 'She is everything. He is just Ken.',
    'After leaving the perfectly ordered world of Barbie Land for the real world, Barbie and Ken discover that life outside their familiar roles is far more complicated than they expected.',
    'https://media.themoviedb.org/t/p/w500/iuFNMS8U5cb6xfzi51Dbkovj7vM.jpg', 'https://media.themoviedb.org/t/p/w1280/1esAE8sLJRWWFsLLeh5r3g2WanI.jpg', null, 0],
  ['midnight-raga', 'Oppenheimer', 'English', 'R', 180, '2023-07-21', 'now-showing', 8.6, 84, 'Christopher Nolan', 'The world forever changes.',
    'Physicist J. Robert Oppenheimer leads the wartime scientific effort to develop the first atomic bomb, then faces the personal and political consequences of its creation.',
    'https://media.themoviedb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg', 'https://media.themoviedb.org/t/p/w1280/neeNHeXjMF5fXoCJRsOmkNGC7q.jpg', null, 0],
  ['saffron-skies', 'The Devil Wears Prada 2', 'English', 'PG-13', null, '2026-05-01', 'now-showing', 7.2, 79, 'David Frankel', 'Andy returns to Runway.',
    'Andy Sachs returns to Runway as editor Miranda Priestly navigates a changing media landscape. Their paths cross again with Emily Charlton, now leading a luxury brand.',
    'https://media.themoviedb.org/t/p/w500/xTI42pmsP5EDnvsNJPEDubwWBQO.jpg', 'https://media.themoviedb.org/t/p/w1280/dRoFlUbH1l51ACmx5oPCI3jfvZq.jpg', null, 0],
  ['velvet-ashes', 'Scream 7', 'English', 'R', 114, '2026-02-27', 'now-showing', 6.8, 82, 'Kevin Williamson', 'Burn it all down.',
    'A new Ghostface killer targets Sidney Prescott in the town where she has built a new life, forcing her to confront the threat and the past she hoped to leave behind.',
    'https://media.themoviedb.org/t/p/w500/jjyuk0edLiW8vOSnlfwWCCLpbh5.jpg', 'https://media.themoviedb.org/t/p/w1280/3eUyLEF5M0ky3h6KJsWiWzaakB8.jpg', null, 0],
  ['harbour-lights', 'Jurassic World Rebirth', 'English', 'PG-13', 134, '2025-07-02', 'now-showing', 7.1, 86, 'Gareth Edwards', 'A new mission among the dinosaurs.',
    'Five years after the last Jurassic World story, a specialist team heads to an isolated island to collect samples from three of the largest remaining dinosaurs.',
    'https://media.themoviedb.org/t/p/w500/1RICxzeoNCAO5NpcRMIgg1XT6fm1.jpg', 'https://media.themoviedb.org/t/p/w1280/zNriRTr0kWwyaXPzdg1EIxf0BWk.jpg', null, 0],
  ['clockmakers-daughter', 'Toy Story 5', 'English', 'U', 102, '2026-06-19', 'now-showing', 8.0, 90, 'Andrew Stanton', 'It is on.',
    'When Bonnie becomes absorbed in a new tablet, Woody, Buzz, Jessie and the rest of the toys have to compete with a new kind of playtime distraction.',
    'https://media.themoviedb.org/t/p/w500/sfQtVlIHljToOwYjhe21KPGzZWK.jpg', 'https://media.themoviedb.org/t/p/w1280/qjTqY5coNiz6sVtPng40IzltsoN.jpg', null, 1],
  ['kala-dhoop', 'Inside Out 2', 'English', 'PG', 96, '2024-06-14', 'now-showing', 8.2, 74, 'Kelsey Mann', 'Make room for new emotions.',
    "As Riley enters her teenage years, her mind's familiar Emotions face a major change when new arrivals, including Anxiety, take over the control room.",
    'https://media.themoviedb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg', 'https://media.themoviedb.org/t/p/w1280/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg', null, 0],
  ['iron-lotus', 'Mortal Kombat II', 'English', 'A', null, '2026-05-15', 'now-showing', 6.5, 80, 'Simon McQuoid', 'The battle for Earthrealm continues.',
    "Johnny Cage joins the champions as they face a deadly tournament and the threat of Shao Kahn's rule over Earthrealm.",
    'https://media.themoviedb.org/t/p/w500/hwRdDFIhaEmpRgoki805YvyyjZf.jpg', 'https://media.themoviedb.org/t/p/w1280/4EAAwpylq313qrDqpCxulUrXBNF.jpg', null, 0],
  ['starfall', 'Dune: Part Three', 'English', 'TBA', 165, '2026-12-18', 'coming-soon', 0.0, 95, 'Denis Villeneuve', 'The epic conclusion.',
    'Years after taking the throne, Paul Atreides faces the consequences of his rule and a growing conspiracy that threatens the people closest to him.',
    'https://media.themoviedb.org/t/p/w500/q2oFnkbhYsDKsYMMl8PyGOAPfjY.jpg', 'https://i.ytimg.com/vi/NdvqHc56lE0/hqdefault.jpg', 'https://www.youtube.com/watch?v=NdvqHc56lE0', 0],
  ['ivory-room', 'Avengers: Doomsday', 'English', 'UA 13+', 150, '2026-12-18', 'coming-soon', 0.0, 87, 'Anthony Russo, Joe Russo', 'The end begins.',
    'The Avengers face Doctor Doom as realities collide. Old alliances return, new fractures open, and the team has one last window to stop a war that could end every world they have ever saved.',
    '/posters/doomsday.jpg', 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1600&q=80', 'https://www.youtube.com/watch?v=X1aFkAkFASk', 0],
];

const GENRES = [
  [1, ['Action', 'Adventure']],
  [2, ['Action', 'Adventure', 'Fantasy']],
  [3, ['Comedy', 'Adventure', 'Fantasy']],
  [4, ['Drama', 'History', 'Thriller']],
  [5, ['Comedy', 'Drama']],
  [6, ['Horror', 'Mystery', 'Thriller']],
  [7, ['Adventure', 'Sci-Fi', 'Action', 'Thriller']],
  [8, ['Animation', 'Family', 'Comedy', 'Adventure']],
  [9, ['Animation', 'Family', 'Comedy']],
  [10, ['Action', 'Fantasy', 'Adventure']],
  [11, ['Sci-Fi', 'Adventure', 'Drama']],
  [12, ['Action', 'Sci-Fi']],
];

const CAST = [
  [1, ['Tom Holland', 'Zendaya', 'Sadie Sink', 'Liza Colon-Zayas', 'Jon Bernthal', 'Mark Ruffalo']],
  [2, ['Matt Damon', 'Tom Holland', 'Anne Hathaway', 'Robert Pattinson', 'Zendaya', 'Charlize Theron']],
  [3, ['Margot Robbie', 'Ryan Gosling', 'America Ferrera', 'Kate McKinnon']],
  [4, ['Cillian Murphy', 'Emily Blunt', 'Matt Damon', 'Robert Downey Jr.']],
  [5, ['Anne Hathaway', 'Meryl Streep', 'Emily Blunt', 'Stanley Tucci']],
  [6, ['Neve Campbell', 'Courteney Cox', 'Isabel May', 'Jasmin Savoy Brown']],
  [7, ['Scarlett Johansson', 'Mahershala Ali', 'Jonathan Bailey', 'Rupert Friend']],
  [8, ['Tom Hanks', 'Tim Allen', 'Joan Cusack', 'Greta Lee']],
  [9, ['Amy Poehler', 'Maya Hawke', 'Kensington Tallman', 'Ayo Edebiri']],
  [10, ['Karl Urban', 'Adeline Rudolph', 'Ludi Lin', 'Jessica McNamee']],
  [11, ['Timothee Chalamet', 'Zendaya', 'Florence Pugh', 'Rebecca Ferguson', 'Jason Momoa']],
  [12, ['Robert Downey Jr.', 'Chris Hemsworth', 'Chris Evans', 'Scarlett Johansson', 'Tom Holland', 'Benedict Cumberbatch']],
];

const USERS = [
  ['Aarav Sharma', 'aarav.sharma@example.com', '9810012345'],
  ['Meghna Roy', 'meghna.roy@example.com', '9820034567'],
  ['Rohan Deshmukh', 'rohan.d@example.com', '9830045678'],
  ['Zoya Merchant', 'zoya.m@example.com', '9840056789'],
  ['Kabir Varma', 'kabir.v@example.com', '9850067890'],
];

// Generate the next three days of showtimes (the SQL equivalent of
// database/03_sample_data.sql, with today's date recalculated every time the
// demo needs a fresh slate).
const SHOWS_SQL = `INSERT INTO movie_show (movie_id, theatre_id, show_date, show_time, format, price)
  SELECT m.movie_id, t.theatre_id, date('now', '+' || d.n || ' day'), s.show_time, s.format, s.price
    FROM movie m
    JOIN theatre t ON (t.theatre_id % 3) = (m.movie_id % 3)
   CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2) d
   CROSS JOIN (SELECT '10:15:00' AS show_time, '2D' AS format, 220.00 AS price
               UNION ALL SELECT '16:20:00', '3D', 280.00
               UNION ALL SELECT '19:30:00', '2D', 220.00) s
   WHERE m.movie_status = 'now-showing'`;

const PREMIUM_SHOWS_SQL = `INSERT INTO movie_show (movie_id, theatre_id, show_date, show_time, format, price)
  SELECT 1, 3, date('now', '+0 day'), '22:10:00', 'IMAX', 420.00
   UNION ALL SELECT 2, 3, date('now', '+1 day'), '22:10:00', 'IMAX', 420.00
   UNION ALL SELECT 8, 6, date('now', '+0 day'), '13:00:00', '2D', 200.00`;

const REVIEWS = [
  [1, 1, 9, 'must-watch', 'An unforgiving masterclass that demands an IMAX screen', 'The sound design and the action set pieces are worth the ticket price alone.', 0, 42, '2026-08-02', ['Acting', 'Sound Design', 'Cinematography']],
  [1, 2, 8, 'must-watch', 'Gripping and visually stunning', 'The second act slows down a little but the climax more than compensates.', 0, 19, '2026-08-05', ['Visuals', 'Direction']],
  [1, 3, 6, 'worth-it', 'Visually stunning but slow in the middle', 'If you like slow-burn stories you will enjoy it; otherwise wait for OTT.', 0, 8, '2026-08-09', ['Slow Paced', 'Cinematography']],
  [2, 4, 9, 'must-watch', 'Nolan at his most ambitious', 'A long film that never feels long. Watch it on the biggest screen you can find.', 0, 27, '2026-07-20', ['Direction', 'Music & BGM']],
  [4, 5, 10, 'must-watch', 'A career defining performance', 'The courtroom third act is outstanding.', 0, 33, '2026-07-25', ['Acting', 'Screenplay']],
  [9, 2, 8, 'must-watch', 'A warm, funny sequel', 'The new emotions fit perfectly into the story.', 0, 12, '2026-06-20', ['Family Friendly', 'Music & BGM']],
];

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
const FEE_PER_TICKET = 28;
const TOTAL_SEATS = 136;

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function json(data, status = 200) {
  return Response.json(data, {
    status,
    headers: {
      'content-type': 'application/json; charset=utf-8',
      'cache-control': 'no-store',
      'access-control-allow-origin': '*',
    },
  });
}

const fail = (status, message) => json({ error: message }, status);

/** Seat price: rows A-B premium x1.55, C-F executive x1.20, G-J normal x1.00. */
function seatPrice(basePrice, seatNo) {
  const row = String(seatNo || '').trim().toUpperCase().charAt(0);
  const factor = row === 'A' || row === 'B' ? 1.55 : row >= 'C' && row <= 'F' ? 1.2 : 1;
  return Math.round(basePrice * factor);
}

/** '19:30:00' -> '07:30 PM' (same format as the Java API). */
function formatTime(value) {
  const [h, m] = String(value || '00:00:00').split(':').map(Number);
  const suffix = h >= 12 ? 'PM' : 'AM';
  const hour12 = h % 12 === 0 ? 12 : h % 12;
  return `${String(hour12).padStart(2, '0')}:${String(m).padStart(2, '0')} ${suffix}`;
}

const isoDateTime = (value) => String(value || '').replace(' ', 'T');

let schemaReady = false;

async function initDb(env) {
  if (!schemaReady) {
    // Every statement uses IF NOT EXISTS, so this is safe to re-run.
    await env.DB.batch(SCHEMA.map((sql) => env.DB.prepare(sql)));
    schemaReady = true;
  }

  // One-shot seed: the INSERT OR IGNORE below is atomic, so when several
  // requests arrive together only the first one runs the 200+ inserts.
  const claim = await env.DB.prepare(
    "INSERT OR IGNORE INTO meta (key, value) VALUES ('seed', 'v1')",
  ).run();
  if (!claim.meta || !claim.meta.changes) return;

  const statements = [];
  for (const sql of SEED_HEAD) statements.push(sql);

  for (const m of MOVIES) {
    statements.push({
      sql: `INSERT INTO movie (slug, title, language, certification, duration_min, release_date,
              movie_status, rating, popularity, director, tagline, description,
              poster_url, backdrop_url, trailer_url, featured)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)`,
      args: m,
    });
  }
  for (const [movieId, list] of GENRES) {
    for (const genre of list) {
      statements.push({ sql: 'INSERT INTO movie_genre (movie_id, genre) VALUES (?,?)', args: [movieId, genre] });
    }
  }
  for (const [movieId, list] of CAST) {
    list.forEach((actor, index) => {
      statements.push({ sql: 'INSERT INTO movie_cast (movie_id, actor_name, position) VALUES (?,?,?)', args: [movieId, actor, index + 1] });
    });
  }
  statements.push(SHOWS_SQL, PREMIUM_SHOWS_SQL);

  for (const u of USERS) {
    statements.push({ sql: 'INSERT INTO app_user (full_name, email, phone) VALUES (?,?,?)', args: u });
  }

  // Sample bookings (they fire the seat triggers, so availability drops).
  statements.push(
    { sql: `INSERT INTO booking (user_id, show_id, seat_count, subtotal, convenience_fee, total_amount, payment_method, booking_status)
            VALUES (1,1,2,0,0,0,'UPI','CONFIRMED')` },
    { sql: `INSERT INTO booking (user_id, show_id, seat_count, subtotal, convenience_fee, total_amount, payment_method, booking_status)
            VALUES (2,5,1,0,0,0,'Credit / Debit Card','CONFIRMED')` },
    { sql: `INSERT INTO booking (user_id, show_id, seat_count, subtotal, convenience_fee, total_amount, payment_method, booking_status)
            VALUES (1,9,3,0,0,0,'Net Banking','CONFIRMED')` },
  );

  const sampleSeats = [
    [1, 1, 'A1'], [1, 1, 'A2'],
    [2, 5, 'C3'],
    [3, 9, 'G7'], [3, 9, 'G8'], [3, 9, 'G9'],
  ];
  for (const [bookingId, showId, seat] of sampleSeats) {
    statements.push({
      sql: `INSERT INTO booking_seat (booking_id, show_id, seat_no, price)
            SELECT ?, ?, ?, ROUND(price * ?) FROM movie_show WHERE show_id = ?`,
      args: [bookingId, showId, seat,
        seat.startsWith('A') || seat.startsWith('B') ? 1.55 : seat >= 'C' && seat <= 'F' ? 1.2 : 1,
        showId],
    });
  }

  statements.push(
    { sql: `UPDATE booking SET
              subtotal = (SELECT IFNULL(SUM(price),0) FROM booking_seat WHERE booking_id = booking.booking_id),
              convenience_fee = seat_count * ${FEE_PER_TICKET},
              total_amount = (SELECT IFNULL(SUM(price),0) FROM booking_seat WHERE booking_id = booking.booking_id) + seat_count * ${FEE_PER_TICKET}
            WHERE booking_id IN (1,2,3)` },
  );

  for (const [movieId, userId, rating, verdict, headline, text, spoilers, likes, date, tags] of REVIEWS) {
    statements.push({
      sql: `INSERT INTO review (movie_id, user_id, rating, verdict, headline, review_text, has_spoilers, likes, review_date)
            VALUES (?,?,?,?,?,?,?,?,?)`,
      args: [movieId, userId, rating, verdict, headline, text, spoilers, likes, date],
    });
    for (const tag of tags) {
      statements.push({
        sql: `INSERT INTO review_tag (review_id, tag)
              SELECT review_id, ? FROM review ORDER BY review_id DESC LIMIT 1`,
        args: [tag],
      });
    }
  }

  try {
    await env.DB.batch(statements.map((s) => (typeof s === 'string'
      ? env.DB.prepare(s)
      : env.DB.prepare(s.sql).bind(...(s.args || [])))));
  } catch (problem) {
    // Release the claim so the next request retries the whole seed.
    await env.DB.prepare("DELETE FROM meta WHERE key = 'seed'").run();
    throw problem;
  }
}

/** Keeps the demo alive: if every showtime is in the past, publish a new run. */
async function ensureShowtimes(env) {
  const latest = await env.DB.prepare('SELECT MAX(show_date) AS last FROM movie_show').first();
  const today = new Date().toISOString().slice(0, 10);
  if (latest && latest.last && String(latest.last) >= today) return;
  if (!latest || !latest.last) return; // seeding handles the empty case
  await env.DB.batch([env.DB.prepare(SHOWS_SQL), env.DB.prepare(PREMIUM_SHOWS_SQL)]);
}

// ---------------------------------------------------------------------------
// Query + mapping layer
// ---------------------------------------------------------------------------
const MOVIE_SELECT = `SELECT m.*,
    (SELECT COUNT(*) FROM review r WHERE r.movie_id = m.movie_id) AS review_count,
    IFNULL((SELECT ROUND(AVG(r.rating), 1) FROM review r WHERE r.movie_id = m.movie_id), 0) AS audience_rating,
    IFNULL((SELECT CASE WHEN COUNT(*) = 0 THEN 0
                        ELSE CAST(ROUND(SUM(CASE WHEN r.rating >= 7 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)) AS INTEGER)
                   END
              FROM review r WHERE r.movie_id = m.movie_id), 0) AS recommend_percent
  FROM movie m`;

const SHOW_SELECT = `SELECT ms.show_id, m.slug AS movie_slug, m.title AS movie_title,
    t.slug AS theatre_slug, t.theatre_name, c.slug AS city_slug,
    ms.show_date, ms.show_time, ms.format, ms.price, ms.seats_available
  FROM movie_show ms
  JOIN movie m   ON m.movie_id   = ms.movie_id
  JOIN theatre t ON t.theatre_id = ms.theatre_id
  JOIN city c    ON c.city_id    = t.city_id`;

const BOOKING_SELECT = `SELECT b.booking_id, b.user_id, u.full_name, b.show_id,
    m.slug AS movie_slug, m.title AS movie_title, t.slug AS theatre_slug, t.theatre_name,
    ms.show_date, ms.show_time, b.seat_count, b.subtotal, b.convenience_fee,
    b.total_amount, b.payment_method, b.booking_status, b.booking_time
  FROM booking b
  JOIN app_user u    ON u.user_id    = b.user_id
  JOIN movie_show ms ON ms.show_id   = b.show_id
  JOIN movie m       ON m.movie_id   = ms.movie_id
  JOIN theatre t     ON t.theatre_id = ms.theatre_id`;

const REVIEW_SELECT = `SELECT r.review_id, m.slug AS movie_slug, u.full_name, u.email AS author_email,
    r.rating, r.verdict, r.headline, r.review_text, r.has_spoilers, r.likes, r.review_date,
    EXISTS (SELECT 1 FROM booking b JOIN movie_show ms ON ms.show_id = b.show_id
             WHERE b.user_id = r.user_id AND ms.movie_id = r.movie_id
               AND b.booking_status = 'CONFIRMED') AS verified
  FROM review r
  JOIN movie m    ON m.movie_id = r.movie_id
  JOIN app_user u ON u.user_id  = r.user_id`;

async function movieJson(env, row, genres, cast) {
  return {
    id: row.slug,
    title: row.title,
    tagline: row.tagline,
    genres,
    language: row.language,
    rating: row.rating,
    audienceRating: row.audience_rating,
    recommendPercent: row.recommend_percent,
    reviewCount: row.review_count,
    popularity: row.popularity,
    duration: row.duration_min || 0,
    releaseDate: row.release_date,
    status: row.movie_status,
    certification: row.certification,
    director: row.director,
    cast,
    description: row.description,
    poster: row.poster_url,
    backdrop: row.backdrop_url,
    trailerUrl: row.trailer_url,
    featured: !!row.featured,
  };
}

async function withDetails(env, rows) {
  if (!rows.length) return [];
  const genres = await env.DB.prepare('SELECT movie_id, genre FROM movie_genre ORDER BY genre').all();
  const cast = await env.DB.prepare('SELECT movie_id, actor_name FROM movie_cast ORDER BY position').all();
  const byMovie = (list) => {
    const map = new Map();
    for (const item of list.results || []) {
      if (!map.has(item.movie_id)) map.set(item.movie_id, []);
      map.get(item.movie_id).push(item.genre ?? item.actor_name);
    }
    return map;
  };
  const genreMap = byMovie(genres);
  const castMap = byMovie(cast);
  return Promise.all(rows.map((row) => movieJson(env, row, genreMap.get(row.movie_id) || [], castMap.get(row.movie_id) || [])));
}

function showJson(row) {
  return {
    id: String(row.show_id),
    movieId: row.movie_slug,
    title: row.movie_title,
    theatreId: row.theatre_slug,
    theatreName: row.theatre_name,
    city: row.city_slug,
    date: row.show_date,
    time: formatTime(row.show_time),
    format: row.format,
    price: row.price,
    seatsAvailable: row.seats_available,
  };
}

function bookingJson(row, seats) {
  return {
    id: String(row.booking_id),
    movieId: row.movie_slug,
    title: row.movie_title,
    theatreId: row.theatre_slug,
    theatreName: row.theatre_name,
    showId: String(row.show_id),
    date: row.show_date,
    time: formatTime(row.show_time),
    seats,
    seatCount: row.seat_count,
    subtotal: row.subtotal,
    convenienceFee: row.convenience_fee,
    total: row.total_amount,
    method: row.payment_method,
    status: row.booking_status,
    bookedAt: isoDateTime(row.booking_time),
    userName: row.full_name,
  };
}

async function bookingSeats(env, bookingId) {
  const rows = await env.DB.prepare('SELECT seat_no FROM booking_seat WHERE booking_id = ? ORDER BY seat_no')
    .bind(bookingId).all();
  return (rows.results || []).map((r) => r.seat_no);
}

async function reviewJson(env, row) {
  const tags = await env.DB.prepare('SELECT tag FROM review_tag WHERE review_id = ? ORDER BY tag')
    .bind(row.review_id).all();
  return {
    id: `rev-${row.review_id}`,
    reviewId: row.review_id,
    movieId: row.movie_slug,
    authorName: row.full_name,
    authorEmail: row.author_email,
    rating: row.rating,
    verdict: row.verdict,
    headline: row.headline,
    content: row.review_text,
    tags: (tags.results || []).map((t) => t.tag),
    hasSpoilers: !!row.has_spoilers,
    likes: row.likes,
    date: row.review_date,
    verifiedBooking: !!row.verified,
  };
}

async function findOrCreateUser(env, email, fullName) {
  const existing = await env.DB.prepare('SELECT * FROM app_user WHERE email = ?').bind(email).first();
  if (existing) return existing;
  await env.DB.prepare('INSERT INTO app_user (full_name, email) VALUES (?, ?)').bind(fullName, email).run();
  return env.DB.prepare('SELECT * FROM app_user WHERE email = ?').bind(email).first();
}

async function createBooking(env, form) {
  const email = requireField(form, 'email');
  const name = form.name || 'Guest';
  const showId = Number(requireField(form, 'showId'));
  const method = form.method || 'UPI';
  const seats = [...new Set(String(requireField(form, 'seats')).split(',')
    .map((s) => s.trim().toUpperCase()).filter(Boolean))];

  if (!seats.length) throw new HttpError(400, 'Please select at least one seat.');

  const user = await findOrCreateUser(env, email, name);
  const show = await env.DB.prepare('SELECT * FROM movie_show WHERE show_id = ?').bind(showId).first();
  if (!show) throw new HttpError(400, `Show ${showId} does not exist.`);

  const taken = await env.DB.prepare('SELECT seat_no FROM booking_seat WHERE show_id = ?').bind(showId).all();
  const takenSet = new Set((taken.results || []).map((r) => r.seat_no));
  const conflict = seats.filter((seat) => takenSet.has(seat));
  if (conflict.length) {
    throw new HttpError(409, `These seats are already booked: ${conflict.join(', ')}`);
  }
  if (show.seats_available < seats.length) {
    throw new HttpError(409, `Only ${show.seats_available} seats are left for this show.`);
  }

  const subtotal = seats.reduce((sum, seat) => sum + seatPrice(show.price, seat), 0);
  const fee = seats.length * FEE_PER_TICKET;

  const created = await env.DB.prepare(
    `INSERT INTO booking (user_id, show_id, seat_count, subtotal, convenience_fee, total_amount, payment_method)
     VALUES (?,?,?,?,?,?,?)`,
  ).bind(user.user_id, showId, seats.length, subtotal, fee, subtotal + fee, method).run();

  const bookingId = created.meta.last_row_id;

  try {
    await env.DB.batch(seats.map((seat) => env.DB.prepare(
      'INSERT INTO booking_seat (booking_id, show_id, seat_no, price) VALUES (?,?,?,?)',
    ).bind(bookingId, showId, seat, seatPrice(show.price, seat))));
  } catch {
    // Someone else took one of the seats in the meantime: undo the booking row.
    await env.DB.prepare('DELETE FROM booking WHERE booking_id = ?').bind(bookingId).run();
    throw new HttpError(409, 'One of the selected seats was just booked by someone else. Please pick again.');
  }

  const row = await env.DB.prepare(`${BOOKING_SELECT} WHERE b.booking_id = ?`).bind(bookingId).first();
  return json(bookingJson(row, await bookingSeats(env, bookingId)), 201);
}

function requireField(form, key) {
  const value = form[key];
  if (value === undefined || value === null || String(value).trim() === '') {
    throw new HttpError(400, `${key} is required.`);
  }
  return String(value).trim();
}

// ---------------------------------------------------------------------------
// Routes
// ---------------------------------------------------------------------------
async function handleApi(request, env, url) {
  const method = request.method.toUpperCase();
  if (method === 'OPTIONS') {
    return new Response(null, {
      status: 204,
      headers: {
        'access-control-allow-origin': '*',
        'access-control-allow-methods': 'GET,POST,DELETE,OPTIONS',
        'access-control-allow-headers': 'Content-Type',
      },
    });
  }

  const parts = url.pathname.replace(/^\/api\/?/, '').split('/').filter(Boolean);
  const query = Object.fromEntries(url.searchParams.entries());
  const form = method === 'POST'
    ? Object.fromEntries(new URLSearchParams(await request.text()).entries())
    : {};

  if (!parts.length) return fail(404, 'Unknown endpoint.');

  await initDb(env);
  await ensureShowtimes(env);

  switch (parts[0]) {
    // ---- health + cities ------------------------------------------------
    case 'health':
      return json({ status: 'ok', database: 'noirbox_db' });

    case 'cities': {
      const rows = await env.DB.prepare('SELECT slug, city_name, state_name FROM city ORDER BY city_id').all();
      return json((rows.results || []).map((c) => ({ id: c.slug, name: c.city_name, state: c.state_name })));
    }

    // ---- movies ---------------------------------------------------------
    case 'movies': {
      if (method !== 'GET') return fail(405, 'Method not allowed.');

      if (parts.length === 1) {
        const rows = query.status
          ? await env.DB.prepare(`${MOVIE_SELECT} WHERE m.movie_status = ? ORDER BY m.popularity DESC`).bind(query.status).all()
          : await env.DB.prepare(`${MOVIE_SELECT} ORDER BY m.popularity DESC`).all();
        let movies = await withDetails(env, rows.results || []);
        if (query.q) {
          const needle = query.q.toLowerCase();
          movies = movies.filter((m) => m.title.toLowerCase().includes(needle));
        }
        return json(movies);
      }

      const slug = decodeURIComponent(parts[1]);

      if (parts.length === 3 && parts[2] === 'reviews') {
        const rows = await env.DB.prepare(
          `${REVIEW_SELECT} WHERE m.slug = ? ORDER BY r.likes DESC, r.review_date DESC`,
        ).bind(slug).all();
        return json(await Promise.all((rows.results || []).map((r) => reviewJson(env, r))));
      }

      if (parts.length === 2) {
        const row = await env.DB.prepare(`${MOVIE_SELECT} WHERE m.slug = ?`).bind(slug).first();
        if (!row) return fail(404, 'Movie not found.');
        const [movie] = await withDetails(env, [row]);
        return json(movie);
      }

      return fail(404, 'Unknown movie endpoint.');
    }

    // ---- shows ----------------------------------------------------------
    case 'shows': {
      if (method !== 'GET') return fail(405, 'Method not allowed.');

      if (parts.length === 1) {
        if (!query.movieId) return fail(400, 'movieId is required.');
        const clauses = ['m.slug = ?'];
        const args = [query.movieId];
        if (query.date) { clauses.push('ms.show_date = ?'); args.push(query.date); }
        else clauses.push("ms.show_date >= date('now')");
        if (query.city) { clauses.push('c.slug = ?'); args.push(query.city); }
        const rows = await env.DB.prepare(
          `${SHOW_SELECT} WHERE ${clauses.join(' AND ')} ORDER BY ms.show_date, ms.show_time`,
        ).bind(...args).all();
        return json((rows.results || []).map(showJson));
      }

      const showId = Number(parts[1]);
      if (!Number.isFinite(showId)) return fail(400, 'A numeric value in the request is invalid.');
      const show = await env.DB.prepare(`${SHOW_SELECT} WHERE ms.show_id = ?`).bind(showId).first();
      if (!show) return fail(404, 'Show not found.');

      if (parts.length === 2) return json(showJson(show));

      if (parts.length === 3 && parts[2] === 'seats') {
        const booked = await env.DB.prepare('SELECT seat_no FROM booking_seat WHERE show_id = ? ORDER BY seat_no')
          .bind(showId).all();
        return json({
          showId: String(showId),
          seatsAvailable: show.seats_available,
          totalSeats: TOTAL_SEATS,
          booked: (booked.results || []).map((r) => r.seat_no),
          price: {
            premium: seatPrice(show.price, 'A1'),
            executive: seatPrice(show.price, 'C1'),
            normal: seatPrice(show.price, 'G1'),
          },
        });
      }

      return fail(404, 'Unknown show endpoint.');
    }

    // ---- bookings -------------------------------------------------------
    case 'bookings': {
      if (method === 'POST' && parts.length === 1) return createBooking(env, form);

      if (method === 'POST' && parts.length === 3 && parts[2] === 'cancel') {
        const bookingId = Number(parts[1]);
        const row = await env.DB.prepare('SELECT booking_status FROM booking WHERE booking_id = ?')
          .bind(bookingId).first();
        if (!row) return fail(400, `Booking ${bookingId} was not found.`);
        if (row.booking_status === 'CANCELLED') return fail(400, `Booking ${bookingId} is already cancelled.`);

        await env.DB.batch([
          env.DB.prepare('DELETE FROM booking_seat WHERE booking_id = ?').bind(bookingId),
          env.DB.prepare("UPDATE booking SET booking_status = 'CANCELLED' WHERE booking_id = ?").bind(bookingId),
        ]);
        return json({ status: 'CANCELLED' });
      }

      if (method === 'GET' && parts.length === 1) {
        if (!query.email) return fail(400, 'email is required.');
        const user = await env.DB.prepare('SELECT * FROM app_user WHERE email = ?').bind(query.email).first();
        if (!user) return json([]);
        const rows = await env.DB.prepare(
          `${BOOKING_SELECT} WHERE b.user_id = ? ORDER BY b.booking_time DESC`,
        ).bind(user.user_id).all();
        const items = [];
        for (const row of rows.results || []) {
          items.push(bookingJson(row, await bookingSeats(env, row.booking_id)));
        }
        return json(items);
      }

      if (method === 'GET' && parts.length === 2) {
        const row = await env.DB.prepare(`${BOOKING_SELECT} WHERE b.booking_id = ?`).bind(Number(parts[1])).first();
        if (!row) return fail(404, 'Booking not found.');
        return json(bookingJson(row, await bookingSeats(env, row.booking_id)));
      }

      return fail(404, 'Unknown booking endpoint.');
    }

    // ---- reviews --------------------------------------------------------
    case 'reviews': {
      if (method === 'POST' && parts.length === 1) {
        const email = requireField(form, 'email');
        const name = form.name || 'Anonymous Cinephile';
        const user = await findOrCreateUser(env, email, name);

        const slug = requireField(form, 'movieId');
        const movie = await env.DB.prepare('SELECT movie_id FROM movie WHERE slug = ?').bind(slug).first();
        if (!movie) return fail(404, 'Movie not found.');

        const rating = Number(requireField(form, 'rating'));
        if (!(rating >= 1 && rating <= 10)) throw new HttpError(400, 'Rating must be between 1 and 10.');
        const verdict = rating >= 8 ? 'must-watch' : rating >= 6 ? 'worth-it' : 'skip';
        const headline = requireField(form, 'headline').trim();
        const content = form.content || '';
        const spoilers = String(form.hasSpoilers || 'false').toLowerCase() === 'true' ? 1 : 0;

        const inserted = await env.DB.prepare(
          `INSERT INTO review (movie_id, user_id, rating, verdict, headline, review_text, has_spoilers)
           VALUES (?,?,?,?,?,?,?)`,
        ).bind(movie.movie_id, user.user_id, rating, verdict, headline, content, spoilers).run();

        const reviewId = inserted.meta.last_row_id;
        const tags = String(form.tags || '').split(',').map((t) => t.trim()).filter(Boolean);
        if (tags.length) {
          await env.DB.batch(tags.map((tag) => env.DB.prepare(
            'INSERT INTO review_tag (review_id, tag) VALUES (?, ?)',
          ).bind(reviewId, tag)));
        }

        const row = await env.DB.prepare(`${REVIEW_SELECT} WHERE r.review_id = ?`).bind(reviewId).first();
        return json(await reviewJson(env, row), 201);
      }

      if (method === 'POST' && parts.length === 3 && parts[2] === 'vote') {
        const row = await env.DB.prepare(
          'UPDATE review SET likes = likes + 1 WHERE review_id = ? RETURNING likes',
        ).bind(Number(parts[1])).first();
        return json({ likes: row ? row.likes : 0 });
      }

      if (method === 'DELETE' && parts.length === 2) {
        if (!query.email) return fail(400, 'email is required.');
        const removed = await env.DB.prepare(
          `DELETE FROM review WHERE review_id = ?
             AND user_id IN (SELECT user_id FROM app_user WHERE email = ?)
           RETURNING review_id`,
        ).bind(Number(parts[1]), query.email).all();
        const deleted = (removed.results || []).length;
        if (!deleted) return fail(403, 'You can only delete your own review.');
        return json({ deleted });
      }

      return fail(404, 'Unknown review endpoint.');
    }

    default:
      return fail(404, 'Unknown endpoint.');
  }
}

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------
export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname.startsWith('/api')) {
      try {
        return await handleApi(request, env, url);
      } catch (problem) {
        if (problem instanceof HttpError) return fail(problem.status, problem.message);
        console.error('NOIRBOX API error:', problem && problem.stack ? problem.stack : problem);
        return fail(500, `Server error: ${problem && problem.message ? problem.message : 'unexpected'}`);
      }
    }

    const asset = await env.ASSETS.fetch(request);
    if (asset.status === 404 && request.method === 'GET') {
      // React Router owns the client-side routes: serve the shell without a
      // redirect so /movies/spiderman survives a refresh or a shared link.
      return env.ASSETS.fetch(new URL('/', url).toString());
    }
    return asset;
  },
};
