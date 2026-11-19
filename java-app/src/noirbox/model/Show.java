package noirbox.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** One screening: a movie in a theatre on a date and time. */
public class Show {

    private int showId;
    private int movieId;
    private String movieSlug;
    private String movieTitle;
    private int theatreId;
    private String theatreSlug;
    private String theatreName;
    private String citySlug;
    private String cityName;
    private LocalDate showDate;
    private LocalTime showTime;
    private String format;
    private double price;
    private int seatsAvailable;

    public int getShowId() {
        return showId;
    }

    public void setShowId(int showId) {
        this.showId = showId;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getMovieSlug() {
        return movieSlug;
    }

    public void setMovieSlug(String movieSlug) {
        this.movieSlug = movieSlug;
    }

    public String getTheatreSlug() {
        return theatreSlug;
    }

    public void setTheatreSlug(String theatreSlug) {
        this.theatreSlug = theatreSlug;
    }

    public String getCitySlug() {
        return citySlug;
    }

    public void setCitySlug(String citySlug) {
        this.citySlug = citySlug;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public int getTheatreId() {
        return theatreId;
    }

    public void setTheatreId(int theatreId) {
        this.theatreId = theatreId;
    }

    public String getTheatreName() {
        return theatreName;
    }

    public void setTheatreName(String theatreName) {
        this.theatreName = theatreName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public LocalDate getShowDate() {
        return showDate;
    }

    public void setShowDate(LocalDate showDate) {
        this.showDate = showDate;
    }

    public LocalTime getShowTime() {
        return showTime;
    }

    public void setShowTime(LocalTime showTime) {
        this.showTime = showTime;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getSeatsAvailable() {
        return seatsAvailable;
    }

    public void setSeatsAvailable(int seatsAvailable) {
        this.seatsAvailable = seatsAvailable;
    }

    @Override
    public String toString() {
        return String.format("Show %-3d | %-35s | %-22s | %s %s | %-4s | Rs.%.0f | %d seats left",
                showId, movieTitle, theatreName + " (" + cityName + ")",
                showDate, showTime, format, price, seatsAvailable);
    }
}
