package noirbox.model;

import java.util.ArrayList;
import java.util.List;

/** One movie row from the movie table. */
public class Movie {

    private int movieId;
    private String slug;
    private String title;
    private String language;
    private String certification;
    private int durationMin;
    private String releaseDate;
    private String status;
    private double rating;
    private int popularity;
    private String director;
    private String tagline;
    private String description;
    private String posterUrl;
    private String backdropUrl;
    private String trailerUrl;
    private boolean featured;

    // audience figures, calculated by the view v_movie_rating
    private int reviewCount;
    private double audienceRating;
    private int recommendPercent;

    private final List<String> genres = new ArrayList<>();
    private final List<String> cast = new ArrayList<>();

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCertification() {
        return certification;
    }

    public void setCertification(String certification) {
        this.certification = certification;
    }

    public int getDurationMin() {
        return durationMin;
    }

    public void setDurationMin(int durationMin) {
        this.durationMin = durationMin;
    }

    public String getReleaseDate() {
        return releaseDate == null ? "" : releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getPopularity() {
        return popularity;
    }

    public void setPopularity(int popularity) {
        this.popularity = popularity;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getBackdropUrl() {
        return backdropUrl;
    }

    public void setBackdropUrl(String backdropUrl) {
        this.backdropUrl = backdropUrl;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public double getAudienceRating() {
        return audienceRating;
    }

    public void setAudienceRating(double audienceRating) {
        this.audienceRating = audienceRating;
    }

    public int getRecommendPercent() {
        return recommendPercent;
    }

    public void setRecommendPercent(int recommendPercent) {
        this.recommendPercent = recommendPercent;
    }

    public List<String> getGenres() {
        return genres;
    }

    public List<String> getCast() {
        return cast;
    }

    public String getGenresAsText() {
        return String.join(", ", genres);
    }

    @Override
    public String toString() {
        return String.format("[%d] %s (%s, %d min) rating %.1f | %s | %s",
                movieId, title, certification, durationMin, rating,
                getGenresAsText(), status);
    }
}
