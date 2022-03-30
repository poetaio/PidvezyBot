package bot.trip_update_handler.remove_trip;

import bot.trip_update_handler.ConcurrentTripQueue;
import models.dao.SendTripDao;

public class RemoveTripProducer {
    private final ConcurrentTripQueue removeTripQueue;

    public RemoveTripProducer(ConcurrentTripQueue removeTripQueue) {
        this.removeTripQueue = removeTripQueue;
    }

    public void produce(SendTripDao trip) {
        while (removeTripQueue.isFull()) {
            try {
                Thread.sleep(30);
                removeTripQueue.waitOnFull();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        removeTripQueue.add(trip);
        removeTripQueue.notifyAllForEmpty();
    }
}
