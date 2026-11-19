-- =====================================================================
-- NOIRBOX - SAMPLE DATA
-- Same cities, theatres and movies that are shown in the React frontend.
-- Run order: 01_schema.sql -> 02_triggers.sql -> 03_sample_data.sql
-- =====================================================================

USE noirbox_db;

-- ---------------------------- CITIES --------------------------------
INSERT INTO city (city_name, state_name, slug) VALUES
('Mumbai',     'Maharashtra', 'mumbai'),
('Delhi NCR',  'Delhi',       'delhi'),
('Bengaluru',  'Karnataka',   'bengaluru'),
('Hyderabad',  'Telangana',   'hyderabad'),
('Pune',       'Maharashtra', 'pune'),
('Chennai',    'Tamil Nadu',  'chennai');

-- --------------------------- THEATRES -------------------------------
INSERT INTO theatre (theatre_name, chain, city_id, locality, amenities, slug) VALUES
('PVR Luxe Palladium',   'PVR',      1, 'Lower Parel',  'Dolby Atmos, Recliner, M-Ticket', 'pvr-palladium'),
('INOX R-City',          'INOX',     1, 'Ghatkopar',    '4K Laser, Food Court, M-Ticket',  'inox-rcity'),
('IMAX Wadala',          'IMAX',     1, 'Wadala',       'IMAX, Parking, M-Ticket',         'imax-wadala'),
('PVR Select Citywalk',  'PVR',      2, 'Saket',        'Dolby Atmos, Recliner, Valet',    'pvr-select-delhi'),
('Cinepolis DLF Avenue', 'Cinepolis',2, 'Saket',        '4DX, M-Ticket, Cafe',             'cinepolis-dlf'),
('PVR Orion Mall',       'PVR',      3, 'Rajajinagar',  'IMAX, Recliner, M-Ticket',        'pvr-orion'),
('INOX Mantri Square',   'INOX',     3, 'Malleswaram',  'Dolby 7.1, Food Court',           'inox-mantri'),
('AMB Cinemas Gachibowli','AMB',     4, 'Gachibowli',   'Gold Class, IMAX, Valet',         'amb-hyderabad'),
('PVR ICON Pavilion',    'PVR',      5, 'Shivajinagar', 'Atmos, Recliner',                 'pvr-icon-pune'),
('SPI Palazzo Nexus',    'SPI',      6, 'Vadapalani',   'Dolby Atmos, M-Ticket',           'spi-chennai');

-- ---------------------------- MOVIES --------------------------------
INSERT INTO movie (slug, title, language, certification, duration_min, release_date,
                   movie_status, rating, popularity, director, tagline, description,
                   poster_url, backdrop_url, trailer_url, featured) VALUES
('spiderman', 'Spider-Man: Brand New Day', 'English', 'UA 13+', 130, '2026-07-31',
 'now-showing', 8.4, 96, 'Destin Daniel Cretton', 'A brand new day.',
 'Peter Parker tries to rebuild a normal life in New York after the multiverse crisis. A new threat rises with the city, and Spider-Man has to choose who he is when the world no longer remembers the hero in the mask.',
 '/posters/brand-new-day.jpg',
 'https://images.unsplash.com/photo-1635805737707-575885ab0820?auto=format&fit=crop&w=1600&q=80',
 'https://www.youtube.com/watch?v=62bIsvRcPv0', TRUE),
('the-odyssey', 'The Odyssey', 'English', 'UA 16+', 173, '2026-07-17',
 'now-showing', 8.1, 91, 'Christopher Nolan', 'Defy the gods.',
 'After the Trojan War, Odysseus, king of Ithaca, begins a long voyage home. Across sea, myth and memory he fights to reunite with Penelope and to outlast the will of the gods.',
 '/posters/odyssey.jpg',
 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1600&q=80',
 'https://www.youtube.com/watch?v=Mzw2ttJD2qQ', FALSE),
('last-monsoon', 'Barbie', 'English', 'PG-13', 114, '2023-07-21',
 'now-showing', 7.9, 88, 'Greta Gerwig', 'She is everything. He is just Ken.',
 'After leaving the perfectly ordered world of Barbie Land for the real world, Barbie and Ken discover that life outside their familiar roles is far more complicated than they expected.',
 'https://media.themoviedb.org/t/p/w500/iuFNMS8U5cb6xfzi51Dbkovj7vM.jpg',
 'https://media.themoviedb.org/t/p/w1280/1esAE8sLJRWWFsLLeh5r3g2WanI.jpg',
 NULL, FALSE),
