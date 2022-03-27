package bots.pidvezy_chat_bot.tasks;

import models.QueueTrip;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.trip_services.TripQueueService;

import java.util.List;

/**
 * Service to update current trip application viewed by driver every "@Constants.DRIVER_UPDATE_INTERVAL" seconds
 */
public abstract class DriverViewUpdateTask implements Runnable {
    protected abstract void sendNoTripsAvailable(long chatId) throws TelegramApiException;
    protected abstract void sendTripOffer(long chatId, QueueTrip passengerDao) throws TelegramApiException;
    protected abstract List<Long> getDriversToUpdate();

    @Override
    public void run() {
        try {
            while (true) {
                for (Long driverChatId : getDriversToUpdate()) {

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

                    if (nextPassengerDao == null) {
                        sendNoTripsAvailable(driverChatId);
                    }
                    else if (lastPassengerDao == null ||
                            (nextPassengerDao.getPassengerChatId() != lastPassengerDao.getPassengerChatId() ||
                                    !nextPassengerDao.getAddress().equals(lastPassengerDao.getAddress()) ||
                                    !nextPassengerDao.getDetails().equals(lastPassengerDao.getDetails())))
                        sendTripOffer(driverChatId, nextPassengerDao);
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException | TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
