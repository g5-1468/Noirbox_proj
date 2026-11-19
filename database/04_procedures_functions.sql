-- =====================================================================
-- NOIRBOX - STORED PROGRAMS
-- Functions, procedures, cursors, control structures and exception
-- handlers used by the Java application and demonstrated in the viva.
-- Run after 01_schema.sql (data not required).
-- =====================================================================

USE noirbox_db;
DELIMITER $$

-- ---------------------------------------------------------------------
-- FUNCTION 1 : seat category from the row letter
--   A-B -> Premium, C-F -> Executive, G-J -> Normal
--   (same rule the React seat map uses)
-- ---------------------------------------------------------------------
CREATE FUNCTION fn_seat_category(p_seat VARCHAR(5))
RETURNS VARCHAR(15)
DETERMINISTIC
BEGIN
    DECLARE v_row CHAR(1);
    SET v_row = LEFT(UPPER(p_seat), 1);

    IF v_row IN ('A', 'B') THEN
        RETURN 'Premium';
    ELSEIF v_row IN ('C', 'D', 'E', 'F') THEN
        RETURN 'Executive';
    ELSEIF v_row IN ('G', 'H', 'I', 'J') THEN
        RETURN 'Normal';
    ELSE
        RETURN 'Invalid';
    END IF;
END$$

-- ---------------------------------------------------------------------
-- FUNCTION 2 : price of one seat for a given show
--   base price * category multiplier, rounded to the nearest rupee
-- ---------------------------------------------------------------------
CREATE FUNCTION fn_seat_price(p_show_id INT, p_seat VARCHAR(5))
RETURNS DECIMAL(8,2)
READS SQL DATA
BEGIN
    DECLARE v_base   DECIMAL(8,2);
    DECLARE v_mult   DECIMAL(4,2);

    SELECT price INTO v_base FROM movie_show WHERE show_id = p_show_id;
    SET v_base = IFNULL(v_base, 0);

    SET v_mult = CASE fn_seat_category(p_seat)
                     WHEN 'Premium'   THEN 1.55
                     WHEN 'Executive' THEN 1.20
                     ELSE 1.00
                 END;

    RETURN ROUND(v_base * v_mult);
END$$

-- ---------------------------------------------------------------------
-- FUNCTION 3 : recommendation percentage of a movie
--   (reviews with rating >= 7  /  total reviews * 100)
-- ---------------------------------------------------------------------
CREATE FUNCTION fn_recommend_percent(p_movie_id INT)
RETURNS INT
READS SQL DATA
BEGIN
    DECLARE v_total INT DEFAULT 0;
    DECLARE v_good  INT DEFAULT 0;

    SELECT COUNT(*), SUM(CASE WHEN rating >= 7 THEN 1 ELSE 0 END)
      INTO v_total, v_good
      FROM review
     WHERE movie_id = p_movie_id;

    IF v_total = 0 THEN
        RETURN 0;
    END IF;

    RETURN ROUND(v_good * 100 / v_total);
END$$

-- ---------------------------------------------------------------------
-- FUNCTION 4 : loyalty points = 1 point per 100 rupees spent
-- ---------------------------------------------------------------------
CREATE FUNCTION fn_loyalty_points(p_user_id INT)
RETURNS INT
READS SQL DATA
BEGIN
    DECLARE v_spent DECIMAL(12,2);

    SELECT IFNULL(SUM(total_amount), 0)
      INTO v_spent
      FROM booking
     WHERE user_id = p_user_id
       AND booking_status = 'CONFIRMED';

    RETURN FLOOR(v_spent / 100);
END$$

-- ---------------------------------------------------------------------
-- PROCEDURE 1 : book a ticket using a comma separated seat list
--   Example: CALL sp_book_ticket(1, 1, 'A1,A2', 'UPI', @id, @total);
--   Demonstrates : transaction, SELECT ... FOR UPDATE row lock,
--                  WHILE loop, exception handler, SIGNAL, rollback.
-- ---------------------------------------------------------------------
CREATE PROCEDURE sp_book_ticket(
    IN  p_user_id  INT,
    IN  p_show_id  INT,
    IN  p_seats    VARCHAR(200),
    IN  p_method   VARCHAR(30),
    OUT p_booking_id INT,
    OUT p_total      DECIMAL(10,2)
)
BEGIN
    DECLARE v_count    INT DEFAULT 0;
    DECLARE v_index    INT DEFAULT 1;
    DECLARE v_seat     VARCHAR(5);
    DECLARE v_price    DECIMAL(8,2);
    DECLARE v_subtotal DECIMAL(10,2) DEFAULT 0;
    DECLARE v_fee      DECIMAL(10,2) DEFAULT 0;
    DECLARE v_available INT DEFAULT 0;
    DECLARE v_remaining VARCHAR(200);

    -- any SQL error -> undo the whole booking
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SET p_seats = UPPER(REPLACE(p_seats, ' ', ''));
    SET v_count = LENGTH(p_seats) - LENGTH(REPLACE(p_seats, ',', '')) + 1;
    SET v_remaining = p_seats;

    START TRANSACTION;

    -- lock the show row so two users cannot take the last seats together
    SELECT seats_available INTO v_available
      FROM movie_show
     WHERE show_id = p_show_id
       FOR UPDATE;

    IF v_available < v_count THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Not enough seats available for this show.';
    END IF;

    INSERT INTO booking (user_id, show_id, seat_count, subtotal,
                         convenience_fee, total_amount, payment_method)
    VALUES (p_user_id, p_show_id, v_count, 0, 0, 0, p_method);

    SET p_booking_id = LAST_INSERT_ID();

    WHILE v_index <= v_count DO
        SET v_seat  = SUBSTRING_INDEX(v_remaining, ',', 1);
        SET v_price = fn_seat_price(p_show_id, v_seat);

        INSERT INTO booking_seat (booking_id, show_id, seat_no, price)
        VALUES (p_booking_id, p_show_id, v_seat, v_price);

        SET v_subtotal  = v_subtotal + v_price;
        SET v_remaining = SUBSTRING(v_remaining, LENGTH(v_seat) + 2);
        SET v_index     = v_index + 1;
    END WHILE;

    SET v_fee   = v_count * 28;
    SET p_total = v_subtotal + v_fee;

    UPDATE booking
       SET subtotal = v_subtotal,
           convenience_fee = v_fee,
           total_amount = p_total
     WHERE booking_id = p_booking_id;

    COMMIT;