('midnight-raga', 'Oppenheimer', 'English', 'R', 180, '2023-07-21',
 'now-showing', 8.6, 84, 'Christopher Nolan', 'The world forever changes.',
 'Physicist J. Robert Oppenheimer leads the wartime scientific effort to develop the first atomic bomb, then faces the personal and political consequences of its creation.',
 'https://media.themoviedb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg',
 'https://media.themoviedb.org/t/p/w1280/neeNHeXjMF5fXoCJRsOmkNGC7q.jpg',
 NULL, FALSE),
('saffron-skies', 'The Devil Wears Prada 2', 'English', 'PG-13', NULL, '2026-05-01',
 'now-showing', 7.2, 79, 'David Frankel', 'Andy returns to Runway.',
 'Andy Sachs returns to Runway as editor Miranda Priestly navigates a changing media landscape. Their paths cross again with Emily Charlton, now leading a luxury brand.',
 'https://media.themoviedb.org/t/p/w500/xTI42pmsP5EDnvsNJPEDubwWBQO.jpg',
 'https://media.themoviedb.org/t/p/w1280/dRoFlUbH1l51ACmx5oPCI3jfvZq.jpg',
 NULL, FALSE),
('velvet-ashes', 'Scream 7', 'English', 'R', 114, '2026-02-27',
 'now-showing', 6.8, 82, 'Kevin Williamson', 'Burn it all down.',
 'A new Ghostface killer targets Sidney Prescott in the town where she has built a new life, forcing her to confront the threat and the past she hoped to leave behind.',
 'https://media.themoviedb.org/t/p/w500/jjyuk0edLiW8vOSnlfwWCCLpbh5.jpg',
 'https://media.themoviedb.org/t/p/w1280/3eUyLEF5M0ky3h6KJsWiWzaakB8.jpg',
 NULL, FALSE),
('harbour-lights', 'Jurassic World Rebirth', 'English', 'PG-13', 134, '2025-07-02',
 'now-showing', 7.1, 86, 'Gareth Edwards', 'A new mission among the dinosaurs.',
 'Five years after the last Jurassic World story, a specialist team heads to an isolated island to collect samples from three of the largest remaining dinosaurs.',
 'https://media.themoviedb.org/t/p/w500/1RICxzeoNCAO5NpcRMIgg1XT6fm1.jpg',
 'https://media.themoviedb.org/t/p/w1280/zNriRTr0kWwyaXPzdg1EIxf0BWk.jpg',
 NULL, FALSE),
('clockmakers-daughter', 'Toy Story 5', 'English', 'U', 102, '2026-06-19',
 'now-showing', 8.0, 90, 'Andrew Stanton', 'It is on.',
 'When Bonnie becomes absorbed in a new tablet, Woody, Buzz, Jessie and the rest of the toys have to compete with a new kind of playtime distraction.',
 'https://media.themoviedb.org/t/p/w500/sfQtVlIHljToOwYjhe21KPGzZWK.jpg',
 'https://media.themoviedb.org/t/p/w1280/qjTqY5coNiz6sVtPng40IzltsoN.jpg',
 NULL, TRUE),
('kala-dhoop', 'Inside Out 2', 'English', 'PG', 96, '2024-06-14',
 'now-showing', 8.2, 74, 'Kelsey Mann', 'Make room for new emotions.',
 'As Riley enters her teenage years, her mind''s familiar Emotions face a major change when new arrivals, including Anxiety, take over the control room.',
 'https://media.themoviedb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg',
 'https://media.themoviedb.org/t/p/w1280/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg',
 NULL, FALSE),
('iron-lotus', 'Mortal Kombat II', 'English', 'A', NULL, '2026-05-15',
 'now-showing', 6.5, 80, 'Simon McQuoid', 'The battle for Earthrealm continues.',
 'Johnny Cage joins the champions as they face a deadly tournament and the threat of Shao Kahn''s rule over Earthrealm.',
 'https://media.themoviedb.org/t/p/w500/hwRdDFIhaEmpRgoki805YvyyjZf.jpg',
 'https://media.themoviedb.org/t/p/w1280/4EAAwpylq313qrDqpCxulUrXBNF.jpg',
 NULL, FALSE),
('starfall', 'Dune: Part Three', 'English', 'TBA', 165, '2026-12-18',
 'coming-soon', 0.0, 95, 'Denis Villeneuve', 'The epic conclusion.',
 'Years after taking the throne, Paul Atreides faces the consequences of his rule and a growing conspiracy that threatens the people closest to him.',
 'https://media.themoviedb.org/t/p/w500/q2oFnkbhYsDKsYMMl8PyGOAPfjY.jpg',
 'https://i.ytimg.com/vi/NdvqHc56lE0/hqdefault.jpg',
 'https://www.youtube.com/watch?v=NdvqHc56lE0', FALSE),
