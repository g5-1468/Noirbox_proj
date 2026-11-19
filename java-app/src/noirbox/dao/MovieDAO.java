package noirbox.dao;

import noirbox.db.JdbcHelper;
import noirbox.model.Movie;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** All SQL related to the movie table. */
public class MovieDAO {

    private static final String BASE_SELECT =
            "SELECT m.movie_id, m.slug, m.title, m.language, m.certification, m.duration_min, "
          + "m.release_date, m.movie_status, m.rating, m.popularity, m.director, "
          + "m.tagline, m.description, "
          + "m.poster_url, m.backdrop_url, m.trailer_url, m.featured, "
          + "IFNULL(v.review_count, 0) AS review_count, "
          + "IFNULL(v.avg_rating, 0) AS audience_rating, "
          + "IFNULL(v.recommend_percent, 0) AS recommend_percent "
          + "FROM movie m "
          + "LEFT JOIN v_movie_rating v ON v.movie_id = m.movie_id ";

    public List<Movie> findAll() {
        List<Movie> movies = JdbcHelper.query(
                BASE_SELECT + "ORDER BY m.popularity DESC", this::mapMovie);
        movies.forEach(this::loadDetails);
        return movies;
    }

    public List<Movie> findByStatus(String status) {
        List<Movie> movies = JdbcHelper.query(
                BASE_SELECT + "WHERE m.movie_status = ? ORDER BY m.popularity DESC",
                this::mapMovie, status);
        movies.forEach(this::loadDetails);
        return movies;
    }

    public Optional<Movie> findById(int movieId) {
        List<Movie> movies = JdbcHelper.query(
                BASE_SELECT + "WHERE m.movie_id = ?", this::mapMovie, movieId);
        movies.forEach(this::loadDetails);
        return movies.stream().findFirst();
    }

    /** The website addresses a movie by its slug, e.g. "spiderman". */
    public Optional<Movie> findBySlug(String slug) {
        List<Movie> movies = JdbcHelper.query(
                BASE_SELECT + "WHERE m.slug = ?", this::mapMovie, slug);
        movies.forEach(this::loadDetails);
        return movies.stream().findFirst();
    }

    /** Case-insensitive title search, used by the "search a movie" option. */
    public List<Movie> searchByTitle(String text) {
        List<Movie> movies = JdbcHelper.query(
                BASE_SELECT + "WHERE m.title LIKE ? ORDER BY m.title",
                this::mapMovie, "%" + text + "%");
        movies.forEach(this::loadDetails);
        return movies;
    }

    /** Loads the multi-valued attributes: genres and cast. */
    private void loadDetails(Movie movie) {
        movie.getGenres().addAll(JdbcHelper.query(
                "SELECT genre FROM movie_genre WHERE movie_id = ? ORDER BY genre",
                resultSet -> resultSet.getString("genre"),
                movie.getMovieId()));

        movie.getCast().addAll(JdbcHelper.query(
                "SELECT actor_name FROM movie_cast WHERE movie_id = ? ORDER BY position",
                resultSet -> resultSet.getString("actor_name"),
                movie.getMovieId()));
    }

    /** RowMapper method reference: ResultSet -> Movie. */
    private Movie mapMovie(ResultSet resultSet) throws SQLException {
        Movie movie = new Movie();
        movie.setMovieId(resultSet.getInt("movie_id"));
        movie.setSlug(resultSet.getString("slug"));
        movie.setTitle(resultSet.getString("title"));
        movie.setLanguage(resultSet.getString("language"));
        movie.setCertification(resultSet.getString("certification"));
        movie.setDurationMin(resultSet.getInt("duration_min"));
        movie.setReleaseDate(resultSet.getString("release_date"));
        movie.setStatus(resultSet.getString("movie_status"));
        movie.setRating(resultSet.getDouble("rating"));
        movie.setPopularity(resultSet.getInt("popularity"));
        movie.setDirector(resultSet.getString("director"));
        movie.setTagline(resultSet.getString("tagline"));
        movie.setDescription(resultSet.getString("description"));
        movie.setPosterUrl(resultSet.getString("poster_url"));
        movie.setBackdropUrl(resultSet.getString("backdrop_url"));
        movie.setTrailerUrl(resultSet.getString("trailer_url"));
        movie.setFeatured(resultSet.getBoolean("featured"));
        movie.setReviewCount(resultSet.getInt("review_count"));
        movie.setAudienceRating(resultSet.getDouble("audience_rating"));
        movie.setRecommendPercent(resultSet.getInt("recommend_percent"));
        return movie;
    }
}
