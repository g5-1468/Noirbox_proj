package noirbox.dao;

import noirbox.db.JdbcHelper;
import noirbox.model.Booking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * SQL for booking and booking_seat.
 * The insert methods take an open Connection because the service class runs
 * them inside one transaction (commit / rollback is controlled there).
 */
public class BookingDAO {

    private static final String BASE_SELECT =
            "SELECT b.booking_id, b.user_id, u.full_name, b.show_id, "
          + "m.slug AS movie_slug, m.title AS movie_title, "
          + "t.slug AS theatre_slug, t.theatre_name, ms.show_date, ms.show_time, "
          + "b.seat_count, b.subtotal, "
          + "b.convenience_fee, b.total_amount, b.payment_method, b.booking_status, "
          + "b.booking_time "
          + "FROM booking b "
          + "JOIN app_user   u  ON u.user_id  = b.user_id "
          + "JOIN movie_show ms ON ms.show_id = b.show_id "
          + "JOIN movie      m  ON m.movie_id = ms.movie_id "
          + "JOIN theatre    t  ON t.theatre_id = ms.theatre_id ";

    public List<Booking> findByUser(int userId) {
        List<Booking> bookings = JdbcHelper.query(
                BASE_SELECT + "WHERE b.user_id = ? ORDER BY b.booking_time DESC",
                this::mapBooking, userId);
        bookings.forEach(this::loadSeats);
        return bookings;
    }

    public List<Booking> findAll() {
        List<Booking> bookings = JdbcHelper.query(
                BASE_SELECT + "ORDER BY b.booking_time DESC", this::mapBooking);
        bookings.forEach(this::loadSeats);
        return bookings;
    }

    public Optional<Booking> findById(int bookingId) {
        List<Booking> bookings = JdbcHelper.query(
                BASE_SELECT + "WHERE b.booking_id = ?", this::mapBooking, bookingId);
        bookings.forEach(this::loadSeats);
        return bookings.stream().findFirst();
    }

    /** Seats that are already taken for a show (booked or cancelled -> released). */
    public List<String> findBookedSeats(int showId) {
        return JdbcHelper.query(
                "SELECT seat_no FROM booking_seat WHERE show_id = ? ORDER BY seat_no",
                resultSet -> resultSet.getString("seat_no"), showId);
    }

    /** Price of one seat, calculated by the MySQL function fn_seat_price. */
    public double findSeatPrice(Connection connection, int showId, String seatNo)
            throws SQLException {
        String sql = "SELECT fn_seat_price(?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, showId);
            statement.setString(2, seatNo);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble(1);
                }
            }
        }
        throw new SQLException("Could not calculate a price for seat " + seatNo);
    }

    public int insertBooking(Connection connection, int userId, int showId, int seatCount,
                             double subtotal, double fee, String method) throws SQLException {
        String sql = "INSERT INTO booking (user_id, show_id, seat_count, subtotal, "
                   + "convenience_fee, total_amount, payment_method) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement statement =
                     connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, userId);
            statement.setInt(2, showId);
            statement.setInt(3, seatCount);
            statement.setDouble(4, subtotal);
            statement.setDouble(5, fee);
            statement.setDouble(6, subtotal + fee);
            statement.setString(7, method);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Booking id was not generated.");
    }

    public void insertBookingSeat(Connection connection, int bookingId, int showId,
                                  String seatNo, double price) throws SQLException {
        String sql = "INSERT INTO booking_seat (booking_id, show_id, seat_no, price) "
                   + "VALUES (?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bookingId);
            statement.setInt(2, showId);
            statement.setString(3, seatNo);
            statement.setDouble(4, price);
            statement.executeUpdate();   // trigger reduces seats_available
        }
    }

    private void loadSeats(Booking booking) {
        List<String> seats = JdbcHelper.query(
                "SELECT seat_no FROM booking_seat WHERE booking_id = ? ORDER BY seat_no",
                resultSet -> resultSet.getString("seat_no"), booking.getBookingId());
        booking.getSeats().addAll(seats);
    }

    private Booking mapBooking(ResultSet resultSet) throws SQLException {
        Booking booking = new Booking();
        booking.setBookingId(resultSet.getInt("booking_id"));
        booking.setUserId(resultSet.getInt("user_id"));
        booking.setUserName(resultSet.getString("full_name"));
        booking.setShowId(resultSet.getInt("show_id"));
        booking.setMovieSlug(resultSet.getString("movie_slug"));
        booking.setMovieTitle(resultSet.getString("movie_title"));
        booking.setTheatreSlug(resultSet.getString("theatre_slug"));
        booking.setTheatreName(resultSet.getString("theatre_name"));
        booking.setShowDate(resultSet.getDate("show_date").toLocalDate());
        booking.setShowTime(LocalTime.parse(resultSet.getString("show_time")));
        booking.setSeatCount(resultSet.getInt("seat_count"));
        booking.setSubtotal(resultSet.getDouble("subtotal"));
        booking.setConvenienceFee(resultSet.getDouble("convenience_fee"));
        booking.setTotalAmount(resultSet.getDouble("total_amount"));
        booking.setPaymentMethod(resultSet.getString("payment_method"));
        booking.setStatus(resultSet.getString("booking_status"));

        Timestamp bookedAt = resultSet.getTimestamp("booking_time");
        if (bookedAt != null) {
            booking.setBookingTime(bookedAt.toLocalDateTime());
        }
        return booking;
    }

}
