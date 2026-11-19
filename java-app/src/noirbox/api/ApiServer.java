package noirbox.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import noirbox.dao.BookingDAO;
import noirbox.dao.CityDAO;
import noirbox.dao.MovieDAO;
import noirbox.dao.ReviewDAO;
import noirbox.dao.ShowDAO;
import noirbox.db.DataAccessException;
import noirbox.db.JdbcHelper;
import noirbox.exception.BookingException;
import noirbox.exception.SeatUnavailableException;
import noirbox.model.Booking;
import noirbox.model.City;
import noirbox.model.Movie;
import noirbox.model.Review;
import noirbox.model.Show;
import noirbox.model.User;
import noirbox.service.BookingService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * REST API for the NOIRBOX website.
 *
 * The React frontend calls these endpoints; every endpoint reads or writes
 * MySQL through the same DAO / service classes as the console application.
 * No framework is used - the JDK's own HttpServer is enough for a college
 * project and keeps the code easy to explain.
 *
 * Endpoints
 *   GET  /api/health
 *   GET  /api/cities
 *   GET  /api/movies?status=now-showing
 *   GET  /api/movies/{slug}
 *   GET  /api/movies/{slug}/reviews
 *   GET  /api/shows?movieId=slug&date=YYYY-MM-DD&city=mumbai
 *   GET  /api/shows/{id}
 *   GET  /api/shows/{id}/seats
 *   POST /api/bookings            email, name, showId, seats, method
 *   GET  /api/bookings?email=...
 *   GET  /api/bookings/{id}
 *   POST /api/bookings/{id}/cancel
 *   POST /api/reviews             email, name, movieId, rating, headline, content, tags
 *   POST /api/reviews/{id}/vote
 *   DELETE /api/reviews/{id}?email=...
 */
public class ApiServer {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    private final HttpServer server;
    private final MovieDAO movieDAO = new MovieDAO();
    private final ShowDAO showDAO = new ShowDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final CityDAO cityDAO = new CityDAO();
    private final BookingService bookingService = new BookingService();

    public ApiServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", this::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    // ------------------------------------------------------------------
    // Routing
    // ------------------------------------------------------------------
    private void handle(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET,POST,DELETE,OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);

        if ("OPTIONS".equals(method)) {
            send(exchange, 204, "text/plain", "");
            return;
        }

