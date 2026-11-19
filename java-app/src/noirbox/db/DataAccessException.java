package noirbox.db;

/**
 * Unchecked exception used when a database operation fails.
 * The original SQLException is kept as the cause so the message can be
 * printed in the console without crashing the whole application.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
