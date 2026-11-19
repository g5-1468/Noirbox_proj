# NOIRBOX — booking website

React + Vite front end for the cinema booking project. It is **not** a mock any more:
movies, shows, booked seats, bookings and reviews are loaded from MySQL through the
Java API in `../java-app`.

## Run

The API and the website are two processes:

```powershell
# terminal 1 - Java API + MySQL
cd ..\java-app
.\serve.bat

# terminal 2 - website
cd ..\Project-1-main
npm run dev
```

Then open the URL Vite prints (http://localhost:5173). The page banner turns red if the
API is not reachable.

`npm run build` produces a static build in `dist/`; the API URL can be overridden with
`VITE_API_URL` if the server runs somewhere else.

## Screens

- Home: hero carousel, now showing, popular, coming soon (all from the database).
- Movies: search + genre/language/rating filters.
- Movie details: cast, trailer, audience verdict, reviews (live).
- Showtimes: date strip, theatres and showtimes for the selected city.
- Seats: live seat map — seats booked in MySQL are disabled and shown as occupied.
- Checkout: demo payment, real booking transaction; a taken seat returns HTTP 409.
- Confirmation / My bookings: bookings from MySQL, with cancel (seats are released).

## Data rules (shared with the Java app and the database)

Rows A–B premium (×1.55), C–F executive (×1.20), G–J normal (×1.00); ₹28 convenience
fee per ticket; 136 seats per show. Cities and theatres come from the database, so the
navbar selection matches the stored slugs (`mumbai`, `pvr-palladium`, …).

Project documentation: `../docs/DBMS_Report.md`, `../docs/Java_Report.md`,
`../docs/Viva_QA.md`.