        try {
            String path = exchange.getRequestURI().getPath().substring("/api".length());
            List<String> parts = new ArrayList<>(Arrays.stream(path.split("/"))
                    .filter(part -> !part.isEmpty())
                    .toList());
            Map<String, String> query = parseForm(exchange.getRequestURI().getRawQuery());

            if (parts.isEmpty()) {
                sendError(exchange, 404, "Unknown endpoint.");
                return;
            }

            switch (parts.get(0)) {
                case "health" -> sendJson(exchange, 200,
                        "{\"status\":\"ok\",\"database\":\"noirbox_db\"}");
                case "cities" -> sendJson(exchange, 200, citiesJson());
                case "movies" -> handleMovies(exchange, method, parts, query);
                case "shows" -> handleShows(exchange, method, parts, query);
                case "bookings" -> handleBookings(exchange, method, parts, query);
                case "reviews" -> handleReviews(exchange, method, parts, query);
                default -> sendError(exchange, 404, "Unknown endpoint.");
            }

        } catch (SeatUnavailableException conflict) {
            sendError(exchange, 409, conflict.getMessage());
        } catch (BookingException bookingProblem) {
            sendError(exchange, 400, bookingProblem.getMessage());
        } catch (NumberFormatException badNumber) {
            sendError(exchange, 400, "A numeric value in the request is invalid.");
        } catch (IllegalArgumentException badRequest) {
            sendError(exchange, 400, badRequest.getMessage());
        } catch (DataAccessException databaseProblem) {
            sendError(exchange, 500, "Database error: " + databaseProblem.getMessage());
        } catch (Exception unexpected) {
            unexpected.printStackTrace();
            sendError(exchange, 500, "Server error: " + unexpected.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // /movies
    // ------------------------------------------------------------------
    private void handleMovies(HttpExchange exchange, String method,
                              List<String> parts, Map<String, String> query) throws IOException {
        if (!"GET".equals(method)) {
            sendError(exchange, 405, "Method not allowed.");
            return;
        }

        if (parts.size() == 1) {
            String status = query.get("status");
            String search = query.get("q");
            List<Movie> movies = status != null && !status.isEmpty()
                    ? movieDAO.findByStatus(status)
                    : movieDAO.findAll();

            if (search != null && !search.isEmpty()) {
                String needle = search.toLowerCase(Locale.ROOT);
                movies = movies.stream()
                        .filter(movie -> movie.getTitle().toLowerCase(Locale.ROOT).contains(needle))
                        .toList();
            }
            sendJson(exchange, 200, moviesJson(movies));
            return;
        }

        String slug = parts.get(1);

        if (parts.size() == 3 && "reviews".equals(parts.get(2))) {
            sendJson(exchange, 200, reviewsJson(reviewDAO.findByMovieSlug(slug)));
            return;
        }

        if (parts.size() == 2) {
            Optional<Movie> movie = movieDAO.findBySlug(slug);
            if (movie.isEmpty()) {
                sendError(exchange, 404, "Movie not found.");
                return;
            }
            sendJson(exchange, 200, movieJson(movie.get()));
            return;
        }

        sendError(exchange, 404, "Unknown movie endpoint.");
    }

    // ------------------------------------------------------------------
    // /shows
    // ------------------------------------------------------------------
    private void handleShows(HttpExchange exchange, String method,
                             List<String> parts, Map<String, String> query) throws IOException {
        if (!"GET".equals(method)) {
            sendError(exchange, 405, "Method not allowed.");
            return;
        }

        if (parts.size() == 1) {
            String movieSlug = query.get("movieId");
            String city = query.get("city");
            String dateText = query.get("date");

            if (movieSlug == null || movieSlug.isEmpty()) {
                sendError(exchange, 400, "movieId is required.");
                return;
            }

            List<Show> shows = dateText != null && !dateText.isEmpty()
                    ? showDAO.findByMovieSlugAndDate(movieSlug, LocalDate.parse(dateText), city)
                    : showDAO.findByMovieSlugAndCity(movieSlug, city);

            sendJson(exchange, 200, showsJson(shows));
            return;
        }

        int showId = Integer.parseInt(parts.get(1));
        Optional<Show> show = showDAO.findById(showId);

        if (show.isEmpty()) {
            sendError(exchange, 404, "Show not found.");
            return;
        }

        if (parts.size() == 2) {
            sendJson(exchange, 200, showJson(show.get()));
            return;
        }

        if (parts.size() == 3 && "seats".equals(parts.get(2))) {
            sendJson(exchange, 200, seatsJson(show.get()));
            return;
        }

        sendError(exchange, 404, "Unknown show endpoint.");
    }

    // ------------------------------------------------------------------
    // /bookings
    // ------------------------------------------------------------------
    private void handleBookings(HttpExchange exchange, String method,
                                List<String> parts, Map<String, String> query)
            throws IOException, BookingException {

        if ("POST".equals(method) && parts.size() == 1) {
            Map<String, String> form = parseForm(readBody(exchange));

            String email = required(form, "email");
            String name = form.getOrDefault("name", "Guest");
            int showId = Integer.parseInt(required(form, "showId"));
            String methodName = form.getOrDefault("method", "UPI");
            List<String> seats = Arrays.stream(required(form, "seats").split(","))
                    .map(String::trim)
                    .filter(seat -> !seat.isEmpty())
                    .map(String::toUpperCase)
                    .distinct()
                    .toList();

            User user = reviewDAO.findOrCreateByEmail(email, name);
            Booking booking = bookingService.bookTicket(
                    user.getUserId(), showId, seats, methodName);

            Booking saved = bookingDAO.findById(booking.getBookingId()).orElse(booking);
            saved.setUserName(user.getFullName());
            sendJson(exchange, 201, bookingJson(saved));
            return;
        }

        if ("POST".equals(method) && parts.size() == 3 && "cancel".equals(parts.get(2))) {
            int bookingId = Integer.parseInt(parts.get(1));
            bookingService.cancelBooking(bookingId);
            sendJson(exchange, 200, "{\"status\":\"CANCELLED\"}");
            return;
        }

        if ("GET".equals(method) && parts.size() == 1) {
            String email = query.get("email");
            if (email == null || email.isEmpty()) {
                sendError(exchange, 400, "email is required.");
                return;
            }
            List<Booking> bookings = reviewDAO.findByEmail(email)
                    .map(user -> bookingDAO.findByUser(user.getUserId()))
                    .orElse(List.of());
            sendJson(exchange, 200, bookingsJson(bookings));
            return;
        }

        if ("GET".equals(method) && parts.size() == 2) {
            int bookingId = Integer.parseInt(parts.get(1));
            Optional<Booking> booking = bookingDAO.findById(bookingId);
            if (booking.isEmpty()) {
                sendError(exchange, 404, "Booking not found.");
                return;
            }
            sendJson(exchange, 200, bookingJson(booking.get()));
            return;
        }

        sendError(exchange, 404, "Unknown booking endpoint.");
    }

    // ------------------------------------------------------------------
    // /reviews
    // ------------------------------------------------------------------
    private void handleReviews(HttpExchange exchange, String method,
                               List<String> parts, Map<String, String> query) throws IOException {

        if ("POST".equals(method) && parts.size() == 1) {
            Map<String, String> form = parseForm(readBody(exchange));

            String email = required(form, "email");
            String name = form.getOrDefault("name", "Anonymous Cinephile");
            User user = reviewDAO.findOrCreateByEmail(email, name);

            Optional<Movie> movie = movieDAO.findBySlug(required(form, "movieId"));
            if (movie.isEmpty()) {
                sendError(exchange, 404, "Movie not found.");
                return;
            }

            Review review = new Review();
            review.setMovieId(movie.get().getMovieId());
            review.setUserId(user.getUserId());
            review.setRating(Integer.parseInt(required(form, "rating")));
            review.setHeadline(required(form, "headline"));
            review.setReviewText(form.getOrDefault("content", ""));
            review.setHasSpoilers("true".equalsIgnoreCase(form.getOrDefault("hasSpoilers", "false")));

            List<String> tags = Arrays.stream(form.getOrDefault("tags", "").split(","))
                    .map(String::trim)
                    .filter(tag -> !tag.isEmpty())
                    .toList();

            int reviewId = reviewDAO.insert(review, tags);
            Review saved = reviewDAO.findById(reviewId).orElse(review);
            sendJson(exchange, 201, reviewJson(saved));
            return;
        }

        if ("POST".equals(method) && parts.size() == 3 && "vote".equals(parts.get(2))) {
            int reviewId = Integer.parseInt(parts.get(1));
            int likes = reviewDAO.addVote(reviewId);
            sendJson(exchange, 200, "{\"likes\":" + likes + "}");
            return;
        }

        if ("DELETE".equals(method) && parts.size() == 2) {
            int reviewId = Integer.parseInt(parts.get(1));
            String email = query.get("email");
            if (email == null || email.isEmpty()) {
                sendError(exchange, 400, "email is required.");
                return;
            }
            int deleted = reviewDAO.deleteOwnReview(reviewId, email);
            if (deleted == 0) {
                sendError(exchange, 403, "You can only delete your own review.");
                return;
            }
            sendJson(exchange, 200, "{\"deleted\":" + deleted + "}");
            return;
        }

        sendError(exchange, 404, "Unknown review endpoint.");
    }

    // ------------------------------------------------------------------
    // JSON serialisation
    // ------------------------------------------------------------------
    private String citiesJson() {
        List<String> items = new ArrayList<>();
        for (City city : cityDAO.findAll()) {
            items.add("{"
                    + "\"id\":" + Json.quote(city.getSlug()) + ","
                    + "\"name\":" + Json.quote(city.getName()) + ","
                    + "\"state\":" + Json.quote(city.getState())
                    + "}");
        }
        return Json.array(items);
    }

    private String moviesJson(List<Movie> movies) {
        List<String> items = new ArrayList<>();
        for (Movie movie : movies) {
            items.add(movieJson(movie));
        }
        return Json.array(items);
    }

    private String movieJson(Movie movie) {
        return "{"
                + "\"id\":" + Json.quote(movie.getSlug()) + ","
                + "\"title\":" + Json.quote(movie.getTitle()) + ","
                + "\"tagline\":" + Json.quote(movie.getTagline()) + ","
                + "\"genres\":" + Json.strings(movie.getGenres()) + ","
                + "\"language\":" + Json.quote(movie.getLanguage()) + ","
                + "\"rating\":" + Json.number(movie.getRating()) + ","
                + "\"audienceRating\":" + Json.number(movie.getAudienceRating()) + ","
                + "\"recommendPercent\":" + movie.getRecommendPercent() + ","
                + "\"reviewCount\":" + movie.getReviewCount() + ","
                + "\"popularity\":" + movie.getPopularity() + ","
                + "\"duration\":" + movie.getDurationMin() + ","
                + "\"releaseDate\":" + Json.quote(movie.getReleaseDate()) + ","
                + "\"status\":" + Json.quote(movie.getStatus()) + ","
                + "\"certification\":" + Json.quote(movie.getCertification()) + ","
                + "\"director\":" + Json.quote(movie.getDirector()) + ","
                + "\"cast\":" + Json.strings(movie.getCast()) + ","
                + "\"description\":" + Json.quote(movie.getDescription()) + ","
                + "\"poster\":" + Json.quote(movie.getPosterUrl()) + ","
                + "\"backdrop\":" + Json.quote(movie.getBackdropUrl()) + ","
                + "\"trailerUrl\":" + Json.quote(movie.getTrailerUrl()) + ","
                + "\"featured\":" + movie.isFeatured()
                + "}";
    }

    private String showsJson(List<Show> shows) {
        List<String> items = new ArrayList<>();
        for (Show show : shows) {
            items.add(showJson(show));
        }
        return Json.array(items);
    }

    private String showJson(Show show) {
        return "{"
                + "\"id\":" + Json.quote(String.valueOf(show.getShowId())) + ","
                + "\"movieId\":" + Json.quote(show.getMovieSlug()) + ","
                + "\"title\":" + Json.quote(show.getMovieTitle()) + ","
                + "\"theatreId\":" + Json.quote(show.getTheatreSlug()) + ","
                + "\"theatreName\":" + Json.quote(show.getTheatreName()) + ","
                + "\"city\":" + Json.quote(show.getCitySlug()) + ","
                + "\"date\":" + Json.quote(String.valueOf(show.getShowDate())) + ","
                + "\"time\":" + Json.quote(formatTime(show.getShowTime())) + ","
                + "\"format\":" + Json.quote(show.getFormat()) + ","
                + "\"price\":" + Json.number(show.getPrice()) + ","
                + "\"seatsAvailable\":" + show.getSeatsAvailable()
                + "}";
    }

    private String seatsJson(Show show) {
        List<String> booked = bookingService.getBookedSeats(show.getShowId());
        return "{"
                + "\"showId\":" + Json.quote(String.valueOf(show.getShowId())) + ","
                + "\"seatsAvailable\":" + show.getSeatsAvailable() + ","
                + "\"totalSeats\":136,"
                + "\"booked\":" + Json.strings(booked) + ","
                + "\"price\":{"
                + "\"premium\":" + Json.number(seatPrice(show.getShowId(), "A1")) + ","
                + "\"executive\":" + Json.number(seatPrice(show.getShowId(), "C1")) + ","
                + "\"normal\":" + Json.number(seatPrice(show.getShowId(), "G1"))
                + "}"
                + "}";
    }

    private double seatPrice(int showId, String seat) {
        List<Double> prices = JdbcHelper.query(
                "SELECT fn_seat_price(?, ?)", resultSet -> resultSet.getDouble(1), showId, seat);
        return prices.isEmpty() ? 0 : prices.get(0);
    }

    private String bookingsJson(List<Booking> bookings) {
        List<String> items = new ArrayList<>();
        for (Booking booking : bookings) {
            items.add(bookingJson(booking));
        }
        return Json.array(items);
    }

    private String bookingJson(Booking booking) {
        return "{"
                + "\"id\":" + Json.quote(String.valueOf(booking.getBookingId())) + ","
                + "\"movieId\":" + Json.quote(booking.getMovieSlug()) + ","
                + "\"title\":" + Json.quote(booking.getMovieTitle()) + ","
                + "\"theatreId\":" + Json.quote(booking.getTheatreSlug()) + ","
                + "\"theatreName\":" + Json.quote(booking.getTheatreName()) + ","
                + "\"showId\":" + Json.quote(String.valueOf(booking.getShowId())) + ","
                + "\"date\":" + Json.quote(String.valueOf(booking.getShowDate())) + ","
                + "\"time\":" + Json.quote(formatTime(booking.getShowTime())) + ","
                + "\"seats\":" + Json.strings(booking.getSeats()) + ","
                + "\"seatCount\":" + booking.getSeatCount() + ","
                + "\"subtotal\":" + Json.number(booking.getSubtotal()) + ","
                + "\"convenienceFee\":" + Json.number(booking.getConvenienceFee()) + ","
                + "\"total\":" + Json.number(booking.getTotalAmount()) + ","
                + "\"method\":" + Json.quote(booking.getPaymentMethod()) + ","
                + "\"status\":" + Json.quote(booking.getStatus()) + ","
                + "\"bookedAt\":" + Json.quote(String.valueOf(booking.getBookingTime())) + ","
                + "\"userName\":" + Json.quote(booking.getUserName())
                + "}";
    }

    private String reviewsJson(List<Review> reviews) {
        List<String> items = new ArrayList<>();
        for (Review review : reviews) {
            items.add(reviewJson(review));
        }
        return Json.array(items);
    }

    private String reviewJson(Review review) {
        return "{"
                + "\"id\":" + Json.quote("rev-" + review.getReviewId()) + ","
                + "\"reviewId\":" + review.getReviewId() + ","
                + "\"movieId\":" + Json.quote(review.getMovieSlug()) + ","
                + "\"authorName\":" + Json.quote(review.getUserName()) + ","
                + "\"authorEmail\":" + Json.quote(review.getAuthorEmail()) + ","
                + "\"rating\":" + review.getRating() + ","
                + "\"verdict\":" + Json.quote(review.getVerdict()) + ","
                + "\"headline\":" + Json.quote(review.getHeadline()) + ","
                + "\"content\":" + Json.quote(review.getReviewText()) + ","
                + "\"tags\":" + Json.strings(review.getTags()) + ","
                + "\"hasSpoilers\":" + review.isHasSpoilers() + ","
                + "\"likes\":" + review.getLikes() + ","
                + "\"date\":" + Json.quote(String.valueOf(review.getReviewDate())) + ","
                + "\"verifiedBooking\":" + review.isVerifiedBooking()
                + "}";
    }

    private String formatTime(LocalTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
    }

    // ------------------------------------------------------------------
    // HTTP helpers
    // ------------------------------------------------------------------
    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Parses both the query string and a form body (key=value&key2=value2). */
    private Map<String, String> parseForm(String raw) {
        Map<String, String> values = new HashMap<>();
        if (raw == null || raw.isEmpty()) {
            return values;
        }
        for (String pair : raw.split("&")) {
            int separator = pair.indexOf('=');
            if (separator < 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, separator), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(separator + 1), StandardCharsets.UTF_8);
            values.put(key, value);
        }
        return values;
    }

    private String required(Map<String, String> form, String key) {
        String value = form.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required.");
        }
        return value;
    }

    private void sendJson(HttpExchange exchange, int status, String body) {
        send(exchange, status, "application/json; charset=utf-8", body);
    }

    private void sendError(HttpExchange exchange, int status, String message) {
        send(exchange, status, "application/json; charset=utf-8",
                "{\"error\":" + Json.quote(message) + "}");
    }

    private void send(HttpExchange exchange, int status, String contentType, String body) {
        try {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        } catch (IOException ignored) {
            exchange.close();
        }
    }
}
