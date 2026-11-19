package noirbox.dao;

import noirbox.db.JdbcHelper;
import noirbox.model.Review;
import noirbox.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** SQL for reviews, review tags and users. */
public class ReviewDAO {

    private static final String BASE_SELECT =
            "SELECT r.review_id, r.movie_id, m.slug AS movie_slug, m.title AS movie_title, "
          + "r.user_id, u.full_name, u.email AS author_email, r.rating, r.verdict, "
          + "r.headline, r.review_text, r.has_spoilers, r.likes, r.review_date, "
          + "EXISTS (SELECT 1 FROM booking b "
          + "        JOIN movie_show ms ON ms.show_id = b.show_id "
          + "        WHERE b.user_id = r.user_id AND ms.movie_id = r.movie_id "
          + "          AND b.booking_status = 'CONFIRMED') AS verified "
          + "FROM review r "
          + "JOIN movie m    ON m.movie_id = r.movie_id "
          + "JOIN app_user u ON u.user_id  = r.user_id ";

    public List<Review> findByMovieSlug(String movieSlug) {
        List<Review> reviews = JdbcHelper.query(
                BASE_SELECT + "WHERE m.slug = ? ORDER BY r.likes DESC, r.review_date DESC",
                this::mapReview, movieSlug);
        reviews.forEach(this::loadTags);
        return reviews;
    }

    public List<Review> findByMovie(int movieId) {
        List<Review> reviews = JdbcHelper.query(
                BASE_SELECT + "WHERE r.movie_id = ? ORDER BY r.likes DESC, r.review_date DESC",
                this::mapReview, movieId);
        reviews.forEach(this::loadTags);
        return reviews;
    }

    /** Average review score of a movie (0 when the movie has no reviews). */
    public double averageRating(int movieId) {
        List<Double> values = JdbcHelper.query(
                "SELECT IFNULL(ROUND(AVG(rating),1),0) FROM review WHERE movie_id = ?",
                resultSet -> resultSet.getDouble(1), movieId);
        return values.isEmpty() ? 0 : values.get(0);
    }

    /** Inserts a review and its tags. Returns the generated review id. */
    public int insert(Review review, List<String> tags) {
        int reviewId = JdbcHelper.insert(
                "INSERT INTO review (movie_id, user_id, rating, headline, review_text, has_spoilers) "
              + "VALUES (?,?,?,?,?,?)",
                review.getMovieId(), review.getUserId(), review.getRating(),
                review.getHeadline(), review.getReviewText(), review.isHasSpoilers());

        for (String tag : tags) {
            JdbcHelper.update("INSERT INTO review_tag (review_id, tag) VALUES (?, ?)",
                    reviewId, tag);
        }
        return reviewId;
    }

    public Optional<Review> findById(int reviewId) {
        List<Review> reviews = JdbcHelper.query(
                BASE_SELECT + "WHERE r.review_id = ?", this::mapReview, reviewId);
        reviews.forEach(this::loadTags);
        return reviews.stream().findFirst();
    }

    /** A user may only delete their own review. */
    public int deleteOwnReview(int reviewId, String email) {
        return JdbcHelper.update(
                "DELETE r FROM review r JOIN app_user u ON u.user_id = r.user_id "
              + "WHERE r.review_id = ? AND u.email = ?",
                reviewId, email);
    }

    public int addVote(int reviewId) {
        JdbcHelper.update("UPDATE review SET likes = likes + 1 WHERE review_id = ?", reviewId);
        List<Integer> likes = JdbcHelper.query(
                "SELECT likes FROM review WHERE review_id = ?",
                resultSet -> resultSet.getInt(1), reviewId);
        return likes.isEmpty() ? 0 : likes.get(0);
    }

    public List<User> findAllUsers() {
        return JdbcHelper.query(
                "SELECT user_id, full_name, email, phone FROM app_user ORDER BY full_name",
                this::mapUser);
    }

    public Optional<User> findByEmail(String email) {
        return JdbcHelper.query(
                "SELECT user_id, full_name, email, phone FROM app_user WHERE email = ?",
                this::mapUser, email).stream().findFirst();
    }

    /** Login by email: the user is created the first time the email is used. */
    public User findOrCreateByEmail(String email, String fullName) {
        Optional<User> existing = findByEmail(email);

        if (existing.isPresent()) {
            return existing.get();
        }

        JdbcHelper.update("INSERT INTO app_user (full_name, email) VALUES (?, ?)",
                fullName, email);

        return findByEmail(email).orElseThrow(
                () -> new IllegalStateException("User could not be created."));
    }

    private void loadTags(Review review) {
        review.getTags().addAll(JdbcHelper.query(
                "SELECT tag FROM review_tag WHERE review_id = ? ORDER BY tag",
                resultSet -> resultSet.getString("tag"),
                review.getReviewId()));
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setUserId(resultSet.getInt("user_id"));
        user.setFullName(resultSet.getString("full_name"));
        user.setEmail(resultSet.getString("email"));
        user.setPhone(resultSet.getString("phone"));
        return user;
    }

    private Review mapReview(ResultSet resultSet) throws SQLException {
        Review review = new Review();
        review.setReviewId(resultSet.getInt("review_id"));
        review.setMovieId(resultSet.getInt("movie_id"));
        review.setMovieSlug(resultSet.getString("movie_slug"));
        review.setMovieTitle(resultSet.getString("movie_title"));
        review.setUserId(resultSet.getInt("user_id"));
        review.setUserName(resultSet.getString("full_name"));
        review.setAuthorEmail(resultSet.getString("author_email"));
        review.setRating(resultSet.getInt("rating"));
        review.setVerdict(resultSet.getString("verdict"));
        review.setHeadline(resultSet.getString("headline"));
        review.setReviewText(resultSet.getString("review_text"));
        review.setHasSpoilers(resultSet.getBoolean("has_spoilers"));
        review.setLikes(resultSet.getInt("likes"));
        review.setVerifiedBooking(resultSet.getBoolean("verified"));
        review.setReviewDate(resultSet.getDate("review_date").toLocalDate());
        return review;
    }
}
