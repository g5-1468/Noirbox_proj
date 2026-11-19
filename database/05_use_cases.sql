-- =====================================================================
-- NOIRBOX - USE CASE QUERIES
-- Each block is one use case of the project, written as a normal report
-- item: (a) problem statement, (b) SQL, (c) expected output.
-- Covers joins, aggregates, sub-queries, set operations, views, DCL,
-- transactions and locking.
-- =====================================================================

USE noirbox_db;

-- =====================================================================
-- UC1. Browse all movies that are currently showing
-- Problem : a visitor opens the app and wants the "Now Showing" list.
-- =====================================================================
SELECT movie_id, title, certification, duration_min, rating
FROM movie
WHERE movie_status = 'now-showing'
ORDER BY popularity DESC;

-- =====================================================================
-- UC2. Show every screening of one movie in a city on a date
-- Problem : after picking a movie the user needs theatre, time and price.
-- Concepts : INNER JOIN of four tables, WHERE, ORDER BY
-- =====================================================================
SELECT ms.show_id, m.title, t.theatre_name, c.city_name,
       ms.show_date, ms.show_time, ms.format, ms.price, ms.seats_available
FROM movie_show ms
JOIN movie   m ON m.movie_id   = ms.movie_id
JOIN theatre t ON t.theatre_id = ms.theatre_id
JOIN city    c ON c.city_id    = t.city_id
WHERE m.title = 'Spider-Man: Brand New Day'
  AND c.city_name = 'Mumbai'
  AND ms.show_date = CURDATE()
ORDER BY ms.show_time;

-- =====================================================================
-- UC3. Seats already booked for a show, plus seat price by category
-- Problem : the seat map must grey out taken seats and show the price.
-- Concepts : LEFT JOIN, NOT IN, stored function
-- =====================================================================
SELECT s.seat_no,
       fn_seat_category(s.seat_no) AS category,
       fn_seat_price(1, s.seat_no) AS price,
       CASE WHEN bs.seat_no IS NULL THEN 'AVAILABLE' ELSE 'BOOKED' END AS status
FROM (
    SELECT CONCAT(r.row_letter, n.num) AS seat_no
    FROM (SELECT 'A' AS row_letter UNION ALL SELECT 'B' UNION ALL SELECT 'C'
          UNION ALL SELECT 'D' UNION ALL SELECT 'E' UNION ALL SELECT 'F'
          UNION ALL SELECT 'G' UNION ALL SELECT 'H' UNION ALL SELECT 'I'
          UNION ALL SELECT 'J') r
    JOIN (SELECT 1 AS num UNION ALL SELECT 2 UNION ALL SELECT 3
          UNION ALL SELECT 4 UNION ALL SELECT 5) n
) s
LEFT JOIN booking_seat bs
       ON bs.show_id = 1 AND bs.seat_no = s.seat_no
ORDER BY s.seat_no;

-- =====================================================================
-- UC4. Book a ticket  (transaction + stored procedure)
-- Problem : store the booking, its seats, and keep seats_available correct
--           so that two users can never book the same seat.
-- Concepts : START TRANSACTION, SELECT ... FOR UPDATE, WHILE loop,
--            EXCEPTION HANDLER, ROLLBACK, triggers
-- =====================================================================
CALL sp_book_ticket(3, 22, 'C1,C2', 'UPI', @new_booking, @new_total);
SELECT @new_booking AS booking_id, @new_total AS total_amount;

-- proof that the same seat cannot be booked twice (rolls back, error 1062)
-- CALL sp_book_ticket(4, 22, 'C1', 'UPI', @b2, @t2);

-- =====================================================================
-- UC5. Cancel a booking and release the seats
-- =====================================================================
CALL sp_cancel_booking(@new_booking, @message);
SELECT @message AS result;

-- =====================================================================
-- UC6. Audience verdict : average rating and recommendation percentage
-- Problem : the movie details page shows an audience verdict card.
-- Concepts : LEFT JOIN, COUNT, AVG, CASE, GROUP BY, HAVING
-- =====================================================================
SELECT m.title,
       COUNT(r.review_id)                          AS review_count,
       ROUND(AVG(r.rating), 1)                     AS avg_rating,
       fn_recommend_percent(m.movie_id)            AS recommend_percent,
       SUM(CASE WHEN r.rating BETWEEN 9 AND 10 THEN 1 ELSE 0 END) AS nine_or_ten
FROM movie m
LEFT JOIN review r ON r.movie_id = m.movie_id
GROUP BY m.movie_id, m.title
HAVING COUNT(r.review_id) > 0
ORDER BY avg_rating DESC;

-- =====================================================================
-- UC7. Revenue and booking count per theatre
-- Problem : the admin needs to know which theatre sells the most.
-- Concepts : multi-table JOIN, COUNT, SUM, GROUP BY, HAVING
-- =====================================================================
SELECT t.theatre_name, c.city_name,
       COUNT(b.booking_id)        AS total_bookings,
       SUM(b.seat_count)          AS tickets_sold,
       IFNULL(SUM(b.total_amount), 0) AS revenue
