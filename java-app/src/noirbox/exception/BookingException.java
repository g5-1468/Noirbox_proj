package noirbox.exception;

/**
 * Checked exception thrown when a booking cannot be completed
 * (seat already taken, show full, booking not found, ...).
 * It is caught in Main and only the message is printed.
 */
public class BookingException extends Exception {

    private static final long serialVersionUID = 1L;

    public BookingException(String message) {
        super(message);
    }

    public BookingException(String message, Throwable cause) {
        super(message, cause);
    }
}