END$$

-- ---------------------------------------------------------------------
-- PROCEDURE 2 : cancel a booking and release its seats
--   The AFTER DELETE trigger puts the seats back into seats_available.
-- ---------------------------------------------------------------------
CREATE PROCEDURE sp_cancel_booking(
    IN  p_booking_id INT,
    OUT p_message    VARCHAR(100)
)
BEGIN
    DECLARE v_status VARCHAR(15) DEFAULT NULL;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_message = 'Cancellation failed: database error.';
    END;

    START TRANSACTION;

    SELECT booking_status INTO v_status
      FROM booking
     WHERE booking_id = p_booking_id
       FOR UPDATE;

    IF v_status IS NULL THEN
        SET p_message = 'Booking not found.';
        ROLLBACK;
    ELSEIF v_status = 'CANCELLED' THEN
        SET p_message = 'Booking is already cancelled.';
        ROLLBACK;
    ELSE
        DELETE FROM booking_seat WHERE booking_id = p_booking_id;
        UPDATE booking
           SET booking_status = 'CANCELLED'
         WHERE booking_id = p_booking_id;
        COMMIT;
        SET p_message = 'Booking cancelled and seats released.';
    END IF;
END$$

-- ---------------------------------------------------------------------
-- PROCEDURE 3 : cursor report - revenue of every theatre in a city
--   CALL sp_theatre_report('Mumbai');
--   Demonstrates : CURSOR, OPEN / FETCH / CLOSE, CONTINUE HANDLER
-- ---------------------------------------------------------------------
CREATE PROCEDURE sp_theatre_report(IN p_city VARCHAR(50))
BEGIN
    DECLARE v_done     INT DEFAULT 0;
    DECLARE v_theatre  VARCHAR(100);
    DECLARE v_bookings INT;
    DECLARE v_revenue  DECIMAL(12,2);

    DECLARE cur CURSOR FOR
        SELECT t.theatre_name,
               COUNT(b.booking_id),
               IFNULL(SUM(b.total_amount), 0)
          FROM theatre t
          JOIN city c        ON c.city_id  = t.city_id
          LEFT JOIN movie_show ms ON ms.theatre_id = t.theatre_id
          LEFT JOIN booking b     ON b.show_id = ms.show_id
                                 AND b.booking_status = 'CONFIRMED'
         WHERE c.city_name = p_city
         GROUP BY t.theatre_id, t.theatre_name
         ORDER BY t.theatre_name;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    SELECT CONCAT('--- Theatre report for ', p_city, ' ---') AS report;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO v_theatre, v_bookings, v_revenue;
        IF v_done = 1 THEN
            LEAVE read_loop;
        END IF;

        SELECT CONCAT(v_theatre, ' | bookings: ', v_bookings,
                      ' | revenue: Rs.', v_revenue) AS line_item;
    END LOOP;

    CLOSE cur;
END$$

-- ---------------------------------------------------------------------
-- PROCEDURE 4 : cursor report - all shows of one movie with seats sold
--   CALL sp_movie_show_report(1);
-- ---------------------------------------------------------------------
CREATE PROCEDURE sp_movie_show_report(IN p_movie_id INT)
BEGIN
    DECLARE v_done     INT DEFAULT 0;
    DECLARE v_date     DATE;
    DECLARE v_time     TIME;
    DECLARE v_format   VARCHAR(10);
    DECLARE v_theatre  VARCHAR(100);
    DECLARE v_avail    INT;
    DECLARE v_sold     INT;

    DECLARE cur CURSOR FOR
        SELECT ms.show_date, ms.show_time, ms.format, t.theatre_name,
               ms.seats_available,
               (SELECT COUNT(*) FROM booking b
                 WHERE b.show_id = ms.show_id
                   AND b.booking_status = 'CONFIRMED') AS sold
          FROM movie_show ms
          JOIN theatre t ON t.theatre_id = ms.theatre_id
         WHERE ms.movie_id = p_movie_id
         ORDER BY ms.show_date, ms.show_time;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    SELECT CONCAT('--- Show report for movie id ', p_movie_id, ' ---') AS report;

    OPEN cur;

    show_loop: LOOP
        FETCH cur INTO v_date, v_time, v_format, v_theatre, v_avail, v_sold;
        IF v_done = 1 THEN
            LEAVE show_loop;
        END IF;

        SELECT CONCAT(v_date, ' ', v_time, ' | ', v_format, ' | ', v_theatre,
                      ' | booked tickets: ', v_sold,
                      ' | seats left: ', v_avail) AS line_item;
    END LOOP;

    CLOSE cur;
END$$

DELIMITER ;

-- ---------------------------------------------------------------------
-- Quick check after creating everything:
--   SHOW FUNCTION STATUS WHERE Db = 'noirbox_db';
--   SHOW PROCEDURE STATUS WHERE Db = 'noirbox_db';
-- ---------------------------------------------------------------------
