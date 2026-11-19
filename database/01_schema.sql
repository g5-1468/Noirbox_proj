-- =====================================================================
-- NOIRBOX - Online Movie Ticket Booking System
-- DBMS Project (24CSE0209) - Database Schema (DDL)
--
-- Run this file first. It creates the database, all tables, constraints,
-- indexes and views that the Java application uses.
--
-- Tested on : MySQL 8.0
-- Command   : mysql -u root -p < 01_schema.sql
-- =====================================================================

DROP DATABASE IF EXISTS noirbox_db;
CREATE DATABASE noirbox_db CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE noirbox_db;

-- ---------------------------------------------------------------------
-- 1. CITY : every theatre belongs to one city (used by the city selector)
-- ---------------------------------------------------------------------
CREATE TABLE city (
    city_id     INT PRIMARY KEY AUTO_INCREMENT,
    city_name   VARCHAR(50) NOT NULL UNIQUE,
    state_name  VARCHAR(50) NOT NULL,
    slug        VARCHAR(30) NOT NULL UNIQUE   -- id used by the website ("mumbai")
);

-- ---------------------------------------------------------------------
-- 2. THEATRE : multiplex inside a city
-- ---------------------------------------------------------------------
CREATE TABLE theatre (
    theatre_id   INT PRIMARY KEY AUTO_INCREMENT,
    theatre_name VARCHAR(100) NOT NULL,
    chain        VARCHAR(30)  NOT NULL,
    city_id      INT          NOT NULL,
    locality     VARCHAR(60)  NOT NULL,
    amenities    VARCHAR(200),
    slug         VARCHAR(40)  NOT NULL UNIQUE, -- id used by the website
    CONSTRAINT fk_theatre_city FOREIGN KEY (city_id)
        REFERENCES city (city_id)
        ON UPDATE CASCADE
);

-- ---------------------------------------------------------------------
-- 3. MOVIE : one row per film
--    Genres are stored in movie_genre (1NF: one value per column)
-- ---------------------------------------------------------------------
CREATE TABLE movie (
    movie_id      INT PRIMARY KEY AUTO_INCREMENT,
    slug          VARCHAR(40)  NOT NULL UNIQUE, -- id used by the website
    title         VARCHAR(120) NOT NULL,
    language      VARCHAR(30)  NOT NULL DEFAULT 'English',
    certification VARCHAR(15),
    duration_min  INT CHECK (duration_min BETWEEN 60 AND 300),
    release_date  DATE,
    movie_status  ENUM('now-showing','coming-soon') NOT NULL DEFAULT 'now-showing',
    rating        DECIMAL(3,1) NOT NULL DEFAULT 0 CHECK (rating BETWEEN 0 AND 10),
    popularity    INT NOT NULL DEFAULT 0,
    director      VARCHAR(100),
    tagline       VARCHAR(150),
    description   TEXT,
    poster_url    VARCHAR(300),
    backdrop_url  VARCHAR(400),
    trailer_url   VARCHAR(300),
    featured      BOOLEAN NOT NULL DEFAULT FALSE
);

