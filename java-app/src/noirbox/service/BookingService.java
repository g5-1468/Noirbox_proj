package noirbox.service;

import noirbox.dao.BookingDAO;
import noirbox.dao.ShowDAO;
import noirbox.db.DBConnection;
import noirbox.exception.BookingException;
import noirbox.exception.SeatUnavailableException;
import noirbox.model.Booking;
import noirbox.model.Show;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for booking and cancelling tickets.
 *
 * The booking is done inside ONE JDBC transaction:
 *   1. lock the show row              (SELECT ... FOR UPDATE)
 *   2. read the price of every seat   (MySQL function fn_seat_price)
 *   3. insert the booking row
 *   4. insert one booking_seat row per seat
 *   5. COMMIT  - or ROLLBACK if any step fails
 *
 * If two users pick the same seat, the UNIQUE(show_id, seat_no) constraint
 * rejects the second insert and the whole transaction is rolled back.
 */
public class BookingService {

    public static final double CONVENIENCE_FEE_PER_TICKET = 28.0;

    private final BookingDAO bookingDAO = new BookingDAO();
    private final ShowDAO showDAO = new ShowDAO();

    public Booking bookTicket(int userId, int showId, List<String> seats, String paymentMethod)
            throws BookingException {

        if (seats == null || seats.isEmpty()) {
            throw new BookingException("Please select at least one seat.");
        }

        List<String> bookedSeats = bookingDAO.findBookedSeats(showId);
        List<String> conflict = new ArrayList<>();
        for (String seat : seats) {
            if (bookedSeats.contains(seat.toUpperCase())) {
                conflict.add(seat);
            }
        }
        if (!conflict.isEmpty()) {
            throw new SeatUnavailableException(
                    "These seats are already booked: " + String.join(", ", conflict));
        }

        double subtotal = 0;
        double fee = seats.size() * CONVENIENCE_FEE_PER_TICKET;

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                // 1. lock the show row until the transaction finishes
                try (PreparedStatement lock = connection.prepareStatement(
                        "SELECT seats_available FROM movie_show WHERE show_id = ? FOR UPDATE")) {
                    lock.setInt(1, showId);
                    try (ResultSet resultSet = lock.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new BookingException("Show " + showId + " does not exist.");
                        }
                        int available = resultSet.getInt("seats_available");
                        if (available < seats.size()) {
                            throw new SeatUnavailableException(
                                    "Only " + available + " seats are left for this show.");
                        }
                    }
                }

                // 2. price of every seat comes from the database function
                for (String seat : seats) {
                    subtotal += bookingDAO.findSeatPrice(connection, showId, seat);
                }

                // 3. insert the booking
                int bookingId = bookingDAO.insertBooking(
                        connection, userId, showId, seats.size(), subtotal, fee, paymentMethod);

                // 4. insert the seats (trigger updates seats_available)
                for (String seat : seats) {
                    double price = bookingDAO.findSeatPrice(connection, showId, seat);
                    bookingDAO.insertBookingSeat(connection, bookingId, showId, seat, price);
                }

                // 5. everything worked
                connection.commit();

                return buildBooking(bookingId, userId, showId, seats,
                        subtotal, fee, paymentMethod);

            } catch (SQLIntegrityConstraintViolationException duplicate) {
                connection.rollback();
                throw new SeatUnavailableException(
                        "One of the selected seats was just booked by someone else. Please pick again.",
                        duplicate);
            } catch (SQLException | BookingException problem) {
                connection.rollback();
                if (problem instanceof BookingException bookingProblem) {
                    throw bookingProblem;
                }
                throw new BookingException("Booking failed: " + problem.getMessage(), problem);
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException connectionProblem) {
            throw new BookingException("Could not connect to the database: "
                    + connectionProblem.getMessage(), connectionProblem);
        }
    }

    /** Cancels a booking: status becomes CANCELLED and the seats are released. */
    public void cancelBooking(int bookingId) throws BookingException {
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                String status;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT booking_status FROM booking WHERE booking_id = ? FOR UPDATE")) {
                    select.setInt(1, bookingId);
                    try (ResultSet resultSet = select.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new BookingException("Booking " + bookingId + " was not found.");
                        }
                        status = resultSet.getString("booking_status");
                    }
                }

                if ("CANCELLED".equals(status)) {
                    throw new BookingException("Booking " + bookingId + " is already cancelled.");
                }

                // the AFTER DELETE trigger adds the seats back to seats_available
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM booking_seat WHERE booking_id = ?")) {
                    delete.setInt(1, bookingId);
                    delete.executeUpdate();
                }

                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE booking SET booking_status = 'CANCELLED' WHERE booking_id = ?")) {
                    update.setInt(1, bookingId);
                    update.executeUpdate();
                }

                connection.commit();

            } catch (SQLException problem) {
                connection.rollback();
                throw new BookingException("Cancellation failed: " + problem.getMessage(), problem);
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException connectionProblem) {
            throw new BookingException("Could not connect to the database: "
                    + connectionProblem.getMessage(), connectionProblem);
        }
    }

    public List<String> getBookedSeats(int showId) {
        return bookingDAO.findBookedSeats(showId);
    }

    /** Loads the saved booking from the database so the console shows real data. */
    private Booking buildBooking(int bookingId, int userId, int showId, List<String> seats,
                                 double subtotal, double fee, String paymentMethod)
            throws BookingException {

        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setUserId(userId);
        booking.setShowId(showId);
        booking.setSeatCount(seats.size());
        booking.setSubtotal(subtotal);
        booking.setConvenienceFee(fee);
        booking.setTotalAmount(subtotal + fee);
        booking.setPaymentMethod(paymentMethod);
        booking.setStatus("CONFIRMED");
        booking.setBookingTime(LocalDateTime.now());
        booking.getSeats().addAll(seats);

        Show show = showDAO.findById(showId)
                .orElseThrow(() -> new BookingException("Show details could not be loaded."));
        booking.setMovieTitle(show.getMovieTitle());
        booking.setTheatreName(show.getTheatreName());
        booking.setShowDate(show.getShowDate());
        booking.setShowTime(show.getShowTime());

        return booking;
    }
}
