package bot.trip_update_handler.remove_trip;

import bot.trip_update_handler.ConcurrentTripQueue;

public class RemoveTripConsumer implements Runnable {
    private volatile boolean runFlag = true;
    private final OnRemoveTrip onRemoveTrip;
    private final ConcurrentTripQueue removeTripQueue;

    public RemoveTripConsumer(OnRemoveTrip onRemoveTrip, ConcurrentTripQueue removeTripQueue) {
        this.onRemoveTrip = onRemoveTrip;
        this.removeTripQueue = removeTripQueue;
    }

    @Override
    public void run() {
        while (runFlag) {
            try {
                if (removeTripQueue.isEmpty()) {
                    removeTripQueue.waitOnEmpty();
                }
                onRemoveTrip.onRemoveTrip(removeTripQueue.remove());
                Thread.sleep(30);
                removeTripQueue.notifyAllForFull();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
