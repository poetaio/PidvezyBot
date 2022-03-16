package bots;

import models.QueueTrip;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.driver_services.DriverViewUpdateService;
import services.trip_services.TripQueueService;

/**
 * Service to update current trip application viewed by driver every "@Constants.DRIVER_UPDATE_INTERVAL" seconds
 */
public abstract class DriverViewUpdateTask implements Runnable {
    protected abstract void sendNoTripsAvailable(long chatId) throws TelegramApiException;
    protected abstract void sendTripOffer(long chatId, QueueTrip passengerDao) throws TelegramApiException;

    @Override
    public void run() {
        try {
            while (true) {
                for (Long driverChatId : DriverViewUpdateService.getInstance().getDriversToUpdate()) {

                    // last trip viewed by driver
                    // todo: remove
                    QueueTrip lastPassengerDao = TripQueueService.getInstance()
                            .getPassengerDaoByDriver(driverChatId);
                    // next trip for driver to view
                    QueueTrip nextPassengerDao = TripQueueService.getInstance().getNextFree(driverChatId);

                    // if no trips were available, and no trip is found
                    // do nothing
                    if (lastPassengerDao == null && nextPassengerDao == null)
                        continue;

                    if (nextPassengerDao == null)
                        sendNoTripsAvailable(driverChatId);
                    else
                        sendTripOffer(driverChatId, nextPassengerDao);
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException | TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
