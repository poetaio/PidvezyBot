package bots.pidvezy_group_bot.trip_update_handler.new_trip;

import bots.pidvezy_group_bot.trip_update_handler.ConcurrentTripQueue;
import models.dao.SendTripDao;

public class NewTripProducer {
    private final ConcurrentTripQueue tripQueue;

    public NewTripProducer(ConcurrentTripQueue tripQueue) {
        this.tripQueue = tripQueue;
    }

    public void produce(SendTripDao o) {
        // while queue is full, the producer gets into the loop, and stops on full_object.wait() method
        // all producers are released by the consumer, but it can be repeatedly be caught on wait()
        while (tripQueue.isFull()) {
            try {
                Thread.sleep(30);
                tripQueue.waitOnFull();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        tripQueue.add(o);
        // tell everybody that queue is not empty anymore
        tripQueue.notifyAllForEmpty();
    }
}
