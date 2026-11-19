-- Release every reserved seat in noirbox_db by deleting all booking_seat rows.
-- trg_booking_seat_after_delete fires for each deleted row and restores
-- movie_show.seats_available, so every show goes back to 136 available.
-- The booking header rows stay (status is still whatever it was) — the seats
-- themselves are gone, which is what "release all seats" means.
USE noirbox_db;

-- Run this to release all seats in one statement:
DELETE FROM booking_seat;

-- Optional: also mark the bookings cancelled at the same time so they are no
-- longer "active". Do this BEFORE deleting the seats if you want a clean
-- cancelled+empty state in one go:
-- UPDATE booking SET booking_status = 'CANCELLED'
-- WHERE  booking_status <> 'CANCELLED';

-- Verify the release worked:
SELECT COUNT(*) AS seat_rows_remaining FROM booking_seat;

SELECT show_id,
       seats_available,
       (136 - seats_available) AS seats_booked_now
FROM   movie_show
WHERE  seats_available < 136
ORDER BY show_id;

-- After the release, every show should be back to 136 available and the
-- seat_rows_remaining should be 0.
