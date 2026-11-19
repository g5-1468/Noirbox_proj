package noirbox.exception;

/**
 * Thrown when the selected seats cannot be booked because somebody else
 * already booked them (or the show is full). The API answers this with
 * HTTP 409 so the website can ask the user to pick other seats.
 */
public class SeatUnavailableException extends BookingException {

    private static final long serialVersionUID = 1L;

    public SeatUnavailableException(String message) {
        super(message);
    }

    public SeatUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
