package bots;

import models.dao.QueuePassengerDao;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import services.DriverUpdateService;
import services.PassengerQueueService;

/**
 * Service to update current trip application viewed by driver every N seconds
 */
public abstract class DriverMessageScheduler implements Runnable {
    protected abstract void sendNoTripsAvailable(long chatId) throws TelegramApiException;
    protected abstract void sendTripOffer(long chatId, QueuePassengerDao passengerDao) throws TelegramApiException;

    @Override
    public void run() {
        try {
            while (true) {
                for (Long driverChatId : DriverUpdateService.getInstance().getDriversToUpdate()) {
                    QueuePassengerDao lastPassengerDao = PassengerQueueService.getInstance()
                            .getPassengerDaoByDriver(driverChatId);
                    QueuePassengerDao passengerDao = PassengerQueueService.getInstance().getNextFree(driverChatId);

                    if (lastPassengerDao == null && passengerDao == null)
                        continue;

                    if (passengerDao == null)
                        sendNoTripsAvailable(driverChatId);
                    else
                        sendTripOffer(driverChatId, passengerDao);
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException | TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