('ivory-room', 'Avengers: Doomsday', 'English', 'UA 13+', 150, '2026-12-18',
 'coming-soon', 0.0, 87, 'Anthony Russo, Joe Russo', 'The end begins.',
 'The Avengers face Doctor Doom as realities collide. Old alliances return, new fractures open, and the team has one last window to stop a war that could end every world they have ever saved.',
 '/posters/doomsday.jpg',
 'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1600&q=80',
 'https://www.youtube.com/watch?v=X1aFkAkFASk', FALSE);

-- --------------------------- GENRES ---------------------------------
INSERT INTO movie_genre (movie_id, genre) VALUES
(1,'Action'), (1,'Adventure'),
(2,'Action'), (2,'Adventure'), (2,'Fantasy'),
(3,'Comedy'), (3,'Adventure'), (3,'Fantasy'),
(4,'Drama'), (4,'History'), (4,'Thriller'),
(5,'Comedy'), (5,'Drama'),
(6,'Horror'), (6,'Mystery'), (6,'Thriller'),
(7,'Adventure'), (7,'Sci-Fi'), (7,'Action'), (7,'Thriller'),
(8,'Animation'), (8,'Family'), (8,'Comedy'), (8,'Adventure'),
(9,'Animation'), (9,'Family'), (9,'Comedy'),
(10,'Action'), (10,'Fantasy'), (10,'Adventure'),
(11,'Sci-Fi'), (11,'Adventure'), (11,'Drama'),
(12,'Action'), (12,'Sci-Fi');

-- ---------------------------- CAST ----------------------------------
INSERT INTO movie_cast (movie_id, actor_name, position) VALUES
(1,'Tom Holland',1),(1,'Zendaya',2),(1,'Sadie Sink',3),(1,'Liza Colon-Zayas',4),(1,'Jon Bernthal',5),(1,'Mark Ruffalo',6),
(2,'Matt Damon',1),(2,'Tom Holland',2),(2,'Anne Hathaway',3),(2,'Robert Pattinson',4),(2,'Zendaya',5),(2,'Charlize Theron',6),
(3,'Margot Robbie',1),(3,'Ryan Gosling',2),(3,'America Ferrera',3),(3,'Kate McKinnon',4),
(4,'Cillian Murphy',1),(4,'Emily Blunt',2),(4,'Matt Damon',3),(4,'Robert Downey Jr.',4),
(5,'Anne Hathaway',1),(5,'Meryl Streep',2),(5,'Emily Blunt',3),(5,'Stanley Tucci',4),
(6,'Neve Campbell',1),(6,'Courteney Cox',2),(6,'Isabel May',3),(6,'Jasmin Savoy Brown',4),
(7,'Scarlett Johansson',1),(7,'Mahershala Ali',2),(7,'Jonathan Bailey',3),(7,'Rupert Friend',4),
(8,'Tom Hanks',1),(8,'Tim Allen',2),(8,'Joan Cusack',3),(8,'Greta Lee',4),
(9,'Amy Poehler',1),(9,'Maya Hawke',2),(9,'Kensington Tallman',3),(9,'Ayo Edebiri',4),
(10,'Karl Urban',1),(10,'Adeline Rudolph',2),(10,'Ludi Lin',3),(10,'Jessica McNamee',4),
(11,'Timothee Chalamet',1),(11,'Zendaya',2),(11,'Florence Pugh',3),(11,'Rebecca Ferguson',4),(11,'Jason Momoa',5),
(12,'Robert Downey Jr.',1),(12,'Chris Hemsworth',2),(12,'Chris Evans',3),(12,'Scarlett Johansson',4),(12,'Tom Holland',5),(12,'Benedict Cumberbatch',6);

-- ---------------------------- SHOWS ---------------------------------
-- Generated for today, tomorrow and the day after, so the demo always
-- has "upcoming" shows regardless of the date it is evaluated.
INSERT INTO movie_show (movie_id, theatre_id, show_date, show_time, format, price)
SELECT m.movie_id,
       t.theatre_id,
       DATE_ADD(CURDATE(), INTERVAL d.n DAY),
       s.show_time,
       s.format,
       s.price
FROM movie m
JOIN theatre t ON MOD(t.theatre_id, 3) = MOD(m.movie_id, 3)
CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2) d
CROSS JOIN (
    SELECT CAST('10:15:00' AS TIME) AS show_time, '2D' AS format, 220.00 AS price
    UNION ALL SELECT CAST('16:20:00' AS TIME), '3D', 280.00
    UNION ALL SELECT CAST('19:30:00' AS TIME), '2D', 220.00
) s
WHERE m.movie_status = 'now-showing';

