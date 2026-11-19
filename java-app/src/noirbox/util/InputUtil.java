package noirbox.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Console input helpers with validation.
 * Every method re-asks until the value is valid, so the program never
 * crashes because of wrong input (exception handling demo).
 */
public class InputUtil {

    private static final Scanner SCANNER = new Scanner(System.in);

    private InputUtil() {
    }

    /** Reads one line; if the input stream is closed the program stops cleanly. */
    private static String nextLine() {
        if (!SCANNER.hasNextLine()) {
            throw new IllegalStateException("No more input available.");
        }
        return SCANNER.nextLine();
    }

    public static String readLine(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("  ! This field cannot be empty.");
        }
    }

    public static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String value = nextLine().trim();
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) {
                    return number;
                }
                System.out.println("  ! Enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  ! '" + value + "' is not a valid number.");
            }
        }
    }

    public static double readAmount(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  ! Please enter a number.");
            }
        }
    }

    public static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt + " (yyyy-mm-dd, press Enter for today): ");
            String value = nextLine().trim();
            if (value.isEmpty()) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("  ! Use the format yyyy-mm-dd, for example 2026-11-18.");
            }
        }
    }

    /** Reads "A1,A2" and returns a clean list of seat numbers. */
    public static List<String> readSeats(String prompt) {
        while (true) {
            System.out.print(prompt + " (example A1,A2): ");
            String value = nextLine().trim().toUpperCase();
            List<String> seats = new ArrayList<>();
            boolean valid = !value.isEmpty();

            for (String part : value.split(",")) {
                String seat = part.trim();
                if (!isValidSeat(seat)) {
                    valid = false;
                    break;
                }
                if (!seats.contains(seat)) {
                    seats.add(seat);
                }
            }
            if (valid && !seats.isEmpty()) {
                return seats;
            }
            System.out.println("  ! Invalid seat. Rows A-B have seats 1-12, rows C-J have 1-14.");
        }
    }

    /** Same layout as the seat map in the UI: A-B = 12 seats, C-J = 14 seats. */
    public static boolean isValidSeat(String seat) {
        if (!seat.matches("[A-J]\\d{1,2}")) {
            return false;
        }
        String row = seat.substring(0, 1);
        int number = Integer.parseInt(seat.substring(1));
        int seatCount = (row.equals("A") || row.equals("B")) ? 12 : 14;
        return number >= 1 && number <= seatCount;
    }

    public static void close() {
        SCANNER.close();
    }
}