FROM theatre t
JOIN city c             ON c.city_id = t.city_id
LEFT JOIN movie_show ms ON ms.theatre_id = t.theatre_id
LEFT JOIN booking b     ON b.show_id = ms.show_id
                       AND b.booking_status = 'CONFIRMED'
GROUP BY t.theatre_id, t.theatre_name, c.city_name
HAVING revenue > 0
ORDER BY revenue DESC;

-- =====================================================================
-- UC8. Movies rated above the overall average  (nested sub-query)
-- =====================================================================
SELECT title, rating
FROM movie
WHERE rating > (SELECT AVG(rating) FROM movie WHERE rating > 0)
ORDER BY rating DESC;

-- =====================================================================
-- UC9. Customers who booked or reviewed  (UNION - removes duplicates)
-- =====================================================================
SELECT u.full_name, u.email, 'BOOKED' AS activity
FROM app_user u
WHERE u.user_id IN (SELECT user_id FROM booking)
UNION
SELECT u.full_name, u.email, 'REVIEWED'
FROM app_user u
WHERE u.user_id IN (SELECT user_id FROM review)
ORDER BY full_name;

-- =====================================================================
-- UC10. Shows with no booking yet  (NOT EXISTS - correlated sub-query)
-- =====================================================================
SELECT ms.show_id, m.title, ms.show_date, ms.show_time
FROM movie_show ms
JOIN movie m ON m.movie_id = ms.movie_id
WHERE NOT EXISTS (SELECT 1 FROM booking b WHERE b.show_id = ms.show_id)
ORDER BY ms.show_date, ms.show_time
LIMIT 10;

-- =====================================================================
-- UC11. Views created in the schema
-- =====================================================================
SELECT * FROM v_show_details  WHERE city_name = 'Mumbai' LIMIT 10;
SELECT * FROM v_movie_rating  ORDER BY avg_rating DESC;
SELECT * FROM v_booking_summary ORDER BY booking_id;

-- =====================================================================
-- UC12. A booking with all its seats  (join + aggregate)
-- =====================================================================
SELECT b.booking_id, u.full_name, m.title,
       GROUP_CONCAT(bs.seat_no ORDER BY bs.seat_no) AS seats,
       COUNT(bs.seat_no) AS seat_count,
       b.total_amount, b.booking_status
FROM booking b
JOIN app_user    u  ON u.user_id  = b.user_id
JOIN movie_show  ms ON ms.show_id = b.show_id
JOIN movie       m  ON m.movie_id = ms.movie_id
JOIN booking_seat bs ON bs.booking_id = b.booking_id
GROUP BY b.booking_id, u.full_name, m.title, b.total_amount, b.booking_status;

-- =====================================================================
-- UC13. Transaction demo without a procedure (manual COMMIT / ROLLBACK)
-- Problem : two bookings for the same seat. The UNIQUE constraint makes
--           the second INSERT fail, and we roll back the whole booking.
-- =====================================================================
START TRANSACTION;
INSERT INTO booking (user_id, show_id, seat_count, subtotal,
                     convenience_fee, total_amount, payment_method)
VALUES (5, 10, 1, 0, 0, 0, 'UPI');
SET @demo_booking = LAST_INSERT_ID();
-- read the base price first, because the trigger updates movie_show
SET @demo_price = (SELECT price FROM movie_show WHERE show_id = 10);
INSERT INTO booking_seat (booking_id, show_id, seat_no, price)
VALUES (@demo_booking, 10, 'A1', ROUND(@demo_price * 1.55));
-- try the same seat again in the same show -> duplicate key error
-- INSERT INTO booking_seat (booking_id, show_id, seat_no, price)
-- VALUES (@demo_booking, 10, 'A1', ROUND(@demo_price * 1.55));
ROLLBACK;    -- nothing from the demo is saved

-- =====================================================================
-- UC14. Cursor reports
-- =====================================================================
CALL sp_theatre_report('Mumbai');
CALL sp_movie_show_report(1);

-- =====================================================================
-- UC15. Security : create a limited application user (run as root)
-- =====================================================================
-- CREATE USER 'noirbox_app'@'localhost' IDENTIFIED BY 'Noirbox@123';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON noirbox_db.* TO 'noirbox_app'@'localhost';
-- GRANT EXECUTE ON noirbox_db.* TO 'noirbox_app'@'localhost';
-- REVOKE DELETE ON noirbox_db.* FROM 'noirbox_app'@'localhost';
-- SHOW GRANTS FOR 'noirbox_app'@'localhost';
-- FLUSH PRIVILEGES;

-- =====================================================================
-- UC16. Trigger evidence : seat availability and audit trail
-- =====================================================================
SELECT show_id, seats_available FROM movie_show WHERE show_id IN (1, 22);
SELECT * FROM audit_log ORDER BY log_id DESC LIMIT 10;

-- =====================================================================
-- UC17. Index check : the query uses idx_show_movie_date
-- =====================================================================
EXPLAIN SELECT * FROM movie_show
WHERE movie_id = 1 AND show_date = CURDATE();
