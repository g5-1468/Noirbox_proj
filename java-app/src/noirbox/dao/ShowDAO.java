package noirbox.dao;

import noirbox.db.JdbcHelper;
import noirbox.model.Show;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** All SQL related to the movie_show table. */
public class ShowDAO {

    private static final String BASE_SELECT =
            "SELECT ms.show_id, ms.movie_id, m.slug AS movie_slug, m.title AS movie_title, "
          + "m.language, ms.theatre_id, t.slug AS theatre_slug, t.theatre_name, "
          + "c.slug AS city_slug, c.city_name, ms.show_date, ms.show_time, ms.format, "
          + "ms.price, ms.seats_available "
          + "FROM movie_show ms "
          + "JOIN movie m   ON m.movie_id   = ms.movie_id "
          + "JOIN theatre t ON t.theatre_id = ms.theatre_id "
          + "JOIN city c    ON c.city_id    = t.city_id ";

    /** Shows of one movie on one date, optionally limited to a city. */
    public List<Show> findByMovieSlugAndDate(String movieSlug, LocalDate date, String citySlug) {
        if (citySlug == null || citySlug.isEmpty()) {
            return JdbcHelper.query(
                    BASE_SELECT + "WHERE m.slug = ? AND ms.show_date = ? ORDER BY ms.show_time",
                    this::mapShow, movieSlug, date);
        }
        return JdbcHelper.query(
                BASE_SELECT + "WHERE m.slug = ? AND ms.show_date = ? AND c.slug = ? "
                            + "ORDER BY ms.show_time",
                this::mapShow, movieSlug, date, citySlug);
    }

    /** All upcoming shows of one movie in a city (used for the theatre count). */
    public List<Show> findByMovieSlugAndCity(String movieSlug, String citySlug) {
        return JdbcHelper.query(
                BASE_SELECT + "WHERE m.slug = ? AND c.slug = ? AND ms.show_date >= CURDATE() "
                            + "ORDER BY ms.show_date, ms.show_time",
                this::mapShow, movieSlug, citySlug);
    }

    public List<Show> findByMovieAndDate(int movieId, LocalDate date) {
        return JdbcHelper.query(
                BASE_SELECT + "WHERE ms.movie_id = ? AND ms.show_date = ? ORDER BY ms.show_time",
                this::mapShow, movieId, date);
    }

    public List<Show> findUpcoming(int limit) {
        return JdbcHelper.query(
                BASE_SELECT + "WHERE ms.show_date >= CURDATE() "
                            + "ORDER BY ms.show_date, ms.show_time LIMIT ?",
                this::mapShow, limit);
    }

    public Optional<Show> findById(int showId) {
        List<Show> shows = JdbcHelper.query(
                BASE_SELECT + "WHERE ms.show_id = ?", this::mapShow, showId);
        return shows.stream().findFirst();
    }

    private Show mapShow(ResultSet resultSet) throws SQLException {
        Show show = new Show();
        show.setShowId(resultSet.getInt("show_id"));
        show.setMovieId(resultSet.getInt("movie_id"));
        show.setMovieSlug(resultSet.getString("movie_slug"));
        show.setMovieTitle(resultSet.getString("movie_title"));
        show.setTheatreId(resultSet.getInt("theatre_id"));
        show.setTheatreSlug(resultSet.getString("theatre_slug"));
        show.setTheatreName(resultSet.getString("theatre_name"));
        show.setCitySlug(resultSet.getString("city_slug"));
        show.setCityName(resultSet.getString("city_name"));
        show.setShowDate(resultSet.getDate("show_date").toLocalDate());
        show.setShowTime(LocalTime.parse(resultSet.getString("show_time")));
        show.setFormat(resultSet.getString("format"));
        show.setPrice(resultSet.getDouble("price"));
        show.setSeatsAvailable(resultSet.getInt("seats_available"));
        return show;
    }
}
