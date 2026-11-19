package noirbox;

import noirbox.concurrent.SeatBookingSimulator;
import noirbox.dao.BookingDAO;
import noirbox.dao.MovieDAO;
import noirbox.dao.ReviewDAO;
import noirbox.dao.ShowDAO;
import noirbox.db.DBConnection;
import noirbox.db.DataAccessException;
import noirbox.exception.BookingException;
import noirbox.model.Booking;
import noirbox.model.Movie;
import noirbox.model.Review;
import noirbox.model.Show;
import noirbox.model.User;
import noirbox.service.BookingService;
import noirbox.service.ReportService;
import noirbox.service.StoredProcedureService;
import noirbox.util.InputUtil;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * NOIRBOX - Online Movie Ticket Booking System
 * Console application that uses the same database as the React frontend.
 */
public class Main {

    private static final MovieDAO MOVIE_DAO = new MovieDAO();
    private static final ShowDAO SHOW_DAO = new ShowDAO();
    private static final BookingDAO BOOKING_DAO = new BookingDAO();
    private static final ReviewDAO REVIEW_DAO = new ReviewDAO();
    private static final BookingService BOOKING_SERVICE = new BookingService();
    private static final ReportService REPORT_SERVICE = new ReportService();
    private static final StoredProcedureService PROCEDURE_SERVICE = new StoredProcedureService();

    private static User loggedInUser;

    public static void main(String[] args) {
        printBanner();

        if (!databaseIsReachable()) {
            return;
        }

        try {
            loggedInUser = login();
        } catch (IllegalStateException noInput) {
            System.out.println("\nNo input available. Exiting.");
            InputUtil.close();
            return;
        }
        System.out.println("\nWelcome, " + loggedInUser.getFullName() + "!");

        boolean running = true;
        while (running) {
            printMenu();
            int choice;
            try {
                choice = InputUtil.readInt("Choose an option: ", 0, 10);
            } catch (IllegalStateException noInput) {
                System.out.println("\nEnd of input. Bye!");
                break;
            }
            System.out.println();

            try {
                switch (choice) {
                    case 1 -> browseMovies();
                    case 2 -> viewShows();
                    case 3 -> showSeatMap();
                    case 4 -> bookTicket();
                    case 5 -> myBookings();
                    case 6 -> cancelBooking();
                    case 7 -> writeReview();
                    case 8 -> reports();
                    case 9 -> SeatBookingSimulator.run();
                    case 10 -> storedProcedureReports();
                    case 0 -> running = false;
                    default -> System.out.println("Invalid option.");
                }
            } catch (BookingException bookingProblem) {
                System.out.println("\n[Booking error] " + bookingProblem.getMessage());
            } catch (DataAccessException databaseProblem) {
                System.out.println("\n[Database error] " + databaseProblem.getMessage());
            } catch (SQLException sqlProblem) {
                System.out.println("\n[SQL error] " + sqlProblem.getMessage());
            } catch (Exception unexpected) {
                System.out.println("\n[Unexpected error] " + unexpected.getMessage());
            }
        }

        System.out.println("\nThank you for using NOIRBOX. Goodbye!");
        InputUtil.close();
    }

    // ------------------------------------------------------------------
    // Startup
    // ------------------------------------------------------------------
    private static void printBanner() {
        System.out.println("=====================================================");
        System.out.println("   NOIRBOX - Online Movie Ticket Booking System");
        System.out.println("   Java + JDBC + MySQL project (Semester III)");
        System.out.println("=====================================================");
    }

    private static boolean databaseIsReachable() {
        try (Connection connection = DBConnection.getConnection()) {
            System.out.println("Connected to database: " + connection.getCatalog());
            return true;
        } catch (SQLException e) {
            System.out.println("\nCould not connect to MySQL.");
            System.out.println("Reason: " + e.getMessage());
            System.out.println("\nChecklist:");
            System.out.println("  1. MySQL server is running.");
            System.out.println("  2. You ran database/01_schema.sql ... 04_procedures_functions.sql.");
            System.out.println("  3. config/db.properties has the correct url, user and password.");
            System.out.println("  4. mysql-connector-j-8.x.jar is inside the lib folder.");
            return false;
        }
    }

    private static User login() {
        System.out.println("\n--- Login (email only, like the demo login in the UI) ---");
        while (true) {
            String email = InputUtil.readLine("Email: ");
            if (!email.contains("@")) {
                System.out.println("  ! Please enter a valid email address.");
                continue;
            }

            Optional<User> existing = REVIEW_DAO.findByEmail(email);
            if (existing.isPresent()) {
                return existing.get();
            }

            String name = InputUtil.readLine("New user - enter your name: ");
            return REVIEW_DAO.findOrCreateByEmail(email, name);
        }
    }