-- Actors of a movie (multi-valued attribute -> separate table)
CREATE TABLE movie_cast (
    movie_id   INT          NOT NULL,
    actor_name VARCHAR(80)  NOT NULL,
    position   INT          NOT NULL,
    PRIMARY KEY (movie_id, actor_name),
    CONSTRAINT fk_cast_movie FOREIGN KEY (movie_id)
        REFERENCES movie (movie_id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- 4. MOVIE_GENRE : multi-valued attribute of MOVIE (A movie has many genres)
-- ---------------------------------------------------------------------
CREATE TABLE movie_genre (
    movie_id INT         NOT NULL,
    genre    VARCHAR(30) NOT NULL,
    PRIMARY KEY (movie_id, genre),
    CONSTRAINT fk_genre_movie FOREIGN KEY (movie_id)
        REFERENCES movie (movie_id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- 5. MOVIE_SHOW : a screening of a movie in a theatre on a date/time
-- ---------------------------------------------------------------------
CREATE TABLE movie_show (
    show_id         INT PRIMARY KEY AUTO_INCREMENT,
    movie_id        INT  NOT NULL,
    theatre_id      INT  NOT NULL,
    show_date       DATE NOT NULL,
    show_time       TIME NOT NULL,
    format          ENUM('2D','3D','IMAX','4DX','Luxe','Gold') NOT NULL DEFAULT '2D',
    price           DECIMAL(8,2) NOT NULL CHECK (price >= 100),
    seats_available INT NOT NULL DEFAULT 136,
    UNIQUE KEY uq_show_slot (movie_id, theatre_id, show_date, show_time, format),
    CONSTRAINT fk_show_movie FOREIGN KEY (movie_id)
        REFERENCES movie (movie_id),
    CONSTRAINT fk_show_theatre FOREIGN KEY (theatre_id)
        REFERENCES theatre (theatre_id)
);

-- ---------------------------------------------------------------------
-- 6. APP_USER : customer profile (the frontend "demo login")
-- ---------------------------------------------------------------------
CREATE TABLE app_user (
    user_id   INT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(80)  NOT NULL,
    email     VARCHAR(120) NOT NULL UNIQUE,
    phone     VARCHAR(15)
);

-- ---------------------------------------------------------------------
-- 7. BOOKING : one ticket purchase for one show
-- ---------------------------------------------------------------------
CREATE TABLE booking (
    booking_id      INT PRIMARY KEY AUTO_INCREMENT,
    user_id         INT NOT NULL,
    show_id         INT NOT NULL,
    booking_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    seat_count      INT NOT NULL CHECK (seat_count > 0),
    subtotal        DECIMAL(10,2) NOT NULL DEFAULT 0,
    convenience_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_amount    DECIMAL(10,2) NOT NULL DEFAULT 0,
    payment_method  VARCHAR(30) NOT NULL,
    booking_status  ENUM('CONFIRMED','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id)
        REFERENCES app_user (user_id),
    CONSTRAINT fk_booking_show FOREIGN KEY (show_id)
        REFERENCES movie_show (show_id)
);

-- ---------------------------------------------------------------------
-- 8. BOOKING_SEAT : the seats inside a booking
--    UNIQUE(show_id, seat_no) is what physically stops double booking.
-- ---------------------------------------------------------------------
CREATE TABLE booking_seat (
    booking_id INT         NOT NULL,
    show_id    INT         NOT NULL,
    seat_no    VARCHAR(5)  NOT NULL,
    price      DECIMAL(8,2) NOT NULL,
    PRIMARY KEY (booking_id, seat_no),
    UNIQUE KEY uq_seat_per_show (show_id, seat_no),
    CONSTRAINT fk_seat_booking FOREIGN KEY (booking_id)
        REFERENCES booking (booking_id) ON DELETE CASCADE,
    CONSTRAINT fk_seat_show FOREIGN KEY (show_id)
        REFERENCES movie_show (show_id)
);

-- ---------------------------------------------------------------------
-- 9. REVIEW : audience review written after watching a movie
-- ---------------------------------------------------------------------
CREATE TABLE review (
    review_id    INT PRIMARY KEY AUTO_INCREMENT,
    movie_id     INT NOT NULL,
    user_id      INT NOT NULL,
    rating       INT NOT NULL CHECK (rating BETWEEN 1 AND 10),
    verdict      ENUM('must-watch','worth-it','skip') NOT NULL DEFAULT 'worth-it',
    headline     VARCHAR(150) NOT NULL,
    review_text  TEXT,
    has_spoilers BOOLEAN NOT NULL DEFAULT FALSE,
    likes        INT NOT NULL DEFAULT 0,
    review_date  DATE NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT fk_review_movie FOREIGN KEY (movie_id)
        REFERENCES movie (movie_id) ON DELETE CASCADE,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id)
        REFERENCES app_user (user_id)
);

-- ---------------------------------------------------------------------
-- 10. REVIEW_TAG : multi-valued attribute of REVIEW ("Acting", "Music"...)
-- ---------------------------------------------------------------------
CREATE TABLE review_tag (
    review_id INT         NOT NULL,
    tag       VARCHAR(40) NOT NULL,
    PRIMARY KEY (review_id, tag),
    CONSTRAINT fk_tag_review FOREIGN KEY (review_id)
        REFERENCES review (review_id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- 11. AUDIT_LOG : written automatically by triggers (demonstrates triggers)
-- ---------------------------------------------------------------------
CREATE TABLE audit_log (
    log_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    table_name  VARCHAR(40)  NOT NULL,
    action_name VARCHAR(40)  NOT NULL,
    detail      VARCHAR(255),
    logged_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- INDEXES : added on the columns used most often in WHERE / JOIN
-- ---------------------------------------------------------------------
CREATE INDEX idx_show_movie_date ON movie_show (movie_id, show_date);
CREATE INDEX idx_show_theatre     ON movie_show (theatre_id);
CREATE INDEX idx_booking_user     ON booking (user_id);
CREATE INDEX idx_booking_show     ON booking (show_id);
CREATE INDEX idx_review_movie     ON review (movie_id);
CREATE INDEX idx_movie_status     ON movie (movie_status);

-- ---------------------------------------------------------------------
-- VIEWS
-- ---------------------------------------------------------------------

-- Every show with movie + theatre + city information (used by show listing)
CREATE VIEW v_show_details AS
SELECT ms.show_id,
       m.movie_id,
       m.title        AS movie_title,
       m.certification,
       m.duration_min,
       t.theatre_id,
       t.theatre_name,
       c.city_name,
       ms.show_date,
       ms.show_time,
       ms.format,
       ms.price,
       ms.seats_available
FROM movie_show ms
JOIN movie   m ON m.movie_id   = ms.movie_id
JOIN theatre t ON t.theatre_id = ms.theatre_id
JOIN city    c ON c.city_id    = t.city_id;

-- Rating summary per movie (the "audience verdict" block of the UI)
CREATE VIEW v_movie_rating AS
SELECT m.movie_id,
       m.title,
       COUNT(r.review_id) AS review_count,
       IFNULL(ROUND(AVG(r.rating), 1), 0) AS avg_rating,
       IFNULL(ROUND(SUM(CASE WHEN r.rating >= 7 THEN 1 ELSE 0 END) * 100
                    / NULLIF(COUNT(r.review_id), 0)), 0) AS recommend_percent
FROM movie m
LEFT JOIN review r ON r.movie_id = m.movie_id
GROUP BY m.movie_id, m.title;

-- One line per booking, used by "My Bookings" and the Java report
CREATE VIEW v_booking_summary AS
SELECT b.booking_id,
       u.full_name,
       u.email,
       m.title        AS movie_title,
       t.theatre_name,
       ms.show_date,
       ms.show_time,
       b.seat_count,
       b.total_amount,
       b.payment_method,
       b.booking_status,
       b.booking_time
FROM booking b
JOIN app_user   u  ON u.user_id    = b.user_id
JOIN movie_show ms ON ms.show_id   = b.show_id
JOIN movie      m  ON m.movie_id   = ms.movie_id
JOIN theatre    t  ON t.theatre_id = ms.theatre_id;
