-- =====================================================================
-- NOIRBOX - TRIGGERS
-- Run AFTER 01_schema.sql and BEFORE 03_sample_data.sql, so that the
-- sample bookings also update seat availability and audit_log.
-- =====================================================================

USE noirbox_db;
DELIMITER $$

-- ---------------------------------------------------------------------
-- Trigger 1: when a seat is booked, reduce seats_available for that show
--            and write an entry in audit_log.
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_booking_seat_after_insert
AFTER INSERT ON booking_seat
FOR EACH ROW
BEGIN
    UPDATE movie_show
       SET seats_available = seats_available - 1
     WHERE show_id = NEW.show_id;

    INSERT INTO audit_log (table_name, action_name, detail)
    VALUES ('booking_seat', 'SEAT_BOOKED',
            CONCAT('Booking ', NEW.booking_id, ' booked seat ', NEW.seat_no));
END$$

-- ---------------------------------------------------------------------
-- Trigger 2: when a cancellation deletes the seat rows, give the seats
--            back to the show. This keeps availability always correct.
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_booking_seat_after_delete
AFTER DELETE ON booking_seat
FOR EACH ROW
BEGIN
    UPDATE movie_show
       SET seats_available = seats_available + 1
     WHERE show_id = OLD.show_id;

    INSERT INTO audit_log (table_name, action_name, detail)
    VALUES ('booking_seat', 'SEAT_RELEASED',
            CONCAT('Booking ', OLD.booking_id, ' released seat ', OLD.seat_no));
END$$

-- ---------------------------------------------------------------------
-- Trigger 3: validate a review before it is stored.
--            (rating range + clean headline text)
-- ---------------------------------------------------------------------
CREATE TRIGGER trg_review_before_insert
BEFORE INSERT ON review
FOR EACH ROW
BEGIN
    IF NEW.rating < 1 OR NEW.rating > 10 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Rating must be between 1 and 10.';
    END IF;

    SET NEW.headline = TRIM(NEW.headline);

    IF NEW.rating >= 8 THEN
        SET NEW.verdict = 'must-watch';
    ELSEIF NEW.rating >= 6 THEN
        SET NEW.verdict = 'worth-it';
    ELSE
        SET NEW.verdict = 'skip';
    END IF;
END$$

DELIMITER ;