    private static void printMenu() {
        System.out.println("\n--------------------- MENU ---------------------");
        System.out.println(" 1. Browse movies");
        System.out.println(" 2. View shows of a movie");
        System.out.println(" 3. Seat map and prices");
        System.out.println(" 4. Book a ticket");
        System.out.println(" 5. My bookings");
        System.out.println(" 6. Cancel a booking");
        System.out.println(" 7. Write a review");
        System.out.println(" 8. Reports and CSV export");
        System.out.println(" 9. Concurrency demo (threads)");
        System.out.println("10. Stored procedure reports (DBMS)");
        System.out.println(" 0. Exit");
        System.out.println("-----------------------------------------------");
    }

    // ------------------------------------------------------------------
    // 1. Browse movies
    // ------------------------------------------------------------------
    private static void browseMovies() {
        System.out.println("1. All movies   2. Now showing   3. Coming soon   4. Search by title");
        int option = InputUtil.readInt("Choice: ", 1, 4);

        List<Movie> movies = switch (option) {
            case 2 -> MOVIE_DAO.findByStatus("now-showing");
            case 3 -> MOVIE_DAO.findByStatus("coming-soon");
            case 4 -> MOVIE_DAO.searchByTitle(InputUtil.readLine("Title contains: "));
            default -> MOVIE_DAO.findAll();
        };

        if (movies.isEmpty()) {
            System.out.println("No movie found.");
            return;
        }
        movies.forEach(movie -> System.out.println("  " + movie));
    }

    // ------------------------------------------------------------------
    // 2. View shows
    // ------------------------------------------------------------------
    private static void viewShows() {
        int movieId = InputUtil.readInt("Movie id: ", 1, 999);
        LocalDate date = InputUtil.readDate("Show date");

        List<Show> shows = SHOW_DAO.findByMovieAndDate(movieId, date);
        if (shows.isEmpty()) {
            System.out.println("No shows for this movie on " + date + ".");
            return;
        }
        shows.forEach(show -> System.out.println("  " + show));
    }

    // ------------------------------------------------------------------
    // 3. Seat map
    // ------------------------------------------------------------------
    private static void showSeatMap() {
        int showId = InputUtil.readInt("Show id: ", 1, 999999);

        Optional<Show> found = SHOW_DAO.findById(showId);
        if (found.isEmpty()) {
            System.out.println("Show not found.");
            return;
        }
        Show show = found.get();
        List<String> booked = BOOKING_SERVICE.getBookedSeats(showId);

        System.out.println("\n" + show);
        System.out.println("Booked seats : " + (booked.isEmpty() ? "none" : booked));
        System.out.println("Seats left   : " + show.getSeatsAvailable() + " of 136");

        String[] rows = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
        System.out.println("\nSeat map (X = booked):");
        for (String row : rows) {
            int seatCount = (row.equals("A") || row.equals("B")) ? 12 : 14;
            StringBuilder line = new StringBuilder("  " + row + " ");
            for (int number = 1; number <= seatCount; number++) {
                String seat = row + number;
                line.append(booked.contains(seat) ? " X " : " . ");
            }
            System.out.println(line);
        }

        System.out.println("\nPrice table for this show:");
        System.out.printf("  Premium   (rows A-B) : Rs.%.0f%n", seatPrice(show.getPrice(), "A"));
        System.out.printf("  Executive (rows C-F) : Rs.%.0f%n", seatPrice(show.getPrice(), "C"));
        System.out.printf("  Normal    (rows G-J) : Rs.%.0f%n", seatPrice(show.getPrice(), "G"));
        System.out.println("  Convenience fee      : Rs.28 per ticket");
    }

    /** Same rule as fn_seat_category + fn_seat_price in MySQL. */
    private static double seatPrice(double basePrice, String seat) {
        String row = seat.substring(0, 1);
        double multiplier = switch (row) {
            case "A", "B" -> 1.55;
            case "C", "D", "E", "F" -> 1.20;
            default -> 1.00;
        };
        return Math.round(basePrice * multiplier);
    }

    // ------------------------------------------------------------------
    // 4. Book a ticket
    // ------------------------------------------------------------------
    private static void bookTicket() throws BookingException, SQLException {
        int showId = InputUtil.readInt("Show id: ", 1, 999999);
        Optional<Show> found = SHOW_DAO.findById(showId);
        if (found.isEmpty()) {
            System.out.println("Show not found.");
            return;
        }
        Show show = found.get();
        System.out.println(show);

        List<String> seats = InputUtil.readSeats("Seats");
        System.out.println("1. UPI   2. Credit / Debit Card   3. Net Banking");
        int methodChoice = InputUtil.readInt("Payment method: ", 1, 3);
        String method = switch (methodChoice) {
            case 1 -> "UPI";
            case 2 -> "Credit / Debit Card";
            default -> "Net Banking";
        };

        System.out.println("\n1. Book with JDBC transaction   2. Book with stored procedure");
        int style = InputUtil.readInt("Booking style: ", 1, 2);

        if (style == 2) {
            int bookingId = PROCEDURE_SERVICE.bookWithProcedure(
                    loggedInUser.getUserId(), showId, String.join(",", seats), method);
            System.out.println("Booked through sp_book_ticket. Booking id: " + bookingId);
            return;
        }

        Booking booking = BOOKING_SERVICE.bookTicket(
                loggedInUser.getUserId(), showId, seats, method);

        System.out.println("\n----- BOOKING CONFIRMED -----");
        System.out.println("Booking id : " + booking.getBookingId());
        System.out.println("Movie      : " + booking.getMovieTitle());
        System.out.println("Theatre    : " + booking.getTheatreName());
        System.out.println("When       : " + booking.getShowDate() + " " + booking.getShowTime());
        System.out.println("Seats      : " + String.join(", ", booking.getSeats()));
        System.out.printf("Subtotal   : Rs.%.2f%n", booking.getSubtotal());
        System.out.printf("Fee        : Rs.%.2f%n", booking.getConvenienceFee());
        System.out.printf("Total paid : Rs.%.2f (%s)%n", booking.getTotalAmount(), method);
    }

