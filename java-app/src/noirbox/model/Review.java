package noirbox.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** An audience review of a movie. */
public class Review {

    private int reviewId;
    private int movieId;
    private String movieSlug;
    private String movieTitle;
    private int userId;
    private String userName;
    private String authorEmail;
    private boolean verifiedBooking;
    private final List<String> tags = new ArrayList<>();
    private int rating;
    private String verdict;
    private String headline;
    private String reviewText;
    private boolean hasSpoilers;
    private int likes;
    private LocalDate reviewDate;

    public int getReviewId() {
        return reviewId;
    }

    public void setReviewId(int reviewId) {
        this.reviewId = reviewId;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMovieSlug() {
        return movieSlug;
    }

    public void setMovieSlug(String movieSlug) {
        this.movieSlug = movieSlug;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public boolean isVerifiedBooking() {
        return verifiedBooking;
    }

    public void setVerifiedBooking(boolean verifiedBooking) {
        this.verifiedBooking = verifiedBooking;
    }

    public List<String> getTags() {
        return tags;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public boolean isHasSpoilers() {
        return hasSpoilers;
    }

    public void setHasSpoilers(boolean hasSpoilers) {
        this.hasSpoilers = hasSpoilers;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public LocalDate getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(LocalDate reviewDate) {
        this.reviewDate = reviewDate;
    }

    @Override
    public String toString() {
        return String.format("%d/10 [%s] %s - %s (%s)", rating, verdict, headline,
                userName == null ? "" : userName,
                reviewDate == null ? "" : reviewDate);
    }
}
