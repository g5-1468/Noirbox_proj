package noirbox.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** One confirmed (or cancelled) ticket purchase. */
public class Booking {

    private int bookingId;
    private int userId;
    private String userName;
    private int showId;
    private String movieSlug;
    private String movieTitle;
    private String theatreSlug;
    private String theatreName;
    private LocalDate showDate;
    private LocalTime showTime;
    private int seatCount;
    private double subtotal;
    private double convenienceFee;
    private double totalAmount;
    private String paymentMethod;
    private String status;
    private LocalDateTime bookingTime;
    private final List<String> seats = new ArrayList<>();

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int bookingId) {
        this.bookingId = bookingId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getShowId() {
        return showId;
    }

    public void setShowId(int showId) {
        this.showId = showId;
    }

    public String getMovieSlug() {
        return movieSlug;
    }

    public void setMovieSlug(String movieSlug) {
        this.movieSlug = movieSlug;
    }

    public String getTheatreSlug() {
        return theatreSlug;
    }

    public void setTheatreSlug(String theatreSlug) {
        this.theatreSlug = theatreSlug;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getTheatreName() {
        return theatreName;
    }

    public void setTheatreName(String theatreName) {
        this.theatreName = theatreName;
    }

    public LocalDate getShowDate() {
        return showDate;
    }

    public void setShowDate(LocalDate showDate) {
        this.showDate = showDate;
    }

    public LocalTime getShowTime() {
        return showTime;
    }

    public void setShowTime(LocalTime showTime) {
        this.showTime = showTime;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(int seatCount) {
        this.seatCount = seatCount;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getConvenienceFee() {
        return convenienceFee;
    }

    public void setConvenienceFee(double convenienceFee) {
        this.convenienceFee = convenienceFee;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public void setBookingTime(LocalDateTime bookingTime) {
        this.bookingTime = bookingTime;
    }

    public List<String> getSeats() {
        return seats;
    }

    @Override
    public String toString() {
        return String.format("Booking %-4d | %-32s | %s %s | seats %-14s | Rs.%.0f | %s",
                bookingId, movieTitle, showDate, showTime,
                String.join(",", seats), totalAmount, status);
    }
}