    // ------------------------------------------------------------------
    // 5. My bookings
    // ------------------------------------------------------------------
    private static void myBookings() {
        List<Booking> bookings = BOOKING_DAO.findByUser(loggedInUser.getUserId());
        if (bookings.isEmpty()) {
            System.out.println("You have no bookings yet.");
            return;
        }
        System.out.println("Bookings of " + loggedInUser.getFullName() + ":");
        bookings.forEach(booking -> System.out.println("  " + booking));
    }

    // ------------------------------------------------------------------
    // 6. Cancel
    // ------------------------------------------------------------------
    private static void cancelBooking() throws BookingException, SQLException {
        int bookingId = InputUtil.readInt("Booking id to cancel: ", 1, 999999);

        System.out.println("1. Cancel with JDBC   2. Cancel with stored procedure");
        int style = InputUtil.readInt("Choice: ", 1, 2);

        if (style == 2) {
            System.out.println(PROCEDURE_SERVICE.cancelWithProcedure(bookingId));
        } else {
            BOOKING_SERVICE.cancelBooking(bookingId);
            System.out.println("Booking cancelled and seats released.");
        }
    }

    // ------------------------------------------------------------------
    // 7. Review
    // ------------------------------------------------------------------
    private static void writeReview() {
        int movieId = InputUtil.readInt("Movie id: ", 1, 999);
        int rating = InputUtil.readInt("Rating (1-10): ", 1, 10);
        String headline = InputUtil.readLine("Headline: ");
        String text = InputUtil.readLine("Your review: ");
        String spoilers = InputUtil.readLine("Contains spoilers? (y/n): ");

        Review review = new Review();
        review.setMovieId(movieId);
        review.setUserId(loggedInUser.getUserId());
        review.setRating(rating);
        review.setHeadline(headline);
        review.setReviewText(text);
        review.setHasSpoilers(spoilers.equalsIgnoreCase("y"));

        List<String> tags = List.of("Acting", "Direction");
        int reviewId = REVIEW_DAO.insert(review, tags);

        System.out.println("\nReview saved with id " + reviewId + ".");
        System.out.println("The trigger trg_review_before_insert set the verdict automatically.");
        System.out.println("Current audience average for this movie: "
                + REVIEW_DAO.averageRating(movieId) + "/10");
    }

    // ------------------------------------------------------------------
    // 8. Reports
    // ------------------------------------------------------------------
    private static void reports() {
        System.out.printf("Total confirmed revenue : Rs.%.2f%n", REPORT_SERVICE.totalRevenue());

        Map<String, Long> perMovie = REPORT_SERVICE.bookingsPerMovie();
        System.out.println("\nBookings per movie:");
        perMovie.forEach((movie, count) -> System.out.println("  " + movie + " -> " + count));

        System.out.println("\nTop movies by audience rating:");
        REPORT_SERVICE.moviesByAudienceRating().stream()
                .limit(5)
                .forEach(movie -> System.out.println("  " + REPORT_SERVICE.movieSummary(movie)));

        System.out.println("\nTop 5 bookings by amount:");
        REPORT_SERVICE.bookingsByAmount().stream()
                .limit(5)
                .forEach(booking -> System.out.println("  " + booking));

        try {
            Path file = REPORT_SERVICE.exportBookingsCsv(loggedInUser.getUserId());
            System.out.println("\nYour bookings were exported to: " + file.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("CSV export failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // 10. Stored procedure reports
    // ------------------------------------------------------------------
    private static void storedProcedureReports() throws SQLException {
        System.out.println("1. Theatre revenue report by city   2. Show report by movie id");
        int option = InputUtil.readInt("Choice: ", 1, 2);

        if (option == 1) {
            String city = InputUtil.readLine("City name (Mumbai / Delhi NCR / ...): ");
            PROCEDURE_SERVICE.showTheatreReport(city);
        } else {
            int movieId = InputUtil.readInt("Movie id: ", 1, 999);
            PROCEDURE_SERVICE.showMovieReport(movieId);
        }
    }
}