-- A few IMAX / premium shows for the big screens
INSERT INTO movie_show (movie_id, theatre_id, show_date, show_time, format, price) VALUES
(1, 3, DATE_ADD(CURDATE(), INTERVAL 0 DAY), '22:10:00', 'IMAX', 420.00),
(2, 3, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '22:10:00', 'IMAX', 420.00),
(8, 6, DATE_ADD(CURDATE(), INTERVAL 0 DAY), '13:00:00', '2D',   200.00);

-- ---------------------------- USERS ---------------------------------
INSERT INTO app_user (full_name, email, phone) VALUES
('Aarav Sharma',  'aarav.sharma@example.com', '9810012345'),
('Meghna Roy',    'meghna.roy@example.com',   '9820034567'),
('Rohan Deshmukh','rohan.d@example.com',      '9830045678'),
('Zoya Merchant', 'zoya.m@example.com',       '9840056789'),
('Kabir Varma',   'kabir.v@example.com',      '9850067890');

-- --------------------------- BOOKINGS -------------------------------
-- Totals are filled in below from the real seat prices of each show.
-- These inserts also fire the triggers, so seats_available drops from 136.
INSERT INTO booking (user_id, show_id, seat_count, subtotal, convenience_fee,
                     total_amount, payment_method, booking_status) VALUES
(1, 1, 2, 0, 0, 0, 'UPI',               'CONFIRMED'),
(2, 5, 1, 0, 0, 0, 'Credit / Debit Card','CONFIRMED'),
(1, 9, 3, 0, 0, 0, 'Net Banking',       'CONFIRMED');

-- Seat prices come from the show's base price and the seat category
-- (A-B premium x1.55, C-F executive x1.20, G-J normal x1.00).
-- The base price is read into a variable first: the trigger on booking_seat
-- updates movie_show, so that same table cannot be read by the INSERT.
SET @price1 = (SELECT price FROM movie_show WHERE show_id = 1);
SET @price5 = (SELECT price FROM movie_show WHERE show_id = 5);
SET @price9 = (SELECT price FROM movie_show WHERE show_id = 9);

INSERT INTO booking_seat (booking_id, show_id, seat_no, price) VALUES
(1, 1, 'A1', ROUND(@price1 * 1.55)),
(1, 1, 'A2', ROUND(@price1 * 1.55)),
(2, 5, 'C3', ROUND(@price5 * 1.20)),
(3, 9, 'G7', ROUND(@price9 * 1.00)),
(3, 9, 'G8', ROUND(@price9 * 1.00)),
(3, 9, 'G9', ROUND(@price9 * 1.00));

-- Fill the booking totals from its seat rows (subtotal + Rs.28 per ticket).
UPDATE booking b
JOIN (SELECT booking_id, SUM(price) AS seat_total
        FROM booking_seat GROUP BY booking_id) s
  ON s.booking_id = b.booking_id
SET b.subtotal        = s.seat_total,
    b.convenience_fee = b.seat_count * 28,
    b.total_amount    = s.seat_total + b.seat_count * 28;

-- --------------------------- REVIEWS --------------------------------
INSERT INTO review (movie_id, user_id, rating, verdict, headline, review_text,
                    has_spoilers, likes, review_date) VALUES
(1, 1, 9, 'must-watch', 'An unforgiving masterclass that demands an IMAX screen',
 'The sound design and the action set pieces are worth the ticket price alone.', FALSE, 42, '2026-08-02'),
(1, 2, 8, 'must-watch', 'Gripping and visually stunning',
 'The second act slows down a little but the climax more than compensates.', FALSE, 19, '2026-08-05'),
(1, 3, 6, 'worth-it', 'Visually stunning but slow in the middle',
 'If you like slow-burn stories you will enjoy it; otherwise wait for OTT.', FALSE, 8, '2026-08-09'),
(2, 4, 9, 'must-watch', 'Nolan at his most ambitious',
 'A long film that never feels long. Watch it on the biggest screen you can find.', FALSE, 27, '2026-07-20'),
(4, 5, 10, 'must-watch', 'A career defining performance',
 'The courtroom third act is outstanding.', FALSE, 33, '2026-07-25'),
(9, 2, 8, 'must-watch', 'A warm, funny sequel',
 'The new emotions fit perfectly into the story.', FALSE, 12, '2026-06-20');

INSERT INTO review_tag (review_id, tag) VALUES
(1,'Acting'), (1,'Sound Design'), (1,'Cinematography'),
(2,'Visuals'), (2,'Direction'),
(3,'Slow Paced'), (3,'Cinematography'),
(4,'Direction'), (4,'Music & BGM'),
(5,'Acting'), (5,'Screenplay'),
(6,'Family Friendly'), (6,'Music & BGM');
