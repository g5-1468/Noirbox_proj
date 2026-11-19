package noirbox.service;

import noirbox.dao.BookingDAO;
import noirbox.dao.MovieDAO;
import noirbox.dao.ReviewDAO;
import noirbox.model.Booking;
import noirbox.model.Movie;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Reporting + file export.
 * Shows Collections, Comparator, lambda expressions, Stream API,
 * try-with-resources file writing and an ExecutorService thread pool.
 */
public class ReportService {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final MovieDAO movieDAO = new MovieDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();

    /** Number of bookings per movie, sorted by count (Stream + Collectors). */
    public Map<String, Long> bookingsPerMovie() {
        return bookingDAO.findAll().stream()
                .collect(Collectors.groupingBy(
                        Booking::getMovieTitle,
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    /** Bookings sorted by amount, highest first (Comparator + lambda). */
    public List<Booking> bookingsByAmount() {
        return bookingDAO.findAll().stream()
                .sorted(Comparator.comparingDouble(Booking::getTotalAmount).reversed())
                .toList();
    }

    public double totalRevenue() {
        return bookingDAO.findAll().stream()
                .filter(booking -> "CONFIRMED".equals(booking.getStatus()))
                .mapToDouble(Booking::getTotalAmount)
                .sum();
    }

    /** Movies ordered by their real audience average rating. */
    public List<Movie> moviesByAudienceRating() {
        Map<Integer, Double> averages = new LinkedHashMap<>();
        for (Movie movie : movieDAO.findAll()) {
            averages.put(movie.getMovieId(), reviewDAO.averageRating(movie.getMovieId()));
        }
        return movieDAO.findAll().stream()
                .sorted(Comparator.comparingDouble(
                        (Movie movie) -> averages.getOrDefault(movie.getMovieId(), 0.0)).reversed())
                .toList();
    }

    /** Writes one CSV line per booking using BufferedWriter (file handling). */
    public Path exportBookingsCsv(int userId) throws IOException {
        List<Booking> bookings = bookingDAO.findByUser(userId);
        Path folder = Path.of("reports");
        Files.createDirectories(folder);
        Path file = folder.resolve("bookings_user_" + userId + ".csv");

        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("BookingId,Movie,Theatre,Date,Time,Seats,Total,Status");
            writer.newLine();
            for (Booking booking : bookings) {
                writer.write(String.join(",",
                        String.valueOf(booking.getBookingId()),
                        booking.getMovieTitle(),
                        booking.getTheatreName(),
                        String.valueOf(booking.getShowDate()),
                        String.valueOf(booking.getShowTime()),
                        String.join(" ", booking.getSeats()),
                        String.format("%.2f", booking.getTotalAmount()),
                        booking.getStatus()));
                writer.newLine();
            }
        }
        return file;
    }

    /**
     * Runs two reports at the same time using a thread pool.
     * Returns the two results in a list.
     */
    public List<String> runReportsConcurrently() throws InterruptedException, ExecutionException {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<String> revenueTask = () -> {
            Thread.sleep(300);   // pretend the report is heavy
            return String.format("Total revenue: Rs.%.2f", totalRevenue());
        };

        Callable<String> movieTask = () -> {
            Thread.sleep(300);
            Map<String, Long> counts = bookingsPerMovie();
            return "Movies with bookings: " + counts;
        };

        try {
            List<Future<String>> futures = new ArrayList<>();
            futures.add(pool.submit(revenueTask));
            futures.add(pool.submit(movieTask));

            List<String> results = new ArrayList<>();
            for (Future<String> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdown();
        }
    }

    /** Prints the audience verdict of one movie using the SQL views. */
    public String movieSummary(Movie movie) {
        double average = reviewDAO.averageRating(movie.getMovieId());
        int reviews = reviewDAO.findByMovie(movie.getMovieId()).size();
        return String.format("%-35s | %d reviews | average %.1f/10",
                movie.getTitle(), reviews, average);
    }
}
