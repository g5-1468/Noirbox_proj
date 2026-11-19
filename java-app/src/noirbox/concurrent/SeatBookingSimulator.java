package noirbox.concurrent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Small demo used in the viva to explain why seat booking must be protected.
 *
 * Three threads try to reserve seats in the same show at the same time.
 * Without synchronization two threads could both see a seat as free
 * (race condition). The synchronized method makes "check + reserve" atomic.
 * The database does the same thing with SELECT ... FOR UPDATE and the
 * UNIQUE(show_id, seat_no) constraint.
 */
public class SeatBookingSimulator {

    private final Set<String> bookedSeats =
            Collections.synchronizedSet(new HashSet<>());

    /**
     * synchronized so only one thread can check-and-book at a time.
     * @return true when the seat was free and is now booked
     */
    public synchronized boolean reserve(String seat) {
        if (bookedSeats.contains(seat)) {
            return false;
        }
        bookedSeats.add(seat);
        return true;
    }

    public static void run() {
        SeatBookingSimulator simulator = new SeatBookingSimulator();
        ExecutorService pool = Executors.newFixedThreadPool(3);

        // three users all want the same premium seat A1
        String[] users = {"Aarav", "Meghna", "Zoya"};

        System.out.println("Three users are trying to book seat A1 at the same time...");

        for (String user : users) {
            pool.submit(() -> {
                boolean booked = simulator.reserve("A1");
                if (booked) {
                    System.out.println(user + " -> BOOKED seat A1");
                } else {
                    System.out.println(user + " -> seat A1 already taken");
                }
            });
        }

        pool.shutdown();
        try {
            pool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Finally booked seats: " + simulator.bookedSeats);
        System.out.println("Only ONE booking is possible, so the seat is never sold twice.");
    }
}
