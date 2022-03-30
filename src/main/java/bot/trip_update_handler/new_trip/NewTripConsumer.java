package bot.trip_update_handler.new_trip;

import bot.trip_update_handler.ConcurrentTripQueue;
import lombok.Setter;

@Setter
public class NewTripConsumer implements Runnable {
    private volatile boolean runFlag = true;
    private final OnNewTrip onNewTrip;
    private final ConcurrentTripQueue tripQueue;

    public NewTripConsumer(OnNewTrip onNewTrip, ConcurrentTripQueue tripQueue) {
        this.onNewTrip = onNewTrip;
        this.tripQueue = tripQueue;
    }

    @Override
    public void run() {
        while (runFlag) {
            try {
                // sleeps while nothing to consume
                if (tripQueue.isEmpty()) {
                    tripQueue.waitOnEmpty();
                }
                onNewTrip.onNewTrip(tripQueue.remove());
                Thread.sleep(30);
                // if producer waits while queue is full, it will wake up and "produce"
                tripQueue.notifyAllForFull();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
